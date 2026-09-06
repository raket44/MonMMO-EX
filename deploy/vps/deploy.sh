#!/bin/bash
# Builds both servers and pushes them to the VPS, then restarts them.
#   deploy/vps/deploy.sh            builds, refuses to restart while a player is online
#   deploy/vps/deploy.sh --force    restarts even with players online
#   deploy/vps/deploy.sh --no-build pushes the existing build/install dists
#   deploy/vps/deploy.sh --data     also pushes the map tables, retail data and key (80 MB,
#                                   slow on a home uplink - only when those files changed)
# Only files whose checksum differs from the server's copy are uploaded: a code change is a
# handful of our own jars, not the 90 MB of third-party ones. Run from Git Bash in the repo root.
set -e
VPS=${MONMMO_VPS:-root@91.98.41.154}
KEY=${MONMMO_VPS_KEY:-$HOME/.ssh/monmmo_vps}
SSH="ssh -i $KEY -o BatchMode=yes $VPS"
cd "$(dirname "$0")/../.."

force=0; build=1; data=0
for a in "$@"; do case "$a" in --force) force=1;; --no-build) build=0;; --data) data=1;; esac; done

if [ $build = 1 ]; then
  echo "Building dists..."
  ./gradlew -q :server.game:installDist :server.login:installDist
fi

online=$($SSH "ss -Htn state established '( sport = :7777 )' | wc -l")
if [ "$online" != "0" ] && [ $force = 0 ]; then
  echo "$online player(s) online on the VPS - not restarting. Re-run with --force to kick them."
  exit 1
fi

# Stage the exact tree the server should hold, then diff it against the server's checksums.
stage=$(mktemp -d)
trap 'rm -rf "$stage"' EXIT
mkdir -p "$stage/server.game" "$stage/server.login" "$stage/deploy/vps"
cp -r server.game/build/install/server.game/lib "$stage/server.game/lib"
cp -r server.login/build/install/server.login/lib "$stage/server.login/lib"
cp deploy/vps/docker-compose.yml deploy/vps/monmmo-login.service deploy/vps/monmmo-game.service deploy/vps/backup.sh "$stage/deploy/vps/"
if [ $data = 1 ]; then
  mkdir -p "$stage/data/pokemmo"
  cp server.game/src/main/resources/game.private.pem "$stage/server.game/game.private.pem"
  cp server.game/*.txt "$stage/server.game/"
  cp data/pokemmo/monsters.json "$stage/data/pokemmo/"
fi

remote_sums=$($SSH "cd /opt/monmmo && find server.game/lib server.login/lib deploy/vps -type f -print0 2>/dev/null | xargs -0 sha256sum" || true)
# sha256sum prints "hash  path" on Linux and "hash *./path" on Windows; normalise both.
norm() { awk '{ sub(/^\*?\.\//, "", $2); print $1 "  " $2 }'; }
(cd "$stage" && find . -type f -print0 | xargs -0 sha256sum) | norm | sort > "$stage.local"
echo "$remote_sums" | norm | sort > "$stage.remote"
changed=$(comm -23 "$stage.local" "$stage.remote" | sed 's/^[0-9a-f]*  //')
stale=$(comm -13 <(cut -d' ' -f3- "$stage.local" | sort) <(cut -d' ' -f3- "$stage.remote" | grep '/lib/' | sort) || true)
rm -f "$stage.local" "$stage.remote"

count=$(echo "$changed" | grep -c . || true)
echo "Uploading $count changed file(s)$( [ -n "$stale" ] && echo ", removing $(echo "$stale" | grep -c .) stale jar(s)" )..."
{ [ -n "$changed" ] && (cd "$stage" && echo "$changed" | tar czf - -T -) || tar czf - -T /dev/null; } \
  | $SSH "set -e; cd /opt/monmmo; tar xzf -; \
    for f in $stale; do rm -f \"\$f\"; done; \
    cp deploy/vps/docker-compose.yml docker-compose.yml; cp deploy/vps/backup.sh backup.sh; chmod +x backup.sh; \
    cp deploy/vps/monmmo-login.service deploy/vps/monmmo-game.service /etc/systemd/system/; systemctl daemon-reload; \
    chown -R monmmo:monmmo /opt/monmmo/server.game /opt/monmmo/server.login /opt/monmmo/data /opt/monmmo/logs; \
    docker compose up -d --quiet-pull 2>/dev/null; \
    systemctl enable monmmo-login monmmo-game >/dev/null 2>&1; \
    systemctl restart monmmo-login; sleep 3; systemctl restart monmmo-game; sleep 8; \
    systemctl is-active monmmo-login monmmo-game; \
    tail -n 2 logs/server-login.log logs/server-game.log"
echo "Deployed."
