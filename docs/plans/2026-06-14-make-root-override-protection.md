# Make Root Override Protection

## Status: Planned

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
