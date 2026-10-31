"""Checks for report outcomes, timings, and safe rendering."""

import importlib.util
import io
import json
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("test_report", Path(__file__).with_name("test-report.py"))
report = importlib.util.module_from_spec(spec)
spec.loader.exec_module(report)


class TestReportTest(unittest.TestCase):
    def test_go_counts_outcomes_and_package_build_errors(self):
        events = [
            {"Package": "gym", "Test": "parent/subtest", "Action": "output", "Output": "expected db error\n"},
            {"Package": "gym", "Test": "parent/subtest", "Action": "pass", "Elapsed": 0.2},
            {"Package": "gym", "Test": "failure", "Action": "fail", "Elapsed": 0.1},
            {"Package": "gym", "Action": "fail"},
            {"Package": "auth", "Test": "skipped", "Action": "skip"},
            {"Package": "broken", "Action": "output", "Output": "compile error\n"},
            {"Package": "broken", "Action": "fail"},
        ]
        results = report.parse_go("diagnostic\n" + "\n".join(map(json.dumps, events)))
        self.assertEqual(report.counts(results), {"pass": 1, "fail": 1, "error": 1, "skip": 1})
        self.assertEqual(results[0]["duration"], 0.2)
        self.assertIn("expected db error", results[0]["details"])
        self.assertEqual(results[-1]["details"], "compile error\n")

    def test_node_preserves_failure_trace_and_times(self):
        results = report.parse_node('''<testsuites>
          <testcase name="passing" time="0.1"/>
          <testcase name="failure" time="0.2"><failure message="assertion">stack trace</failure></testcase>
          <testcase name="error"><error>setup failed</error></testcase>
          <testcase name="skip"><skipped message="reason"/></testcase>
        </testsuites>''')
        self.assertEqual(report.counts(results), {"pass": 1, "fail": 1, "error": 1, "skip": 1})
        self.assertEqual(results[1]["duration"], 0.2)
        self.assertIn("stack trace", results[1]["details"])

    def test_rust_measures_live_output_and_ignored_tests(self):
        timing = report.RustTiming()
        with patch.object(report.time, "perf_counter", side_effect=[1, 1.5, 2, 2.25, 3, 3]):
            for character in "test auth::valid - should panic ... ok\ntest auth::bad ... FAILED\ntest slow ... ignored, needs service\n":
                timing.feed(character)
        self.assertEqual([item["status"] for item in timing.records], ["pass", "fail", "skip"])
        self.assertEqual(timing.records[0]["duration"], 0.5)
        self.assertEqual(timing.records[1]["duration"], 0.25)

    def test_python_reports_assertions_errors_skips_and_subtests(self):
        class Examples(unittest.TestCase):
            def test_pass(self):
                pass

            def test_fail(self):
                self.fail("assertion detail")

            def test_error(self):
                raise ValueError("error detail")

            @unittest.skip("skip reason")
            def test_skip(self):
                pass

            def test_subtest(self):
                with self.subTest(value=1):
                    self.fail("subtest detail")

        result = unittest.TextTestRunner(stream=io.StringIO(), resultclass=report.TimedResult).run(
            unittest.defaultTestLoader.loadTestsFromTestCase(Examples))
        self.assertEqual(report.counts(result.records), {"pass": 1, "fail": 2, "error": 1, "skip": 1})
        self.assertTrue(all(item["duration"] >= 0 for item in result.records))
        self.assertIn("Traceback", next(item["details"] for item in result.records if item["status"] == "error"))
        self.assertTrue(any("subtest detail" in item["details"] for item in result.records))

    def test_missing_runner_is_an_error_and_html_escapes_output(self):
        import tempfile
        with tempfile.TemporaryDirectory() as directory:
            with patch.object(report, "REPORT", Path(directory)), patch.object(report.subprocess, "run", side_effect=FileNotFoundError("missing tool")):
                result = report.run_module("frontend")
        self.assertNotEqual(result["exit_code"], 0)
        self.assertEqual(result["tests"][0]["status"], "error")
        result["tests"][0].update(name="<script>bad</script>", details="<img onerror=bad>")
        page = report.render_report([result], 1)
        self.assertIn("&lt;script&gt;bad&lt;/script&gt;", page)
        self.assertNotIn("<img onerror=bad>", page)
