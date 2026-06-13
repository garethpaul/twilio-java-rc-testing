# Strict Dial Form Parsing

## Status: Completed

## Context

The dial route parses `number` and `dialToken` independently and returns the
first matching form value. Duplicate security-relevant fields are therefore
ambiguous, while malformed percent encoding collapses into a missing value
instead of being identified as an invalid form submission.

## Priority

Parse the bounded form once and reject duplicate or malformed relevant fields
before authorization, provider configuration, or call setup. This addresses
the roadmap's malformed-form integration gap without broadening the endpoint.

## Requirements

- R1. Parse `number` and `dialToken` from one bounded UTF-8 form body pass.
- R2. Reject duplicate `number` fields with HTTP 400.
- R3. Reject duplicate `dialToken` fields with HTTP 400.
- R4. Reject malformed percent encoding in a relevant field name or value with
  HTTP 400.
- R5. Keep unknown form fields ignored so ordinary browser submissions remain
  compatible.
- R6. Perform strict parsing before dial authorization, provider configuration,
  or Twilio client setup while preserving the existing pre-body rate limit.
- R7. Preserve method, media type, body-size, E.164, callback, authorization,
  provider-error, response-header, and dry-run safeguards.
- R8. Add route-level tests, portable contracts, hostile mutations, and
  completed maintenance documentation.

## Implementation Units

### Parse relevant form fields strictly

**Files:** `src/main/java/org/example/Main.java`

Replace repeated first-match extraction with a small parsed result that tracks
the two accepted fields. Decode names before classification, decode relevant
values exactly once, and throw a dedicated invalid-form exception for malformed
encoding or duplicate accepted fields.

### Exercise route behavior

**Files:** `src/test/java/org/example/MainTest.java`

Add integration cases for duplicate phone numbers, duplicate dial tokens,
malformed relevant names and values, accepted unknown fields, uniform response
text, and no provider invocation.

### Preserve contracts and records

**Files:** `scripts/check-baseline.sh`, `README.md`, `SECURITY.md`, `VISION.md`,
`CHANGES.md`, `docs/plans/2026-06-13-strict-dial-form-parsing.md`

Require single-pass parsing, both duplicate guards, malformed-encoding failure,
route ordering, tests, docs, and completed verification evidence.

## Verification Plan

- focused strict-form and authorization route tests
- full Maven `make check` and external-working-directory gate under hard
  timeouts
- hostile mutations for each duplicate guard, malformed name/value handling,
  parser ordering, unknown-field compatibility, provider-call prevention,
  documentation, and plan status
- Maven dependency consistency/audit baseline, XML/shell/Java syntax,
  intended-path, generated-artifact, whitespace, and changed-line secret audits

## Scope Boundaries

- Do not change rate-limit thresholds, parse the body before rate limiting, or
  alter authorization and provider-configuration response precedence.
- Do not reject unknown fields, add multipart/JSON support, change dependencies,
  or make live Twilio calls.

## Work Completed

- Replaced independent first-match extraction with one bounded `DialForm`
  parsing pass for `number` and `dialToken`.
- Rejected duplicate relevant fields, malformed relevant field names or values,
  and missing separators with a generic HTTP 400 response.
- Preserved unknown-field compatibility and the existing pre-body live rate
  limit, then passed the parsed values into the unchanged authorization and
  provider configuration path.
- Added loopback integration coverage plus fail-closed source, ordering,
  documentation, and completed-plan contracts.

## Verification

- The focused `MainTest` class passed 37 tests, including duplicate number,
  duplicate token, malformed name/value, generic response, live configuration
  nondisclosure, and accepted unknown-field cases.
- Full repository and external-working-directory `make check` passed under
  explicit hard timeouts, including compilation, all JUnit tests, packaging,
  dependency/security baseline checks, and the executable jar assembly.
- Focused hostile mutations for each duplicate guard, malformed decoding,
  parser and rate-limit ordering, unknown-field compatibility, response text,
  route tests, documentation, and plan status were rejected.
- Maven XML, workflow YAML, shell syntax, Java compilation, intended-path,
  generated-artifact, `git diff --check`, and changed-line secret audits passed
  before shipment.
- No Twilio credentials, live calls, or external callback requests were used.
