"""Run the actual workflow commands with the emulator action's per-line shells."""

import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parent.parent


def maestro_commands():
    workflow = (ROOT / ".github/workflows/tests.yml").read_text()
    step = workflow.split("      - name: Run Maestro smoke tests on a fresh emulator\n", 1)[1]
    step = step.split("      - name:", 1)[0]
    script = step.split("          script: |\n", 1)[1]
    return [line.strip() for line in script.splitlines() if line.strip() and not line.strip().startswith("#")]


@unittest.skipUnless(shutil.which("bash"), "Bash is required")
class MaestroWorkflowTest(unittest.TestCase):
    def run_workflow(self, maestro_status=0, diagnostic_failure=False):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            app = root / "pulse-android"
            app.mkdir()
            scripts = root / "scripts"
            scripts.mkdir()
            for name in ("run-android-maestro.sh", "prepare-android-emulator.sh"):
                shutil.copyfile(ROOT / "scripts" / name, scripts / name)
            fake_bin = app / "bin"
            fake_bin.mkdir()
            stubs = {
                "adb": """#!/usr/bin/env bash
echo "adb $*" >> calls.txt
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
mkdir -p build/reports/maestro/debug
echo 'Maestro diagnostic log' > build/reports/maestro/debug/maestro.log
exit "$TEST_MAESTRO_STATUS"
""",
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
                    timeout=30,
                )
                results.append(result)
                if result.returncode:
                    break
            calls = (app / "calls.txt").read_text()
            return results[-1], calls

    def test_actual_step_reaches_maestro_in_separate_shells_with_java_21(self):
        result, calls = self.run_workflow()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("Maestro smoke tests passed", result.stdout)
        self.assertIn("maestro test --format junit --output build/reports/maestro/junit.xml", calls)
        self.assertIn("--flatten-debug-output ../maestro/android/", calls)
        self.assertIn("java 21", calls)
        self.assertNotIn("keep-the-gradle-jdk", calls)

    def test_maestro_failure_preserves_status_and_collects_diagnostics(self):
        result, calls = self.run_workflow(maestro_status=7)
        self.assertEqual(result.returncode, 7, result.stderr)
        self.assertIn("adb logcat -d", calls)
        self.assertIn("Maestro diagnostic log", result.stdout)

    def test_diagnostic_failure_does_not_replace_maestro_failure(self):
        result, calls = self.run_workflow(maestro_status=7, diagnostic_failure=True)
        self.assertEqual(result.returncode, 7, result.stderr)
        self.assertIn("adb logcat -d", calls)


if __name__ == "__main__":
    unittest.main()
