#!/bin/sh
# Converts the soundtrack MP3s in Music/ to WAV files the game can play.
# Java's built-in audio cannot read MP3, so this runs once after you change a track.
# macOS only (uses the built-in afconvert). The WAVs are committed, so other
# machines do not need to run this.
set -e
cd "$(dirname "$0")/.."
mkdir -p res/music
conv() {
    afconvert -f WAVE -d LEI16@32000 -c 1 "Music/$1" "res/music/$2"
    echo "wrote res/music/$2"
}
conv "School Bell Adventure.mp3" title.wav
conv "Pencil Tap Battle.mp3" world1.wav
conv "Deadline Chaos.mp3" world2.wav
conv "Huge Wave.mp3" hugewave.wav
