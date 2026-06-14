# Changes

## 2026-06-14

- Expanded the canonical Java verification matrix from `main`-only pushes to
  all branch pushes, with shell and JUnit guards against branch filters.
- Documented Java 8 source compatibility, hosted Java 8/11/17/21 verification,
  Maven 3.6.3 as a reproduced local baseline, and the exact Twilio Java 12.1.1
  pin without claiming unverified minimum tool versions.

## 2026-06-13

- Rejected duplicate phone/token fields and malformed percent encoding with a
  generic `400` before dial authorization or provider configuration.
- Limited live dial requests to five attempts per process each minute before
  body parsing or token comparison, with `429` and `Retry-After` responses.
- Added deterministic window, route-ordering, response-header, and dry-run
  independence coverage for the live-attempt boundary.
- Authorized live dial requests before returning detailed Twilio credential,
  sender, or callback-origin configuration errors, while preserving
  unauthenticated dry runs.

## 2026-06-12

- Required the exact `application/x-www-form-urlencoded` media type on the
  dial route while accepting case-insensitive parameterized variants, with
  loopback and fail-closed baseline coverage for spoofed content types.

## 2026-06-10

- Converted Twilio SDK runtime failures into a generic HTTP 502 response and
  added an injectable provider-boundary regression test.
- Added centralized no-store, Content Security Policy, permissions, referrer,
  framing, and MIME-sniffing response headers with loopback integration tests.
- Added loopback regression coverage for the 8 KiB form-body limit and its
  generic HTTP 413 response.
- Fixed GitHub Actions to Ubuntu 24.04 with annotated immutable actions and
  scoped concurrency, disabled persisted checkout credentials, rejected extra
  workflow files, and made every Maven target root-independent.
- Replaced the Twilio Java 9.0.0 release candidate with stable 12.1.1 after
  verifying unchanged call and TwiML APIs across Java 8, 11, 17, and 21.
- Removed Spark and Jetty after hosted scanning found advisories affecting the
  entire Jetty 9.4 line, and preserved routes on Java's built-in HTTP server.
- Added real HTTP integration coverage and fixed packaged-JAR classpath
  resource loading for the bundled form.
- Required a separately configured, constant-time checked authorization token
  before any live dial attempt.
- Pinned Jackson 2.18.8 and Apache HttpCore 5.3.6 to resolve the remaining
  hosted Snyk findings in Twilio's transitive dependency graph.
- Added least-privilege GitHub Actions verification across Java 8, 11, 17, and 21
  with immutable action pins and Maven caching.
- Added JUnit contracts for exact dependency versions and hosted workflow
  security settings.

## 2026-06-09

- Removed unused Apache Spark Streaming, Velocity, and WebJars declarations from
  the Maven build with JUnit source-contract coverage.
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
