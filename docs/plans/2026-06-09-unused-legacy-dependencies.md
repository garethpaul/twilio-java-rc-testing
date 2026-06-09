# Unused Legacy Dependencies

## Status: Completed

## Context

The Maven project declared Apache Spark Streaming, Apache Velocity, and WebJars
version properties even though the checked-in Java source only imports Spark
Java and Twilio SDK APIs. Keeping unused legacy dependencies in a live-call
sample increases dependency surface and package size without supporting the
current flow.

## Objectives

- Remove unused legacy dependency declarations from `pom.xml`.
- Keep Spark Java, Twilio SDK, and JUnit dependencies intact.
- Add a source-contract test so the unused dependency declarations do not
  return accidentally.
- Preserve the existing Maven build and verification gates.

## Work Completed

- Removed unused WebJars version properties.
- Removed unused Apache Spark Streaming and Velocity dependencies.
- Added a JUnit source-contract test for the dependency cleanup.
- Extended docs-plan coverage to require this completed plan.
- Updated README, VISION, and CHANGES.

## Verification

- Negative: source review showed no checked-in imports for Apache Spark
  Streaming, Velocity, or WebJars.
- `mvn -q -DskipTests compile`
- `mvn -q test`
- `mvn -q -DskipTests package`
- `make check`
- `make verify`
- `git diff --check`

## Follow-Up Candidates

- Add dependency-tree documentation for the remaining Spark Java and Twilio SDK
  runtime surface.
- Add a mock Twilio client seam before deeper route-level integration tests.
