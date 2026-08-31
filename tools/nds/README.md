# NDS ROM readers

Standalone `java <File>.java` tools (JDK 11+ single-file mode) that read an NDS ROM's filesystem
and NARC archives. Written 2026-08-29 once ROM parsing was allowed; the client itself does the
same thing at runtime, so these mirror it rather than guess.

| tool | what it does |
|---|---|
| `NdsProbe.java <rom>` | game code, FAT size, and NARC entry counts for the paths the client loads |
| `ListNarcs.java <rom> [count]` | every NARC with its entry count (filter by count) |
| `Gen5Text.java <rom> <narc> <file>` | decrypts one Gen 5 message archive |
| `Fields.java <rom>` | per-byte statistics over the map-header records, used to locate fields |
| `UnovaDir.java <rom> <out>` | writes `region;bank;map;name` for every Unova map |

Pass narc paths WITHOUT the leading slash and set `MSYS_NO_PATHCONV=1`, or Git Bash rewrites
`/a/0/0/2` into a Windows path.

## What was decoded (White, IRAO)

- **Map headers**: `/a/0/1/2` file 0 — 20496 bytes = **427 records x 48 bytes**. Byte **26** of a
  record is the location-name index. The client's array is 436 = these 427 plus its own 9 custom
  maps, which is why its out-of-range errors reported `length 436`.
- **Map id**: header index = `(map << 8) | bank`, so `bank = index & 0xFF`, `map = index >> 8`.
  Verified: index 356 = bank 100 map 1 = Route 10, the known Unova anchor 2:100:1.
- **Location names**: `/a/0/0/2` file 89 — 117 entries (Nuvema Town, Accumula Town, ...). The
  client shows these via string ids `region*1000 + 140000 + index`.
- **Gen 5 text**: sections at `0x0C + 4*i`; entry table `{u32 offset, u16 length}` from block
  start; XOR key starts at **0x7C89** and advances `+0x2983` per entry, rotating left 3 bits per
  character (`key = (key << 3 | key >> 13) & 0xFFFF`). The 0x7C89 base came from the client's own
  decoder (`f.EO`) - the commonly published `0x2983 * (i+1)` does not decode these ROMs.

Still to do: warp/exit events per map (for tying maps together server-side), and the same pass for
HeartGold and Platinum (their decomps already cover names, so only warps are missing there).

## Warp extraction and auditing

`Warps.java <rom> <out>` writes `region;bank;map;x;y;dir;destBank;destMap;destX;destY`.

Warps are **paired**: record offset +2 is the destination WARP index, and the player lands on that
partner warp's tile. The client discards that field (it re-derives pairing), so it is easy to miss —
using the destination's first warp instead makes every building in a town exit through the same
door. Offset +4 is an exit direction (raw 1-4 through `f.NN.Ai1` to 0-3 = DOWN, UP, LEFT, RIGHT),
which is what lets a warp fire when you walk off the mat you are standing on.

Not every record is a walkable door. Script-driven ones (Gear Station subway, Elite Four rooms)
carry tile (0,-1) or (0,0) and are dropped; warps whose destination is -1 are resolved at run time
by the game and are recovered here by sending the player back along the link that landed them on
that tile — that is what keeps the Pokemon League from being a dead end.

Audit tools, run after any regeneration:

| tool | question it answers |
|---|---|
| `Audit.java <rom>` | are the raw records sane - destinations, partner indices, directions |
| `RoundTrip.java <table>` | walk through each door: is there a warp back on the tile you land on |
| `Stranded.java <table>` | is there any map you can walk into and not walk out of |

Current Unova numbers: 788 warps over 390 maps, 0 maps with no way out, 0 landing tiles without a
return warp, 735 clean round trips. The remainder are legitimate: 42 land on a boundary that
continues to a third map (Castelia's street sections) and 31 are one-way links.
