# Changes

## 2026-06-09

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
