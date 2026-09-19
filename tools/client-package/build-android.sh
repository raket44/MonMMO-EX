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
  # The daemon keeps ~400 MB resident for nothing once staging is done, and this machine has none
  # to spare. KEEP_DAEMON=1 to leave it running when builds are back to back.
  [ "${KEEP_DAEMON:-0}" = 1 ] || (cd "$REPO" && ./gradlew.bat --stop >/dev/null 2>&1 || true)
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
# The wordmark's accent is hue-shifted from the client's own logo (yellow -> cyan), which is the
# dark theme's title screen without putting anyone else's file in the package. Set
# TITLE_ACCENT= to leave the stock yellow alone.
"$JDK/java.exe" "$TOOLS/theme-mod/MakeTitleBadge.java" \
  "$WORK/stocktheme/assets/data/themes/default/res/bg.png" "$BATTLE_TTF" \
  "${BADGE_TEXT:-Pirated Version}" "${BADGE_COLOUR:-#6BEBF0}" \
  "$OVERLAY/assets/data/themes/default/res/bg.png" \
  "#F0EA6B" "${TITLE_ACCENT-#6BEBF0}"
export BATTLE_TTF

# 2b2. The login background. Retail defines no `login-background` image at all - the slot does not
#      exist - so shipping one means adding the area to gfx.xml and pointing logingui at it in
#      main-widgets.xml. Both are patched from the retail files at build time rather than kept as
#      copies, so they follow whatever the APK ships. See theme-assets/CREDITS.md for the artwork.
LOGIN_BG="$TOOLS/theme-assets/login-background.png"
if [ -f "$LOGIN_BG" ]; then
  echo "== adding the login background"
  unzip -o -q "$APK" "assets/data/themes/default/gfx.xml" \
    "assets/data/themes/default/main-widgets.xml" -d "$WORK/stocktheme"
  cp "$LOGIN_BG" "$OVERLAY/assets/data/themes/default/res/background.png"
  awk '
    /<\/themes>/ && !done {
      print "\t<images file=\"res/background.png\">";
      print "\t\t<area name=\"login-background\" xywh=\"*\"/>";
      print "\t</images>";
      done = 1;
    }
    { print }
  ' "$WORK/stocktheme/assets/data/themes/default/gfx.xml" \
    > "$OVERLAY/assets/data/themes/default/gfx.xml"
  awk '
    { print }
    /<theme name="logingui"/ && !done {
      print "    <param name=\"background\"><image>login-background</image></param>";
      done = 1;
    }
  ' "$WORK/stocktheme/assets/data/themes/default/main-widgets.xml" \
    > "$OVERLAY/assets/data/themes/default/main-widgets.xml"
  grep -q "login-background" "$OVERLAY/assets/data/themes/default/gfx.xml" \
    && grep -q "login-background" "$OVERLAY/assets/data/themes/default/main-widgets.xml" \
    || { echo "ERROR: could not wire login-background into the theme"; exit 1; }
fi

