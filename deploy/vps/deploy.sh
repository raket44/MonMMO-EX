#!/bin/bash
# Builds both servers and pushes them to the VPS, then restarts them.
#   deploy/vps/deploy.sh            builds, refuses to restart while a player is online
#   deploy/vps/deploy.sh --force    restarts even with players online
#   deploy/vps/deploy.sh --no-build pushes the existing build/install dists
# Run from Git Bash (or the Claude shell) in the repo root.
set -e
VPS=${MONMMO_VPS:-root@91.98.41.154}
KEY=${MONMMO_VPS_KEY:-$HOME/.ssh/monmmo_vps}
SSH="ssh -i $KEY -o BatchMode=yes $VPS"
cd "$(dirname "$0")/../.."

force=0; build=1
for a in "$@"; do case "$a" in --force) force=1;; --no-build) build=0;; esac; done

if [ $build = 1 ]; then
  echo "Building dists..."
  ./gradlew -q :server.game:installDist :server.login:installDist
fi

online=$($SSH "ss -Htn state established '( sport = :7777 )' | wc -l")
if [ "$online" != "0" ] && [ $force = 0 ]; then
  echo "$online player(s) online on the VPS - not restarting. Re-run with --force to kick them."
  exit 1
fi

echo "Uploading..."
tar czf - \
  --transform 's#^server.game/build/install/server.game/lib#server.game/lib#' \
  --transform 's#^server.login/build/install/server.login/lib#server.login/lib#' \
  --transform 's#^server.game/src/main/resources/game.private.pem#server.game/game.private.pem#' \
  server.game/build/install/server.game/lib \
  server.login/build/install/server.login/lib \
  server.game/src/main/resources/game.private.pem \
  server.game/*.txt \
  data/pokemmo/monsters.json \
  deploy/vps/docker-compose.yml deploy/vps/monmmo-login.service deploy/vps/monmmo-game.service deploy/vps/backup.sh \
  | $SSH "set -e; cd /opt/monmmo; rm -rf server.game/lib.new server.login/lib.new; tar xzf -; \
    rm -rf server.game/lib.old server.login/lib.old; \
    cp deploy/vps/docker-compose.yml docker-compose.yml; cp deploy/vps/backup.sh backup.sh; chmod +x backup.sh; \
    cp deploy/vps/monmmo-login.service deploy/vps/monmmo-game.service /etc/systemd/system/; systemctl daemon-reload; \
    chown -R monmmo:monmmo /opt/monmmo/server.game /opt/monmmo/server.login /opt/monmmo/data /opt/monmmo/logs; \
    docker compose up -d --quiet-pull 2>/dev/null; \
    systemctl enable monmmo-login monmmo-game >/dev/null 2>&1; systemctl restart monmmo-login; sleep 3; systemctl restart monmmo-game; sleep 8; \
    systemctl is-active monmmo-login monmmo-game; \
    tail -n 2 logs/server-login.log logs/server-game.log"
echo "Deployed."
