#!/bin/sh
# Packs 11/1 for the website. Run from anywhere: games/11-1/tools/build_web.sh
# Writes web/play/, which the browser page runs through CheerpJ.
set -e
cd "$(dirname "$0")/.."
OUT=../../web/play
BUILD=$(mktemp -d)
javac -nowarn --release 17 -cp lib/acm.jar -d "$BUILD" $(find src -name '*.java')
jar cf "$OUT/11-1.jar" -C "$BUILD" .
cp lib/acm.jar "$OUT/acm.jar"
rm -rf "$OUT/res" "$BUILD"
mkdir -p "$OUT/res/sprites"
cp -R res/music res/sfx res/maps res/gear res/ui "$OUT/res/"
cp -R res/sprites/neon "$OUT/res/sprites/neon"
echo "Packed into web/play"
