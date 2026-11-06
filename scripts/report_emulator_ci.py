#!/usr/bin/env python3
"""Summarize emulator job failures without changing the required CI pass gate.

A failed Android emulator action may fail before our test script runs (SDK
download/boot), inside adb/package manager, or in real UI assertions. The
diagnosis here is advisory: it never converts a failed job into a success.
"""
from __future__ import annotations

import argparse
import os
from pathlib import Path
import re
import xml.etree.ElementTree as ET


INFRA_SIGNATURES = (
    re.compile(r"Failure calling service package: Broken pipe \(32\)", re.I),
    re.compile(r"Error reading Zip content from a SeekableByteChannel", re.I),
    re.compile(r"\bdevice offline\b", re.I),
    re.compile(r"no devices/emulators found", re.I),
    re.compile(r"adb: .*device .* not found", re.I),
    re.compile(r"unable to connect to adb daemon", re.I),
    re.compile(r"failed to connect to localhost:5554", re.I),
)
ASSERTION_SIGNATURES = (
    re.compile(r"\b(?:java\.lang\.)?AssertionError:", re.I),
    re.compile(r"\bAssertionFailedError:", re.I),
    re.compile(r"\bComposeNotIdleException:", re.I),
    re.compile(r"\bComposeTimeoutException:", re.I),
)


def failing_test_cases(build_dir: Path) -> int:
    """Count explicit JUnit failure/error nodes, not generic Gradle failures."""
    total = 0
    roots = (build_dir / "outputs/androidTest-results", build_dir / "test-results")
    for directory in roots:
        if not directory.exists():
            continue
        for xml_file in directory.rglob("*.xml"):
            try:
                root = ET.parse(xml_file).getroot()
            except (ET.ParseError, OSError):
                continue
            for case in root.iter("testcase"):
                if any(child.tag in ("failure", "error") for child in case):
                    total += 1
    return total


def classify(outcome: str, phase: str, log: str, failed_tests: int = 0) -> tuple[str, str]:
    """Classification is for triage only; never override GitHub job outcome."""
    if outcome == "success":
        return "passed", "The emulator action passed. All required checks remain enforced."
    if outcome in ("cancelled", "skipped"):
        return "not evaluated", "The action did not complete; do not interpret this as a test pass."
    if outcome != "failure":
        return "unclassified", "The emulator action has no final failure or success result."
    if not phase:
        return (
            "likely infrastructure (setup/boot)",
            "The GridBlast test script never started. Inspect system-image download, emulator provisioning and cold boot in the action log.",
        )
    if failed_tests or any(sig.search(log) for sig in ASSERTION_SIGNATURES):
        return (
            "test assertion failure",
            "A JUnit failure or assertion is recorded. Treat this as a test regression until investigated; do not auto-retry it away.",
        )
    if any(sig.search(log) for sig in INFRA_SIGNATURES):
        return (
            "likely infrastructure (adb/package service)",
            "A known transient emulator/adb signature is present without recorded assertion failures. Inspect raw logs and rerun only the affected job if appropriate.",
        )
    if phase in ("script-started", "adb-ready"):
        return (
            "likely infrastructure (device/configuration)",
            "The failure happened before UI instrumentation began. Inspect emulator/adb configuration diagnostics.",
        )
    return (
        "unclassified failure",
        "The test runner started, but evidence does not identify an assertion or known infrastructure signature. Investigate before choosing a rerun.",
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--outcome", required=True)
    parser.add_argument("--api", required=True)
    parser.add_argument("--profile", required=True)
    parser.add_argument("--diagnostics-dir", type=Path, required=True)
    args = parser.parse_args()

    directory: Path = args.diagnostics_dir
    directory.mkdir(parents=True, exist_ok=True)
    phase_file = directory / "phase.txt"
    phase = phase_file.read_text(encoding="utf-8").strip() if phase_file.exists() else ""
    test_log = directory / "gradle-connected-test.log"
    log = test_log.read_text(encoding="utf-8", errors="replace") if test_log.exists() else ""
    assertion_count = failing_test_cases(Path("app/build"))
    label, details = classify(args.outcome, phase, log, assertion_count)

    report = (
        f"### GridBlast emulator CI triage: API {args.api} / {args.profile}\n\n"
        f"- **Action outcome:** {args.outcome} (unchanged by diagnostics)\n"
        f"- **Advisory classification:** {label}\n"
        f"- **Last recorded phase:** {phase or 'script not started'}\n"
        f"- **JUnit test cases with explicit failures/errors:** {assertion_count}\n\n"
        f"{details}\n\n"
        "The full emulator action log remains the source of truth. "
        "Passing assertions, real save recovery and APK upgrade checks are never waived. "
        "See [CI reliability issue #60](https://github.com/ProhibitedTV/GridBlast/issues/60).\n"
    )
    (directory / "triage.md").write_text(report, encoding="utf-8")
    print(report)
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as summary:
            summary.write(report + "\n")


if __name__ == "__main__":
    main()
