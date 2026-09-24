# Gen 5 (Unova) elevators

Investigation of the Castelia office-building elevators in Pokemon White (IRAO), for MonMMO-EX,
the owner's own self-hosted server. Data extractor: `tools/nds/Elevators5.java`. Outputs:
`server.game/nds-elevators-2.txt` and `server.game/nds-script-ranges-2.txt`.

"Verified" below means read off ROM bytes or disassembled ARM code. "Inferred" means it fits all
the evidence but no instruction was traced to prove it.

## 1. The 10000+ "standard scripts" are ordinary script files (verified)

The belief that ids 10000+ live in no script file was wrong. Overlay 10 holds a **script-id range
table** at RAM `0x021701C8`: 46 records of 10 bytes each, `u16 first, u16 last, u16 script file
(/a/0/5/7), u16 arc (always 3), u16 text bank (/a/0/0/3)`. The lookup function at ov10
`0x02158CF0` (literal `0x021701C8` at `0x02158D8C`, loop bound `cmp r2,#46`) walks the records
in order and returns the first match's file and text bank, plus the entry index `id - first`.
When nothing matches and `id >= 1`, the script is map-local: the header's script file (+6) and
text bank (+10), entry `id - 1`. When `id == 0` it falls back to file 854 / text bank 158.

The ranges you already know check out against this table: 2100-2199 → file 855 / bank 346 (the
nurse), 2800-2819 → 862 / 283, 7000-7799 → 864, 8000-8799 → 865 (hidden items), 3000-6999 → 863.
All 46 ranges are in `nds-script-ranges-2.txt`.

| bg script | range → file / text bank | entry | what it is |
|---|---|---|---|
| 10405-10411 | 10405-10424 → **888 / 261** | 0-6 | elevator panel beside the door, var 0x8020 = 0 (one per building) |
| 10412-10418 | same | 7-13 | elevator door tile, var 0x8020 = 1 (one per building) |
| 10431 | 10430-10434 → 890 / 461 | 1 | Nimbasa map 93 (11,9): `CMD_208` picks a mode, then `CallStart` opens an app. Bank 461 is the Battle Test dialogue. **Not an elevator** (no 0x1CB). Which feature it is has not been confirmed. |
| 10270 (for reference) | 10270-10279 → 876 / 257 | 0 | Nacrene: Deoxys and the meteor |
| 10315 (for reference) | 10315-10324 → 880 / 421 | 0 | Nimbasa map 95: Rotom and the appliance boxes |

Text bank 261: `0 Cancel, 1 1F, 2 2F, 3 (empty), 4 11F, 5 22F, 6 47F, 7 55F, 8 Which floor would
you like to go to?`

## 2. Where the floor list lives (verified)

Each panel script starts with `LockAll; StoreValueInVar 0x8020 kind; 0x1CB <s32>; CallRoutine
@622`. **Opcode 0x1CB takes a u32** that is relative to the end of the command. It points at the
building's **floor table**, which sits inside script file 888 right after the building's entries.
Handler ov21 `0x021B6AF8` reads the word with `0x0201134C`, adds the script pc (`[ctx+20]`), and
stores the result at `[work+0x10C]` (ov10 `0x0215A6F0`).

Floor table: 10-byte rows. The list ends at the first row whose first u16 is 0xFFFF (counter
ov21 `0x021B6AC8`; row i is at `table + 10*i`, ov21 `0x021B6ABC`).

| offset | field |
|---|---|
| +0 | u16 label = entry in the script's text bank (261) |
| +2 | u16 destination map header (bank = h & 0xFF, map = h >> 8) |
| +4 | u16 x (tile) |
| +6 | u16 height |
| +8 | u16 row / "y" (tile) |

The 1CD handler shifts +4/+6/+8 left by 16 into a VecFx32 (x, y, z). One tile is 16 units x
FX32_ONE, so the values are tile coordinates. It is **inferred** that +6 is the height and +8 the
row: the order matches the warp records (x, height, y), and every row here holds 7, 0, 0.

The seven tables (all in file 888; every landing is (7,0), inside the lift car):

| table @ | floors (label → header) | panels | rows emitted |
|---|---|---|---|
| 136 | 1F → 44, 47F → 45, 55F → 46 | maps 44, 45, 46 (x2) | 18 |
| 220 | 1F → 47, 22F → 48 | 47, 48 | 8 |
| 296 | 1F → 49, 11F → 50 | 49, 50 | 8 |
| 372 | 1F → 54, 11F → 55 | 54, 55 | 8 |
| 448 | 1F → 56, 2F → 57 | 56, 57 | 8 |
| 524 | 1F → 58, 47F → 59 | 58, 59 | 8 |
| 600 | 1F → 60, 47F → 61 | 60, 61 | 8 |

Total: 66 rows. That is one row per (panel bg event, floor), and the current floor is included
with `current=1`. No other script in the corpus uses 0x1CB, so these seven buildings are White's
only table-driven elevators.

## 3. What the panel script does, step by step (shared routine at file 888 @622)

1. `PlaySound 1351`. Routine @740: `StoreHeroPosition`, make a door object at hero + (2, -1)
   (`CMD_127 kind 1`), open it, walk into the car, close the door, free the door, then face
   south (movement @872). The walk is movement @860 for kind 0 (east 1, north 2) and @852 for
   kind 1 (north 2). Both paths end on **(7,0)**, the car tile, which matches the tables.
