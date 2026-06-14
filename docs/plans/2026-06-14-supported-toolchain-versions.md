# Supported Toolchain Versions

## Status: Completed

## Context

The repository compiles Java 8 bytecode, verifies the sample on four hosted
Java runtimes, and pins the Twilio SDK, but contributors must currently infer
those support boundaries from the POM and workflow. Maven is also exercised
locally at a known version without a documented minimum-version claim.

## Priority

Document the exact compile, runtime, build-tool, and Twilio SDK baselines that
the canonical gate actually verifies.

## Requirements

- Document Java 8 source and target compatibility.
- Document hosted runtime verification on Java 8, 11, 17, and 21.
- Record Maven 3.6.3 as a reproduced local baseline without claiming it is the
  minimum supported Maven release.
- Document the exact Twilio Java SDK 12.1.1 pin and its Java 8 compatibility
  boundary.
- Keep the POM and workflow as the executable sources of truth.
- Add fail-closed README, baseline, suite, roadmap, changelog, and completed-plan
  contracts plus hostile mutations.

## Verification

- Focused documentation and baseline contracts passed.
- The repository and external-directory `make check` passed.
- Both runs used Java 8 and Maven 3.6.3.
- Eight hostile supported-version mutations were rejected across the Java
  matrix, compiler target, Maven baseline, SDK pin, documentation, suite,
  roadmap, and plan-status contracts.
- Final artifact, credential, exact-diff, and hosted matrix audits remain the
  shipping gate.

## Scope Boundary

This change does not upgrade Java, Maven, Twilio, or transitive dependencies;
change compilation output; perform a live Twilio request; or claim untested
runtime and build-tool versions.
