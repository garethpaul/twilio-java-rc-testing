# Make Root Override Protection

## Status: Completed

## Context

The Makefile derives `ROOT` from its own location so targets work when invoked
outside the checkout. GNU Make command-line assignments still take precedence
over an ordinary `:=` assignment, allowing `make ROOT=/other/path check` to
redirect builds and the repository guard away from the intended checkout.

## Priority

Protect the repository root as an internal invariant while preserving the
documented `MVN` tool override. This keeps every Make target anchored to the
same reviewed sources and verification script.

## Requirements

- R1. Define `ROOT` with GNU Make's `override` directive and derive it from the
  loaded Makefile path.
- R2. Keep `MVN ?= mvn` so callers and hosted jobs can select a Maven binary.
- R3. Prove `lint`, `test`, `build`, and `check` ignore a hostile command-line
  `ROOT` assignment.
- R4. Preserve successful invocation from a working directory outside the
  checkout.
- R5. Add portable static contracts and hostile mutations for the protected
  root assignment without changing application behavior or dependencies.
- R6. Record completed local verification before shipment.

## Implementation Units

### Protect the internal Make root

**Files:** `Makefile`

Change the existing Makefile-derived assignment to `override ROOT := ...` and
leave the `MVN` override contract unchanged.

### Enforce the contract

**Files:** `scripts/check-baseline.sh`, `src/test/java/org/example/DocsPlansTest.java`

Require the exact protected assignment and reject weaker ordinary, recursive,
environment-derived, or current-directory root definitions.

### Record verification

**Files:** `docs/plans/2026-06-14-make-root-override-protection.md`

Mark the plan completed only after focused tests, the full gate, external and
hostile-root invocations, mutations, and repository audits pass.

## Verification Plan

- focused `DocsPlansTest`
- full `make check` with a hard timeout
- external-working-directory `make -C <checkout> check`
- hostile `make -C <checkout> ROOT=<empty-directory> check`
- mutations removing `override`, restoring `CURDIR`, changing to recursive
  assignment, weakening static tests, or removing plan completion evidence
- `git diff --check`, intended-path, generated-artifact, and changed-line
  secret audits

## Scope Boundaries

- Do not remove the `MVN` override or pin a machine-specific Maven path.
- Do not alter Java sources, runtime behavior, dependencies, workflow coverage,
  or live Twilio safeguards.
- Do not merge or close any stacked pull request without owner authorization.

## Work Completed

- Protected the Makefile-derived repository root with GNU Make's `override`
  directive while preserving the configurable Maven command.
- Added shell and JUnit contracts for the exact protected assignment, the
  Makefile-derived path, and the Maven override.
- Added the plan to the repository's required maintenance-document set.

## Verification

- The focused `DocsPlansTest#checkGateRunsScriptedBaseline` test passed on
  Amazon Corretto 8 with Maven 3.6.3.
- A hostile `make ROOT=<empty-directory> lint` invocation ignored the supplied
  root and compiled the intended checkout.
- Full `make check`, external-working-directory `make -C <checkout> check`, and
  hostile `make -C <checkout> ROOT=<empty-directory> check` each passed under
  a 300-second timeout, including compilation, 42 JUnit tests, packaging, and
  the scripted baseline.
- Eight hostile mutations were rejected: removing `override`, using `CURDIR`,
  changing to recursive assignment, deriving from the first loaded Makefile,
  removing Maven configurability, weakening either static guard, and removing
  completed plan status.
- Makefile, shell, Java, intended-path, generated-artifact, `git diff --check`,
  and changed-line secret audits passed before shipment.
- No Twilio credentials, live calls, or external callback requests were used.
