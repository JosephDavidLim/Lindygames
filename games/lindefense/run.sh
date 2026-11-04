#!/usr/bin/env bash
# Builds (if needed) and launches the game.
set -euo pipefail
cd "$(dirname "$0")"
source scripts/java-env.sh
lindefense_require_jdk
[ -d build/lindefense ] || ./build.sh
# JAVA_OPTS passes JVM flags, e.g. JAVA_OPTS="-Dlindefense.demo=build/snapshots" ./run.sh
exec java ${JAVA_OPTS:-} -cp build:lib/acm.jar lindefense.Main "$@"
