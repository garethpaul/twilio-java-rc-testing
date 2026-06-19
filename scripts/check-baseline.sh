#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
README="$ROOT_DIR/README.md"
MAKEFILE="$ROOT_DIR/Makefile"
GITIGNORE="$ROOT_DIR/.gitignore"
DOCS_PLANS="$ROOT_DIR/docs/plans"
WORKFLOW="$ROOT_DIR/.github/workflows/check.yml"

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
  "docs/plans/2026-06-10-http-response-headers.md" \
  "docs/plans/2026-06-10-provider-failure-response.md" \
  "docs/plans/2026-06-13-live-dial-authorization-order.md" \
  "docs/plans/2026-06-13-live-dial-rate-limit.md" \
  "docs/plans/2026-06-13-strict-dial-form-parsing.md" \
  "docs/plans/2026-06-14-all-branch-push-coverage.md" \
  "docs/plans/2026-06-14-loopback-read-timeout.md" \
  "docs/plans/2026-06-14-make-root-override-protection.md" \
  "docs/plans/2026-06-14-supported-toolchain-versions.md" \
  "docs/plans/2026-06-19-live-dial-at-most-once.md" \
  "scripts/check-baseline.sh"; do
  require_file "$path"
done

for loopback_timeout_contract in \
  'private static final int LOOPBACK_TIMEOUT_MILLIS = 10_000;' \
  'connection.setConnectTimeout(LOOPBACK_TIMEOUT_MILLIS);' \
  'connection.setReadTimeout(LOOPBACK_TIMEOUT_MILLIS);'; do
  if ! grep -Fq -- "$loopback_timeout_contract" "$ROOT_DIR/src/test/java/org/example/MainTest.java"; then
    printf '%s\n' "Loopback integration timeout contract is missing: $loopback_timeout_contract" >&2
    exit 1
  fi
done

for supported_version_contract in \
  'Java source and target: 8' \
  'Verified Java runtimes: 8, 11, 17, and 21' \
  'Reproduced local Maven baseline: 3.6.3' \
  'Twilio Java SDK: exactly 12.1.1' \
  'minimum supported Maven release' \
  'docs/plans/2026-06-14-supported-toolchain-versions.md'; do
  if ! grep -Fq -- "$supported_version_contract" "$README"; then
    printf '%s\n' "README supported-version contract is missing: $supported_version_contract" >&2
    exit 1
  fi
done

if ! grep -Fq '<java.version>1.8</java.version>' "$ROOT_DIR/pom.xml"; then
  printf '%s\n' "pom.xml must preserve Java 8 source compatibility." >&2
  exit 1
fi

if ! grep -Fq '<version>12.1.1</version>' "$ROOT_DIR/pom.xml"; then
  printf '%s\n' "pom.xml must preserve the exact Twilio Java 12.1.1 pin." >&2
  exit 1
fi

if ! grep -Fq 'java-version: ["8", "11", "17", "21"]' "$WORKFLOW"; then
  printf '%s\n' "Hosted verification must preserve Java 8, 11, 17, and 21." >&2
  exit 1
fi

