# Port Parsing Guard

## Status: Completed

## Context

The Spark server reads the assigned `PORT` value for Heroku-style deployment.
The original helper parsed `PORT` directly, so a malformed or zero value could
throw before the server started instead of falling back to the local default.

## Objectives

- Preserve the `4567` local default when no valid assigned port is present.
- Accept positive assigned port numbers with surrounding whitespace.
- Reject malformed, blank, zero, or out-of-range assigned ports without
  throwing during startup.

## Work Completed

- Added `portFromEnv(Map<String, String>)` for testable port parsing.
- Updated `getHerokuAssignedPort()` to delegate to the safe helper.
- Added JUnit coverage for missing, valid, malformed, zero, and out-of-range
  `PORT` values.
- Updated README, VISION, and CHANGES with the deployment guard.

## Verification

- `mvn -q test`
- `make lint`
- `make test`
- `make build`
- `make check`
- `make verify`
- `git diff --check`

## Follow-Up Candidates

- Add integration-test coverage around Spark route registration.
- Document supported Java, Maven, and Twilio SDK versions.
