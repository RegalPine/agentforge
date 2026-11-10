"""报告门禁自身的正负例，避免零测试或错误分组被误判成功。"""
from pathlib import Path
import runpy
import tempfile
import unittest

GUARD = runpy.run_path(str(Path(__file__).with_name("verify-test-reports.py")))


class ReportGuardTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.reports = self.root / "agentforge-test/target/surefire-reports"
        self.reports.mkdir(parents=True)

    def report(self, profile="security", child="", index=1):
        name = next(iter(GUARD["MARKERS"][profile]))
        xml = f'<testsuite name="{name}"><testcase name="case">{child}</testcase></testsuite>'
        (self.reports / f"TEST-{index}.xml").write_text(xml, encoding="utf-8")

    def test_empty_reports_fail(self):
        with self.assertRaises(ValueError):
            GUARD["verify"](self.root, "security")

    def test_real_execution_passes(self):
        self.report()
        self.assertEqual((1, 1), GUARD["verify"](self.root, "security"))

    def test_skipped_marker_fails(self):
        self.report(child="<skipped/>")
        with self.assertRaises(ValueError):
            GUARD["verify"](self.root, "security")

    def test_failure_is_not_counted_as_success(self):
        self.report(child="<failure/>")
        with self.assertRaises(ValueError):
            GUARD["verify"](self.root, "security")

    def test_wrong_profile_fails(self):
        self.report()
        self.report("benchmark", index=2)
        with self.assertRaises(ValueError):
            GUARD["verify"](self.root, "security")

    def test_dtd_is_rejected(self):
        (self.reports / "TEST-dtd.xml").write_text('<!DOCTYPE testsuite><testsuite name="x"/>')
        with self.assertRaises(ValueError):
            GUARD["verify"](self.root, "unit")


if __name__ == "__main__":
    unittest.main()
