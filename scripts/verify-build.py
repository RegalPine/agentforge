#!/usr/bin/env python3
"""运行分组构建，绑定源码摘要并生成机器可读证据；不会执行提交或部署。"""
import argparse
import hashlib
import json
from pathlib import Path
import runpy
import subprocess
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[1]


def snapshot():
    files = {ROOT / "pom.xml", ROOT / "mvnw", ROOT / "mvnw.cmd"}
    for module in ROOT.glob("agentforge-*"):
        files.add(module / "pom.xml")
        files.update((module / "src").rglob("*"))
    studio = ROOT / "agentforge-studio"
    for pattern in ("*.json", "*.yaml", "apps/*/src/**/*", "apps/*/*.json", "apps/*/*.ts",
                    "packages/*/src/**/*", "packages/*/*.json", "packages/*/*.ts"):
        files.update(studio.glob(pattern))
    for directory in (".github", ".mvn", "deploy", "scripts"):
        files.update((ROOT / directory).rglob("*"))
    return {str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest()
            for path in sorted(files) if path.is_file() and not path.is_symlink()
            and "__pycache__" not in path.parts and path.suffix not in (".pyc", ".log")}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("profile", choices=["unit", "security", "benchmark", "integration", "all-tests"])
    args = parser.parse_args()
    before = snapshot()
    started = datetime.now(timezone.utc).isoformat()
    command = [str(ROOT / "mvnw"), "clean", "verify", "-B", "-ntp", "-q", f"-P{args.profile}"]
    result = subprocess.run(command, cwd=ROOT, check=False)
    problems = []
    suites = tests = 0
    if snapshot() != before:
        problems.append("构建期间源码发生变化，本次结果不能绑定当前源码")
    if result.returncode:
        problems.append(f"Maven 退出码 {result.returncode}")
    else:
        try:
            guard = runpy.run_path(str(ROOT / "scripts/verify-test-reports.py"))
            suites, tests = guard["verify"](ROOT, args.profile)
        except (ValueError, KeyError) as error:
            problems.append(str(error))
    digest = hashlib.sha256(json.dumps(before, sort_keys=True).encode()).hexdigest()
    evidence = {
        "profile": args.profile, "startedAt": started,
        "completedAt": datetime.now(timezone.utc).isoformat(),
        "sourceDigest": digest, "sourceHashes": before, "command": command,
        "mavenExitCode": result.returncode, "suites": suites, "executedTests": tests,
        "status": "FAILED" if problems else "PASSED", "problems": problems,
    }
    # 放在 logs 下，后续 Maven clean 不会清除其他批次的证据。
    output = ROOT / "logs/verification" / f"{args.profile}-{digest[:12]}.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"证据：{output}\n状态：{evidence['status']}；套件：{suites}；实际测试：{tests}")
    for problem in problems:
        print(problem)
    return 1 if problems else 0


if __name__ == "__main__":
    raise SystemExit(main())
