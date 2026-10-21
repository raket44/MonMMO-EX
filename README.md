# PokeMMO Pirated Version

A Pokemon MMO server and Android client built by RaKeT and the Lost Knights, with the
Expansion species, the Fairy type, raids, breeding and more built straight into the client.

Based on OpenMMO by Fiereu.

## ToC
- [Building](#building)
- [Configuration](#configuration)
- [Releases](#releases)
- [Documentation](#documentation)
- [License](#license)
- [Disclaimer](#disclaimer)

## Building

Game data is generated at build time from the [pret](https://github.com/pret)
decompilation projects, which are vendored as git submodules under `decomp/`.
Clone the repository with its submodules:

```bash
git clone --recurse-submodules <repo-url>
# or, for an existing clone:
git submodule update --init --recursive
```

Without the submodules the `:codegen` build fails, because the generators have no
decomp data to read. The one exception is `decomp/pokeblack`, which holds the
item data as committed files rather than a submodule, because its source
repository is private.

### ROMs

A dialog id is a file offset into the retail GBA ROM. The decomp is byte-identical
to it, so the generator encodes a text from the decomp, finds those bytes in the
ROM, and packs the offset into the id. The decomp alone has no offsets.

Resolving them needs the retail ROMs, so the result is committed instead, one
file per region under `codegen/dialog/`. The build reads only those, which is
why CI and the release images get the same dialog as a local build.

ROMs are needed only to **change** that table, after a decomp bump renames or
adds a label. Put the Emerald (`BPEE`) and FireRed (`BPRE`) ROMs in `roms/`,
which is **gitignored** because this project ships no ROMs, and run:

```bash
./gradlew :codegen:refreshDialogTable   # then commit codegen/dialog/
```

Filenames do not matter, each ROM is identified by the game code in its GBA
header.

## Configuration

All local configuration and secrets live in a `.env` file at the repository
root. It is **gitignored**, never commit it. Use the tracked
[`.env.example`](.env.example) as the template:

```bash
cp .env.example .env          # then edit the values
docker compose up -d          # start all docker containers
./gradlew runAll              # build and run the servers
```

For local-only tweaks to the container setup, create a
`docker-compose.override.yml` (also gitignored). 
Docker Compose merges it automatically on `docker compose up`. 
For deployment,supply a proper `.env` and run `docker compose -f docker-compose.yml up -d` to skip any override.

### Game server address

The login server hands the client an address to reach the game server on. It
defaults to loopback, which serves a client on that same machine and nothing
else. Set `GAME_SERVER_PUBLIC_IPV4` for anything reachable from elsewhere,
`GAME_SERVER_PUBLIC_IPV6` if you serve one, and `GAME_SERVER_PORT` if it is not
7777. That is the whole of it for a normal deployment.

The same response also carries a local address and hostname, which every capture
of the retail server has as `127.0.0.1` and `localhost` no matter how public the
addresses beside them are. `GAME_SERVER_LOCAL_ADDRESS` and
`GAME_SERVER_LOCAL_HOSTNAME` exist to experiment with that, not to be pointed at
your public address.

An empty value is not the same as an unset one. It replaces the default with
nothing, and the server refuses to start rather than advertising an address no
client can reach.

### First account

A fresh login database has no users in it at all. Set `PIRATED_ADMIN_USERNAME`
and `PIRATED_ADMIN_PASSWORD` and the server creates that account on startup,
but only while the user table is still empty. It never touches an account that
already exists, so changing either value later does nothing and is not a way to
reset a password. Leave both unset and no account is created.

The password is stored as unsalted SHA-1, because that is what the client sends
and the server only ever compares what arrives.

### Server key

Both servers share one private key. A local build generates it, so development
needs no setup.

Released archives ship no keys. Generate a pair with
`./gradlew :keys:generateGame` and pass the private key to both servers through
`PIRATED_GAME_PRIVATE_KEY` (the PEM) or `PIRATED_GAME_PRIVATE_KEY_FILE` (a path
to it). Clients need a patched build carrying the matching public key.

## Releases

Build a server image yourself with:

```bash
./gradlew :server.login:installDist
docker build -f server.login/Dockerfile -t pirated-login .
```

The production server is deployed with `deploy/vps/deploy.sh`.

The version lives in `gradle.properties` and applies to every module, do not
edit it by hand.

## Documentation

The documentation lives in the `docs` folder in this repository.

## License

AGPL-3.0, see [LICENSE](LICENSE). This project started from OpenMMO by Fiereu and has
been rebuilt and extended since.

## Disclaimer
[PokeMMO](https://pokemmo.eu/) is not affiliated with this project in any way.
Hosting/Using a private server might be against the [PokeMMO ToS](https://pokemmo.com/tos/).
