# PokeMMO Pirated Version

RaKeT's own Pokemon MMO: a Kotlin server and a patched build of the PokeMMO Android client,
run on a private VPS for people he knows. Kanto, Hoenn and the DS regions share one world, and
the Expansion species, the Fairy type, raids, breeding and more are built straight into the
client's own code and data, not added as mods. Players supply their own ROMs.

## Contents
- [Layout](#layout)
- [Building the server](#building-the-server)
- [Building the Android client](#building-the-android-client)
- [Running and deploying](#running-and-deploying)
- [Configuration](#configuration)
- [Documentation](#documentation)
- [License](#license)
- [Disclaimer](#disclaimer)

## Layout

The repository sits in a project folder next to the files that are not in git:

```
PokeMMO-Pirated\
  repo\       this repository
  local\      tools (smali, apktool), build cache, APK signing key, backups
  apks\       the retail PokeMMO APK the client is built from
  builds\     built APKs (PokeMMO-Pirated.apk)
  roms-nds\   DS ROMs, only for re-extracting DS data
```

The build scripts find these from `repo\..`, so keep them side by side. `SETUP.md` covers a new
machine.

## Building the server

Game data is generated at build time from the [pret](https://github.com/pret) decompilations
and `pokeemerald-expansion`, which are git submodules under `decomp/`. Clone with them:

```bash
git clone --recurse-submodules <repo-url>
# or, for an existing clone:
git submodule update --init --recursive
```

Without the submodules the `:codegen` build fails. `decomp/pokeblack` holds Unova's item data
as committed files instead of a submodule.

```bash
./gradlew :server.login:installDist :server.game:installDist
./gradlew :server.game:test
```

### ROMs

A GBA dialog id is a file offset into the retail ROM, so resolving ids needs the ROMs. The
result is committed, one file per region under `codegen/dialog/`, and the build reads only those.
ROMs are needed only to **change** that table after a decomp bump: put the Emerald (`BPEE`) and
FireRed (`BPRE`) ROMs in `roms/` (gitignored, this project ships no ROMs) and run:

```bash
./gradlew :codegen:refreshDialogTable   # then commit codegen/dialog/
```

DS data (maps, scripts, encounters) is extracted from the ROMs by `tools/nds` and committed the
same way.

## Building the Android client

```bash
bash tools/client-package/build-android.sh            # Git Bash
bash tools/client-package/build-android.sh --rebuild-dex   # after changing android-smali
```

It stages the Expansion content into the retail APK's own `data.pak` and string table, applies
the patched client classes in `tools/client-package/android-smali`, points the client at the
VPS, replaces the server keys, and signs the result as `builds\PokeMMO-Pirated.apk`. Every
patched class is explained in `tools/client-package/android-smali/README.md`.

## Running and deploying

Production is the VPS. Deploy from the repository root:

```bash
bash deploy/vps/deploy.sh            # refuses while players are online
bash deploy/vps/deploy.sh --data     # also uploads server.game/*.txt, monsters.json and keys
bash deploy/vps/deploy.sh --force    # restarts even with players online
```

Accounts on the VPS are managed with `vps-user.cmd`. For local testing, `docker compose up -d`
starts the two Postgres databases and `start-servers.bat` / `stop-local-servers.cmd` run the
servers.

## Configuration

All local configuration and secrets live in a `.env` file at the repository root. It is
**gitignored**, never commit it. Use [`.env.example`](.env.example) as the template.

### Game server address

The login server hands the client an address to reach the game server on. It defaults to
loopback, which only serves a client on the same machine. Set `GAME_SERVER_PUBLIC_IPV4` for
anything reachable from elsewhere, `GAME_SERVER_PUBLIC_IPV6` if you serve one, and
`GAME_SERVER_PORT` if it is not 7777.

The same response also carries a local address and hostname, which retail always sends as
`127.0.0.1` and `localhost`. `GAME_SERVER_LOCAL_ADDRESS` and `GAME_SERVER_LOCAL_HOSTNAME` exist
to experiment with that, not to be pointed at your public address. An empty value replaces the
default with nothing, and the server refuses to start rather than advertise an unreachable
address.

### First account

A fresh login database has no users. Set `PIRATED_ADMIN_USERNAME` and `PIRATED_ADMIN_PASSWORD`
and the server creates that account on startup, but only while the user table is still empty.
Changing either value later does nothing and is not a way to reset a password.

The password is stored as unsalted SHA-1, because that is what the client sends.

### Server key

Both servers share one private key, generated into `keys/build/` and re-stamped into the
servers' resources on every build. The Android client carries the matching public key, so the
key a server runs with must be the one the APK was built against. Pass a different key with
`PIRATED_GAME_PRIVATE_KEY` (the PEM) or `PIRATED_GAME_PRIVATE_KEY_FILE` (a path to it). See
`keys/README.md`.

## Documentation

`docs/` holds the client reverse-engineering notes (packet map, data.pak, story ids, Unova
elevators) and the docs site under `docs/src/content/docs/` (database, scripts, story state,
generators). `CLAUDE.md` indexes them all.

## License

AGPL-3.0, see [LICENSE](LICENSE). Based on OpenMMO by Fiereu.

## Disclaimer

[PokeMMO](https://pokemmo.eu/) is not affiliated with this project in any way. Hosting or using a
private server might be against the [PokeMMO ToS](https://pokemmo.com/tos/).
