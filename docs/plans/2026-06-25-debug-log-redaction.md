# Debug Log Redaction Guidance

Status: Completed

## Context

The repository already keeps checked-in logging at `info`, prevents provider
failure details from reaching HTTP responses, redacts dial targets in response
messages, and uses an inert SLF4J binding. The remaining roadmap item was to
make safe local troubleshooting and log sharing explicit.

## Scope

- Document a fail-closed list of credentials, request data, phone numbers,
  Twilio identifiers, callback origins, and exception details that must not be
  logged or shared.
- Prefer an allowlist of generic diagnostic fields over object or payload dumps.
- Define a review checklist and credential-rotation response for possible
  exposure.
- Keep the guide indexed from operator and security documentation.
- Add executable repository contracts so the guidance cannot silently vanish.

## Completed work

- Added `docs/debug-log-redaction.md` with safe-field examples, forbidden data,
  a pre-sharing checklist, and incident response guidance.
- Linked the guide from `README.md` and `SECURITY.md`.
- Closed the roadmap item in `VISION.md` and recorded the cycle in `CHANGES.md`.
- Added JUnit and shell baseline contracts for the guide, its plan, and its
  security-critical topics.

## Verification

- `scripts/check-baseline.sh` fails when the guide, plan, index links, or
  required redaction topics are absent.
- `make check` verifies the complete Maven and repository baseline.

