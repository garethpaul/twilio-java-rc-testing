# Scripted Baseline Check

## Status: Completed

## Context

The root Makefile already runs Maven compile, test, and package gates, while
the repository maintenance assumptions lived mostly in JUnit source-contract
tests. A small shell guard makes the baseline visible to maintainers before
they inspect Java test code.

## Objectives

- Keep `make check` as the root verification command.
- Add a script-level baseline guard for required repository files.
- Check completed docs-plan metadata without needing to read JUnit tests.
- Keep tracked local editor metadata out of the portable sample.

## Work Completed

- Added `scripts/check-baseline.sh`.
- Wired the script into `make check` after the Maven verification gate.
- Added JUnit coverage that keeps the scripted baseline guard in the Makefile.
- Updated README, VISION, and CHANGES.

## Verification

- `mvn -q test`
- `make check`
- `git diff --check`

## Follow-Up Candidates

- Add a Maven wrapper if this repository needs pinned Maven execution.
- Expand the script with dependency-version checks if the RC test baseline
  needs stricter toolchain pinning.
