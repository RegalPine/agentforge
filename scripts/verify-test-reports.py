#!/usr/bin/env python3
"""校验 clean 构建的测试报告；关键套件缺失或全跳过不能成为绿灯。"""
import argparse
from pathlib import Path
import xml.etree.ElementTree as ET

MARKERS = {
    "unit": {"io.github.regalpine.agentforge.arch.G4VerificationTest"},
    "security": {"io.github.regalpine.agentforge.server.http.AuthenticatedHandlerTest"},
    "benchmark": {"io.github.regalpine.agentforge.benchmark.FixedWorkloadBenchmarkTest"},
    "integration": {"io.github.regalpine.agentforge.persistence.postgresql.PostgresPersistenceIT"},
}


def verify(root, profile):
    reports = []
    for module in root.glob("agentforge-*"):
        for runner in ("surefire-reports", "failsafe-reports"):
            reports.extend((module / "target" / runner).glob("TEST-*.xml"))
    executed = set()
    total = 0
    problems = []
    for report in sorted(reports):
        data = report.read_bytes()
        if len(data) > 50_000_000 or b"<!DOCTYPE" in data.upper() or b"<!ENTITY" in data.upper():
            raise ValueError(f"拒绝包含 DTD、实体声明或过大内容的报告：{report}")
        suite = ET.fromstring(data)
        name = suite.attrib["name"]
        cases = suite.findall("testcase")
        active = [case for case in cases if case.find("skipped") is None]
        if active:
            executed.add(name)
        total += len(active)
        if any(case.find("failure") is not None or case.find("error") is not None for case in cases):
            problems.append(f"失败套件：{name}")
    required = set().union(*MARKERS.values()) if profile == "all-tests" else MARKERS[profile]
    for missing in sorted(required - executed):
        problems.append(f"关键套件未执行或全跳过：{missing}")
    if profile != "all-tests":
        forbidden = set().union(*(v for k, v in MARKERS.items() if k != profile))
        for unexpected in sorted(forbidden & executed):
            problems.append(f"其他分组套件意外执行（请先 clean）：{unexpected}")
    if total == 0:
        problems.append("没有实际执行的测试")
    if problems:
        raise ValueError("\n".join(problems))
    return len(executed), total


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("profile", choices=[*MARKERS, "all-tests"])
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    try:
        suites, tests = verify(args.root, args.profile)
        print(f"{args.profile}: {suites} 个套件、{tests} 项实际执行，关键分组校验通过")
    except (ValueError, ET.ParseError, KeyError) as error:
        parser.exit(1, str(error) + "\n")