# 2c. The app icon - done the way Android documents it, because nothing else worked. Android 8+
#     draws the adaptive icon, and its foreground layer must be a density-qualified bitmap
#     (108dp per bucket) or a vector. Retail's is a vector in drawable-anydpi-v24; a bitmap dropped
#     into that slot decodes at density 65534 - a few pixels - and the launcher smears its average
#     colour across the mask. That was seven installs of a grey circle.
#     So the resource side is rebuilt with apktool: decode retail's resources, swap the vector for
#     PNGs at five densities, set the background colour, put our mipmaps in, build. The packager
#     then takes resources.arsc, the manifest and res/ from that build (-Dmonmmo.resourcesApk) and
#     everything else from retail. Resource ids are pinned by apktool's public.xml; the build
#     checks the icon chain, the ids and the dex afterwards. The desktop window icons come from
#     the same art via IconSet.
ICON_SRC="$TOOLS/app-icon/app-icon-source.png"
APKTOOL="${USERPROFILE:-$HOME}/.monmmo/tools/apktool_3.0.3.jar"
RES_APK="-"
if [ -f "$ICON_SRC" ]; then
  echo "== rebuilding the resource side with our icon (apktool)"
  [ -f "$APKTOOL" ] || { echo "ERROR: apktool missing at $APKTOOL"; exit 1; }
  AT="$CACHE/apktool"; DEC="$AT/decoded"
  if [ ! -d "$DEC/res" ]; then
    "$JDK/java.exe" -Xmx2g -jar "$APKTOOL" d -s --no-assets -f -o "$DEC" "$APK" > "$AT.log" 2>&1 \
      || { tail -5 "$AT.log"; echo "ERROR: apktool decode failed"; exit 1; }
  fi
  rm -rf "$WORK/ladder"
  "$JDK/java.exe" "$TOOLS/app-icon/DensityLadder.java" "$ICON_SRC" "$WORK/ladder"
  rm -f "$DEC/res/drawable-anydpi-v24/pokemmo_foreground.xml"
  for b in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
    mkdir -p "$DEC/res/drawable-$b" "$DEC/res/mipmap-$b"
    cp "$WORK/ladder/drawable-$b/pokemmo_foreground.png" "$DEC/res/drawable-$b/"
    cp "$WORK/ladder/mipmap-$b/pokemmo.png" "$DEC/res/mipmap-$b/pokemmo.png"
  done
  sed -i "s|<color name=\"pokemmo_background\">#[0-9a-fA-F]*</color>|<color name=\"pokemmo_background\">${ICON_PLATE:-#ff14161a}</color>|" "$DEC/res/values/colors.xml"
  grep -q "pokemmo_background\">${ICON_PLATE:-#ff14161a}<" "$DEC/res/values/colors.xml" || { echo "ERROR: background colour not set"; exit 1; }
  RES_APK="$AT/retail-icon.apk"
  "$JDK/java.exe" -Xmx2g -jar "$APKTOOL" b -f -o "$RES_APK" "$DEC" > "$AT-build.log" 2>&1 \
    || { tail -5 "$AT-build.log"; echo "ERROR: apktool build failed"; exit 1; }
  "$JDK/java.exe" "$TOOLS/app-icon/IconChain.java" "$RES_APK" > "$AT-chain.log" \
    || { cat "$AT-chain.log"; echo "ERROR: rebuilt resources do not resolve the icon"; exit 1; }
  for d in classes.dex classes2.dex; do
    cmp -s <(unzip -p "$RES_APK" $d) <(unzip -p "$APK" $d) || { echo "ERROR: apktool altered $d"; exit 1; }
  done
  # The desktop window icons, from the same art.
  rm -rf "$WORK/icons"; mkdir -p "$WORK/icons" "$OVERLAY/assets/data/icons"
  "$JDK/java.exe" "$TOOLS/app-icon/IconSet.java" "$ICON_SRC" "$WORK/icons" > /dev/null
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
  rm -rf "$WORK/dex" "$WORK/smali"; mkdir -p "$WORK/dex"   # a stale tree would keep dropped patches
  unzip -o -q "$APK" classes.dex -d "$WORK/dex"
  "$JDK/java.exe" -Xmx3g -cp "$CP" com.android.tools.smali.baksmali.Main d "$WORK/dex/classes.dex" -o "$WORK/smali" --api 21
  # Retail r32645 has 8392 classes and the disassembly keeps all 8392 on this filesystem (checked
  # 2026-09-19; `f/` alone holds 6735, which was once misread as classes lost to case collisions -
  # there are none). The 2026-09-18 launch crash (NoClassDefFoundError: Lf/Vk;) was a patch naming
  # a class that does not exist in r32645, not a disassembly loss. The count check stays as a cheap
  # guard against a broken or partial disassembly.
  want=$("$JDK/java.exe" -cp "$CP" com.android.tools.smali.baksmali.Main list classes "$WORK/dex/classes.dex" | wc -l)
  got=$(find "$WORK/smali" -name "*.smali" | wc -l)
  if [ "$got" -lt "$want" ]; then
    echo "ERROR: disassembly produced $got of $want classes; not assembling. The cached dex is untouched."
    exit 1
  fi
  echo "   disassembly kept all $want classes"
  cp -r "$TOOLS/android-smali/." "$WORK/smali/"     # every package we patch, not just f/
  "$JDK/java.exe" -Xmx3g -cp "$CP" com.android.tools.smali.smali.Main a "$WORK/smali" -o "$DEX" --api 21
else
  echo "== reusing cached classes.dex ($(stat -c%s "$DEX") bytes; --rebuild-dex to redo it)"
fi

# 5. Package, sign, align.
echo "== packaging"
rm -rf "$WORK/apk"; mkdir -p "$WORK/apk"
"$JDK/java.exe" -Xmx2g "-Dmonmmo.resourcesApk=$RES_APK" "$TOOLS/ApkPackager.java" prepare "$APK" "$WORK/apk/unsigned.apk" "$HOST" "$MOD" \
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
echo "-- code patches that add members (everything we add is named monmmo*)"
unzip -p "$OUT" classes.dex > "$WORK/shipped.dex"
missing=0
# Only members we DECLARE (.method/.field) or CALL (->name); smali labels like :monmmo_not_fairy
# assemble into jump offsets and never appear in a dex as text.
for marker in $(grep -rhoE "^\.(method|field).*[ (]monmmo[A-Za-z0-9_]+|->monmmo[A-Za-z0-9_]+" "$TOOLS/android-smali" \
    | grep -oE "monmmo[A-Za-z0-9_]+" | sort -u); do
  if strings -a -n 6 "$WORK/shipped.dex" | grep -q "$marker"; then
    echo "   $marker: present"
  else
    echo "   $marker: MISSING from the shipped dex"; missing=1
  fi
done
[ "$missing" = 0 ] || { echo "ERROR: a code patch did not reach the APK (rebuild the dex: --rebuild-dex)"; exit 1; }
if [ -f "$ICON_SRC" ]; then
  echo "-- launcher icon, decoded from the built APK the way Android resolves it"
  "$JDK/java.exe" "$TOOLS/app-icon/IconChain.java" "$OUT" | sed 's/^/   /' \
    || { echo "ERROR: the icon chain does not resolve to our art"; exit 1; }
fi
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
