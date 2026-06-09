# POST Invalid Dial Target

## Status: Completed

## Context

The `/dial-phone` route is now POST-only because it can place live outbound
calls when live-send configuration is explicitly enabled. The invalid dial
target response still told users to enter a valid number "in the URL", which
conflicted with the checked-in form flow and the POST-only route contract.

## Objectives

- Preserve E.164 validation before dry-run or live-call behavior.
- Keep invalid dial-target responses concise and non-sensitive.
- Remove stale URL wording from the POST form submission path.
- Require the checked-in POST form phone input before submission.
- Add JUnit coverage so the error text remains aligned with POST forms.
- Keep the completed plan required by `make check`.

## Work Completed

- Extracted the invalid dial-target response into `invalidDialTargetMessage()`.
- Updated the `/dial-phone` 400 response to use form-aligned wording.
- Marked the checked-in POST form phone input as required.
- Added JUnit coverage for the message and a source check against stale URL
  wording.
- Added JUnit coverage for the required phone input.
- Updated docs-plan tests to require this completed plan.
- Updated README, VISION, and CHANGES.

## Verification

- `mvn -q -Dtest=MainTest test`
- `mvn -q test`
- `mvn -q -DskipTests package`
- `make check`
- `git diff --check`

## Follow-Up Candidates

- Add integration-test coverage around Spark route responses.
- Add a mock Twilio client path for local live-call tests.
