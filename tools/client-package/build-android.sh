#!/usr/bin/env bash
# Build the MonMMO-EX Android client from a retail PokeMMO APK - the whole thing, in one command.
#
#   build-android.sh [--host H] [--apk retail.apk] [--out out.apk] [--theme theme.zip]
#                    [--rebuild-dex] [--verify-dex] [--skip-stage]
#
# This replaces package-android.cmd, which silently shipped a client with NO code patches: it never
# passed the rebuilt classes.dex or the asset overlay, so Fairy, the badge tables, the Pokedex
# regions and the weekday fix were all missing from anything it built. It also replaces the pair of
# bash scripts that lived in a session temp folder, along with the hand-made overlay files they
# pointed at (now in tools/client-package/android-overlay).
#
# What goes into the client:
#   stock data      the retail APK's own data.pak + strings_en.xml, staged with our content
#   classes.dex     retail dex + tools/client-package/android-smali (cached; see --rebuild-dex)
#   overlay         android-overlay/ (hand-made: Fairy button, atlas pages) + the staged mod's art
#   theme mod       composed by theme-mod/compose-theme-mod.sh, enabled by the packager's config
#   config          server host/ports and the client keys, written by ApkPackager
set -euo pipefail

HOST=91.98.41.154
APK="$USERPROFILE/Downloads/pokemmo-r32645.apk"
OUT="$USERPROFILE/Downloads/MonMMO.apk"
THEME="$USERPROFILE/Downloads/Dark Theme - Mobile(1).zip"
REBUILD_DEX=0 VERIFY_DEX=0 SKIP_STAGE=0
while [ $# -gt 0 ]; do
  case "$1" in
    --host) HOST=$2; shift 2;;
    --apk) APK=$2; shift 2;;
    --out) OUT=$2; shift 2;;
    --theme) THEME=$2; shift 2;;
    --rebuild-dex) REBUILD_DEX=1; shift;;
    --verify-dex) VERIFY_DEX=1; shift;;
    --skip-stage) SKIP_STAGE=1; shift;;
    *) echo "unknown option: $1"; exit 2;;
  esac
done

REPO="$(cd "$(dirname "$0")/../.." && pwd)"
TOOLS="$REPO/tools/client-package"
CACHE="$HOME/.monmmo/build"
JDK=${JDK:-"/c/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot/bin"}
KEYDIR="$HOME/.monmmo/android-signing"
SMALI="${USERPROFILE:-$HOME}/.monmmo/tools/smali-3.0.10"   # Windows-form: a POSIX path breaks java -cp
STOCK="$CACHE/stock-root" STAGE="$CACHE/stage" OVERLAY="$CACHE/overlay" WORK="$CACHE/work"
DEX="$CACHE/classes.dex" MOD="$CACHE/monmmo-theme.zip"

for f in "$APK" "$KEYDIR/monmmo-ex.p12" "$KEYDIR/password.txt"; do
  [ -f "$f" ] || { echo "ERROR: missing $f"; exit 1; }
done
mkdir -p "$CACHE"

# 1. The retail APK's own data is the base everything is staged against.
if [ "$SKIP_STAGE" = 0 ]; then
  echo "== staging content against the APK's own data"
  rm -rf "$STOCK" "$STAGE"; mkdir -p "$STOCK/data/strings" "$WORK"
  "$SYSTEMROOT/System32/tar.exe" -xf "$APK" -C "$WORK" assets/data/data.pak assets/data/strings/strings_en.xml
  mv "$WORK/assets/data/data.pak" "$STOCK/data/data.pak"
  mv "$WORK/assets/data/strings/strings_en.xml" "$STOCK/data/strings/strings_en.xml"
  (cd "$REPO" && ./gradlew.bat :launcher:stageExpansionClientContent \
      "-Pexpansion.clientRoot=$STOCK" "-Pexpansion.outputDir=$STAGE" --console=plain -q)
  echo "== proving retail content is preserved"
  (cd "$REPO" && ./gradlew.bat :launcher:checkRetailPreserved \
      "-Pretail.stock=$STOCK" "-Pretail.staged=$STAGE" --console=plain -q)
fi

