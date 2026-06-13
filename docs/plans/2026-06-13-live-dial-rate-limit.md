# Live Dial Rate Limit

## Status: Completed

## Context

The public `/dial-phone` route requires a shared token before a live Twilio
call, but it currently accepts unlimited authorization attempts. That permits
online token guessing and can amplify accidental or automated call attempts.

## Requirements

- R1. Limit live `/dial-phone` POST attempts before request-body parsing and
  token comparison.
- R2. Use a bounded, dependency-free, process-wide fixed window with no
  attacker-controlled identity map.
- R3. Permit at most five live attempts per 60-second window and return `429`
  with a `Retry-After: 60` response when exhausted.
- R4. Count authorized and unauthorized live attempts so the boundary also
  limits accidental or automated call volume.
- R5. Leave dry-run requests, static content, TwiML, method validation, and
  content-type validation unchanged.
- R6. Add deterministic unit and route coverage for exhaustion, reset, response
  headers, pre-body ordering, and dry-run independence.
- R7. Extend the scripted baseline and public security guidance so removing or
  reordering the limiter fails `make check`.

## Scope Boundaries

- Do not add a distributed store, proxy-header trust, dependencies, retries, or
  per-user identity.
- Do not place a live Twilio call or change the shared-token format.
- Do not claim that a per-process limit replaces provider-side spend controls.

## Implementation Units

### U1. Add the bounded live-attempt limiter

- **Files:** `src/main/java/org/example/Main.java`
- Implement a synchronized fixed window and enforce it before body parsing.

### U2. Add deterministic regression coverage

- **Files:** `src/test/java/org/example/MainTest.java`
- Cover the exact allowance, rejection, reset boundary, `Retry-After`, ordering,
  and dry-run behavior without credentials or network calls.

### U3. Preserve repository contracts and guidance

- **Files:** `scripts/check-baseline.sh`, `src/test/java/org/example/DocsPlansTest.java`,
  `README.md`, `SECURITY.md`, `VISION.md`, `CHANGES.md`
- Register the plan and document the operational and residual boundaries.

## Verification

- `mvn -q -Dtest=MainTest test` passed 35 focused tests.
- Full local, external-directory, and space-containing-path `make check` runs
  passed 40 tests, the package build, and the scripted baseline.
- Nine hostile mutations covering raised limits, a longer window, removed or
  dry-run-wide enforcement, missing retry metadata, wrong status, weakened
  reset boundaries, late body ordering, and stale plan status were rejected.
- Workflow YAML, XML, shell syntax, `git diff --check`, generated-artifact, and
  focused secret reviews are included in final validation.
