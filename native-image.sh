#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

IMAGE="ghcr.io/graalvm/native-image-community:25"
MODULES=(watsonx-ai-core watsonx-ai)
REFLECTION_DENY_PREFIXES_JSON='[
  "com.github.jknack.handlebars",
  "com.google.common",
  "com.google.gson",
  "com.jayway.jsonpath",
  "com.squareup.moshi",
  "com.sun.management.internal",
  "com.sun.org.apache.xalan",
  "com.sun.org.apache.xerces",
  "com.sun.security",
  "jakarta.servlet",
  "javax.servlet",
  "jdk.internal",
  "jdk.management",
  "net.javacrumbs.jsonunit",
  "org.apache.commons",
  "org.apache.johnzon",
  "org.apache.logging",
  "org.apiguardian",
  "org.jcp.xml.dsig",
  "org.json",
  "org.jspecify",
  "org.osgi",
  "org.junit",
  "sun.instrument",
  "sun.invoke.util",
  "sun.management.spi",
  "sun.net.spi",
  "sun.reflect",
  "uk.org.webcompere"
]'

RESOURCE_ALLOW_PREFIXES_JSON='[
  "META-INF/services/com.fasterxml.",
  "META-INF/services/com.ibm.watsonx.",
  "META-INF/services/java.",
  "META-INF/services/javax.",
  "META-INF/services/org.slf4j."
]'

if ! command -v native-image-configure >/dev/null 2>&1; then
  command -v podman >/dev/null 2>&1 || {
    echo "ERROR: GraalVM and podman are not available." >&2
    exit 1
  }

  echo "==> Running inside $IMAGE"

  exec podman run --rm \
    --security-opt label=disable \
    -e LC_ALL=C \
    -e LANG=C \
    -e LC_CTYPE=C \
    --entrypoint bash \
    -v "$ROOT:/workspace" \
    -v "$HOME/.m2:/root/.m2" \
    -w /workspace \
    "$IMAGE" \
    -c "./native-image.sh"
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "==> Installing jq"
  microdnf install -y jq >/dev/null
fi

echo "==> Installing parent POM"
./mvnw install -N -B -ntp -q

echo "==> Installing watsonx-ai-core"
./mvnw install \
  -pl modules/watsonx-ai-core \
  -B -ntp -q \
  -DskipTests -Dmaven.test.skip=true


clean_metadata() {
  local file="$1"

  echo "==> [$(basename "$(dirname "$file")")] Cleaning metadata: $file"
  local tmp
  tmp="$(mktemp)"

  jq \
    --argjson deny  "$REFLECTION_DENY_PREFIXES_JSON" \
    --argjson allow "$RESOURCE_ALLOW_PREFIXES_JSON" \
    '
      def denied_type:
        . as $t |
        (type == "string") and (any($deny[]; . as $p | $t | startswith($p)));

      def denied_entry:
        . as $e |
        if   ($e.type | type) == "string" then ($e.type | denied_type)
        elif ($e.type | type) == "object" then
          if   ($e.type | has("lambda")) then ($e.type.lambda.declaringClass | denied_type)
          elif ($e.type | has("proxy"))  then ($e.type.proxy | map(denied_type) | any)
          else false
          end
        else false
        end;

      def allowed_resource:
        . as $r |
        ([$r.glob, $r.bundle, $r.module] | map(select(. != null))[0] // "") as $v |
        any($allow[]; . as $p | $v | startswith($p));

      .reflection |= map(select(denied_entry | not)) |
      .resources  |= map(select(allowed_resource))
    ' "$file" > "$tmp"

  mv "$tmp" "$file"
}

generate_metadata() {
  local module="$1"
  local module_dir="modules/$module"
  local agent_dir="$module_dir/target/native/agent-output/test"
  local output_dir="$module_dir/src/main/resources/META-INF/native-image/com.ibm.watsonx/$module"

  echo "==> [$module] Running tracing agent"
  ./mvnw -Pnative-agent clean test -pl "$module_dir" -B -ntp

  mapfile -t input_dirs < <(
    find "$agent_dir" \
      -name reachability-metadata.json \
      -exec dirname {} \; |
      sort -u
  )

  if [[ ${#input_dirs[@]} -eq 0 ]]; then
    echo "ERROR: [$module] No reachability metadata generated." >&2
    exit 1
  fi

  local args=()
  for dir in "${input_dirs[@]}"; do
    args+=("--input-dir=$dir")
  done

  echo "==> [$module] Generating metadata"
  native-image-configure generate \
    "${args[@]}" \
    "--output-dir=$output_dir"

  echo "==> [$module] Cleaning generated metadata"
  clean_metadata "$output_dir/reachability-metadata.json"

  echo "==> [$module] Reinstalling with refreshed metadata"
  ./mvnw install \
    -pl "$module_dir" \
    -B -ntp -q \
    -DskipTests -Dmaven.test.skip=true
}

run_native_tests() {
  local module="$1"

  echo "==> [$module] Running native tests"
  ./mvnw -Pnative clean test -pl "modules/$module" -B -ntp
}

echo "==> Generating reachability metadata"

for module in "${MODULES[@]}"; do
  generate_metadata "$module"
done

echo "==> Running native tests"

for module in "${MODULES[@]}"; do
  run_native_tests "$module"
done

echo "==> Done"