# 2. The overlay: our hand-made files first, then the staged mod's art under resources/.
echo "== composing the asset overlay"
rm -rf "$OVERLAY"; mkdir -p "$OVERLAY/assets/data/resources"
cp -r "$TOOLS/android-overlay/." "$OVERLAY/"
MODZIP="$STAGE/data/mods/monmmo-lost-knights.zip"
[ -f "$MODZIP" ] || { echo "ERROR: staged mod zip missing: $MODZIP"; exit 1; }
unzip -o -q "$MODZIP" -x info.xml -d "$OVERLAY/assets/data/resources"
for dir in cries sprites/battlesprites sprites/monstericons sprites/followsprites; do
  z=$(unzip -Z1 "$MODZIP" | grep -c "^$dir/.\+[^/]$" || true)
  o=$([ -d "$OVERLAY/assets/data/resources/$dir" ] && find "$OVERLAY/assets/data/resources/$dir" -type f | wc -l || echo 0)
  [ "$z" = "$o" ] || { echo "ERROR: $dir count mismatch (zip $z, overlay $o)"; exit 1; }
done

# 2b. The version line under the baked theme's wordmark, so it is there with the mod disabled too.
#     bg.png and the dialogue font both come from the retail APK; the badge is drawn onto them here.
echo "== drawing the version badge on the stock wordmark"
rm -rf "$WORK/stocktheme"; mkdir -p "$WORK/stocktheme" "$OVERLAY/assets/data/themes/default/res"
unzip -o -q "$APK" "assets/data/themes/default/res/bg.png" \
  "assets/data/themes/default/res/fonts/battle.ttf" -d "$WORK/stocktheme"
BATTLE_TTF="$WORK/stocktheme/assets/data/themes/default/res/fonts/battle.ttf"
"$JDK/java.exe" "$TOOLS/theme-mod/MakeTitleBadge.java" \
  "$WORK/stocktheme/assets/data/themes/default/res/bg.png" "$BATTLE_TTF" \
  "${BADGE_TEXT:-Pirated Version}" "${STOCK_BADGE_COLOUR:-#FFFFFF}" \
  "$OVERLAY/assets/data/themes/default/res/bg.png"
export BATTLE_TTF

# 2c. The app icon. The launcher icon is the `pokemmo` mipmap set; these obfuscated names were read
#     out of this APK's resources.arsc (48/72/96/144/192 px), so they are checked before use - a
#     different retail build would name them differently. The adaptive icon's background layer is a
#     flat colour, not an image, so there is nothing else to replace.
ICON_SRC="$TOOLS/app-icon/app-icon-source.png"
if [ -f "$ICON_SRC" ]; then
  echo "== fitting the app icon"
  rm -rf "$WORK/icons"; mkdir -p "$WORK/icons" "$OVERLAY/res" "$OVERLAY/assets/data/icons"
  "$JDK/java.exe" "$TOOLS/app-icon/IconSet.java" "$ICON_SRC" "$WORK/icons"
  for pair in "Au 48" "Mx 72" "db 96" "hm 144" "xq 192"; do
    set -- $pair
    unzip -l "$APK" "res/$1.png" | grep -q "res/$1.png" \
      || { echo "ERROR: res/$1.png is not in this APK - re-read the mipmap names from resources.arsc"; exit 1; }
    cp "$WORK/icons/icon-$2.png" "$OVERLAY/res/$1.png"
  done
  for n in 16 32 128; do cp "$WORK/icons/icon-$n.png" "$OVERLAY/assets/data/icons/${n}x${n}.png"; done
fi

# 3. The theme, with our Fairy extension, atlas pages and its own badge folded in.
if [ -f "$THEME" ]; then
  echo "== composing the theme mod"
  bash "$TOOLS/theme-mod/compose-theme-mod.sh" "$THEME" "$MOD"
else
  echo "== no theme zip at $THEME - building without it"
  MOD="-"
fi

