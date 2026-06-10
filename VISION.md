## Twilio Java Voice Testing Vision

Twilio Java Voice Testing is a small Java HTTP sample for serving TwiML and
placing outbound calls through configured Twilio credentials.

The repository is useful as a focused integration exercise: it shows
environment-based Twilio setup, a local web server, an ngrok callback URL,
TwiML generation, and outbound call creation.

The goal is to keep the voice test path explicit, reproducible, and safe for live
Twilio accounts.

The current focus is:

Priority:

- Preserve the built-in Java HTTP server and `/twiml` plus `/dial-phone` flow
- Keep account credentials and phone numbers in environment variables
- Maintain `make check` and Maven packaging for a runnable jar
- Keep a scriptable baseline guard for required files and maintenance metadata
- Keep local IDE metadata out of the portable sample
- Keep deployment port parsing predictable and safe for local fallback
- Treat outbound call creation as a live side effect
- Validate phone numbers and required callback configuration before dialing
- Redact dial targets in response messages
- Keep live-call-capable dial actions behind POST form submissions
- Keep invalid dial-target errors aligned with POST form submissions
- Keep the POST form phone input required before submission
- Keep unused legacy dependencies out of the live-call sample package
- Require callback base URLs to be parseable HTTPS origins
- Keep outbound calls in dry-run mode unless explicitly enabled
- Require per-request authorization before any live dial attempt
- Keep Twilio provider failure diagnostics out of HTTP responses
- Keep every HTTP response non-cacheable, non-frameable, and constrained by a
  restrictive browser security policy
- Keep TwiML callback responses explicit and testable
- Keep checked-in runtime logging at info by default

Next priorities:

- Expand integration-test coverage around request limits and malformed forms
- Add a mock Twilio client path for local tests
- Document supported Java, Maven, and Twilio SDK versions
- Add explicit debug-log redaction guidance

Contribution rules:

- One PR = one focused TwiML, call flow, dependency, validation, or documentation change.
- Do not commit credentials, phone numbers, call SIDs, or ngrok URLs.
- Keep live-call behavior explicit and guarded.
- Include local verification notes for changes.

## Security And Responsible Use

Canonical security policy and reporting:

- [`SECURITY.md`](SECURITY.md)

This sample can place real outbound calls. It should validate inputs, keep
credentials out of source control, avoid exposing callback URLs unnecessarily,
and make costs and account effects clear.

## What We Will Not Merge (For Now)

- Checked-in credentials, phone numbers, or callback URLs
- Unvalidated public call endpoints
- Malformed callback URLs that fail after call setup begins
- Live-call defaults without operator confirmation
- Unused legacy dependencies that expand the sample runtime surface
- Dependency changes that expand the runtime surface without a tested need

This list is a roadmap guardrail, not a permanent rule.
Strong user demand and strong technical rationale can change it.
