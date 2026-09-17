#!/usr/bin/env bash
# Compose the client's theme mod: a third-party theme zip exactly as its authors shipped it, plus
# the MonMMO-EX pieces that a theme would otherwise leave behind.
#
#   compose-theme-mod.sh <their-theme.zip> <out.zip>
#
# Why this exists: a theme is a complete set, not a patch. When a mod theme is selected the client
# loads THAT theme's gfx/ui/res, and - verified in the atlas loader (f/Rp0) - if the theme declares
# its own sprite atlas, that atlas REPLACES data/sprites/atlas/main.atlas rather than merging with
# it. So without this step, selecting a theme silently drops:
#   - the Fairy (type 19) move button and its type19 style definitions
#   - the atlas pages carrying the FAIRY type badge (7 languages) and the mega/alpha/omega symbols
#
# Everything here is regenerated from the authors' zip, so their next release drops straight in:
# re-run this script on the new zip. Their info.xml keeps its authors, revision and constants,
# which is what preserves the in-game colour picker and the trainer-card chart toggle.
set -euo pipefail

SRC_ZIP=${1:?usage: compose-theme-mod.sh <their-theme.zip> <out.zip>}
OUT_ZIP=${2:?usage: compose-theme-mod.sh <their-theme.zip> <out.zip>}
HERE="$(cd "$(dirname "$0")" && pwd)"
OVERLAY="$HERE/../android-overlay/assets/data"
JDK=${JDK:-"/c/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot/bin"}

EXT_DIR="data/MonMMOFairy_mobile"   # must not be named `default` or `android` (the parser refuses)
EXT_NAME="MonMMO Fairy"
EXT_REVISION=8                      # their theme ships revision 8 and this client accepts it

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
MOD="$WORK/mod"
mkdir -p "$MOD"
unzip -q "$SRC_ZIP" -d "$MOD"

MOBILE="$MOD/data/DarkTheme_mobile"
[ -d "$MOBILE" ] || { echo "ERROR: $SRC_ZIP has no data/DarkTheme_mobile - is this the right theme?"; exit 1; }

# Source art the client never reads (the authors ship their atlas .psd) has no business on a phone.
find "$MOD" -iname "*.psd" -print -delete | sed 's/^/[theme] dropped /'

# 1. Their atlas misspells one region: flag_fil ships as fflag_fil, so the Filipino flag would be
#    missing from the registry. Every other one of the 458 names matches retail exactly.
if grep -q '^fflag_fil$' "$MOD/atlas/main.atlas"; then
  sed -i 's/^fflag_fil$/flag_fil/' "$MOD/atlas/main.atlas"
  echo "[theme] fixed the fflag_fil region name"
fi

# 2. Append our atlas pages, so the FAIRY badge and the mega/alpha/omega symbols survive the swap.
OUR_ATLAS="$OVERLAY/sprites/atlas/main.atlas"
{ echo; awk '/^monmmo-fairy\.png$/{found=1} found' "$OUR_ATLAS"; } >> "$MOD/atlas/main.atlas"
cp "$OVERLAY/sprites/atlas/monmmo-fairy.png" "$OVERLAY/sprites/atlas/monmmo-evo-symbols.png" "$MOD/atlas/"
echo "[theme] appended $(grep -c '^monmmo-' "$MOD/atlas/main.atlas") MonMMO-EX atlas regions"

# 3. The Fairy move button, drawn from THIS theme's own Psychic button so it matches its language.
mkdir -p "$MOD/$EXT_DIR/res"
"$JDK/java.exe" "$HERE/MakeFairyButton.java" \
  "$MOBILE/res/ui-battle-moves.png" "$MOBILE/textures/star-bold.png" \
  "$MOD/$EXT_DIR/res/monmmo-fairy-move.png"
cp "$HERE/fairy-extension/theme.xml" "$MOD/$EXT_DIR/theme.xml"

# 3b. The version line under this theme's own wordmark, in its own accent colour. BATTLE_TTF is the
#     client's dialogue font, which only the caller has (it lives in the retail APK).
BADGE_TEXT=${BADGE_TEXT:-"Pirated Version"}
BADGE_COLOUR=${BADGE_COLOUR:-"#6BEBF0"}          # the dark theme's cyan, mirroring its own logo
THEME_BG="$MOD/data/DarkTheme_theme/res/bg.png"
if [ -n "${BATTLE_TTF:-}" ] && [ -f "$BATTLE_TTF" ] && [ -f "$THEME_BG" ]; then
  "$JDK/java.exe" "$HERE/MakeTitleBadge.java" "$THEME_BG" "$BATTLE_TTF" \
    "$BADGE_TEXT" "$BADGE_COLOUR" "$WORK/badge.png"
  mv "$WORK/badge.png" "$THEME_BG"
  echo "[theme] drew \"$BADGE_TEXT\" under this theme's wordmark"
else
  echo "[theme] no BATTLE_TTF given - theme ships without the version badge"
fi

# 4. Declare the extension. The client applies enabled extensions on top of whatever theme is
#    selected, so this covers their theme, the stock one, and anything installed later.
awk -v dir="$EXT_DIR" -v name="$EXT_NAME" -v rev="$EXT_REVISION" '
  /<\/resource>/ && !done {
    print "\t<theme_extensions>";
    printf "\t\t<theme_extension path=\"%s/\" name=\"%s\" revision=\"%s\" is_mobile=\"true\"/>\n", dir, name, rev;
    print "\t</theme_extensions>";
    done = 1;
  }
  { print }
' "$MOD/info.xml" > "$WORK/info.xml" && mv "$WORK/info.xml" "$MOD/info.xml"
grep -q "theme_extension " "$MOD/info.xml" || { echo "ERROR: could not add the theme_extensions section"; exit 1; }

# 5. Repack (no `zip` on this machine; jar writes an ordinary zip).
rm -f "$OUT_ZIP"
"$JDK/jar.exe" --create --file "$OUT_ZIP" -C "$MOD" .
echo "[theme] wrote $OUT_ZIP ($(stat -c%s "$OUT_ZIP") bytes)"
