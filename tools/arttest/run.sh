#!/usr/bin/env bash
# Desktop art + logic harness for Stardust Ranch.
# Compiles the non-Android game files (Data, Px, Art, World, Mons, Game) with a stub Bitmap,
# runs a short gameplay simulation, and writes PNG renders of every sprite sheet and map.
# Usage: tools/arttest/run.sh [outDir]   (default tools/arttest/out)
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
CACHE=${ARTTEST_CACHE:-/tmp/arttest-cache}
mkdir -p "$CACHE"
if [ ! -x "$CACHE/kotlinc/bin/kotlinc" ]; then
  curl -sSL -o "$CACHE/kc.zip" https://github.com/JetBrains/kotlin/releases/download/v2.0.21/kotlin-compiler-2.0.21.zip
  unzip -q "$CACHE/kc.zip" -d "$CACHE"
fi
[ -f "$CACHE/json.jar" ] || curl -sSL -o "$CACHE/json.jar" https://maven-central.storage-download.googleapis.com/maven2/org/json/json/20240303/json-20240303.jar
OUT=${1:-$ROOT/tools/arttest/out}
mkdir -p "$OUT"
SRC=$ROOT/app/src/main/java/com/jondude/stardustranch/game
H=$ROOT/tools/arttest
"$CACHE/kotlinc/bin/kotlinc" "$H/Bitmap.kt" "$H/Main.kt" "$SRC/Data.kt" "$SRC/Px.kt" "$SRC/Sprites.kt" "$SRC/Art.kt" "$SRC/World.kt" "$SRC/Mons.kt" "$SRC/Game.kt" \
  -cp "$CACHE/json.jar" -d "$CACHE/arttest.jar" 2>&1 | grep -v "JAVA_TOOL\|^warning" || true
java -cp "$CACHE/arttest.jar:$CACHE/json.jar:$CACHE/kotlinc/lib/kotlin-stdlib.jar" MainKt "$OUT" 2>&1 | grep -v JAVA_TOOL
echo "Renders written to $OUT"
