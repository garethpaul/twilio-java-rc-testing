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
- Maintain the sanitized `scripts/run-make.sh check` entrypoint, direct
  `make check` compatibility, and Maven packaging for a runnable jar
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
- Authorize live requests before disclosing provider configuration details
- Bound the authorized live dial quota after parsing and authorization, and
  preserve it across backward wall-clock adjustments
- Reject duplicate or malformed security-relevant dial form fields before
  authorization and provider configuration
- Strict UTF-8 form decoding rejects malformed bytes before unknown-field filtering.
- Keep loopback integration coverage around request limits and malformed forms
- Keep the injectable Twilio call-sender boundary covered by local tests
- Keep live-call authorization, rate limiting, request-ID deduplication, and
  single-attempt transport behavior at one explicit ownership boundary
- Keep Java, Maven, and Twilio SDK support claims tied to executable repository
  evidence
- Keep hosted verification active for pushes to every branch as well as pull
  requests and manual runs
- Keep Twilio provider failure diagnostics out of HTTP responses
- Keep every HTTP response non-cacheable, non-frameable, and constrained by a
  restrictive browser security policy
- Keep TwiML callback responses explicit and testable
- Keep checked-in runtime logging at info by default
- Keep debug diagnostics allowlisted, locally scoped, and redacted before
  sharing

Next priorities:

- No additional roadmap item is currently queued.

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
