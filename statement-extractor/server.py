"""Private HTTP transport and bounded lifetime for the extraction worker."""

from datetime import datetime, timezone
from email.parser import BytesParser
from email.policy import default
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path
import re
import signal
import subprocess
import sys
import tempfile
import threading
import time

MAX_PDF = 10 << 20
MAX_REQUEST = MAX_PDF + (128 << 10)
MAX_RESPONSE = 32 << 20
TIMEOUT = 120
WORKER = Path(__file__).with_name("extract.py")
WORKERS = threading.BoundedSemaphore(1)
BUCKETS = (0.1, 0.5, 1, 5, 15, 30, 60, 120)
STATS = {"requests": 0, "failures": 0, "inflight": 0, "duration": 0.0, "buckets": [0] * len(BUCKETS)}
LOCK = threading.Lock()
SAFE_ID = re.compile(r"[\w./-]{1,128}", re.ASCII)


def multipart_pdf(content_type, raw):
    if not content_type.startswith("multipart/form-data;") or any(c in content_type for c in "\r\n"):
        return None
    message = BytesParser(policy=default).parsebytes(
        b"Content-Type: " + content_type.encode("ascii") + b"\r\n\r\n" + raw
    )
    if not message.is_multipart() or message.defects:
        return None
    parts = list(message.iter_parts())
    if len(parts) != 1 or parts[0].get_param("name", header="content-disposition") != "file":
        return None
    return parts[0].get_payload(decode=True)


def failure(code):
    return json.dumps({"code": code}, separators=(",", ":")).encode()


def process(pdf):
    with tempfile.TemporaryDirectory(prefix="statement-") as root:
        folder = Path(root)
        source, ocr, result = (folder / name for name in ("source.pdf", "ocr.pdf", "result.json"))
        source.write_bytes(pdf)
        env = dict(os.environ, TMPDIR=root, OMP_THREAD_LIMIT="1", HOME=root)
        with subprocess.Popen(
            [sys.executable, str(WORKER), str(source), str(ocr), str(result)],
            stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
            start_new_session=True, env=env,
        ) as worker:
            try:
                return_code = worker.wait(timeout=TIMEOUT)
            except subprocess.TimeoutExpired:
                try:
                    os.killpg(worker.pid, signal.SIGKILL)
                except ProcessLookupError:
                    pass
                worker.wait()
                return 504, failure("EXTRACTION_TIMEOUT")
        if return_code != 0 or not result.is_file() or result.stat().st_size > MAX_RESPONSE:
            return 502, failure("EXTRACTION_FAILED")
        output = result.read_bytes()
        if output.startswith(b'{"code"'):
            return 422, output
        return 200, output


def record(duration, status):
    with LOCK:
        STATS["requests"] += 1
        STATS["failures"] += status >= 400
        STATS["duration"] += duration
        for i, bound in enumerate(BUCKETS):
            STATS["buckets"][i] += duration <= bound


def render_metrics():
    p = "pulse_statement_extractor"
    with LOCK:
        data = [
            f"# TYPE {p}_requests_total counter",
            f"{p}_requests_total {STATS['requests']}",
            f"# TYPE {p}_failures_total counter",
            f"{p}_failures_total {STATS['failures']}",
            f"# TYPE {p}_inflight gauge",
            f"{p}_inflight {STATS['inflight']}",
            f"# TYPE {p}_request_duration_seconds histogram",
        ]
        for bound, count in zip(BUCKETS, STATS["buckets"]):
            data.append(f'{p}_request_duration_seconds_bucket{{le="{bound}"}} {count}')
        data.extend([
            f'{p}_request_duration_seconds_bucket{{le="+Inf"}} {STATS["requests"]}',
            f"{p}_request_duration_seconds_sum {STATS['duration']}",
            f"{p}_request_duration_seconds_count {STATS['requests']}",
        ])
    return ("\n".join(data) + "\n").encode()


class Handler(BaseHTTPRequestHandler):
    def setup(self):
        super().setup()
        self.connection.settimeout(15)

    def log_message(self, *_):
        pass

    def send(self, status, payload, content_type="application/json"):
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(payload)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(payload)

    def do_GET(self):
        if self.path == "/healthz":
            self.send(200, b'{"status":"ok"}')
        elif self.path == "/metrics":
            self.send(200, render_metrics(), "text/plain; version=0.0.4; charset=utf-8")
        else:
            self.send(404, failure("NOT_FOUND"))

    def do_POST(self):
        if self.path != "/extract":
            self.send(404, failure("NOT_FOUND"))
            return
        start = time.monotonic()
        try:
            status, payload = self.extract_request()
        except (ValueError, UnicodeError, TimeoutError):
            status, payload = 400, failure("INVALID_REQUEST")
        except Exception:
            status, payload = 502, failure("EXTRACTION_FAILED")
        duration = time.monotonic() - start
        record(duration, status)
        request_id = self.headers.get("X-Request-Id", "")
        print(json.dumps({
            "time": datetime.now(timezone.utc).isoformat(),
            "level": "INFO" if status < 500 else "ERROR",
            "msg": "statement extraction finished",
            "status": status,
            "duration_ms": round(duration * 1000),
            "request_id": request_id if SAFE_ID.fullmatch(request_id) else "",
        }))
        self.send(status, payload)

    def extract_request(self):
        size = int(self.headers.get("Content-Length", "0"))
        if size > MAX_REQUEST:
            return 413, failure("FILE_TOO_LARGE")
        if size <= 0:
            return 400, failure("INVALID_REQUEST")
        if not WORKERS.acquire(blocking=False):
            return 429, failure("EXTRACTOR_BUSY")
        try:
            with LOCK:
                STATS["inflight"] += 1
            raw = self.rfile.read(size)
            pdf = multipart_pdf(self.headers.get("Content-Type", ""), raw) if len(raw) == size else None
            if pdf is None:
                return 400, failure("INVALID_REQUEST")
            if len(pdf) > MAX_PDF:
                return 413, failure("FILE_TOO_LARGE")
            if not pdf.startswith(b"%PDF-"):
                return 415, failure("NOT_PDF")
            return process(pdf)
        finally:
            with LOCK:
                STATS["inflight"] -= 1
            WORKERS.release()


class Server(ThreadingHTTPServer):
    def handle_error(self, *_):
        # Keep request tracebacks out of container logs.
        pass


if __name__ == "__main__":
    Server(("0.0.0.0", int(os.getenv("PORT", "8090"))), Handler).serve_forever()
