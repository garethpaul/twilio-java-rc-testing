# twilio-java-rc-testing

<!-- README-OVERVIEW-IMAGE -->
![Project overview](docs/readme-overview.svg)

## Overview

`garethpaul/twilio-java-rc-testing` is a static web project. Twilio Java RC Testing

This README is based on the checked-in source, manifests, scripts, and repository metadata on the `main` branch. The project language mix found during review was: Java (1).

## Repository Contents

- `README.md` - project overview and local usage notes
- `pom.xml`
- `Procfile`
- `SECURITY.md` - security reporting and disclosure guidance
- `src` - source or example code
- `VISION.md` - project direction and maintenance guardrails

Additional scan context:

- Source directories: src
- Dependency and build manifests: Procfile, pom.xml
- Entry points or build surfaces: none detected
- Test-looking files: src/main/resources/public/test.html

## Getting Started

### Prerequisites

- Git

### Setup

```bash
git clone https://github.com/garethpaul/twilio-java-rc-testing.git
cd twilio-java-rc-testing
```

The setup commands above are derived from repository files. Legacy mobile, Python, or JavaScript samples may require older SDKs or package versions than a modern workstation uses by default.

## Running or Using the Project

- Configure `TWILIO_PHONE_NUMBER` and `NGROK_URL` for dry-run testing.
  Configure `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, and
  `TWILIO_SEND_LIVE=true` only when intentionally placing live calls.
- Run `mvn package` and then `java -jar target/Testing1234-1.0-jar-with-dependencies.jar`.
- Open `/` and submit a valid E.164 phone number. The app rejects missing or
  malformed numbers before a dry run or live Twilio call.
- `NGROK_URL` must be a valid HTTPS URL with a host before the app builds a
  TwiML callback URL.
- The `/twiml` route returns TwiML XML with an explicit `application/xml`
  content type.
- Runtime logging defaults to `info`; switch to debug only in a local working
  copy when you are prepared to redact call metadata before sharing logs.

## Testing and Verification

- `make check`
- `mvn test`
- `mvn -DskipTests package`
- Tests keep the checked-in Log4j default at `info` rather than `debug`.
- Tests cover TwiML XML generation and the `/twiml` content type contract.
- Tests cover HTTPS callback URL validation before live-call setup.
- Completed maintenance plans live under `docs/plans` and are checked by
  `make check`.

When the required SDK or runtime is unavailable, use static checks and source review first, then verify on a machine that has the matching platform toolchain.

## Configuration and Secrets

- Detected references to Twilio. Keep API keys, OAuth credentials, tokens, and account-specific values in local configuration only.

## Security and Privacy Notes

- Review changes touching authentication or token handling; examples from the scan include src/main/resources/public/index.html.
- Review changes touching external API calls or credential-adjacent configuration; examples from the scan include pom.xml, src/main/java/org/example/Main.java.
- Review changes touching network requests, sockets, or service endpoints; examples from the scan include pom.xml, src/main/java/org/example/Main.java, src/main/resources/public/index.html.
- Review changes touching file, media, JSON, XML, CSV, OCR, or data parsing; examples from the scan include pom.xml, src/main/resources/public/index.html.

## Maintenance Notes

- See `SECURITY.md` for vulnerability reporting and safe research guidance.
- See `VISION.md` for project direction and contribution guardrails.
- See `docs/plans/2026-06-08-twilio-java-rc-testing-baseline.md` for the
  canonical dry-run dialing and verification baseline.
- See `docs/plans/2026-06-08-twiml-content-type.md` for the TwiML XML response
  contract.
- See `docs/plans/2026-06-09-callback-url-validation.md` for HTTPS callback
  URL validation coverage.

## Contributing

Keep changes small and tied to the project that is already present in this repository. For code changes, document the toolchain used, avoid committing generated dependency directories or local configuration, and update this README when setup or verification steps change.
