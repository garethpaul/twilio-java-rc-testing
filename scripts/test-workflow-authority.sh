#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd -P)
WORKFLOW=$ROOT_DIR/.github/workflows/check.yml
BASELINE=$ROOT_DIR/scripts/check-baseline.sh
EXPECTED_SHA256=2e38858f6c84fdcc9f67e5eb05081aacf4ff8e72486e5ec4b338a6eddb273571
TEMP_ROOT=$(mktemp -d "${TMPDIR:-/tmp}/twilio-java-workflow-authority-XXXXXX")
ORIGINAL=$TEMP_ROOT/check.yml

cp "$WORKFLOW" "$ORIGINAL"
restore_workflow() {
  cp "$ORIGINAL" "$WORKFLOW"
}
trap 'restore_workflow; rm -rf "$TEMP_ROOT"' EXIT HUP INT TERM

actual_sha256=$(sha256sum "$ORIGINAL" | awk '{print $1}')
if [ "$actual_sha256" != "$EXPECTED_SHA256" ]; then
  printf '%s\n' "Canonical workflow fixture hash mismatch: $actual_sha256" >&2
  exit 1
fi

assert_rejected() {
  mutation=$1
  restore_workflow
  case $mutation in
    unicode-escaped-run-key)
      awk '/run: \.\/scripts\/run-make\.sh check/ { print "        \"r\\u0075n\": echo bypass" } { print }' "$ORIGINAL" >"$WORKFLOW"
      ;;
    anchored-key)
      awk '/run: \.\/scripts\/run-make\.sh check/ { print "        &command run: echo anchored" } { print }' "$ORIGINAL" >"$WORKFLOW"
      ;;
    tagged-key)
      awk '/run: \.\/scripts\/run-make\.sh check/ { print "        !<tag:yaml.org,2002:str> run: echo tagged" } { print }' "$ORIGINAL" >"$WORKFLOW"
      ;;
    aliased-key)
      awk '/run: \.\/scripts\/run-make\.sh check/ { print "        *command: echo aliased" } { print }' "$ORIGINAL" >"$WORKFLOW"
      ;;
    extra-pinned-action)
      awk '/- name: Run repository verification/ { print "      - uses: actions/cache@6849a6489940f00c2f30c0fb92c6274307ccb58a" } { print }' "$ORIGINAL" >"$WORKFLOW"
      ;;
    extra-local-action)
      awk '/- name: Run repository verification/ { print "      - uses: ./.github/actions/local-check" } { print }' "$ORIGINAL" >"$WORKFLOW"
      ;;
    custom-shell)
      awk '{ print } /run: \.\/scripts\/run-make\.sh check/ { print "        shell: python" }' "$ORIGINAL" >"$WORKFLOW"
      ;;
    extra-step)
      awk '/- name: Run repository verification/ { print "      - name: Unreviewed extra step" } { print }' "$ORIGINAL" >"$WORKFLOW"
      ;;
    byte-change)
      cat "$ORIGINAL" >"$WORKFLOW"
      printf '%s\n' '# byte change' >>"$WORKFLOW"
      ;;
    *)
      printf '%s\n' "Unknown workflow mutation: $mutation" >&2
      exit 1
      ;;
  esac

  mutated_sha256=$(sha256sum "$WORKFLOW" | awk '{print $1}')
  if [ "$mutated_sha256" = "$EXPECTED_SHA256" ]; then
    printf '%s\n' "Workflow mutation did not change canonical bytes: $mutation" >&2
    exit 1
  fi

  if "$BASELINE" >"$TEMP_ROOT/$mutation.out" 2>"$TEMP_ROOT/$mutation.err"; then
    printf '%s\n' "Workflow mutation was accepted: $mutation" >&2
    exit 1
  fi
  if ! grep -Fq 'Hosted workflow bytes do not match the reviewed canonical contract.' \
    "$TEMP_ROOT/$mutation.err"; then
    printf '%s\n' "Workflow mutation failed outside the exact-byte contract: $mutation" >&2
    exit 1
  fi
}

for mutation in \
  unicode-escaped-run-key \
  anchored-key \
  tagged-key \
  aliased-key \
  extra-pinned-action \
  extra-local-action \
  custom-shell \
  extra-step \
  byte-change; do
  assert_rejected "$mutation"
done

restore_workflow
"$BASELINE"
printf '%s\n' 'Workflow authority tests passed: 9 byte-distinct mutations rejected by the exact hash contract'
