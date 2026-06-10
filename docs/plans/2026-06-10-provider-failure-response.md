# Provider Failure Response

Status: Completed

## Goal

Return a stable HTTP response when Twilio rejects or fails a live call request
without exposing provider diagnostics, credentials, or request metadata.

## Implementation

- Route live call creation through a per-invocation injectable `CallSender`
  boundary without mutable global test state.
- Catch Twilio SDK runtime failures at the dialing boundary.
- Return a generic `502 Twilio call request failed.` result.
- Add a fake provider failure containing secret-like text and assert redaction.

## Verification

- `make check`
- Mutation check: removing the provider exception guard must fail the focused
  live-dial regression test.
