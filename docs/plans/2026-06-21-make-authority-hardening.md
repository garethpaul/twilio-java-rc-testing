# Make Authority Hardening

## Status: Completed

## Context

The portable `make check` gate protected its root but still accepted Make-syntax
Maven values, caller shells, execution-skipping flags, startup files, and
Makefile identity replacement. Later review showed direct GNU Make invocation
could also hide dry-run or ignore-errors state with pre-parse `--eval`.

## Requirements

- Preserve literal, multiword Maven command overrides.
- Reject Make-syntax commands before expansion and keep root/shell authority local.
- Add a fixed-target wrapper that clears all inherited Make control channels
  before GNU Make starts and resolves the physical repository checkout.
- Prove repository and external-directory `make check` behavior.
- Keep all Twilio credentials unset and make no live provider calls.

## Work Completed

- Bound root, shell, Maven command, flag, startup-file, and Makefile identity authority.
- Added `scripts/run-make.sh` as the trusted `check|lint` entrypoint and wired
  hosted verification to it.
- Reproduced hidden `-n` and `-i` state through `--eval`, `GNUMAKEFLAGS`,
  executable `MAKEFILES`, and earlier and later `-f` files before proving the
  wrapper excludes those paths.
- Added executable adversarial regression coverage to `make check`.
- Replaced permissive hosted-workflow YAML scanning with a closed-world SHA-256
  contract over the complete reviewed workflow bytes.
- Added mutations for Unicode-escaped, anchored, tagged, and aliased keys,
  extra pinned and local actions, a custom shell, an extra step, and an
  otherwise harmless byte change; every mutation must fail closed.
- Preserved direct Make compatibility while documenting it as caller authority.

## Verification

- Repository and external-directory wrapper checks passed without changing the
  Maven, Java, or `PATH` caller boundary.
- The canonical 844-byte workflow independently hashed to
  `2e38858f6c84fdcc9f67e5eb05081aacf4ff8e72486e5ec4b338a6eddb273571`
  with `sha256sum`, `shasum -a 256`, and `openssl dgst -sha256`.
- No Twilio credentials, provider endpoints, recipients, or live calls were used.

## Scope Boundaries

No Java behavior, dependency version, credential handling, provider request,
publishing, or deployment changed. Literal `MVN`, Java environment variables,
and `PATH` remain caller-controlled. Direct Make can execute startup files,
earlier or later caller-supplied `-f` files, and `--eval`; the wrapper is the
trusted boundary.
