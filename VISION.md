## Twilio Java RC Testing Vision

Twilio Java RC Testing is a Java/Spark sample for testing a Twilio Java release
candidate by serving TwiML and placing outbound calls through configured Twilio
credentials.

The repository is useful as a focused release-candidate exercise: it shows
environment-based Twilio setup, a local web server, an ngrok callback URL,
TwiML generation, and outbound call creation.

The goal is to keep the RC test path explicit, reproducible, and safe for live
Twilio accounts.

The current focus is:

Priority:

- Preserve the Spark server and `/twiml` plus `/dial-phone` flow
- Keep account credentials and phone numbers in environment variables
- Maintain `make check` and Maven packaging for a runnable jar
- Treat outbound call creation as a live side effect
- Validate phone numbers and required callback configuration before dialing
- Keep outbound calls in dry-run mode unless explicitly enabled

Next priorities:

- Add integration-test coverage around Spark routes
- Add a mock Twilio client path for local tests
- Document supported Java, Maven, and Twilio SDK versions

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
- Live-call defaults without operator confirmation
- Dependency changes that obscure the RC test purpose

This list is a roadmap guardrail, not a permanent rule.
Strong user demand and strong technical rationale can change it.
