# Stable Dependencies and Hosted Verification

Status: Completed

## Problem

The sample still used Twilio Java `9.0.0-rc.1`, Spark `2.9.3`, and had no
hosted verification. The application already had a strong dry-run default and
27 unit/contract tests, but dependency or JDK compatibility regressions could
reach the main branch without running them.

## Plan

1. Replace the Twilio release candidate with the current stable official SDK
   after proving the existing call and TwiML APIs remain source-compatible.
2. Remove Spark and Jetty after hosted scanning confirmed that every Jetty 9.4
   release remains affected by request-smuggling and URI parsing advisories.
3. Preserve the same static, TwiML, and dial routes on Java's built-in HTTP
   server with explicit method, content-type, body-size, and response headers.
4. Pin Twilio's Jackson and Apache HttpCore transitives to the compatible fixed
   releases identified by hosted Snyk scanning.
5. Add least-privilege GitHub Actions verification on Java 8 and Java 11 with
   immutable action pins and a bounded runtime.
6. Add JUnit repository contracts for exact dependency versions and workflow
   security/compatibility settings.

## Verification

- `make check` on Java 8
- `make check` on Java 11
- Packaged JAR HTTP smoke for root, method rejection, dry-run dialing, and TwiML
- Full resolved-graph OSV query with no remaining Spark or Jetty dependency
- Snyk fixed-version verification for Jackson 2.18.7 and HttpCore 5.3.5
- Negative workflow-permission and stale-dependency mutations rejected by tests
- `git diff --check`