for support_evidence in \
  "$ROOT_DIR/CHANGES.md:Maven 3.6.3" \
  "$ROOT_DIR/VISION.md:Keep Java, Maven, and Twilio SDK support claims" \
  "$ROOT_DIR/src/test/java/org/example/DocsPlansTest.java:supportedVersionsMatchExecutableContracts"; do
  support_file=${support_evidence%%:*}
  support_contract=${support_evidence#*:}
  if ! grep -Fq -- "$support_contract" "$support_file"; then
    printf '%s\n' "$support_file is missing supported-version evidence: $support_contract" >&2
    exit 1
  fi
done

SUPPORTED_VERSIONS_PLAN="$DOCS_PLANS/2026-06-14-supported-toolchain-versions.md"
for plan_contract in \
  'Status: Completed' \
  'The repository and external-directory `make check` passed.' \
  'Eight hostile supported-version mutations were rejected'; do
  if ! grep -Fq -- "$plan_contract" "$SUPPORTED_VERSIONS_PLAN"; then
    printf '%s\n' "Supported-version plan is missing verification evidence: $plan_contract" >&2
    exit 1
  fi
done

make_root='override ROOT := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))'
if ! grep -Fxq -- "$make_root" "$MAKEFILE"; then
  printf '%s\n' "Makefile ROOT must be protected and derived from the loaded Makefile." >&2
  exit 1
fi

if ! grep -Fxq -- 'MVN ?= mvn' "$MAKEFILE"; then
  printf '%s\n' "Makefile must preserve the Maven command override." >&2
  exit 1
fi

for rate_limit_contract in \
  'private static final int MAX_LIVE_DIAL_ATTEMPTS = 5;' \
  'private static final long LIVE_DIAL_WINDOW_MILLIS = 60_000L;' \
  'LIVE_DIAL_GATE.tryAcquire(requestId, currentTimeMillis.getAsLong())' \
  'exchange.getResponseHeaders().set("Retry-After", "60")' \
  'new HttpResult(429, "Too many live dial attempts.")' \
  'liveDialRateLimiterAllowsFiveAttemptsAndResetsAfterOneMinute' \
  'liveDialRouteRateLimitsOnlyAfterParsingAndAuthorization' \
  'unauthorizedLiveDialRequestsDoNotConsumeTheAuthorizedRateLimit' \
  'dryRunRouteIgnoresExhaustedLiveDialRateLimit'; do
  if ! grep -Fq -- "$rate_limit_contract" "$ROOT_DIR/src/main/java/org/example/Main.java" && \
     ! grep -Fq -- "$rate_limit_contract" "$ROOT_DIR/src/test/java/org/example/MainTest.java"; then
    printf '%s\n' "Live dial rate-limit contract is missing: $rate_limit_contract" >&2
    exit 1
  fi
done

python3 - "$ROOT_DIR/src/main/java/org/example/Main.java" <<'PY'
from pathlib import Path
import sys

source = Path(sys.argv[1]).read_text(encoding="utf-8")
markers = (
    'if (!isFormContentType(contentType))',
    'byte[] requestBody = readRequestBody(exchange);',
    'dialForm = parseDialForm(requestBody);',
    'HttpResult result = dialPhone(',
)
positions = tuple(source.index(marker) for marker in markers)
if positions != tuple(sorted(positions)):
    raise SystemExit(
        "Live dial request parsing must follow media-type validation."
    )
PY

for strict_form_contract in \
  'private static DialForm parseDialForm(byte[] body) throws InvalidFormException' \
  'String name = decodeFormComponent(parts[0]);' \
  'if (name == null)' \
  'if (!"number".equals(name) && !"dialToken".equals(name) && !"requestId".equals(name))' \
  'String value = decodeFormComponent(parts[1]);' \
  'if (value == null)' \
  'if (phoneNumber != null)' \
  'if (dialToken != null)' \
  'if (requestId != null)' \
  'throw new InvalidFormException();' \
  'sendResponse(exchange, 400, "text/plain; charset=utf-8", "Invalid form submission.")' \
  'dialPhoneRouteRejectsAmbiguousOrMalformedRelevantFormFields' \
  'dialPhoneRouteIgnoresUnknownFormFields'; do
  if ! grep -Fq -- "$strict_form_contract" "$ROOT_DIR/src/main/java/org/example/Main.java" && \
     ! grep -Fq -- "$strict_form_contract" "$ROOT_DIR/src/test/java/org/example/MainTest.java"; then
    printf '%s\n' "Strict dial form contract is missing: $strict_form_contract" >&2
    exit 1
  fi
done

if grep -Fq 'formValue(' "$ROOT_DIR/src/main/java/org/example/Main.java"; then
  printf '%s\n' "Dial form fields must be parsed together instead of first-match extraction." >&2
  exit 1
fi

python3 - "$ROOT_DIR/src/main/java/org/example/Main.java" <<'PY'
from pathlib import Path
import re
import sys

source = Path(sys.argv[1]).read_text(encoding="utf-8")
parser = re.search(
    r"private static DialForm parseDialForm\(byte\[] body\) throws InvalidFormException \{"
    r"(?P<body>.*?)\n    \}",
    source,
    re.DOTALL,
)
if parser is None:
    raise SystemExit("Strict dial form parser body must remain explicit.")
body = parser.group("body")
unknown = re.search(
    r'if \(!"number"\.equals\(name\) && !"dialToken"\.equals\(name\) '
    r'&& !"requestId"\.equals\(name\)\) \{\s*continue;\s*\}',
    body,
)
if unknown is None:
    raise SystemExit("Unknown dial form fields must remain ignored.")
PY

for authorization_order_contract in \
  'if (sendLive && isBlank(TWILIO_DIAL_TOKEN))' \
  'if (sendLive && !authorizedDialToken(TWILIO_DIAL_TOKEN, dialToken))' \
  'liveDialAuthorizationPrecedesProviderConfigurationValidation' \
  'liveDialRouteDoesNotDiscloseProviderConfigurationBeforeAuthorization' \
  'assertFalse(unauthorized.body.contains("TWILIO_"))'; do
  if ! grep -Fq -- "$authorization_order_contract" "$ROOT_DIR/src/main/java/org/example/Main.java" && \
     ! grep -Fq -- "$authorization_order_contract" "$ROOT_DIR/src/test/java/org/example/MainTest.java"; then
    printf '%s\n' "Live dial authorization-order contract is missing: $authorization_order_contract" >&2
    exit 1
  fi
done

authorization_line=$(grep -nF 'if (sendLive && !authorizedDialToken(TWILIO_DIAL_TOKEN, dialToken))' \
  "$ROOT_DIR/src/main/java/org/example/Main.java" | cut -d: -f1)
configuration_line=$(grep -nF 'String configurationError = callConfigurationError(' \
  "$ROOT_DIR/src/main/java/org/example/Main.java" | cut -d: -f1)
if [ -z "$authorization_line" ] || [ -z "$configuration_line" ] || \
   [ "$authorization_line" -ge "$configuration_line" ]; then
  printf '%s\n' "Live dial authorization must precede provider configuration validation." >&2
  exit 1
fi

for at_most_once_contract in \
  'name="requestId" value="__LIVE_DIAL_REQUEST_ID__"' \
  'new HttpResult(409, "Duplicate live dial request.")' \
  'liveDialRequestIdPreventsASecondProviderAttemptAfterAnUnknownOutcome' \
  'liveDialRequiresAnExplicitRequestIdBeforeCallingTheProvider' \
  'class SingleAttemptHttpClient extends HttpClient' \
  '.disableAutomaticRetries()' \
  'twilioTransportMakesOnlyOneAttemptForAProviderFailure' \
  'twilioClientDisablesApacheAutomaticRetries'; do
  if ! grep -Fq -- "$at_most_once_contract" "$ROOT_DIR/src/main/java/org/example/Main.java" && \
     ! grep -Fq -- "$at_most_once_contract" "$ROOT_DIR/src/main/resources/public/index.html" && \
     ! grep -Fq -- "$at_most_once_contract" "$ROOT_DIR/src/test/java/org/example/MainTest.java"; then
    printf '%s\n' "Live dial at-most-once contract is missing: $at_most_once_contract" >&2
    exit 1
  fi
done

workflow_files=$(find "$ROOT_DIR/.github/workflows" -maxdepth 1 -type f -print | sort)
if [ "$workflow_files" != "$WORKFLOW" ]; then
  printf '%s\n%s\n' "Only the canonical check workflow may be present:" "$workflow_files" >&2
  exit 1
fi

for workflow_contract in \
  "permissions:" \
  "contents: read" \
  "persist-credentials: false"; do
  if ! grep -Fq -- "$workflow_contract" "$WORKFLOW"; then
    printf '%s\n' "Hosted verification is missing security contract: $workflow_contract" >&2
    exit 1
  fi
done

if grep -Eq '^[[:space:]]*[[:alnum:]_-]+:[[:space:]]*write([[:space:]]|$)' "$WORKFLOW"; then
  printf '%s\n' "Hosted verification must not grant write permissions." >&2
  exit 1
fi

if grep -Fq "pull_request_target:" "$WORKFLOW"; then
  printf '%s\n' "Hosted verification must not run untrusted changes with pull_request_target." >&2
  exit 1
fi

if grep -Fq "branches:" "$WORKFLOW"; then
  printf '%s\n' "Hosted push verification must cover all branches." >&2
  exit 1
fi

for event_contract in \
  "  pull_request:" \
  "  push:" \
  "  workflow_dispatch:"; do
  if ! grep -Fxq -- "$event_contract" "$WORKFLOW"; then
    printf '%s\n' "Hosted verification is missing canonical event: $event_contract" >&2
    exit 1
  fi
done

for provider_failure_contract in \
  "String requestId," \
  "CallSender callSender" \
  "catch (RuntimeException providerError)" \
  'new HttpResult(502, "Twilio call request failed.")' \
  "liveDialHidesTwilioProviderFailureDetails"; do
  if ! grep -Fq -- "$provider_failure_contract" "$ROOT_DIR/src/main/java/org/example/Main.java" && \
     ! grep -Fq -- "$provider_failure_contract" "$ROOT_DIR/src/test/java/org/example/MainTest.java"; then
    printf '%s\n' "Twilio provider failure contract is missing: $provider_failure_contract" >&2
    exit 1
  fi
done

for form_content_type_contract in \
  "if (!isFormContentType(contentType))" \
  '"application/x-www-form-urlencoded".equalsIgnoreCase(mediaType.trim())' \
  "dialPhoneRouteRequiresTheExactFormMediaType" \
  "application/x-www-form-urlencoded-evil" \
  "Application/X-WWW-Form-Urlencoded; charset=UTF-8"; do
  if ! grep -Fq -- "$form_content_type_contract" "$ROOT_DIR/src/main/java/org/example/Main.java" && \
     ! grep -Fq -- "$form_content_type_contract" "$ROOT_DIR/src/test/java/org/example/MainTest.java"; then
    printf '%s\n' "Form content-type contract is missing: $form_content_type_contract" >&2
    exit 1
  fi
done

if ! grep -Fq '"$(ROOT)/scripts/check-baseline.sh"' "$MAKEFILE"; then
  printf '%s\n' "Makefile must run scripts/check-baseline.sh from make check." >&2
  exit 1
fi

for make_contract in \
  'ROOT := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))' \
  'cd "$(ROOT)" && $(MVN)'; do
  if ! grep -Fq -- "$make_contract" "$MAKEFILE"; then
    printf '%s\n' "Makefile is missing root-independent contract: $make_contract" >&2
    exit 1
  fi
done

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
