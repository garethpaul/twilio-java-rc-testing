# Live Dial At-Most-Once Review

Status: Completed

## Context

The stacked pull requests authorized live calls and added a global rate limit,
but the quota was consumed before authorization and Twilio Java 12.1.1 retried
call-creation failures. A lost response or provider `5xx` could therefore make
multiple outbound POST attempts, while duplicate browser submissions had no
request identity.

## Work Completed

- Moved quota acquisition after strict form parsing, token authorization, and
  provider configuration validation.
- Added a server-rendered request ID, strict duplicate-field handling, and an
  atomic bounded process-local ledger that records an accepted ID before the
  provider boundary.
- Disabled both Twilio helper retries and Apache automatic retries, added five
  second connect and ten second response timeouts, and reused one lazy client.
- Required exact E.164 input, validated Account SID shape, and kept provider
  exceptions and credentials out of responses and logs.
- Added fake-provider, loopback, hostile form, duplicate-outcome, transport,
  dependency, workflow, and source-contract coverage. No live Twilio call was
  made.

## Verification

- The repository and external-directory `make check` passed.
- The JUnit suite passed on local Java 11 and is hosted on Java 8, 11, 17, and 21.
- OSV reported no known dependency vulnerabilities.
- Redacted current-tree and full-history credential scans found no credentials.
- Hostile mutations for authorization order, duplicate IDs, SDK retries, Apache
  retries, request-ID enforcement, Account SID validation, and exact E.164 input
  were rejected.

## Residual Risk

Twilio's Call creation API does not expose provider-side idempotency for this
operation. The ledger is process-local and bounded, so the implementation can
prove one provider attempt per accepted request ID only within the running
process. After a timeout or `502`, inspect Twilio call records before explicitly
using a new request ID. Process restarts and ledger eviction remain outside this
sample's at-most-once guarantee.
