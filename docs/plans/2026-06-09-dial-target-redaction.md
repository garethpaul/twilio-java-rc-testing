# Dial Target Redaction

## Status: Completed

## Context

The `/dial-phone` route validates the submitted E.164 target before dry-run or
live-call behavior, then returns a short response message. That response should
not echo the full submitted phone number.

## Objectives

- Preserve dry-run and live response wording.
- Redact dial targets in route response messages.
- Cover normal and short phone-number redaction in JUnit tests.

## Work Completed

- Added `redactPhoneNumber()` to preserve only the final four characters.
- Updated `dialMessage()` to use the redacted target in dry-run and live
  messages.
- Added JUnit coverage for redacted dry-run output and short-number fallback
  redaction.
- Updated README, VISION, and CHANGES.

## Verification

- Negative check before implementation:
  `mvn -q -Dtest=MainTest test` failed because the dry-run message still
  contained the full submitted phone number.
- `mvn -q -Dtest=MainTest test`
- `mvn -q test`
- `make check`
- `make verify`
- `git diff --check`
