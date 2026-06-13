---
title: "fix: Authorize live dial requests before configuration disclosure"
type: fix
date: 2026-06-13
---

# Authorize Live Dial Requests Before Configuration Disclosure

## Status: Completed

## Context

The live dial path currently validates Twilio credentials, the configured
sender, and the callback origin before it checks the separately configured dial
authorization token. An unauthenticated caller can therefore distinguish
missing or malformed provider configuration from a valid setup.

## Requirements

- R1. In live mode, require the configured dial token and authorize the
  provided token before returning Twilio credential, sender, or callback-origin
  validation details.
- R2. Preserve the existing `503` response when the server-side dial token is
  missing and the existing `403` response for an incorrect provided token.
- R3. Preserve credential-free, unauthenticated dry-run behavior.
- R4. Continue validating all provider configuration before constructing or
  invoking a Twilio client for an authorized live request.
- R5. Add direct and loopback coverage proving unauthorized live requests do
  not disclose provider configuration or invoke the call sender.
- R6. Add a static ordering contract and hostile mutations for authorization
  removal or movement after provider validation.

## Scope Boundaries

This change only reorders validation within the existing live dial path. It
does not change token comparison, add sessions, rate limiting, credential-shape
validation, or make a live Twilio request.

## Implementation Units

### U1. Reorder the Live Authorization Boundary

- **Goal:** Authenticate a live dial request before detailed provider setup is
  evaluated.
- **Files:** `src/main/java/org/example/Main.java`
- **Approach:** Determine live mode after target validation, enforce the
  configured and provided dial tokens, then evaluate Twilio call configuration.
- **Verification:** Dry runs remain unauthenticated; authorized live requests
  still validate provider setup before the call sender runs.

### U2. Add Disclosure and Ordering Coverage

- **Goal:** Prevent validation order from regressing.
- **Files:** `src/test/java/org/example/MainTest.java`,
  `scripts/check-baseline.sh`
- **Approach:** Add direct and loopback cases with deliberately missing
  provider configuration plus a static source-order contract.
- **Verification:** Tests assert `403` without provider-setting names and zero
  sender invocations; hostile mutations must fail.

### U3. Document the Live Request Boundary

- **Goal:** State which errors are available before and after authorization.
- **Files:** `README.md`, `SECURITY.md`, `VISION.md`, `CHANGES.md`, this plan.
- **Approach:** Document authentication-first live behavior without implying
  that the dial token protects dry-run requests.
- **Verification:** The completed plan records actual commands and results.

## Risks

- Accidentally requiring authorization for dry runs would break the safe local
  sample workflow.
- Moving all validation before target checks could alter existing malformed
  phone-number behavior.
- Authorized callers must still receive actionable provider configuration
  errors before any Twilio client is invoked.

## Verification

- `mvn -q -Dtest=MainTest test`: passed 32 focused unit and loopback tests.
- `/tmp/engineering-bar/mutate-twilio-java-auth-order.sh`: rejected five
  removed-authorization, dry-run overreach, wrong-token acceptance,
  configuration-first, and disclosure mutations.
- `git diff --check`: passed.
- `make check`: passed compilation, 37 tests, package assembly, and the
  scripted baseline.
- `make -C /tmp/engineering-bar/twilio-java-auth-order-external/repo check`:
  passed the same full gate from an external temporary repository path.
