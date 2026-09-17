# Android client (r32645) code patches

The 20 files under `f/` are the MonMMO-EX versions of the retail classes they replace, as smali
(smali/baksmali 3.0.10, `--api 21`). They are the source of the `classes.dex` that
`ApkPackager prepare` takes as its rebuilt-dex argument; everything else in the dex is retail.

Rebuild:

1. `baksmali d classes.dex -o work --api 21` on the retail `pokemmo-r32645.apk`'s `classes.dex`
   (`classes2.dex` is untouched).
2. Copy `f/*.smali` from here over `work/f/`.
3. `smali a work -o classes.dex --api 21`, then package (`package-android.cmd`; the packager still
   swaps the server keys in `f/ah6`, `f/ie0`, `f/j75`, which is why those are not kept here).

Only disassemble the classes being checked (`--classes Lf/x;`) when verifying a build: a full
baksmali of the 8.4k-class dex is a 2-3 GB JVM.

What each class changes (every edit is marked `# MonMMO-EX` in the file):

| class | change |
| --- | --- |
| `eb6` | Fairy type: ordinal 19 appended, 20-wide effectiveness table, 12 matchups, name string 9226 |
| `r41` | type/skill badge tables sized for 20 types; Mega/Alpha/Omega evolution badges |
| `rm4` | loads the badge and evolution-symbol atlas regions |
| `aq`, `o80`, `zw0`, `y91`, `zp3`, `pr`, `v67`, `gt0`, `fi7`, `j67`, `xf0` | Pokedex regions 6-9, Expansion species/forms (data.pak sections 6/10/11 extensions), sprite-id fold fix, dex text overrides, evolution tab badges |
| `dw2` | follower-rendered raid npc (`Kk0`) |
| `m12` | 31 Expansion Fairy moves registered to Moonlight's staging (`ub(0x15)`), appended after retail's table |
| `i90`, `z57` | stat-change particles anchored CASTER -> ENEMY so `status/down` falls (see the comment in `i90`) |
| `nb4` | `D60` menu-header weekday = real days since the join anchor (a Sunday midnight in the server zone) mod 7, instead of the 4x in-game day counter |
| `aw3` | `DR1` no longer clears `km0.Es` (the native ROM-sound engine) when one song fails to load. Cries are `Sseqj.loadSSEQ(2, 1, species)` on the Black ROM; an Expansion follower's first cry threw and silenced every map song for the session |
