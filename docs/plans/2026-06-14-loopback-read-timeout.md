# Loopback Integration Read Timeout

## Status: In Progress

## Context

The newly restored feature-branch push matrix exposed a Java 11 failure in
`twimlRouteRequiresPostAndReturnsXml`: the loopback client timed out after two
seconds while the handler performed cold Twilio XML initialization. The same
head passed the parallel pull-request matrix, confirming scheduling-sensitive
test harness behavior rather than an event-specific application defect.

## Priority

Keep loopback integration tests bounded while allowing cold dependency
initialization on a contended hosted runner.

## Requirements

- Define one explicit loopback timeout constant of 10 seconds.
- Use it for both connection and response reads.
- Preserve all HTTP status, header, and body assertions.
- Add a fail-closed repository contract and hostile timeout mutation.

## Verification Plan

- Run the affected TwiML route test repeatedly in fresh Maven invocations.
- Run repository and external-directory `make check`.
- Prove restoring the two-second timeout is rejected.
- Audit the exact diff, generated artifacts, credentials, and whitespace.

## Scope Boundary

This change does not retry requests, suppress timeouts, alter server behavior,
change production limits, or weaken any response assertion.
