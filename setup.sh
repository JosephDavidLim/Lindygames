#!/usr/bin/env bash
# One-shot setup for macOS / Linux:
#   1. finds a compatible JDK (11-25), or downloads Eclipse Temurin JDK 21 into ~/.jdks
#   2. downloads lib/acm.jar if it is missing
#   3. builds the game and runs the headless test suite
# Safe to re-run; it skips anything already in place. No sudo or package manager needed.
set -euo pipefail
cd "$(dirname "$0")"
source scripts/java-env.sh

if lindefense_find_jdk; then
  echo "Using JDK: $JAVA_HOME"
else
  case "$(uname -s)" in
    Darwin) os=mac ;;
    Linux)  os=linux ;;
    *) echo "Unsupported OS for automatic JDK install; install JDK 21 manually (see README)." >&2; exit 1 ;;
  esac
  case "$(uname -m)" in
    arm64|aarch64) arch=aarch64 ;;
    x86_64|amd64)  arch=x64 ;;
    *) echo "Unsupported CPU $(uname -m); install JDK 21 manually (see README)." >&2; exit 1 ;;
  esac
  echo "Downloading Eclipse Temurin JDK 21 ($os/$arch) into ~/.jdks ..."
  mkdir -p "$HOME/.jdks"
  tmp="$(mktemp -d)"
  curl -fL --progress-bar -o "$tmp/jdk.tar.gz" \
    "https://api.adoptium.net/v3/binary/latest/21/ga/$os/$arch/jdk/hotspot/normal/eclipse"
  tar -xzf "$tmp/jdk.tar.gz" -C "$HOME/.jdks"
  rm -rf "$tmp"
  lindefense_require_jdk
  echo "Installed JDK: $JAVA_HOME"
fi

if [ ! -f lib/acm.jar ]; then
  echo "Downloading lib/acm.jar ..."
  mkdir -p lib
  curl -fL -o lib/acm.jar https://cs.stanford.edu/people/eroberts/jtf/acm.jar
fi
unzip -l lib/acm.jar | grep 'acm/program/GraphicsProgram.class' >/dev/null \
  || { echo "lib/acm.jar does not contain acm/program/GraphicsProgram.class" >&2; exit 1; }

./test.sh
echo
echo "Setup complete. Launch the game with: ./run.sh"
