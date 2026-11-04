#!/usr/bin/env bash
# Compiles all Java sources into ./build (requires JDK 11+ and lib/acm.jar).
set -euo pipefail
cd "$(dirname "$0")"
if [ ! -f lib/acm.jar ]; then
  echo "lib/acm.jar is missing. See README.md (Setup) for how to obtain it." >&2
  exit 1
fi
rm -rf build && mkdir -p build
find src -name '*.java' > build/sources.txt
javac -Xlint:all -Xlint:-serial -encoding UTF-8 -d build -cp lib/acm.jar @build/sources.txt
echo "Build OK -> build/"
