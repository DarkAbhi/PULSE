"""Run module suites in parallel and write a standalone HTML test report."""

import concurrent.futures
from datetime import datetime
import html
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
REPORT = ROOT / "test-results"
MODULES = {
    "go": ("Go backend", "go-backend", ["go", "test", "-json", "-race", "-count=1", "-shuffle=on", "-tags=integration", "-timeout=20m", "./..."]),
    "rust": ("Rust backend", "rust-backend", ["cargo", "test", "--locked", "--workspace", "--all-features", "--", "--test-threads=1"]),
    "frontend": ("Frontend", "life-tracker-frontend", ["node", "--test", "--test-reporter=junit"]),
    "mcp": ("Hermes MCP", ".", [sys.executable, __file__, "--python", "hermes-mcp", "test_*.py", "mcp"]),
    "scripts": ("Seed and test tooling", ".", [sys.executable, __file__, "--python", "scripts", "test_*.py", "scripts"]),
}


def test_result(name, status, duration=0, details=""):
    return {"name": name, "status": status, "duration": duration, "details": details}


def parse_go(output):
    records, logs = [], {}
    for line in output.splitlines():
        try:
            event = json.loads(line)
        except json.JSONDecodeError:
            continue
        key = (event.get("Package", ""), event.get("Test", ""))
        if event.get("Output"):
            logs.setdefault(key, []).append(event["Output"])
        if key[1] and event["Action"] in ("pass", "fail", "skip"):
            records.append(test_result(" / ".join(key), event["Action"],
                                       event.get("Elapsed", 0), "".join(logs.get(key, []))))
        elif not key[1] and event["Action"] == "fail":
            if not any(item["status"] == "fail" and item["name"].startswith(key[0] + " / ")
                       for item in records):
                records.append(test_result(key[0], "error", event.get("Elapsed", 0),
                                           "".join(logs.get(key, []))))
    return records


def parse_node(output):
    records = []
    for case in ET.fromstring(output).iter("testcase"):
        status, details = "pass", []
        for tag, result in [("skipped", "skip"), ("failure", "fail"), ("error", "error")]:
            for element in case.findall(tag):
                status = result
                details.append(element.get("message", "") + "\n" + "".join(element.itertext()))
        records.append(test_result(case.get("name", "Unnamed test"), status,
                                   float(case.get("time", 0)), "\n".join(details)))
    return records


class TimedResult(unittest.TextTestResult):
    def startTest(self, test):
        super().startTest(test)
        self.started = time.perf_counter()
        self.current = test_result(test.id(), "pass")
        self.records.append(self.current)

    def stopTest(self, test):
        self.current["duration"] = time.perf_counter() - self.started
        super().stopTest(test)

    def addFailure(self, test, err):
        self.current.update(status="fail", details=self._exc_info_to_string(err, test))
        super().addFailure(test, err)

    def addError(self, test, err):
        self.current.update(status="error", details=self._exc_info_to_string(err, test))
        super().addError(test, err)

    def addSkip(self, test, reason):
        self.current.update(status="skip", details=reason)
        super().addSkip(test, reason)

    def addExpectedFailure(self, test, err):
        self.current.update(status="skip", details="Expected failure\n" + self._exc_info_to_string(err, test))
        super().addExpectedFailure(test, err)

    def addUnexpectedSuccess(self, test):
        self.current.update(status="fail", details="Unexpected success of an expected-failure test")
        super().addUnexpectedSuccess(test)

    def addSubTest(self, test, subtest, err):
        if err is not None:
            self.current["status"] = "fail" if issubclass(err[0], test.failureException) else "error"
            self.current["details"] += str(subtest) + "\n" + self._exc_info_to_string(err, test)
        super().addSubTest(test, subtest, err)

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.records = []


def run_python(directory, pattern, module):
    suite = unittest.defaultTestLoader.discover(str(ROOT / directory), pattern=pattern)
    result = unittest.TextTestRunner(verbosity=2, resultclass=TimedResult, buffer=True).run(suite)
    (REPORT / f"{module}.json").write_text(json.dumps(result.records, indent=2))
    return 0 if result.wasSuccessful() else 1


# ponytail: time serial libtest output until stable Rust supports structured timings.
class RustTiming:
    """Time serial libtest output; includes database startup and cleanup."""
    def __init__(self):
        self.line = ""
        self.started = None
        self.records = []

    def feed(self, character):
        self.line += character
        if self.started is None and re.fullmatch(r"test (\S+)(?: - should panic)? \.\.\. ", self.line):
            self.started = time.perf_counter()
        if character == "\n":
            match = re.fullmatch(r"test (\S+)(?: - should panic)? \.\.\. (ok|FAILED|ignored)(?:, .*|)\n", self.line)
            if match:
                status = {"ok": "pass", "FAILED": "fail", "ignored": "skip"}[match[2]]
                elapsed = time.perf_counter() - self.started if self.started is not None else 0
                self.records.append(test_result(match[1], status, elapsed))
            self.line, self.started = "", None


