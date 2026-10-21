# Pirated Version (PokeMMO Pirated Version)

RaKeT's own PokeMMO server (Kotlin, this repo) plus his patched build of the retail PokeMMO Android
client r32645, run on his own VPS for people he knows. Players supply their own ROMs.

## Before you start

1. Read the memory note `handoff-2026-10-20.md`, then `project-rename.md`.
2. Grep the detailed memory notes for your topic before you dig. `MEMORY.md` is only the index; the
   ~85 notes behind it hold the protocol, opcode, ROM and design facts. They live in
   `%USERPROFILE%\.claude\projects\C--Users-raket-Projects-PokeMMO-Pirated\memory\`.
3. Check the docs below that cover your area.
4. Code and `git log` beat notes. Old notes use the old names (`monmmo`, `de.fiereu.openmmo`,
   `~/.monmmo`, `Downloads\...`); read them as today's `pirated` names and folders.
5. Save what you learn back into the note for that subject (one line or two, with the reason), not a
   new file per finding. In-flight notes are temporary to-dos for interrupted work, not history.

## Folder layout (`C:\Users\raket\Projects\PokeMMO-Pirated`)

- `repo\` this checkout: github.com/raket44/MonMMO-EX, branch `content-parity-2026-09-13`
- `local\` tools (smali, apktool), build cache (`local\build\classes.dex`), android-signing, key and
  VPS backups
- `apks\` retail r32645 (the base) and r32920; `builds\` built APKs; `roms-nds\` DS ROMs;
  `re-tools\` ROM text dumps and RE helpers
- Scripts find these from `repo\..`, so keep them side by side.

## How RaKeT works

- Verify, never guess. Prove client behaviour from the r32645 smali, ROM behaviour from the ROMs,
  script dumps or overlay code, server behaviour from the VPS log. If he says something is in the
  game, it is: find it. Never ask him for screenshots or clips of retail behaviour.
- Short, plain answers. He is usually playtesting in BlueStacks or on his phone. Do the work
  yourself rather than handing him steps.
- Commit and push after every finished piece, then check `git ls-remote`. He has always authorized
  commits and pushes on this repo. Never invent "don't commit X" rules; ask him and record his reason.
- Deploy only when he says "deploy" (`bash deploy/vps/deploy.sh`, add `--data` when
  `server.game/*.txt`, monsters.json or keys changed). "force deploy" (`--force`) restarts with
  players online; check the server log for anyone mid-battle or mid-scene first.
- Never change another player's character in the DB without his OK. Don't ask him to reset story
  flags; find out why the state is what it is.
- Retail first: Expansion content only adds, and is built into the real client code and data, not
  mods or overlays. The theme mod was dropped (Sep 17, removed from the build Oct 21).
- New key items, elevation-lock barriers and design changes need his agreement first.
- Credentials stay in local memory only, never in git, commits or shipped packages.
- His `pokeemerald-expansion` is a separate game; this repo pins official Expansion 1.16.0.
- Naming: OpenMMO (Fiereu, AGPL) is the upstream this grew from; keep only the README credit.
  MonMMO is a different real team RaKeT almost joined; its name must not appear (the GitHub repo
  name is the last trace). Text name = "Pirated Version"; packages = `pirated.*`.

## Repo docs

Client reverse engineering (written against desktop 31914; confirm against r32645 smali):
- `docs/client-packet-map.md` s2c/c2s opcodes and what each packet carries
- `docs/client-data-pak.md` the client's data.pak content file and its sections
- `docs/client-story-ids.md` client story flag/var catalog (the 0x2A whitelist ids)
- `reference/client-31914/README.md` the extracted desktop client classes in reference/

Unova and DS:
- `docs/gen5-elevators.md` Unova elevators: ROM floor tables, panel scripts, floor menu
- `tools/nds/README.md` the NDS ROM readers and extractors in tools/nds (land, npcs, warps, scripts)

Android client build:
- `tools/client-package/android-smali/README.md` every patched r32645 class and why (stale bits:
  `i90`/`z57` were dropped Sep 18; `package-android.cmd` is gone, use `build-android.sh`)
- `tools/client-package/theme-assets/CREDITS.md` the night-sky login background's authors
- `reference/sprite-packs/CREDITS.md` battle sprite sources;
  `reference/sprite-packs/MISSING-SPRITES.md` species still lacking Gen 5-style art

Server concepts and guides (`docs/src/content/docs/`, the docs site; `index.md` files are tables
of contents):
- `concepts/database.md` Flyway migrations as the source of truth, schema rules
- `concepts/interest-manager.md` how events reach nearby players
- `concepts/scripts.md` the script system's pieces
- `concepts/story-state.md` story flags/vars API
- `guides/writing-scripts.md`, `guides/porting-scripts.md` writing and porting map scripts
- `guides/writing-generators.md` codegen generators from the decomps
- `guides/running-the-client.md` running a client against a local server (desktop-era)
- `guides/deploying-the-feed.md` the client feed and its keys (desktop-era)

Setup and data:
- `SETUP.md` setting up a dev machine; `README.md` overview and build
- `keys/README.md` server keys: `keys/build` is canonical and re-stamped into resources every
  build; restore it before the first Gradle build on a new machine
- `reference/berries/README.md` retail berry farming tables
- `protocols.game/src/test/resources/fixtures/README.md` captured packets for codec round-trips
- `common/README.md` the common module

## Main commands

- Android client: `bash tools/client-package/build-android.sh` (Git Bash) ->
  `builds\PokeMMO-Pirated.apk`; `--rebuild-dex` after changing android-smali.
- Server deploy: `bash deploy/vps/deploy.sh [--data] [--force]`; accounts: `vps-user.cmd`.
- Local servers: `start-servers.bat` / `stop-local-servers.cmd`. Production is the VPS.
