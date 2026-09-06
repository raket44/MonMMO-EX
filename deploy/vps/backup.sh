#!/bin/bash
# Nightly dump of both databases into /opt/monmmo/backups, keeping the last 14.
set -e
cd /opt/monmmo
. ./.env
mkdir -p backups
stamp=$(date +%Y%m%d-%H%M)
docker exec login-db pg_dump -U "$LOGIN_DB_USER" -d "$LOGIN_DB_NAME" | gzip > "backups/login-$stamp.sql.gz"
docker exec game-db pg_dump -U "$GAME_DB_USER" -d "$GAME_DB_NAME" | gzip > "backups/game-$stamp.sql.gz"
ls -1t backups/login-*.sql.gz | tail -n +15 | xargs -r rm -f
ls -1t backups/game-*.sql.gz | tail -n +15 | xargs -r rm -f
