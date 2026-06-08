# Dry-Run Dialing Gate

## Problem

The `/dial-phone` route created a live outbound Twilio call whenever complete
credentials and callback configuration were present. For a release-candidate
sample, that made local testing too easy to turn into a billable side effect.

## TDD Evidence

1. Added JUnit tests for explicit live-send opt-in, dry-run configuration, and
   dry-run/live response messages.
2. Ran `mvn -q test` before implementation and confirmed compilation failed on
   the missing dry-run helpers and configuration overload.
3. Added `TWILIO_SEND_LIVE=true` as the live-call gate, kept dry-run as the
   default route behavior, and reran the full verification gate.

## Verification

- `mvn -q test`
- `make lint`
- `make test`
- `make build`
- `make verify`
- `make check`
- `git diff --check`
