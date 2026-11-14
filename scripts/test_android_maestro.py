"""Run the actual workflow commands with the emulator action's per-line shells."""

import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET

from maestro_transport_failure import is_transport_failure


ROOT = Path(__file__).resolve().parent.parent


class TransportClassificationTest(unittest.TestCase):
    def classify(self, *messages):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / "junit.xml"
            root = ET.Element("testsuites")
            suite = ET.SubElement(root, "testsuite")
            for message in messages:
                case = ET.SubElement(suite, "testcase")
                ET.SubElement(case, "failure").text = message
            ET.ElementTree(root).write(report)
            return is_transport_failure(report)

    def test_connection_error_signatures_are_recoverable(self):
        for message in (
            "Command failed (host:transport:emulator-5556): device offline",
            "java.io.IOException: Command failed (host:transport:emulator-5556): device offline",
            "maestro.android.DeviceServerDiedException: Device server died during 'deviceInfo'",
            "maestro.DeviceUnreachableException: Device is unreachable",
        ):
            with self.subTest(message=message):
                self.assertTrue(self.classify(message))

    def test_assertion_failure_is_never_recoverable_even_with_a_disconnect(self):
        assertion = "java.lang.AssertionError: Expected text 'device offline' to be visible"
        self.assertFalse(self.classify(assertion))
        self.assertFalse(self.classify("maestro.DeviceUnreachableException: Device is unreachable", assertion))

    def test_unknown_or_missing_results_do_not_authorize_recovery(self):
        self.assertFalse(self.classify("Unknown error"))
        self.assertFalse(self.classify())
        self.assertFalse(is_transport_failure(ROOT / "nonexistent-maestro-result.xml"))


def maestro_commands():
    workflow = (ROOT / ".github/workflows/tests.yml").read_text()
    step = workflow.split("      - name: Run Maestro smoke tests on a fresh emulator\n", 1)[1]
    step = step.split("      - name:", 1)[0]
    script = step.split("          script: |\n", 1)[1]
    return [line.strip() for line in script.splitlines() if line.strip() and not line.strip().startswith("#")]


