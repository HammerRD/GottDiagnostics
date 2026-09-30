#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export ANDROID_HOME="${ANDROID_HOME:-/workspace/toolchains/android-sdk}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/workspace/.gradle}"
export JAVA_HOME="${JAVA_HOME:-/workspace/toolchains/jdk-17.0.16+8}"
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN_CENTRAL_MIRROR="https://maven-central.storage-download.googleapis.com/maven2"
args=("-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts")
if [[ -n "${HTTPS_PROXY:-}" ]]; then
  read -r proxy_host proxy_port < <(python3 - <<'PY'
import os, urllib.parse
p = urllib.parse.urlparse(os.environ['HTTPS_PROXY'])
print(p.hostname, p.port or 80)
PY
)
  args+=("-Dhttps.proxyHost=$proxy_host" "-Dhttps.proxyPort=$proxy_port" "-Dhttp.proxyHost=$proxy_host" "-Dhttp.proxyPort=$proxy_port")
fi
./gradlew "${args[@]}" --console=plain assembleDebug testDebugUnitTest lintDebug "$@"
