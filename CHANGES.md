# Changes

## 2026-06-08

- Added JUnit coverage and `make verify`/`make check` Maven gates for the Twilio call validation path.
- Pinned the Spring Boot Maven plugin to a Java 8-compatible version so packaging no longer resolves an incompatible RC plugin.
- Rejected missing/malformed dial targets and incomplete Twilio/ngrok configuration before creating outbound calls.