@unittest.skipUnless(shutil.which("bash"), "Bash is required")
class MaestroWorkflowTest(unittest.TestCase):
    def run_workflow(self, maestro_status=0, diagnostic_failure=False, transport_failure="none", missing_report=False):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            app = root / "pulse-android"
            app.mkdir()
            scripts = root / "scripts"
            scripts.mkdir()
            for name in (
                "run-android-maestro.sh", "prepare-android-emulator.sh", "wait-android-device.sh", "maestro_transport_failure.py"
            ):
                shutil.copyfile(ROOT / "scripts" / name, scripts / name)
            fake_bin = app / "bin"
            fake_bin.mkdir()
            if missing_report:
                stale = app / "build/reports/maestro/attempt-1"
                stale.mkdir(parents=True)
                stale_xml = "<testsuites><testcase><failure>maestro.DeviceUnreachableException: stale</failure></testcase></testsuites>"
                (stale / "junit.xml").write_text(stale_xml)
                (stale.parent / "junit.xml").write_text(stale_xml)
            stubs = {
                "adb": """#!/usr/bin/env bash
echo "adb $*" >> calls.txt
if [[ "$*" == get-state ]]; then echo device; fi
if [[ "$*" == 'shell getprop sys.boot_completed' ]]; then echo 1; fi
if [[ "$*" == 'shell echo pulse-maestro-ready' ]]; then echo pulse-maestro-ready; fi
if [[ "$*" == 'shell dumpsys window' ]]; then
  echo 'mCurrentFocus=Window{abc u0 dev.abhishekan.pulse/.MainActivity}'
fi
if [[ -f maestro-started && "$TEST_DIAGNOSTIC_FAILURE" == 1 ]]; then exit 9; fi
""",
                "timeout": """#!/usr/bin/env bash
while [[ "$1" == --* ]]; do shift; done
shift
exec "$@"
""",
                "maestro": """#!/usr/bin/env bash
touch maestro-started
echo "maestro $*" >> calls.txt
echo "JAVA_HOME=$JAVA_HOME" >> calls.txt
while (( $# )); do
  case "$1" in
    --output) output=$2; shift ;;
    --debug-output) debug=$2; shift ;;
  esac
  shift
done
mkdir -p "$(dirname "$output")" "$debug"
echo 'Maestro diagnostic log' > "$debug/maestro.log"
if [[ "$TEST_MISSING_REPORT" == 1 ]]; then exit 7; fi
result=$TEST_MAESTRO_STATUS
failure='java.lang.AssertionError: Expected Welcome back to be visible'
count=$(grep -c '^maestro ' calls.txt)
if [[ "$TEST_TRANSPORT_FAILURE" == persistent || ( "$TEST_TRANSPORT_FAILURE" == once && "$count" == 1 ) ]]; then
  result=7
  failure='Command failed (host:transport:emulator-5556): device offline'
fi
if (( result )); then
  echo "<testsuites><testsuite><testcase><failure>$failure</failure></testcase></testsuite></testsuites>" > "$output"
else
  echo '<testsuites><testsuite><testcase status="SUCCESS"/></testsuite></testsuites>' > "$output"
fi
exit "$result"
""",
                "sleep": "#!/usr/bin/env bash\nexit 0\n",
            }
            for name, content in stubs.items():
                path = fake_bin / name
                path.write_text(content, encoding="utf-8", newline="\n")
                path.chmod(0o755)
            env = dict(os.environ)
            env.update(
                JAVA_HOME="keep-the-gradle-jdk",
                JAVA_HOME_21_X64=str(root / "java 21"),
                TEST_MAESTRO_STATUS=str(maestro_status),
                TEST_DIAGNOSTIC_FAILURE=str(int(diagnostic_failure)),
                TEST_TRANSPORT_FAILURE=transport_failure,
                TEST_MISSING_REPORT=str(int(missing_report)),
                ANDROID_SERIAL="emulator-5556",
            )
            results = []
            # A separate sh process for every command reproduces the real action.
            for command in maestro_commands():
                result = subprocess.run(
                    [shutil.which("bash"), "-c", 'export PATH="$PWD/bin:$PATH"; exec sh -c "$1"', "--", command],
                    cwd=app,
                    env=env,
                    capture_output=True,
                    text=True,
                    timeout=60,
                )
                results.append(result)
                if result.returncode:
                    break
            calls = (app / "calls.txt").read_text()
            report_root = app / "build/reports/maestro"
            if transport_failure == "once":
                self.assertIn("device offline", (report_root / "attempt-1/junit.xml").read_text())
                self.assertNotIn("failure", (report_root / "junit.xml").read_text())
            if missing_report:
                self.assertFalse((report_root / "junit.xml").exists())
                self.assertFalse((report_root / "attempt-1/junit.xml").exists())
            return results[-1], calls

    def test_actual_step_reaches_maestro_in_separate_shells_with_java_21(self):
        result, calls = self.run_workflow()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("Maestro smoke tests passed", result.stdout)
        self.assertIn("maestro --device emulator-5556 test --format junit --output build/reports/maestro/attempt-1/junit.xml", calls)
        self.assertIn("--flatten-debug-output ../maestro/android/", calls)
        self.assertIn("java 21", calls)
        self.assertNotIn("keep-the-gradle-jdk", calls)

    def test_maestro_failure_preserves_status_and_collects_diagnostics(self):
        result, calls = self.run_workflow(maestro_status=7)
        self.assertEqual(result.returncode, 7, result.stderr)
        self.assertIn("adb logcat -d", calls)
        self.assertIn("Maestro diagnostic log", result.stdout)
        self.assertEqual(calls.count("maestro --device"), 1)

    def test_diagnostic_failure_does_not_replace_maestro_failure(self):
        result, calls = self.run_workflow(maestro_status=7, diagnostic_failure=True)
        self.assertEqual(result.returncode, 7, result.stderr)
        self.assertIn("adb logcat -d", calls)

    def test_device_disconnect_gets_one_fresh_session_and_retains_evidence(self):
        result, calls = self.run_workflow(transport_failure="once")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(calls.count("maestro --device"), 2)
        self.assertIn("one fresh session", result.stdout)

    def test_persistent_disconnect_fails_without_a_third_attempt(self):
        result, calls = self.run_workflow(transport_failure="persistent")
        self.assertEqual(result.returncode, 7, result.stderr)
        self.assertEqual(calls.count("maestro --device"), 2)

    def test_stale_connection_report_cannot_authorize_recovery(self):
        result, calls = self.run_workflow(missing_report=True)
        self.assertEqual(result.returncode, 7, result.stderr)
        self.assertEqual(calls.count("maestro --device"), 1)


if __name__ == "__main__":
    unittest.main()
