#!/usr/bin/env bash
# Sourced by build.sh / run.sh / test.sh / setup.sh. Run directly, it prints the chosen JAVA_HOME:
#   export JAVA_HOME="$(scripts/java-env.sh)" && export PATH="$JAVA_HOME/bin:$PATH"
# Selects a JDK the ACM library can compile against and puts it first on PATH.
#
# acm.jar's GraphicsProgram extends javax.swing.JApplet, and the Applet API was
# removed in JDK 26, so the supported range is JDK 11-25 (21 LTS recommended).
# Order tried: $JAVA_HOME, ~/.jdks/* (where setup.sh installs), macOS java_home, PATH.

LINDEFENSE_MIN_JDK=11
LINDEFENSE_MAX_JDK=25

_lindefense_major() {
  # Prints the major version of the given javac, e.g. "21" (empty if not runnable).
  "$1" -version 2>&1 | sed -n 's/^javac \([0-9][0-9]*\).*/\1/p' | head -n 1
}

_lindefense_ok() {
  local v
  [ -x "$1" ] || return 1
  v="$(_lindefense_major "$1")"
  [ -n "$v" ] && [ "$v" -ge "$LINDEFENSE_MIN_JDK" ] && [ "$v" -le "$LINDEFENSE_MAX_JDK" ]
}

# Usage: lindefense_find_jdk  -> exports JAVA_HOME/PATH and returns 0, or returns 1.
lindefense_find_jdk() {
  local c home
  local candidates=()
  [ -n "${JAVA_HOME:-}" ] && candidates+=("$JAVA_HOME/bin/javac")
  for home in "$HOME"/.jdks/*/Contents/Home "$HOME"/.jdks/*; do
    [ -d "$home" ] && candidates+=("$home/bin/javac")
  done
  if [ -x /usr/libexec/java_home ]; then
    for v in 25 21 17 11; do
      home="$(/usr/libexec/java_home -v "$v" 2>/dev/null || true)"
      [ -n "$home" ] && candidates+=("$home/bin/javac")
    done
  fi
  c="$(command -v javac 2>/dev/null || true)"
  [ -n "$c" ] && candidates+=("$c")

  for c in "${candidates[@]}"; do
    if _lindefense_ok "$c"; then
      export JAVA_HOME="$(cd "$(dirname "$c")/.." && pwd)"
      export PATH="$JAVA_HOME/bin:$PATH"
      return 0
    fi
  done
  return 1
}

# Usage: lindefense_require_jdk  -> like find, but exits with a helpful message.
lindefense_require_jdk() {
  if ! lindefense_find_jdk; then
    echo "No compatible JDK found (need JDK $LINDEFENSE_MIN_JDK-$LINDEFENSE_MAX_JDK; JDK 26+ removed the Applet API acm.jar needs)." >&2
    echo "Run ./setup.sh to install JDK 21 into ~/.jdks, or set JAVA_HOME to a JDK 11-25." >&2
    exit 1
  fi
}

# Executed directly (not sourced): print the selected JAVA_HOME.
if [ "${BASH_SOURCE[0]}" = "$0" ]; then
  lindefense_require_jdk
  echo "$JAVA_HOME"
fi