# 4. classes.dex. A full baksmali of the 8.4k-class dex needs a 2-3 GB JVM, so the built dex is
#    cached and only rebuilt on request or when it is missing.
if [ "$REBUILD_DEX" = 1 ] || [ ! -f "$DEX" ]; then
  echo "== rebuilding classes.dex from smali (slow, needs ~3 GB)"
  [ -d "$SMALI" ] || { echo "ERROR: smali tools missing at $SMALI"; exit 1; }
  CP=$(ls "$SMALI"/*.jar | tr '\n' ';')
  rm -rf "$WORK/dex"; mkdir -p "$WORK/dex"
  unzip -o -q "$APK" classes.dex -d "$WORK/dex"
  "$JDK/java.exe" -Xmx3g -cp "$CP" com.android.tools.smali.baksmali.Main d "$WORK/dex/classes.dex" -o "$WORK/smali" --api 21
  cp -r "$TOOLS/android-smali/f/." "$WORK/smali/f/"
  "$JDK/java.exe" -Xmx3g -cp "$CP" com.android.tools.smali.smali.Main a "$WORK/smali" -o "$DEX" --api 21
else
  echo "== reusing cached classes.dex ($(stat -c%s "$DEX") bytes; --rebuild-dex to redo it)"
fi

# 5. Package, sign, align.
echo "== packaging"
rm -rf "$WORK/apk"; mkdir -p "$WORK/apk"
"$JDK/java.exe" -Xmx2g "$TOOLS/ApkPackager.java" prepare "$APK" "$WORK/apk/unsigned.apk" "$HOST" "$MOD" \
  "$REPO/launcher/src/main/resources/game.public.pem" "$REPO/launcher/src/main/resources/chat.public.pem" \
  "$STAGE/data/data.pak" "$STAGE/data/strings/strings_en.xml" "$DEX" "$OVERLAY"
"$JDK/jarsigner.exe" -keystore "$KEYDIR/monmmo-ex.p12" -storetype PKCS12 -storepass:file "$KEYDIR/password.txt" \
  -sigalg SHA256withRSA -digestalg SHA-256 -signedjar "$WORK/apk/v1.apk" "$WORK/apk/unsigned.apk" monmmo > /dev/null
"$JDK/java.exe" -Xmx2g "$TOOLS/ApkPackager.java" finish "$WORK/apk/v1.apk" "$OUT" \
  "$KEYDIR/monmmo-ex.p12" "$KEYDIR/password.txt" monmmo
"$JDK/jarsigner.exe" -verify "$OUT" > /dev/null || { echo "ERROR: v1 signature does not verify"; exit 1; }

# 6. Say what actually shipped, rather than trusting that it did.
echo "== what is in $OUT"
echo "-- config (server properties only; the package must never carry credentials)"
unzip -p "$OUT" assets/config/zzz-monmmo-ex-server.properties | sed 's/^/   /'
echo "-- Fairy and evolution symbols in the stock-theme atlas"
printf "   type19 badge regions: %s, evo symbols: %s\n" \
  "$(unzip -p "$OUT" assets/data/sprites/atlas/main.atlas | grep -c '_type_19_')" \
  "$(unzip -p "$OUT" assets/data/sprites/atlas/main.atlas | grep -c '^monmmo-\(mega\|alpha\|omega\)$')"
if [ "$MOD" != "-" ]; then
  echo "-- theme mod"
  unzip -l "$OUT" | grep "assets/data/mods/" | sed 's/^/   /'
  printf "   Fairy extension: %s, type19 badges in its atlas: %s\n" \
    "$(unzip -p "$OUT" "assets/data/mods/$(basename "$MOD")" > "$WORK/mod.zip" && unzip -Z1 "$WORK/mod.zip" | grep -c 'MonMMOFairy')" \
    "$(unzip -p "$WORK/mod.zip" atlas/main.atlas | grep -c '_type_19_')"
fi
if [ "$VERIFY_DEX" = 1 ]; then
  echo "-- code patches (one class, so this stays cheap)"
  CP=$(ls "$SMALI"/*.jar | tr '\n' ';')
  unzip -p "$OUT" classes.dex > "$WORK/apk-classes.dex"
  rm -rf "$WORK/apk-smali"
  "$JDK/java.exe" -Xmx3g -cp "$CP" com.android.tools.smali.baksmali.Main d "$WORK/apk-classes.dex" \
    -o "$WORK/apk-smali" --api 21 --classes 'Lf/dw2;'
  grep -q 'const/16 v3, 0x4e20' "$WORK/apk-smali/f/dw2.smali" \
    && echo "   dw2.Kk0 follower patch present" || { echo "   ERROR: code patches missing"; exit 1; }
fi
ls -l "$OUT"
