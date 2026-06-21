# twilio-java-rc-testing

<!-- README-OVERVIEW-IMAGE -->
![Project overview](docs/readme-overview.svg)

## Overview

`garethpaul/twilio-java-rc-testing` is a Java HTTP sample for safe dry-run or explicitly enabled live Twilio voice calls.

This README is based on the checked-in source, manifests, scripts, and repository metadata on the `main` branch. The project language mix found during review was: Java (1).

## Repository Contents

- `README.md` - project overview and local usage notes
- `.github/workflows/check.yml` - hosted Java 8, 11, 17, and 21 verification
- `pom.xml`
- `Procfile`
- `scripts/check-baseline.sh` - repository maintenance baseline guard
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
- A JDK from the verified Java 8, 11, 17, or 21 runtime set. The project
  compiles Java 8 source and target bytecode.
- Maven. Maven 3.6.3 is the reproduced local baseline. This does not claim a
  minimum supported Maven release.

### Supported Versions

- Java source and target: 8
- Verified Java runtimes: 8, 11, 17, and 21
- Reproduced local Maven baseline: 3.6.3
- Twilio Java SDK: exactly 12.1.1

`pom.xml` is the source of truth for Java compilation and dependency versions.
`.github/workflows/check.yml` is the source of truth for the hosted runtime
matrix. A version not listed above is unverified rather than necessarily
incompatible.

### Setup

```bash
git clone https://github.com/garethpaul/twilio-java-rc-testing.git
cd twilio-java-rc-testing
```

The setup commands above are derived from repository files. Legacy mobile, Python, or JavaScript samples may require older SDKs or package versions than a modern workstation uses by default.

## Running or Using the Project

