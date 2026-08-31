# The client content file

`data.pak` is gzipped: magic `\x07POKEMMO`, `int32 version` (135), `u8 sectionCount` (16), then
`u8 type, int32 length, payload` for each section. The client's reader is `f/uh.Ou1()`; the
per-section dispatcher `f/uh.eF(ByteBuffer)` is a `tableswitch 1..16` that **skips unknown types**.

## How the launcher writes it

`ClientDataPak.parse(bytes)` decodes the envelope, `edit(codec) { records -> records + additions }`
decodes one section in full and re-encodes it, and `compress()` writes the file back. Sections the
build has no codec for are carried through byte for byte.

The safety property is in `SectionCodec.verify`: before a section is changed, the **stock** payload
is decoded and re-encoded, and the build aborts unless the result is identical to what it started
with. A record layout that is wrong in any field fails there rather than in the client. This exists
because an earlier version appended bytes to a section on an assumed header, which corrupted the
file and stopped the client booting - a failure that looked nothing like its cause.

`ClientDataPakTest` runs the same check against the installed client's own `data.pak`.

Sections are processed **in file order**, and later ones attach to species already registered:
section 1 does `registry.get(id)?.moves = list`, null-safe, so an unknown id is dropped in silence.
Editing sections in place preserves that order; appending a new section 10 at the end would load its
records and lose everything keyed to them.

## What each section supplies

| Section | Contents | Records | Codec |
|---|---|---|---|
| 10 | Species records, 24 bytes fixed | 53 | `SpeciesCodec` |
| 1 | Level-up learnsets | 720 | `LevelUpLearnsetCodec` |
| 2 | Egg, tutor and teachable learnsets | 2097 | `ExtraLearnsetCodec` |
| 6 | Optional species fields, flag-driven | 527 | `SpeciesDetailCodec` |
| 11 | Regional Pokedex lists | 5 | `RegionalDexCodec` |
| 12 | Per-move cost, tier and item price | 559 | `MoveExtraCodec` |
| 4 | Move table: accuracy, power, PP, type, effects | 842 | `MoveCodec` |
| 3, 5, 7, 8, 9, 13, 14, 15, 16 | Not yet identified | | none |

## data.pak overrides the ROMs; it does not replace them

Section 10 holds **only ids 1000-1052**. Dex 1-649 are absent: the client reads their stats, types
and abilities from the ROMs. Section 10 is where species the ROMs cannot supply go, which is why new
species take a wire id above 1052.

Section 6 is sparse in the same way - 527 records, egg groups on just 43 - so it is an override
layer over ROM data. A new species has no ROM entry behind it, so every field it needs must be
written out in full rather than left to default.

Section 1 is the exception: it lists all of 1-649, so learnsets are not read from the ROMs.

## Reading round-trips carefully

Round-trip proves where one record ends and the next begins. It does **not** prove what the fields
mean. Section 12 parses exactly as `u16 id, u16 cost, u8 tier, u16 flags` with a single flag bit
worth eight bytes; it also parses exactly as `u16 id, u16 cost, u16 tier, u8 pairCount, pairs`. Both
consume every byte, and only the second matches the bytecode. Where the boundary sits is a question
for the disassembly.

## Adding a move

Canonical moves 1-559 appear in section 4 with **no flags set at all** - just their effects. Their
accuracy, power, PP and type come from the ROMs. Moves above 559 carry the full definition, which is
how the client's own event moves (1000-1096, named at string `110000 + id`) work.

So an imported move is written the same way: a record with accuracy, power, PP and type, and its
name and description patched into the string table. Ids 560-999 are free; 1000 and up belong to the
client. The fields whose meaning is not settled are left unset rather than zeroed, and the effect
list is left empty - a move added this way lands and deals damage, but its secondary effect is not
scripted yet.

One trap on the Expansion side: 334 moves write their numbers as
`B_UPDATED_MOVE_DATA >= GEN_n ? new : old`. Reading the first number off the line yields neither -
Flying Press came through at power 0 that way. `MoveText` resolves the setting from
`include/config/battle.h` and `include/config/general.h` rather than assuming it.

## Move attributes

The effect list on a move record is not battle scripting. Each `effectId` is a **string id**, and
the parameters fill its placeholders: 3400 is "Makes contact with the foe.", 3502 is
"{00}% chance to Paralyze the foe". These are the lines the info panel prints under a move. The
battle itself is the server's business, which is why no part of data.pak describes one.

`MoveAttributes` derives them from the Expansion's flags, `.argument` and `.additionalEffects`. The
client lists them in a fixed order that is **not** ascending by id - Snatch precedes Protect, and
"Makes contact" sits between the special mechanics and the newer flags. That order was derived from
the client's own 559 records rather than guessed: every pair that co-occurs does so consistently,
138 ordered pairs with no contradictions, giving one total order over all 43 attributes.

`MoveAttributesTest` measures the mapping against those 559 records - 490 match exactly. Most of the
rest are real differences rather than defects: PokeMMO plays by roughly Gen 5 flag rules while the
Expansion is set to `GEN_LATEST`, so it marks Disable, Mimic, Haze and Sketch as bypassing
Substitute where PokeMMO does not. The Expansion is the source of truth here, so we follow it.

## Parsing moves_info.h

Three traps, each of which loses data silently rather than failing:

- **`#if` / `#else` branches**, in 64 entries. Low Kick keeps its Gen 1 flinch behind an `#else`, so
  reading the whole entry gives it a 30% flinch it lost in Gen 2. The directives are resolved
  against the Expansion config.
- **`MOVE_POWER_SHIFT` closes its brace at column 0** where every other entry indents it. A pattern
  anchored on the closing brace runs past it and swallows `MOVE_STONE_AXE`. Entries are anchored on
  the next entry's header instead.
- **`MOVE_10000000_VOLT_THUNDERBOLT`** begins with a digit, so a letters-only symbol pattern never
  declares it and the move disappears from the id map.

`MoveTextTest` asserts that the number of parsed entries equals the number of headers in the file,
which is what catches all three.
