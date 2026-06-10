#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
README="$ROOT_DIR/README.md"
MAKEFILE="$ROOT_DIR/Makefile"
GITIGNORE="$ROOT_DIR/.gitignore"
DOCS_PLANS="$ROOT_DIR/docs/plans"

require_file() {
  path=$1
  if [ ! -f "$ROOT_DIR/$path" ]; then
    printf '%s\n' "Required file is missing: $path" >&2
    exit 1
  fi
}

for path in \
  ".gitignore" \
  ".github/workflows/check.yml" \
  "CHANGES.md" \
  "Makefile" \
  "README.md" \
  "SECURITY.md" \
  "VISION.md" \
  "Procfile" \
  "pom.xml" \
  "system.properties" \
  "src/main/java/org/example/Main.java" \
  "src/test/java/org/example/DocsPlansTest.java" \
  "src/test/java/org/example/MainTest.java" \
  "docs/plans/2026-06-08-twilio-java-rc-testing-baseline.md" \
  "docs/plans/2026-06-09-scripted-baseline-check.md" \
  "docs/plans/2026-06-10-dependencies-and-ci.md" \
  "scripts/check-baseline.sh"; do
  require_file "$path"
done

if ! grep -Fq "scripts/check-baseline.sh" "$MAKEFILE"; then
  printf '%s\n' "Makefile must run scripts/check-baseline.sh from make check." >&2
  exit 1
fi

for target in "lint:" "test:" "build:" "verify:" "check:"; do
  if ! grep -Fq "$target" "$MAKEFILE"; then
    printf '%s\n' "Makefile must expose the $target gate." >&2
    exit 1
  fi
done

if ! grep -Fq "make check" "$README"; then
  printf '%s\n' "README must document the root check gate." >&2
  exit 1
fi

if ! grep -Fq "scripts/check-baseline.sh" "$README"; then
  printf '%s\n' "README must document the scripted baseline guard." >&2
  exit 1
fi

if ! grep -Fq ".idea/" "$GITIGNORE"; then
  printf '%s\n' ".gitignore must ignore the full IntelliJ .idea/ directory." >&2
  exit 1
fi

if grep -Fq ".idea/workspace.xml" "$GITIGNORE"; then
  printf '%s\n' ".gitignore should not only cover one local IntelliJ file." >&2
  exit 1
fi

tracked_editor_metadata=$(git -C "$ROOT_DIR" ls-files '.idea' '.vscode' '*.iml' || true)
if [ -n "$tracked_editor_metadata" ]; then
  printf '%s\n%s\n' "Local editor metadata must not be tracked:" "$tracked_editor_metadata" >&2
  exit 1
fi

found_plan=0
for plan in "$DOCS_PLANS"/*.md; do
  [ -e "$plan" ] || continue
  found_plan=1
  if ! grep -Fq "Status: Completed" "$plan"; then
    printf '%s\n' "$plan must record completed status." >&2
    exit 1
  fi
  if ! grep -Fq "make check" "$plan"; then
    printf '%s\n' "$plan must document make check verification." >&2
    exit 1
  fi
done

if [ "$found_plan" -eq 0 ]; then
  printf '%s\n' "docs/plans must contain completed markdown plans." >&2
  exit 1
fi