- Configure `TWILIO_PHONE_NUMBER` and `NGROK_URL` for dry-run testing.
  Configure `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, and
  `TWILIO_SEND_LIVE=true` only when intentionally placing live calls. Live
  dialing also requires a strong `TWILIO_DIAL_TOKEN`; enter that value in the
  form for each authorized live request. `TWILIO_ACCOUNT_SID` must use the
  canonical `AC` plus 32 hexadecimal characters shape.
- Maven resolves the stable Twilio Java 12.1.1 SDK. HTTP routes use Java's
  built-in server, so the sample does not depend on vulnerable Spark/Jetty 9.4.
- Dependency management keeps Twilio's Java-8-compatible Jackson line at
  2.18.8 and Apache HttpCore at 5.3.6 to include hosted-scanner fixes.
- Run `mvn package` and then `java -jar target/Testing1234-1.0-jar-with-dependencies.jar`.
- The server uses `PORT` when it is a valid positive port number and otherwise
  falls back to `4567` for local runs.
- Open `/` and submit a valid E.164 phone number. The form posts to
  `/dial-phone`; the app rejects missing or malformed numbers before a dry run
  or live Twilio call, response messages redact dial targets, and invalid
  dial-target errors refer to the form submission rather than URL input. The
  checked-in form marks the phone number field as required before submission.
- Twilio SDK failures return a generic `502` response without exposing provider
  diagnostics, credentials, or request metadata.
- Each rendered form carries a fresh request ID. The server atomically rejects
  a repeated live request ID before provider access, retains accepted IDs in a
  bounded process-local ledger, and does not retry Twilio call-creation POSTs.
  Connect and response timeouts are five and ten seconds respectively.
- Duplicate `number`, `dialToken`, or `requestId` fields, malformed pairs, and malformed percent encoding are
  rejected with a generic `400` before authorization or provider setup; unknown
  form fields remain ignored for browser compatibility.
- `NGROK_URL` must be a valid HTTPS origin URL with a host, without path,
  query, fragment, or userinfo, before the app builds a TwiML callback URL.
- The `/twiml` route returns TwiML XML with an explicit `application/xml`
  content type.
- Every HTTP response disables caching and framing, suppresses referrers and
  unused browser capabilities, and applies a restrictive Content Security
  Policy that permits only the existing Bootstrap stylesheet origin.
- Runtime logging defaults to `info`; switch to debug only in a local working
  copy when you are prepared to redact call metadata before sharing logs.

## Testing and Verification

- `scripts/run-make.sh check`
- `make check`
- `scripts/check-baseline.sh`
- The canonical wrapper accepts exactly `check` or `lint`, resolves its physical
  checkout through a bounded symbolic-link chain, clears `MAKEFILES`,
  `MAKEFLAGS`, `MFLAGS`, `MAKEOVERRIDES`, and `GNUMAKEFLAGS`, and invokes the
  physical repository Makefile with fixed system tools. Literal `MVN` values,
  Java environment variables, and executable lookup through `PATH` remain
  caller-controlled so supported local Maven and JDK setups keep working.
- Direct `make` invocation remains a caller-authority boundary: GNU Make can
  execute startup files, earlier or later `-f` files, and `--eval` before or
  outside repository recipes. Use the canonical wrapper for trusted checks.
- `mvn test`
- GitHub Actions runs `scripts/run-make.sh check` for all branch pushes, pull
  requests, and manual dispatches on Java 8, 11, 17, and 21 with read-only repository
  permissions, non-persisted checkout credentials, Ubuntu 24.04, and immutable
  action pins. The baseline rejects branch-filtered pushes and additional
  workflow files.
- `mvn -DskipTests package`
- The baseline script checks required project files, completed docs-plan
  metadata, and local editor metadata hygiene.
- Tests keep the checked-in Log4j default at `info` rather than `debug`.
- Tests cover TwiML XML generation and the `/twiml` content type contract.
- Tests cover HTTPS origin callback URL validation before live-call setup.
- Tests cover safe `PORT` parsing before the built-in HTTP server starts.
- Tests cover dial-target redaction in response messages.
- Tests require a constant-time authorization-token match before live dialing.
  Live requests check that token before returning detailed Twilio credential,
  sender, or callback-origin configuration errors; dry runs remain
  unauthenticated and credential-free.
- Tests limit the process to five live dial attempts per minute before form
  parsing or token comparison. Exhausted requests return `429` with
  `Retry-After: 60`; dry runs are not rate-limited.
- Tests require provider failures to return a generic `502` without leaking
  exception details.
- Tests require oversized dial forms to return `413` before parsing or dialing.
- Tests require one-pass dial-form parsing to reject duplicate relevant fields
  and malformed percent encoding before authorization or provider setup.
- Tests require `/dial-phone` to accept the exact form media type, including
  case-insensitive parameterized variants, while rejecting missing, unrelated,
  and spoofed-prefix content types with `415`.
- Tests keep the live-call-capable `/dial-phone` endpoint and form submission
  on POST rather than GET.
- Tests keep invalid dial-target errors and required phone input aligned with
  the POST form flow.
- Tests keep local IntelliJ `.idea/` metadata ignored and out of the portable
  sample.
- Tests keep legacy Spark, Jetty, Velocity, and WebJars declarations out of the
  Maven build.
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
- See `docs/plans/2026-06-09-port-parsing.md` for safe assigned-port parsing
  coverage.
- See `docs/plans/2026-06-09-callback-origin-validation.md` for callback
  origin validation coverage.
- See `docs/plans/2026-06-09-dial-target-redaction.md` for dial response
  redaction coverage.
- See `docs/plans/2026-06-09-post-dial-route.md` for POST-only dial route
  coverage.
- See `docs/plans/2026-06-09-post-invalid-dial-target.md` for POST-aligned
  invalid dial-target error coverage.
- See `docs/plans/2026-06-09-ide-metadata-ignore.md` for local IDE metadata
  ignore coverage.
- See `docs/plans/2026-06-09-scripted-baseline-check.md` for the scripted
  repository baseline guard.
- See `docs/plans/2026-06-09-unused-legacy-dependencies.md` for unused Maven
  dependency cleanup coverage.
- See `docs/plans/2026-06-10-dependencies-and-ci.md` for stable dependency and
  hosted Java matrix verification.
- See `docs/plans/2026-06-13-live-dial-authorization-order.md` for the
  authentication-first live request boundary.
- See `docs/plans/2026-06-14-supported-toolchain-versions.md` for the Java,
  Maven, and Twilio SDK support boundary.
- See `docs/plans/2026-06-21-make-authority-hardening.md` for the canonical
  wrapper, Maven command, shell, flag, startup-file, and Makefile authority.

## Contributing

Keep changes small and tied to the project that is already present in this repository. For code changes, document the toolchain used, avoid committing generated dependency directories or local configuration, and update this README when setup or verification steps change.
