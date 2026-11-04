#!/usr/bin/env bash
# Builds and runs the performance benchmark (lindefense.tools.PerfBench; see README "Performance").
#   scripts/perfbench.sh [sim|render|all] [-Dperf.option=value ...]
#   PERF_JFR=1 scripts/perfbench.sh render     # also records build/perf.jfr and prints hot methods / allocations
# Everything runs headless: the render benchmark paints the real UI into an offscreen image, no window opens.
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/java-env.sh
lindefense_require_jdk
./build.sh > /dev/null
mode="${1:-all}"
if [ $# -gt 0 ]; then shift; fi
if [ "${PERF_JFR:-0}" = "1" ]; then
  java -XX:StartFlightRecording=filename=build/perf.jfr,settings=profile "$@" -cp build:lib/acm.jar lindefense.tools.PerfBench "$mode"
  jfr view hot-methods build/perf.jfr 2>/dev/null | head -32 || echo "(jfr view needs JDK 21+; open build/perf.jfr in JDK Mission Control)"
  jfr view allocation-by-class build/perf.jfr 2>/dev/null | head -20 || true
else
  java "$@" -cp build:lib/acm.jar lindefense.tools.PerfBench "$mode"
fi
