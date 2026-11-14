"""Exercise the CI wrapper without Android, Gradle, or a 45-minute wait."""

import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


WRAPPER = Path(__file__).with_name("run-android-instrumentation.sh").resolve()


@unittest.skipUnless(shutil.which("bash"), "Bash is required")
class InstrumentationWrapperTest(unittest.TestCase):
    def run_wrapper(
        self,
        *,
        gradle_status=0,
        deadline=False,
        broken_device=False,
        launcher_anr=False,
        wrong_focus=False,
        app_anr=False,
        startup_deadline=False,
    ):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            tools = root / "bin"
            tools.mkdir()
            stubs = {
                "adb": """#!/usr/bin/env bash
echo "adb $*" >> calls.txt
if [[ "$*" == 'shell pidof dev.abhishekan.pulse' ]]; then echo 1234; fi
if [[ "$*" == 'logcat -v brief '* ]]; then echo 'TestRunner: started: example'; fi
if [[ "$*" == 'shell am force-stop com.google.android.apps.nexuslauncher' ]]; then touch launcher-recovered; fi
if [[ "$*" == 'shell dumpsys window' ]]; then
  if [[ "$TEST_LAUNCHER_ANR" == 1 && ! -f launcher-recovered ]]; then
    echo 'mCurrentFocus=Window{abc u0 Application Not Responding: com.google.android.apps.nexuslauncher}'
  elif [[ "$TEST_APP_ANR" == 1 ]]; then
    echo 'mCurrentFocus=Window{abc u0 Application Not Responding: dev.abhishekan.pulse}'
  elif [[ "$TEST_WRONG_FOCUS" == 1 ]]; then
    echo 'mCurrentFocus=Window{abc u0 com.android.launcher3/.Launcher}'
  else
    echo 'mCurrentFocus=Window{abc u0 dev.abhishekan.pulse/.MainActivity}'
  fi
fi
exit 0
""",
                "timeout": """#!/usr/bin/env bash
echo "timeout $*" >> calls.txt
while [[ "$1" == --* ]]; do shift; done
duration=$1
shift
if [[ "$duration" == 45m && "$TEST_DEADLINE" == 1 ]]; then exit 124; fi
if [[ "$duration" == 120s && "$TEST_STARTUP_DEADLINE" == 1 ]]; then exit 124; fi
if [[ "$duration" != 45m && "$TEST_BROKEN_DEVICE" == 1 && -f gradle-started ]]; then exit 124; fi
exec "$@"
""",
                "sleep": "#!/usr/bin/env bash\nexit 0\n",
            }
            for name, content in stubs.items():
                stub = tools / name
                stub.write_text(content, encoding="utf-8", newline="\n")
                stub.chmod(0o755)
            gradle = root / "gradlew"
            gradle.write_text(
                '#!/usr/bin/env bash\necho "gradle $*" >> calls.txt\n'
                'touch gradle-started\necho "Gradle output"\nexit "$TEST_GRADLE_STATUS"\n',
                encoding="utf-8",
                newline="\n",
            )
            gradle.chmod(0o755)
            env = dict(os.environ)
            env.update(
                PATH=str(tools) + os.pathsep + env["PATH"],
                TEST_GRADLE_STATUS=str(gradle_status),
                TEST_DEADLINE=str(int(deadline)),
                TEST_BROKEN_DEVICE=str(int(broken_device)),
                TEST_LAUNCHER_ANR=str(int(launcher_anr)),
                TEST_WRONG_FOCUS=str(int(wrong_focus)),
                TEST_APP_ANR=str(int(app_anr)),
                TEST_STARTUP_DEADLINE=str(int(startup_deadline)),
            )
            result = subprocess.run(
                [
                    shutil.which("bash"),
                    "-c",
                    'export PATH="$PWD/bin:$PATH"; exec bash "$1"',
                    "--",
                    str(WRAPPER),
                ],
                cwd=root,
                env=env,
                capture_output=True,
                text=True,
                timeout=30,
            )
            calls = (root / "calls.txt").read_text()
            output_file = root / "build/reports/device-environment/gradle.txt"
            output = output_file.read_text() if output_file.exists() else ""
            return result, calls, output

    def test_success_preserves_output_and_runs_the_complete_suite(self):
        result, calls, output = self.run_wrapper()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("Gradle output", output)
        self.assertIn("timeout_msec=120000 :app:connectedDebugAndroidTest", calls)
        self.assertNotIn("-e class", calls)
        self.assertNotIn("dumpsys power", calls)
        self.assertNotIn("am force-stop", calls)

    def test_launcher_anr_is_recovered_before_gradle_starts(self):
        result, calls, _ = self.run_wrapper(launcher_anr=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("Pixel Launcher stopped responding", result.stdout)
        self.assertLess(calls.index("am force-stop com.google.android.apps.nexuslauncher"), calls.index("gradle --"))

    def test_unfocused_emulator_fails_before_the_suite_and_collects_evidence(self):
        result, calls, _ = self.run_wrapper(wrong_focus=True)
        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertIn("cannot give Pulse input focus", result.stdout)
        self.assertNotIn("gradle --", calls)
        self.assertIn("dumpsys power", calls)

    def test_app_anr_is_rejected_without_force_stopping_the_app(self):
        result, calls, _ = self.run_wrapper(app_anr=True)
        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertNotIn("am force-stop", calls)
        self.assertNotIn("gradle --", calls)

    def test_startup_deadline_is_not_reported_as_a_45_minute_test_timeout(self):
        result, calls, _ = self.run_wrapper(startup_deadline=True)
        self.assertEqual(result.returncode, 124, result.stderr)
        self.assertIn("Android emulator startup failed", result.stdout)
        self.assertNotIn("45-minute deadline", result.stdout)
        self.assertNotIn("gradle --", calls)

    def test_failure_survives_tee_and_collects_device_evidence(self):
        result, calls, _ = self.run_wrapper(gradle_status=7)
        self.assertEqual(result.returncode, 7, result.stderr)
        self.assertIn("timeout 15s adb shell dumpsys window", calls)
        self.assertIn("timeout 15s adb exec-out screencap -p", calls)
        self.assertIn("run-as dev.abhishekan.pulse kill -3 1234", calls)
        self.assertIn("Instrumentation failed (exit 7)", result.stdout)

    def test_host_deadline_collects_evidence_and_fails(self):
        result, calls, _ = self.run_wrapper(deadline=True)
        self.assertEqual(result.returncode, 124, result.stderr)
        self.assertIn("exceeded its 45-minute deadline", result.stdout)
        self.assertIn("timeout --signal=TERM --kill-after=30s 45m", calls)
        self.assertIn("adb logcat -d", calls)

    def test_unresponsive_device_cannot_replace_original_failure(self):
        result, calls, _ = self.run_wrapper(gradle_status=9, broken_device=True)
        self.assertEqual(result.returncode, 9, result.stderr)
        self.assertIn("timeout 15s adb logcat -d", calls)


if __name__ == "__main__":
    unittest.main()
