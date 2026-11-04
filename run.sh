#!/usr/bin/env bash
# Builds (if needed) and launches the game.
set -euo pipefail
cd "$(dirname "$0")"
[ -d build/lindefense ] || ./build.sh
exec java -cp build:lib/acm.jar lindefense.Main "$@"