def run_module(module):
    title, directory, command = MODULES[module]
    started = time.perf_counter()
    output, records, code = "", [], 1
    print(f"Starting {title}", flush=True)
    try:
        if module in ("mcp", "scripts"):
            (REPORT / f"{module}.json").unlink(missing_ok=True)
        environment = dict(os.environ, RUST_BACKTRACE="1", CARGO_TERM_COLOR="never")
        if module == "rust":
            timing = RustTiming()
            with subprocess.Popen(command, cwd=ROOT / directory, env=environment,
                                  stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True) as process:
                chunks = []
                while character := process.stdout.read(1):
                    chunks.append(character)
                    timing.feed(character)
                code = process.wait()
            output, records = "".join(chunks), timing.records
            for record in records:
                if record["status"] == "fail":
                    record["details"] = output
        else:
            process = subprocess.run(command, cwd=ROOT / directory, env=environment,
                                     capture_output=True, text=True)
            code, output = process.returncode, process.stdout + process.stderr
            if module == "go":
                records = parse_go(output)
            elif module == "frontend":
                records = parse_node(process.stdout)
            else:
                records = json.loads((REPORT / f"{module}.json").read_text())
        if not records or (code and not any(item["status"] in ("fail", "error") for item in records)):
            records.append(test_result("Suite execution", "error", details=output or "No tests reported"))
            code = code or 1
    except Exception as error:
        records.append(test_result("Suite execution", "error", details=str(error) + "\n" + output))
        code = 1
    (REPORT / f"{module}.log").write_text(output)
    print(f"Finished {title}: {len(records)} results, exit {code}", flush=True)
    return {"module": module, "title": title, "duration": time.perf_counter() - started,
            "exit_code": code, "tests": records}


def counts(tests):
    return {status: sum(test["status"] == status for test in tests)
            for status in ("pass", "fail", "error", "skip")}


def render_report(results, elapsed):
    escape = html.escape
    all_tests = [test for module in results for test in module["tests"]]
    total = counts(all_tests)
    parts = [f'''<!doctype html><html lang="en"><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1"><title>PULSE test report</title>
<style>
body{{font:16px system-ui,sans-serif;margin:32px auto;padding:0 20px;max-width:1200px;color:#172033;background:#f8fafc}}
h1,h2{{margin-bottom:8px}} .summary{{display:flex;gap:20px;flex-wrap:wrap;margin:24px 0}}
section{{background:white;border:1px solid #dce3ed;border-radius:12px;padding:20px;margin:24px 0}}
table{{border-collapse:collapse;width:100%;font-size:14px;table-layout:fixed}} th,td{{text-align:left;padding:12px;border-bottom:1px solid #e8edf3;vertical-align:top}}
td{{overflow-wrap:anywhere}} th:nth-child(2),th:nth-child(3){{width:70px}}
.pass{{color:#166534}} .fail,.error{{color:#b91c1c}} .skip{{color:#854d0e}}
pre{{white-space:pre-wrap;overflow-wrap:anywhere;background:#f1f5f9;padding:12px;font-size:12px;max-height:480px;overflow:auto}}
input,select{{font:inherit;padding:8px;margin:0 12px 12px 0;max-width:100%}} a{{color:#1d4ed8}} small{{color:#475569}}
</style><h1>PULSE test report</h1>
<p>{escape(datetime.now().astimezone().isoformat(timespec="seconds"))} · Wall time {elapsed:.2f}s</p>
<div class="summary"><b>{len(all_tests)} results</b><b class="pass">{total['pass']} passed</b>
<b class="fail">{total['fail']} failed</b><b class="error">{total['error']} errors</b><b class="skip">{total['skip']} skipped</b></div>
<p><small>Go results include parent tests and subtests. Rust cases run serially to measure elapsed time from live test output;
modules run in parallel. Timings include test setup/cleanup. Browser flows and production builds are separate.</small></p>
<label for="search">Find test </label><input id="search" type="search" placeholder="Test name or module">
<label for="status">Status </label><select id="status"><option value="">All</option>
<option value="pass">Passed</option><option value="fail">Failed</option><option value="error">Errors</option><option value="skip">Skipped</option></select>''']
    for module in results:
        summary = counts(module["tests"])
        parts.append(f'''<section><h2>{escape(module['title'])}</h2><p>{module['duration']:.2f}s ·
{summary['pass']} passed · {summary['fail']} failed · {summary['error']} errors · {summary['skip']} skipped ·
<a href="{module['module']}.log">Full runner log</a></p><table><thead><tr><th>Test</th><th>Status</th><th>Time</th></tr></thead><tbody>''')
        for test in module["tests"]:
            details = ""
            if test["details"]:
                details = f"<details><summary>Details / output</summary><pre>{escape(test['details'])}</pre></details>"
            parts.append(f'''<tr data-status="{test['status']}" data-search="{escape(module['title'] + ' ' + test['name'], quote=True)}">
<td>{escape(test['name'])}{details}</td><td class="{test['status']}">{test['status'].upper()}</td><td>{test['duration']:.3f}s</td></tr>''')
        parts.append("</tbody></table></section>")
    parts.append('''<script>
const search = document.getElementById('search'), status = document.getElementById('status');
function filter() { for (const row of document.querySelectorAll('tr[data-status]')) {
row.hidden = (status.value && row.dataset.status !== status.value) || !row.dataset.search.toLowerCase().includes(search.value.toLowerCase());
} } search.addEventListener('input', filter); status.addEventListener('change', filter);
</script></html>''')
    return "".join(parts)


def main():
    REPORT.mkdir(exist_ok=True)
    if len(sys.argv) > 1 and sys.argv[1] == "--python":
        return run_python(*sys.argv[2:])
    started = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(MODULES)) as executor:
        results = list(executor.map(run_module, MODULES))
    (REPORT / "results.json").write_text(json.dumps(results, indent=2))
    (REPORT / "index.html").write_text(render_report(results, time.perf_counter() - started))
    print(f"Report: {REPORT / 'index.html'}")
    return int(any(module["exit_code"] for module in results))


if __name__ == "__main__":
    sys.exit(main())
