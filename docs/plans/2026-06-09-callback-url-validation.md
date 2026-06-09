# Callback URL Validation

## Status: Completed

## Context

The RC sample requires `NGROK_URL` before building the TwiML callback URL, but
the validation only checked that the string started with `https://`. Values such
as `https://` or malformed strings could pass configuration checks and fail
later when creating the callback URI for a live call.

## Objectives

- Keep live call setup explicitly guarded.
- Require `NGROK_URL` to be a parseable HTTPS URL.
- Require a callback host before building `/twiml`.
- Cover insecure and malformed URL values with tests.

## Work Completed

- Added `isValidCallbackBaseUrl`.
- Replaced the prefix-only `NGROK_URL` check with URI parsing.
- Added tests for valid, missing, insecure, and malformed callback URLs.
- Updated README, VISION, and CHANGES.

## Verification

- `mvn -q test`
- `mvn -q -DskipTests compile`
- `mvn -q -DskipTests package`
- `make check`
- `make verify`
- `git diff --check`

## Follow-Up Candidates

- Add a mock Twilio client path so `/dial-phone` route behavior can be tested
  without live credentials.
- Document supported Java, Maven, and Twilio SDK versions.
