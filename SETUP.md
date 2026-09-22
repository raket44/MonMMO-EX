# Setting up MonMMO-EX

Everything the server needs to build is in this repository or in one of its submodules.
Four things are deliberately NOT here, and only the last two matter if you are working on
the server.

## 1. Clone with submodules

```bash
git clone --recursive https://github.com/raket44/MonMMO-EX.git
```

Already cloned without `--recursive`:

```bash
git submodule update --init --recursive
```

`decomp/` is nine pret decompilations plus `pokeemerald-expansion`, pinned to the exact commit
the generated species, moves, abilities, learnsets and form tables come from. Do not bump the
Expansion pin casually: it changes game data, so it is a deliberate commit with the regenerated
output beside it.

## 2. Build and run

```bash
./gradlew :server.game:test        # the suite
./gradlew :server.login:installDist :server.game:installDist
```

Postgres comes up with `docker compose up -d`; `.env.example` is the template for `.env`.

## What is NOT in the repository, and why

| | what it is | needed for |
|---|---|---|
| `roms/`, NDS ROMs | your own game ROMs | ONLY re-extracting ROM data (`tools/nds/*`, `refreshDialogTable`). The extracted results are committed, so the server builds and runs without them. |
| retail APK, `Client-31914` | the PokeMMO client we patch | only client packaging (`tools/client-package/build-android.sh`) |
| `keys/build/*.pem` | the server's signing keys | **required to run a server your client will trust** |
| `.env` | DB passwords, session secret | **required to run a server** |

The two required ones are shared directly between maintainers - never through the repository,
an issue, or a chat paste. If the keys differ from the ones the deployed client was built
against, every client fails with `Failed to verify signature` and reconnects in a loop; the
build re-stamps `src/main/resources/*.pem` from `keys/build/` on EVERY build, so editing the
copies does nothing.

## Deploying

`deploy/vps/deploy.sh [--force] [--data]` from the repository root. It refuses while players are
connected unless you pass `--force`, which kicks them.