2. `EventGreyMessage 8` ("Which floor...?"), then `Multi 31 1 0 0 1 var0x8010` opens the list.
3. **`0x1CC` (no arguments)** is handled by ov21 `0x021B6B14`. For each row whose header differs
   from the u16 at `[ctx+4]`, it calls the add-menu-entry function ov10 `0x0215A810` with
   `(label, 0xFFFF, value = row index)`. This is the same function `0xAF` uses; its handler at
   `0x021B0364` reads `text, desc, value(var)`. Verified: the loop, the header compare, and the
   entry value. **Inferred**: that `[ctx+4]` is the current map header. It is the only reading
   that makes sense, because each floor's menu would then hide that floor.
4. `0xAF 0 0xFFFF 255` adds **Cancel** (bank 261 entry 0, value 255). `0xB0` shows the list.
5. If the result is 255 (Cancel) or 0xFFFE (B), jump to the exit (@732).
6. Otherwise: `PlaySound 1672`, `CMD_1AE`, `CMD_1B1`, then **`0x1CD var`** (ov21 `0x021B6B84`,
   reads one u16 var). If `var < rowCount`, it builds a map-change event through ov20
   `0x021854D0(fieldSystem, ?, header, &vec, dir = 1)` → `0x02185478` → location builder arm9
   `0x020141A0(zone, dir, x, y, z)`. It returns 1, which suspends the script across the map
   change. Then `CMD_1AD`, `CMD_1B1`, delay 15, `PlaySound 1768` (the ding).
7. Exit @732 → routine @880 on whichever map you are now on: door at hero + (0, +1), open it,
   **walk south 2** (movement @940), close and free it. The Cancel path runs the same routine,
   so you step back out on the floor you started from.

Direction constant 1 goes through `0x020142C0` first. **Inferred** to mean south (the Gen 4/5
direction enum is N=0, S=1, W=2, E=3). This agrees with facing south before the menu and walking
south after arrival.

### Command-table fix (verified from the handlers; in this branch)

`tools/nds/bw1-script-commands.txt`: `459;CMD_1CB;H` → `L`, and new rows `460;CMD_1CC;` (no
arguments) and `461;CMD_1CD;H`. With the old sizes, the high half of the 0x1CB offset decoded as
a fake `Nop`, and the routine stopped at 0x1CC (`DecodeStopped` @647). After a re-dump, only file
888 changes (entriesOk 3572 → 3573; the whole routine now decodes through 1CC, 1CD and the exit).
`server.game/nds-scripts-2.txt` was **not** regenerated here.

Side finding: the real script command table is in **overlay 10 at RAM `0x0217064C`**. It has 609
Thumb handler pointers (null = unimplemented), is referenced at ov10 `0x02158CCC`, and the count
0x261 is at `0x021705F8`. The elevator handlers live in ov21. The 416-entry table at ov21
`0x021D5EDC` that the decoder-audit memory calls "the script command table" is something else:
its entry for CallRoutine is `handler 0x00000034`. Re-check any conclusion that depends on it.

## 4. How the client shows it

The client has **no elevator floor UI** of its own. The ROM menu is the generic Gen 5
multichoice (0xB2 / 0xAF / 0xB0), and the server already turns that into the client's DS
text-button list: dialog kind wire 49, `DialogService.dsTextListMenu`, which needs all buttons
from one DS bank. The labels and Cancel are all in bank 261, so this works unchanged. Button i
answers index i, and the server maps that index back to the row value (or 255).

In desktop client 31914, "elevator" appears only as a model-name filter in the DS world classes'
door-animation method (`f.uf`, `f.LZ0`, `f.NUl` `.S10`; strings "door", "badgegate", "elevator",
"ele_door1"). So the client animates the elevator door models, which is the `CMD_127/129` door
path the generator already emits as `DsDoor`. The GM Teleport window `f.EU1` only builds
`//moveto2` chat commands and holds no floor data. None of this was checked on the r32645
Android client.

## 5. What the server still needs (not wired here)

1. **Bind the chunk ids through the ROM range table** (`nds-script-ranges-2.txt`) instead of the
   hand-written `chunkFiles()` / `unboundStdCalls()`. At minimum, bind `NDS_CHUNK_10405..10418` →
   `U888` entries 0..13. Careful: a blanket bind changes 10100/10105 (the Center clerks, now served
   by `NdsStandardScripts`) and every other engine id. Decide per id.
2. Re-dump the corpus with the fixed command table so file 888 decodes fully.
3. Generator/interpreter: `CMD_1CB n` → the table is at `pc + 6 + n` in file 888 (the tableOffset
   column). `CMD_1CC` → at run time, the menu rows are the ones for that table whose `current`
   is 0 for the player's current header (text = floorLabelTextId, value = floorIndex). The static
   `SetTextScriptMessage 0 0xFFFF 255` Cancel then follows. Today the generator builds the `Menu`
   items statically at `CloseMulti`, so it needs a dynamic-items marker.
4. `CMD_1CD var` → if `var < rows`: raw warp to (region 2, destBank, destMap, destX, destY),
   facing DOWN, and **keep running the script on the new map**, which does the door and the 2-step
   walk-out.
5. The landing (7,0) is a **blocked** tile in `nds-land-2.txt` (the car is behind the wall row).
   The arrival must not add the generic NDS emergence step, and movement validation must accept
   the scripted walk (7,0) → (7,2).
6. Unverified in game: the boarding routine puts its door object at hero + (2, -1). That gives
   (8,1) from the kind-0 panel and (9,1) from the kind-1 door tile, but the arrival puts it at
   (7,1). Check where the client plays `ele_door1` before trusting `DsDoor` tiles for elevators.
   `CMD_1AE` / `CMD_1AD` / `CMD_1B1` (probably start/stop/wait of the car shake) are unidentified
   and the generator already drops them.

Reproduce: `MSYS_NO_PATHCONV=1 java tools/nds/Elevators5.java "<White.nds>"
server.game/nds-elevators-2.txt server.game/nds-script-ranges-2.txt`
