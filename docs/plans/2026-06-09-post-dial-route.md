# POST Dial Route

## Status: Completed

## Context

The `/dial-phone` route can trigger a live Twilio outbound call when
`TWILIO_SEND_LIVE=true` is configured. Even though dry-run is the default,
live-call-capable actions should not be exposed as GET requests because links,
browser prefetching, crawlers, or bookmarked URLs can trigger GET routes
without an intentional form submission.

## Objectives

- Move the call-triggering Spark route from GET to POST.
- Keep the checked-in HTML form aligned with the POST route.
- Preserve existing phone-number and configuration validation before dialing.
- Add source-contract tests that prevent GET from returning.
- Keep the completed plan required by `make check`.

## Work Completed

- Changed `Main` to register `post("/dial-phone", ...)`.
- Changed the public HTML form to submit with `method="post"`.
- Added JUnit checks for the Spark route and form method.
- Updated docs-plan tests to require this completed plan.
- Updated README, VISION, and CHANGES.

## Verification

- `mvn -q test`
- `mvn -q -DskipTests package`
- `make check`
- `git diff --check`

## Follow-Up Candidates

- Add integration-test coverage around Spark routes.
- Add a mock Twilio client path for local live-call tests.
