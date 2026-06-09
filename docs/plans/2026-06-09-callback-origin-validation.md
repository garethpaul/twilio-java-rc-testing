# Callback Origin Validation

## Status: Completed

## Context

The RC sample builds the TwiML callback by appending `/twiml` to `NGROK_URL`.
Validation already required a parseable HTTPS URL with a host, but it still
accepted paths, queries, fragments, and userinfo. Those forms are not plain
callback origins and can produce ambiguous callback URLs or expose extra
metadata in configuration.

## Objectives

- Keep live call setup explicitly guarded.
- Require `NGROK_URL` to be an HTTPS origin URL.
- Reject path, query, fragment, and userinfo components.
- Preserve plain origins with an optional trailing slash.

## Work Completed

- Tightened `isValidCallbackBaseUrl()` to require HTTPS scheme, host, no
  userinfo, root path only, no query, and no fragment.
- Updated the validation error to describe an HTTPS origin URL.
- Added JUnit coverage for path, query, fragment, userinfo, and trailing slash
  callback base values.
- Updated README, VISION, and CHANGES.

## Verification

- `mvn -q test`
- `mvn -q -DskipTests compile`
- `mvn -q -DskipTests package`
- `make lint`
- `make test`
- `make build`
- `make check`
- `make verify`
- `git diff --check`

## Follow-Up Candidates

- Add integration-test coverage around Spark route registration.
- Add a mock Twilio client path for local route tests.
