# AGENTS.md

## Repository purpose

`garethpaul/twilio-java-rc-testing` is a Java HTTP sample for safe dry-run or explicitly enabled live Twilio voice calls.

## Project structure

- `Makefile` - repository verification targets
- `scripts` - baseline checks and helper scripts
- `docs` - plans, notes, and generated README assets
- `src` - primary source code

## Development commands

- Install dependencies: no repository-specific install command is documented.
- Full baseline: `make check`
- Combined verification: `make verify`
- Lint/static checks: `make lint`
- Tests: `make test`
- Build: `make build`
- If a command above skips because a platform toolchain is missing, verify on a machine with that SDK before claiming platform behavior is tested.

## Coding conventions

- Language mix noted in the README: Java (1).

## Testing guidance

- Test-related files detected: `docs/plans/2026-06-08-twilio-java-rc-testing-baseline.md`, `src/main/resources/public/test.html`, `src/test/`, `src/test/java/org/example/DocsPlansTest.java`, `src/test/java/org/example/MainTest.java`
- Start with the narrowest relevant test or Make target, then run `make check` before handing off if the change is not documentation-only.
- Keep README verification notes in sync when commands, fixtures, or supported toolchains change.

## PR / change guidance

- Keep diffs focused on the requested repository and avoid unrelated modernization or formatting churn.
- Preserve public APIs, sample behavior, file formats, and documented environment variables unless the task explicitly changes them.
- Update tests, README notes, or docs/plans when behavior, security posture, or validation commands change.
- Call out skipped platform validation, legacy toolchain assumptions, and any risky files touched in the final summary.

## Safety and gotchas

- Detected references to Twilio. Keep API keys, OAuth credentials, tokens, and account-specific values in local configuration only.
- See `SECURITY.md` for vulnerability reporting and safe research guidance.
- See `VISION.md` for project direction and contribution guardrails.
- See `docs/plans/2026-06-08-twilio-java-rc-testing-baseline.md` for the canonical dry-run dialing and verification baseline.
- See `docs/plans/2026-06-08-twiml-content-type.md` for the TwiML XML response contract.
- See `docs/plans/2026-06-09-callback-url-validation.md` for HTTPS callback URL validation coverage.
- See `docs/plans/2026-06-10-dependencies-and-ci.md` for the stable SDK, built-in HTTP server, and hosted verification baseline.

## Agent workflow

1. Inspect the README, Makefile, manifests, and the files directly related to the request.
2. Make the smallest source or docs change that satisfies the task; avoid generated, vendored, or local-environment files unless required.
3. Run the narrowest useful validation first, then `make check` or the documented package/platform gate when available.
4. If a required SDK, service credential, or external runtime is unavailable, record the skipped command and why.
5. Summarize changed files, commands run, and remaining risks or follow-up validation.
