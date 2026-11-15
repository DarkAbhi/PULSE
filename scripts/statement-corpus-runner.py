"""Run a redacted corpus acceptance test against an isolated Pulse API."""

from http.cookiejar import CookieJar
import json
import os
from pathlib import Path
import sys
import time
import urllib.error
import urllib.request
import uuid

BASE = os.environ["STATEMENT_API_BASE"]
HTTP = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(CookieJar()))
MAX_PDF = 10 << 20
ALLOWED_REJECTIONS = {
    "UNSUPPORTED_BANK", "RECONCILIATION_FAILED", "BALANCES_UNAVAILABLE",
    "ENCRYPTED_PDF_UNSUPPORTED", "INVALID_ROW", "CURRENCY_MISMATCH", "NOT_PDF",
    "FILE_TOO_LARGE", "ACCOUNT_MISMATCH", "OCR_FAILED", "INVALID_PDF", "PDF_LIMIT_EXCEEDED",
}
EXPECTED_SAMPLES = {
    "amazon-icici-credit-card": ("ICICI Bank", "credit_card", "icici_amazon_credit_v1", 5),
    "axis-my-zone-credit-card": ("Axis Bank", "credit_card", "axis_my_zone_credit_v1", 2),
    "federal-bank-account": ("Federal Bank", "bank_account", "federal_savings_v1", 8),
    "hdfc-millenia-credit-card": ("HDFC Bank", "credit_card", "hdfc_millennia_credit_v1", 23),
    "hdfc-swiggy-credit-card": ("HDFC Bank", "credit_card", "hdfc_swiggy_credit_v1", 13),
    "idfc-bank-account": ("IDFC FIRST Bank", "bank_account", "idfc_first_savings_v1", 10),
    "idfc-credit-card": ("IDFC FIRST Bank", "credit_card", "idfc_first_credit_v1", 2),
    "kotak-bank-account": ("Kotak", "bank_account", "kotak_savings_v1", 22),
    "sbi-bank-account": ("State Bank of India", "bank_account", "sbi_savings_v1", 19),
}


class RunnerError(Exception):
    pass


def request(path, method="GET", body=None, content_type="application/json"):
    if isinstance(body, dict):
        body = json.dumps(body).encode()
    req = urllib.request.Request(BASE + "/api/" + path, data=body, method=method,
                                 headers={"Content-Type": content_type})
    try:
        with HTTP.open(req, timeout=185) as res:
            raw = res.read()
            return res.status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as exc:
        try:
            return exc.code, json.loads(exc.read())
        except ValueError:
            return exc.code, {}


def expect(status, response, stage):
    code, data = response
    if code != status:
        raise RunnerError(f"{stage} returned HTTP {code}")
    return data


def upload(path, account):
    if path.stat().st_size > MAX_PDF:
        return 413, {"code": "FILE_TOO_LARGE"}
    boundary = uuid.uuid4().hex
    data = (
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"account_id\"\r\n\r\n{account}\r\n"
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"statement.pdf\"\r\n"
        "Content-Type: application/pdf\r\n\r\n"
    ).encode() + path.read_bytes() + f"\r\n--{boundary}--\r\n".encode()
    return request("horizon/statement-imports", "POST", data, f"multipart/form-data; boundary={boundary}")


def verify_sample(path, index, account, expected_profile, expected_rows):
    target = account
    start = time.monotonic()
    before = expect(200, request("horizon/transactions?page_size=1"), "read transactions")["total"]
    code, batch = upload(path, target)
    if code != 201:
        reason = batch.get("code")
        if expected_profile or reason not in ALLOWED_REJECTIONS:
            safe = reason if reason in ALLOWED_REJECTIONS else f"HTTP_{code}"
            raise RunnerError(f"sample {index}: unexpected rejection {safe}")
        summary = {}
        if batch.get("batch_id"):
            summary = expect(200, request("horizon/statement-imports/" + batch["batch_id"]), "read rejection")
        after = expect(200, request("horizon/transactions?page_size=1"), "verify rejected transactions")["total"]
        if before != after:
            raise RunnerError(f"sample {index}: rejected import wrote transactions")
        profile = summary.get("profile_id", "") or "none"
        rows = summary.get("counts", {}).get("parsed", 0)
        print(f"sample {index}: profile={profile} rows={rows} reconciliation=failed duplicates=0 reason={reason} seconds={time.monotonic() - start:.2f}")
        return False

    if batch["reconciliation"]["status"] != "passed":
        raise RunnerError(f"sample {index}: ready preview lacks reconciliation")
    if expected_profile and (batch["profile_id"] != expected_profile or batch["counts"]["parsed"] != expected_rows):
        raise RunnerError(f"sample {index}: unexpected profile or transaction count")
    preview = "horizon/statement-imports/" + batch["id"]
    if batch["account_confirmation_required"]:
        batch = expect(200, request(preview + "/rows", "PATCH", {
            "revision": batch["revision"], "account_confirmed": True, "changes": [],
        }), "confirm destination account")
    revision = {"revision": batch["revision"]}
    committed = expect(200, request(preview + "/commit", "POST", revision), "commit")
    if expected_profile and committed["imported"] != expected_rows:
        raise RunnerError(f"sample {index}: expected every checked row to be imported")
    repeated = expect(200, request(preview + "/commit", "POST", revision), "retry commit")
    if committed != repeated:
        raise RunnerError(f"sample {index}: commit retry returned a different result")
    replay_status, replay = upload(path, target)
    if replay_status != 409 or replay.get("code") != "ALREADY_IMPORTED":
        raise RunnerError(f"sample {index}: same-file replay was not detected")
    after = expect(200, request("horizon/transactions?page_size=1"), "verify committed transactions")["total"]
    if after - before != committed["imported"]:
        raise RunnerError(f"sample {index}: incorrect number of committed transactions")
    print(f"sample {index}: profile={batch['profile_id']} rows={batch['counts']['parsed']} reconciliation=passed "
          f"duplicates={committed['duplicates_skipped']} repeat=blocked seconds={time.monotonic() - start:.2f}")
    return True


def main():
    files = sorted(path for path in Path("/corpus").iterdir() if path.is_file() and path.suffix.lower() == ".pdf")
    if not files:
        raise RunnerError("No PDF samples were found.")
    expect(200, request("auth/login", "POST", {
        "username": "statement-test-user", "password": os.environ["STATEMENT_TEST_PASSWORD"],
    }), "login")
    expect(200, request("profile/currency", "PUT", {"currency": "INR"}), "set currency")
    successes = 0
    for index, path in enumerate(files, 1):
        institution, account_type, profile, count = EXPECTED_SAMPLES.get(
            path.stem.casefold(), ("Test institution", "bank_account", "", 0),
        )
        payload = {
            "display_name": f"Corpus test account {index}", "institution_name": institution,
            "account_type": account_type, "currency": "INR",
        }
        if profile and account_type == "bank_account":
            payload["bank_account_subtype"] = "savings"
        saved = expect(201, request("horizon/accounts", "POST", payload), "create test account")
        successes += verify_sample(path, index, saved["id"], profile, count)
    if not successes:
        raise RunnerError("No statement completed the acceptance path.")
    print(f"Corpus result: {successes} reconciled and committed, {len(files) - successes} rejected with a reason.")


if __name__ == "__main__":
    try:
        main()
    except RunnerError as error:
        print(str(error), file=sys.stderr)
        raise SystemExit(1)
    except Exception:
        print("The corpus runner encountered an unexpected error.", file=sys.stderr)
        raise SystemExit(1)
