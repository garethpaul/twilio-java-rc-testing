# Stable Dependencies and Hosted Verification

Status: Completed

## Problem

The sample still used Twilio Java `9.0.0-rc.1`, Spark `2.9.3`, and had no
hosted verification. The application already had a strong dry-run default and
27 unit/contract tests, but dependency or JDK compatibility regressions could
reach the main branch without running them.

## Plan

1. Replace the Twilio release candidate with the current stable official SDK
   after proving the existing call and TwiML APIs remain source-compatible.
2. Apply Spark Java's final `2.9.4` patch release to refresh its Jetty line.
3. Override Spark's remaining Jetty transitive line with the advisory-free
   official `9.4.58` BOM after compatibility verification.
4. Add least-privilege GitHub Actions verification on Java 8 and Java 11 with
   immutable action pins and a bounded runtime.
5. Add JUnit repository contracts for exact dependency versions and workflow
   security/compatibility settings.

## Verification

- `make check` on Java 8
- `make check` on Java 11
- Direct-version OSV queries for Spark, Twilio, Jetty, and JUnit
- Negative workflow-permission and stale-dependency mutations rejected by tests
- `git diff --check`
