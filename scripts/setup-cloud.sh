#!/usr/bin/env bash
set -euo pipefail
# Reuse the isolated checkout; no worktree is needed.
cd "$(dirname "$0")/.."
TOOLS=/workspace/toolchains
mkdir -p "$TOOLS"
if [[ ! -x "$TOOLS/jdk-17.0.16+8/bin/javac" ]]; then
  base='https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.16%2B8'
  archive='OpenJDK17U-jdk_x64_linux_hotspot_17.0.16_8.tar.gz'
  curl -fL "$base/$archive" -o "$TOOLS/jdk.tar.gz"
  curl -fL "$base/$archive.sha256.txt" -o "$TOOLS/jdk.sha256"
  python3 - <<'PY'
from pathlib import Path
import hashlib, tarfile
p = Path('/workspace/toolchains')
assert hashlib.sha256((p/'jdk.tar.gz').read_bytes()).hexdigest() == (p/'jdk.sha256').read_text().split()[0]
with tarfile.open(p/'jdk.tar.gz') as tar:
    tar.extractall(p, filter='data')
PY
fi
export ANDROID_HOME="$TOOLS/android-sdk"
if [[ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]]; then
  curl -fL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o "$TOOLS/android-tools.zip"
  python3 - <<'PY'
from pathlib import Path
import zipfile
p = Path('/workspace/toolchains')
root = p/'android-sdk/cmdline-tools'
zipfile.ZipFile(p/'android-tools.zip').extractall(root)
(root/'cmdline-tools').rename(root/'latest')
PY
  chmod +x "$ANDROID_HOME"/cmdline-tools/latest/bin/*
fi
proxy_args=()
if [[ -n "${HTTPS_PROXY:-}" ]]; then
  read -r proxy_host proxy_port < <(python3 - <<'PY'
import os, urllib.parse
p=urllib.parse.urlparse(os.environ['HTTPS_PROXY'])
print(p.hostname, p.port or 80)
PY
)
  proxy_args=(--proxy=http "--proxy_host=$proxy_host" "--proxy_port=$proxy_port")
fi
# Use the image's Java trust store for SDK downloads.
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
set +o pipefail
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "${proxy_args[@]}" --licenses
license_status=${PIPESTATUS[1]}
set -o pipefail
[[ $license_status == 0 ]]
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "${proxy_args[@]}" 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools'
export JAVA_HOME="$TOOLS/jdk-17.0.16+8"
./scripts/build-cloud.sh
