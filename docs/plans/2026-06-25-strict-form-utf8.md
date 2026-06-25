# Strict Form UTF-8 Decoding

## Status: Completed

## Problem

The bounded dial form body and percent-decoded components used Java's
replacement-character UTF-8 behavior. Malformed raw or percent-encoded bytes
in an unknown field could therefore be ignored and a valid dry-run request
could still return HTTP 200.

## Design

- Decode the complete bounded body with a reporting UTF-8 decoder before form tokenization.
- Return the existing generic invalid-form HTTP 400 for malformed or unmappable bytes.
- Keep unknown well-formed fields ignored for browser compatibility.
- Preserve body-size, media-type, authorization, configuration, rate-limit, and provider boundaries.

## Test-First Evidence

- RED: malformed raw and percent-encoded UTF-8 in an ignored field returned HTTP 200 and a dry-run response.
- GREEN: both requests return the generic HTTP 400 before field filtering.

## Verification

- The focused loopback integration test passed after the fix.
- Full repository and external-directory `make check` passed with 53 tests.
- A hostile mutation restoring replacement-character decoding was rejected by the canonical baseline.
- No Twilio credentials were loaded and no live call was made.
