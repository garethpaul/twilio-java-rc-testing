# Strict Dial Form Parsing

## Status: Planned

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
