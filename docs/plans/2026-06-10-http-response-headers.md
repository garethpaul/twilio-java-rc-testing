# HTTP Response Security Headers

Status: Completed

## Problem

The built-in HTTP server set the response content type and disabled MIME
sniffing, but the live-dial form and its token-bearing submissions could still
be cached, framed, or loaded without a restrictive browser policy. Hosted CI
also used a floating Ubuntu runner and Make targets depended on the caller's
working directory.

## Plan

1. Apply security headers centrally so HTML, TwiML, errors, and dial responses
   share the same policy.
2. Disable response caching and framing.
3. Suppress referrer data and unused camera, geolocation, and microphone
   capabilities.
4. Add a Content Security Policy limited to same-origin forms and the existing
   integrity-pinned Bootstrap stylesheet.
5. Verify the headers over the real loopback HTTP server.
6. Fix CI to Ubuntu 24.04 with annotated action pins and make Maven commands
   independent of the current directory.

## Verification

- `make check`
- `make -f /path/to/twilio-java-rc-testing/Makefile check` outside the repo
- Packaged JAR HTTP smoke for root, TwiML, dry-run dial, and method rejection
- Header, runner, action annotation, and Makefile mutations rejected by tests
- `git diff --check`
