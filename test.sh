#!/usr/bin/env bash
# Builds and runs the headless verification suite (no window needed).
set -euo pipefail
cd "$(dirname "$0")"
source scripts/java-env.sh
lindefense_require_jdk
./build.sh
exec java -cp build:lib/acm.jar lindefense.test.HeadlessTests
