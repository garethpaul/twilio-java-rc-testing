# Form Content-Type Validation

Status: Completed

## Problem

The live-call-capable `/dial-phone` route checks the request `Content-Type`
with a raw prefix match. That accepts values which only resemble
`application/x-www-form-urlencoded`, including media types with attacker-
controlled suffixes. The route should accept the form media type itself,
optionally followed by parameters, without weakening its existing POST,
body-size, dry-run, or live-dial authorization controls.

## Plan

1. Parse the media type token before any semicolon-delimited parameters.
2. Compare the token case-insensitively with
   `application/x-www-form-urlencoded`.
3. Exercise the real loopback route with a parameterized mixed-case valid
   type and with missing, unrelated, and spoofed-prefix types.
4. Extend the scripted baseline so the route and regression test cannot be
   silently weakened.

## Verification

- Focused Maven route test passed for missing, unrelated, spoofed-prefix, and
  parameterized mixed-case content types.
- `make check` passed 35 tests plus compile, package, and scripted baseline
  gates.
- External-working-directory `make -C` check passed the same full gate.
- Three isolated negative mutations were rejected for prefix matching, case
  sensitivity, and missing spoofed-media-type coverage.
- `git diff --check` passed.
