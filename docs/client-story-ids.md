# Client 31914 story flag/var catalog

Every id the client's story whitelist (`f/LG0.Yw1`) accepts, named by tracing its consumer in
the client bytecode (2026-08-31). The server-side mirror lives in
`server.game/.../services/ClientStoryWhitelist.kt`; StoryClientState only forwards ids listed
here. GBA-region ids are the ROM's own flag numbers, so the story scripts set them at the right
moment and the sync layer forwards them - nothing manual. NDS ids are PokeMMO's numbering.

The client store is one `short -> short` map per region (`f/qK` -> `f/Iq`); a flag is a var
with a nonzero value. Region 0x80 addresses a global/account store (`f/WA`) with no whitelist.

## Badges + champion (`f/qK.O9`, badge HUD counts these)

| region | badges 1..8 | champion slot |
|---|---|---|
| 0 Kanto | 2080-2087 (FRLG FLAG_BADGE01..08_GET) | 2092 |
| 1 Hoenn | 2151-2158 (Emerald FLAG_BADGE01..08_GET) | 2175 (FLAG_SYS_POKEMON_LEAGUE_FLY) |
| 2 Unova | 1521-1528 | 2400 |
| 3 Sinnoh | 1361,1362,1365,1363,1364,1366,1367,1368 (client remaps 3..5) | 2404 |
| 4 Johto | 1361-1368 | 2404 |

## Level caps (`f/LG0.cU1` + `LG0.HA`) - the client enforces these per badge count

Kanto 20,26,32,37,46,47,50,55,62,100 · Hoenn 20,24,28,33,35,38,44,48,58,100 ·
Unova 20,24,27,31,35,38,43,46,56,100 · Sinnoh 20,27,29,34,37,43,46,52,60,100 ·
Johto 20,24,29,32,37,39,41,46,48,100. Johto overrides: 4 badges + flags {2495,2500} -> 39;
5 badges without 1365 -> 40; 8 badges + 1496 -> 55.

## Running Shoes (input controller `f/Ro.EJ1`)

Kanto 2095 (FRLG FLAG_SYS_B_DASH) · Hoenn 2240 (Emerald FLAG_SYS_B_DASH) · Unova 2403 ·
Sinnoh/Johto 1360.

## Fly / town map (`f/FY0` tables + `f/xr0` UI; fly needs move 19 or item 1181 + a badge)

Fly-permission badge per region: Kanto 2082, Hoenn 2156, Unova 1525, Sinnoh 1363, Johto 1365.

- Kanto 2192-2210 = FRLG FLAG_WORLD_MAP_*: 2192 Pallet, 2193 Viridian, 2194 Pewter,
  2195 Cerulean, 2196 Lavender, 2197 Vermilion, 2198 Celadon, 2199 Fuchsia, 2200 Cinnabar,
  2201 Indigo Plateau, 2202 Saffron, 2203-2209 Sevii Islands (One..Seven), 2210 Route 4
  Pokecenter. 2228 = extra landmark (Route 10/Rock Tunnel area).
- Hoenn 2159-2174 = Emerald FLAG_VISITED_*: 2159 Littleroot .. 2174 Ever Grande (MAPSEC
  order: towns then cities). 2216 = Battle Frontier landmark, 2228 = Pokemon League landmark.
- NDS: flag = 2480 + town-map landmark index. Johto indices hardcoded in f/FM
  (0 New Bark 2491, 1 Cherrygrove 2492, 2 Violet 2493, 3 Azalea 2494, 4 Cianwood 2495,
  5 Goldenrod 2496, 6 Olivine 2497, 7 Ecruteak 2498, 8 Mahogany 2499, 9 Lake of Rage 2500,
  10 Blackthorn 2501, 11 Mt. Silver approach 2502; HGSS-Kanto block inferred 2480-2490;
  extras 2507/2510 Safari Zone/2511 Battle Frontier/2513/2515-2517). Unova/Sinnoh orderings
  live in encrypted/data files (Sinnoh /data/tmap_flags.dat), not bytecode; Unova extras
  1568/1570 are fly unlocks for maps MX 376/418.

## Story gates + map state

- `qK.Ki` (exhaustive): engine ids 144-146 -> all 8 Kanto badges; 243-245 -> Johto flag 1496.
- Johto 1490 = hardcoded ALWAYS-TRUE sentinel ("no requirement").
- Johto 1495 = a var counter (read by several systems); 1496 = story gate + level-cap override.
- Johto 2451/2459 = terrain/tile-behavior unlocks (X3.eA1 rewrites behaviors when set).
- Johto 2409 = unlocks a customization option (f/Te).
- Sinnoh var 16469 (0x4055) = DISTORTION WORLD progression; >= 14 changes world geometry
  (tw_arc.narc) - a real client-rendered map-state var.
- Hoenn 303 gates a widget (f/oS).
- Per-badge HM/field-move permission classes exist for NDS regions (badge flags 1362-1368 /
  1524-1527 read by small permission classes).

## Global store (region 0x80, no whitelist)

770 = egg incubator feature unlock (HUD button); 771 = +5 incubator slots; 772/773/774 = +1
slot each.

## Seasonal event trackers (refresh sets in the 0x2A apply path)

Var 1044 Christmas counter · var 1010 Halloween counter · 1520-1531(+1533) Christmas 12-days
species tracker · 2560-2567 Halloween 8-species tracker · 564-575(+588) Lunar New Year zodiac
tracker · 288-427 (5 groups, 108 rows) Halloween progress grid. These live in event stores and
do not collide with same-numbered region ids.

## Whitelisted with NO client consumer (receipt-only; safe to ignore until needed)

Kanto 675, 2116, 2121 · Hoenn 214-216, 253, 281, 305, 306, 421-425, 467-473, 2146 (by
position = Emerald FLAG_SYS_NATIONAL_DEX) · Sinnoh 2546, 2548 · Johto 299, 607 and the
town-map extras beyond their map use · Unova 2400 beyond the champion slot.
