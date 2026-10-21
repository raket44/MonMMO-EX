# RecompSwordC Drop-In Guide

## What goes here

`RecompSwordC.zip` (from the project owner) contains statically recompiled Pokémon Sword CPU code
for PC. Once extracted, place the output object files or static library here so the CMake build
can link them into `libsword_bridge.so`.

## Steps

1. Obtain `RecompSwordC.zip` and extract it.
2. Place recompiled objects into `decomp/pokesword/recomp/`:
   - Either individual `.o` files
   - Or a single `libsword_recomp.a` archive
3. Uncomment the appropriate lines in `CMakeLists.txt` (two options shown there).
4. Build:
   ```sh
   cmake -B build-ommo -DCMAKE_BUILD_TYPE=Release decomp/pokesword/pirated
   cmake --build build-ommo
   ```
5. Copy `build-ommo/libsword_bridge.so` to a directory on the JVM's `LD_LIBRARY_PATH`,
   or set `-Djava.library.path=<dir>` in the server startup flags.

## Without the recomp

`SwordShieldService.kt` falls back to a Kotlin stub encounter table automatically when
`libsword_bridge.so` is absent. All Galar MMO features work; the native recomp only enables
exact Sword game-logic execution (map scripts, NPC AI, etc.).

## Files in this directory

| File | Purpose |
|------|---------|
| `CMakeLists.txt` | Build definition for `libsword_bridge.so` |
| `SwordBridge.h` | C API: `sword_bridge_init`, `sword_bridge_pick_encounter`, `sword_bridge_shutdown` |
| `SwordBridge.cpp` | Implementation + JNI entry points for the JVM |
| `GalarEncounters.h` | Galar encounter table structs and dex ID constants |
| `GalarEncounters.cpp` | Full 15-area encounter table (Routes 1-10, Wild Area zones) |
