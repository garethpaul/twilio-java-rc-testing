# Changes

## 2026-06-09

- Removed tracked IntelliJ `.idea` metadata and added JUnit coverage for the
  broad local IDE ignore rule.
- Added `scripts/check-baseline.sh` to keep required files, completed plan
  metadata, and editor metadata hygiene wired into `make check`.
- Removed stale URL wording from invalid dial-target errors now that
  `/dial-phone` is POST-only.
- Marked the POST form phone number input as required with JUnit coverage.
- Changed the live-call-capable `/dial-phone` action and form submission from
  GET to POST.
- Added JUnit source-contract coverage so the dial route and form stay POST.
- Redacted dial targets in dry-run and live response messages with JUnit
  coverage.
- Tightened `NGROK_URL` validation to require an HTTPS origin without path,
  query, fragment, or userinfo before building the TwiML callback URL.
- Added JUnit coverage for callback origin URL edge cases.
- Added safe `PORT` parsing so invalid or out-of-range assigned ports fall
  back to the local default instead of crashing startup.
- Added JUnit coverage for missing, valid, malformed, and zero `PORT` values.
- Added parseable HTTPS callback URL validation for `NGROK_URL`.
- Added JUnit coverage for malformed and insecure callback URL values.

## 2026-06-08

- Added TwiML response generation tests and an explicit XML content type for
  `/twiml`.
- Changed the checked-in Log4j root logger from `debug` to `info`, with JUnit
  coverage to keep verbose logging opt-in.
- Added canonical `docs/plans` coverage to the Maven test gate.
- Added a default dry-run path for `/dial-phone`; live outbound calls now
  require `TWILIO_SEND_LIVE=true`.
- Added JUnit coverage for live-send opt-in, dry-run configuration, and route
  response messages.
- Added JUnit coverage and `make verify`/`make check` Maven gates for the Twilio call validation path.
- Pinned the Spring Boot Maven plugin to a Java 8-compatible version so packaging no longer resolves an incompatible RC plugin.
- Rejected missing/malformed dial targets and incomplete Twilio/ngrok configuration before creating outbound calls.
