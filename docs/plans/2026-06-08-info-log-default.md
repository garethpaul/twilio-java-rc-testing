# Info Log Default

## Status: Completed

## Context

The app already keeps outbound calls in dry-run mode unless
`TWILIO_SEND_LIVE=true` is set, but the checked-in Log4j configuration defaulted
to `debug`. Verbose logs can include request context and call metadata that
should be reviewed before sharing.

## Objectives

- Preserve the Spark/Twilio release-candidate sample.
- Keep runtime logging at `info` in checked-in configuration.
- Make debug logging a local, explicit troubleshooting choice.
- Add JUnit coverage for the logging default.

## Work Completed

- Changed `src/main/resources/log4j.properties` from `debug` to `info`.
- Added a JUnit test that rejects a checked-in debug root logger.
- Updated README, VISION, and CHANGES.

## Verification

- `mvn test`
- `mvn -DskipTests package`
- `make check`
- `make verify`
- `git diff --check`

## Follow-Up Candidates

- Add explicit debug-log redaction guidance.
- Add route-level tests with a mock Twilio client.
