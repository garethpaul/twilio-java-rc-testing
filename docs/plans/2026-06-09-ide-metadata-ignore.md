# IDE Metadata Ignore

## Status: Completed

## Context

The repository still tracked IntelliJ `.idea` project files while only ignoring
a few specific local IDEA artifacts. Those files capture local JDK and editor
state that should not be part of a portable Twilio Java sample.

## Objectives

- Remove checked-in IntelliJ project metadata.
- Ignore the full `.idea/` directory for future local editor state.
- Preserve Maven, Spark, Twilio, and documentation behavior.
- Add a JUnit contract so the broad IDE ignore rule stays in place.

## Work Completed

- Replaced narrow IntelliJ ignore entries with a `.idea/` directory rule.
- Removed tracked `.idea/misc.xml` and `.idea/vcs.xml`.
- Added docs-plan and `.gitignore` coverage to `DocsPlansTest`.
- Updated README, VISION, and CHANGES.

## Verification

- `mvn test`
- `make check`
- `git diff --check`

## Follow-Up Candidates

- Add a Maven wrapper if this repository needs pinned Maven execution.
- Review whether legacy generated files should be regenerated or documented in
  a dedicated build reproducibility pass.
