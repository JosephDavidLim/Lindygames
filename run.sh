#!/bin/sh
# Builds and starts 11/1. Run it from anywhere: ./run.sh
cd "$(dirname "$0")" || exit 1
mkdir -p out
javac -nowarn -cp lib/acm.jar -d out $(find src -name '*.java') || exit 1
exec java -cp out:lib/acm.jar game.GameApp
