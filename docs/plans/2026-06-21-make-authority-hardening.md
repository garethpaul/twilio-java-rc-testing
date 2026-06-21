# Make Authority Hardening

## Status: Completed

## Context

The portable `make check` gate protected its root but still accepted Make-syntax
Maven values, caller shells, execution-skipping flags, startup files, and
Makefile identity replacement.

## Requirements

- Preserve literal, multiword Maven command overrides.
- Reject Make-syntax commands before expansion and keep root/shell authority local.
- Prove repository and external-directory `make check` behavior.
- Keep all Twilio credentials unset and make no live provider calls.

## Work Completed

- Bound root, shell, Maven command, flag, startup-file, and Makefile identity authority.
- Added executable adversarial regression coverage to `make check`.
- Preserved the documented GNU Make startup and later-`-f` trust boundary.

## Verification

- Sanitized repository and external-directory `make check` passed using public
  Maven Central with live dialing disabled.
- No Twilio credentials, provider endpoints, recipients, or live calls were used.

## Scope Boundaries

No Java behavior, dependency version, credential handling, provider request,
workflow, publishing, or deployment changed. GNU Make startup files can execute
during parsing, and later caller-supplied `-f` files remain outside authority.
