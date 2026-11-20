"""Allow one fresh Maestro session only when every failed case lost its device."""

from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET


TRANSPORT_ERROR = re.compile(
    r"^(?:(?:[\w.$]+\.)?(?:DeviceServerDiedException|DeviceUnreachableException):"
    r"|(?:(?:[\w.$]+\.)?(?:IOException|CommandFailed): )?"
    r"Command failed \(host:transport:[^)]+\): device (?:offline|'[^']+' not found))"
)


def is_transport_failure(report: Path) -> bool:
    try:
        root = ET.parse(report).getroot()
    except (OSError, ET.ParseError):
        return False
    cases = list(root.iter("testcase"))
    failed = []
    for case in cases:
        errors = list(case.findall("failure")) + list(case.findall("error"))
        if errors:
            failed.extend(errors)
        elif case.get("status", "").upper() in {"ERROR", "FAILED", "FAILURE"}:
            return False  # An unexplained failure cannot authorize recovery.
    if not cases or not failed:
        return False
    for failure in failed:
        text = (failure.text or failure.get("message", "")).strip()
        if not TRANSPORT_ERROR.match(text):
            return False
    return True


if __name__ == "__main__":
    sys.exit(0 if len(sys.argv) == 2 and is_transport_failure(Path(sys.argv[1])) else 1)
