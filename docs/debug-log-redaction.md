# Debug Log Redaction

The checked-in application keeps Log4j at `info`, installs an inert SLF4J
binding for dependency logging, and does not emit request or provider
diagnostics. Keep that default. This guide applies when an operator temporarily
adds local diagnostics, enables dependency logging, or collects reverse-proxy
or platform logs while troubleshooting.

Twilio recommends treating credentials like passwords and keeping them outside
source code. Phone numbers and account identifiers are also unnecessary for
most debugging and can be personal or account-linked data. Start in dry-run
mode whenever the failure can be reproduced without placing a call.

## Never log or share

- `TWILIO_AUTH_TOKEN`, `TWILIO_DIAL_TOKEN`, authorization headers, cookies, or
  any other secret. Replace the entire value with `[redacted-secret]`; retaining
  a prefix or suffix is not useful for diagnosis.
- Raw request bodies, form bodies, query strings, or complete HTTP request and
  response dumps. These can contain tokens, phone numbers, callback URLs, and
  future fields not anticipated by a filter.
- Full source or destination phone numbers. If a case truly requires matching
  one number across sanitized lines, preserve only the final four characters,
  matching the application's response redaction: `****1234`.
- Full Account SIDs, API key SIDs, Call SIDs, request IDs, or other provider and
  correlation identifiers. Use a type label such as `[redacted-call-sid]`.
- The configured ngrok or callback origin, URL user information, URL paths,
  fragments, or query parameters. Use `[redacted-callback-origin]`.
- Provider response bodies or exception messages and stack traces until every
  line has been reviewed. Record a stable local category such as
  `provider-call-failed` instead of an exception message.
- Environment dumps, command histories, process listings, heap dumps, or
  screenshots that may contain the configured environment variables.

## Safe diagnostic fields

Prefer a small allowlist rather than trying to remove sensitive fields from a
large object. Fixed event names, route names, HTTP status codes, dry-run versus
live mode, elapsed time, and generic outcomes are normally sufficient. Counts
and rate-limit decisions are safe when they cannot identify a caller.

If local correlation is essential, generate a new case label that is unrelated
to the application's request ID or Twilio identifiers. Do not publish the
mapping. Avoid logging Java objects from the Twilio SDK or HTTP client because
their string representations are not a stable redaction boundary.

Example sanitized record:

```text
event=dial-attempt mode=dry-run outcome=invalid-target status=400 case=local-2
```

## Before sharing a log

1. Reproduce in dry-run mode and keep the checked-in `info` default whenever
   possible. Capture only the shortest useful interval.
2. Copy only the relevant lines into a new text file. Never share the original
   log, an archive of the log directory, or a complete platform export.
3. Replace secrets and identifiers using the rules above, then search the copy
   for the literal configured environment values.
4. Search for Twilio SID shapes such as `AC` or `CA` followed by 32 hexadecimal
   characters, E.164-like values beginning with `+`, `Authorization`, `token`,
   `password`, form field names, URL query delimiters, and multiline stack
   traces. Pattern searches are a backstop, not permission to share a match.
5. Ask a second person to review the sanitized copy when it came from a live
   request or production-like environment.
6. Delete temporary debug changes and sanitized working files when the issue is
   closed. Return logging to the repository default before committing.

Rotate credentials immediately if a secret may have appeared in a log,
screenshot, issue, chat, or artifact. Rotate `TWILIO_AUTH_TOKEN` through the
Twilio Console and replace `TWILIO_DIAL_TOKEN` everywhere it is configured.
Treat uncertain exposure as exposure; deleting a public post does not revoke a
credential.

## References

- [Secure your Twilio credentials](https://www.twilio.com/docs/usage/secure-credentials)
- [Twilio security guidance](https://www.twilio.com/docs/usage/security)
- [Twilio Auth Tokens and how to change them](https://help.twilio.com/articles/223136027)

