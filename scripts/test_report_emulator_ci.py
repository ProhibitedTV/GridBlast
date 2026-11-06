"""Pure Python regressions for advisory Android emulator failure triage."""
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from report_emulator_ci import classify, failing_test_cases


class EmulatorFailureTriageTest(unittest.TestCase):
    def test_pre_script_failure_is_provisioning_not_gameplay(self):
        label, _ = classify("failure", "", "")
        self.assertEqual(label, "likely infrastructure (setup/boot)")

    def test_package_manager_broken_pipe_is_infrastructure(self):
        label, _ = classify(
            "failure", "ui-tests",
            "Failure calling service package: Broken pipe (32) while uninstalling",
        )
        self.assertEqual(label, "likely infrastructure (adb/package service)")

    def test_sdk_image_corruption_is_infrastructure(self):
        label, _ = classify(
            "failure", "ui-tests",
            "Error reading Zip content from a SeekableByteChannel",
        )
        self.assertEqual(label, "likely infrastructure (adb/package service)")

    def test_assertion_takes_precedence_over_adb_signature(self):
        label, _ = classify(
            "failure", "ui-tests",
            "java.lang.AssertionError: wrong score\nFailure calling service package: Broken pipe (32)",
        )
        self.assertEqual(label, "test assertion failure")

    def test_junit_failure_takes_precedence_even_without_text(self):
        label, _ = classify("failure", "ui-tests", "", failed_tests=1)
        self.assertEqual(label, "test assertion failure")

    def test_unknown_test_failure_is_not_dismissed_as_infrastructure(self):
        label, _ = classify("failure", "ui-tests", "Gradle failed unexpectedly")
        self.assertEqual(label, "unclassified failure")

    def test_success_is_never_downgraded_based_on_incidental_log(self):
        label, _ = classify("success", "completed", "java.lang.AssertionError:")
        self.assertEqual(label, "passed")

    def test_cancelled_is_never_called_passed(self):
        label, _ = classify("cancelled", "", "")
        self.assertEqual(label, "not evaluated")

    def test_junit_xml_counts_real_failed_cases(self):
        with TemporaryDirectory() as temp:
            output = Path(temp) / "outputs/androidTest-results/connected"
            output.mkdir(parents=True)
            (output / "TEST-gridblast.xml").write_text(
                '<testsuite tests="2" failures="1">'
                '<testcase name="place"><failure message="wrong board"/></testcase>'
                '<testcase name="undo"/></testsuite>', encoding="utf-8"
            )
            self.assertEqual(failing_test_cases(Path(temp)), 1)

    def test_corrupted_partial_xml_is_not_falsely_called_assertion(self):
        with TemporaryDirectory() as temp:
            output = Path(temp) / "outputs/androidTest-results/connected"
            output.mkdir(parents=True)
            (output / "partial.xml").write_text("<testsuite>", encoding="utf-8")
            self.assertEqual(failing_test_cases(Path(temp)), 0)


if __name__ == "__main__":
    unittest.main()
