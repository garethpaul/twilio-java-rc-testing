# All-Branch Push Coverage

## Status: In Progress

## Context

The canonical workflow verifies pull requests but restricts push verification
to `main`. Stacked remediation heads therefore lack the independent push event
that the repository uses as canonical hosted evidence.

## Priority

Run the existing Java 8, 11, 17, and 21 verification matrix for pushes to every
branch without changing permissions, action pins, or test coverage.

## Requirements

- Replace the `main`-only push filter with an all-branch push trigger.
- Preserve pull-request and manual triggers.
- Add shell and JUnit contracts that reject branch-filtered push coverage.
- Document the all-branch hosted verification boundary.
- Add mutation-sensitive validation for a restored `branches` filter.

## Verification Plan

- Run focused JUnit workflow contracts.
- Run repository and external-directory `make check`.
- Prove a mutation restoring `push.branches` is rejected.
- Audit the exact diff, generated artifacts, credentials, and whitespace.

## Scope Boundary

This change does not alter the Java matrix, Maven commands, permissions,
dependency versions, application behavior, or live Twilio execution.
