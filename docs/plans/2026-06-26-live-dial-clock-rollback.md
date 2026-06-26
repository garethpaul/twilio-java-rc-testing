# Live Dial Clock Rollback

## Status: Completed

## Problem

The fixed-window limiter reset attempts whenever the wall clock moved backward.
An exhausted process could therefore accept additional authorized live calls
before the original 60-second window elapsed. Public guidance also still
described the obsolete pre-authorization quota ordering.

## Scope

- Preserve exhausted quota when the clock moves backward.
- Reset only after time reaches the original window expiry.
- Keep overflow-safe elapsed comparison, five-attempt allowance, `429` response,
  request-ID ledger, authorization order, dry-run behavior, and provider boundary.
- Correct documentation without claiming token-guessing protection.

## Work Completed

- Added a deterministic red-first JUnit rollback regression.
- Added a fail-closed elapsed-window helper to the synchronized limiter.
- Updated source, test, plan, and public documentation contracts.

## Verification Completed

- The focused regression failed before implementation and passed afterward.
- Full `make check` passed with Maven 3.9.11 on Java 21.
- Isolated source, test, and documentation hostile mutations were rejected.

## Boundary

This does not add distributed state, per-client identity, retries, live Twilio
calls, token logging, or a claim that the quota prevents online token guessing.
