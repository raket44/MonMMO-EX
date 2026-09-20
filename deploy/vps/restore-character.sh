#!/bin/bash
# Restores ONE character's Pokemon and bag from a nightly dump on the owner's own VPS.
#   deploy/vps/restore-character.sh <characterId> <dump file name in /opt/monmmo/backups>
# The game server is stopped first (its character cache would overwrite the rows), a fresh dump
# is taken, the character's rows are replaced in one transaction, and the server is started again.
# Nobody else's data is touched; story flags, money and position stay as they are now.
set -euo pipefail
CHAR="${1:?character id}"
DUMP="${2:?dump file name, e.g. game-20260913-1020.sql.gz}"
KEY="${MONMMO_VPS_KEY:-$HOME/.ssh/monmmo_vps}"
HOST="${MONMMO_VPS_HOST:-root@91.98.41.154}"

ssh -i "$KEY" "$HOST" CHAR="$CHAR" DUMP="$DUMP" 'bash -s' <<'REMOTE'
set -euo pipefail
cd /opt/monmmo
. ./.env
D="backups/$DUMP"
[ -f "$D" ] || { echo "no such dump: $D"; exit 1; }
online=$(ss -tn state established "( sport = :7777 )" | tail -n +2 | wc -l)
[ "$online" = 0 ] || { echo "$online player(s) online - log out first"; exit 1; }

./backup.sh
systemctl stop monmmo-game.service
T=$(mktemp -d)
trap 'rm -rf "$T"; systemctl start monmmo-game.service' EXIT

# One decompression to a file: `zcat | grep -m1` dies of SIGPIPE under pipefail.
zcat "$D" > "$T/dump.sql"
PH=$(grep -m1 '^COPY public.pokemon ' "$T/dump.sql")
IH=$(grep -m1 '^COPY public.character_items ' "$T/dump.sql")
awk -v c="$CHAR" '/^COPY public.pokemon /{f=1;next} /^\\\./{f=0} f&&$2==c' "$T/dump.sql" > "$T/pokemon.tsv"
awk -v c="$CHAR" '/^COPY public.character_items /{f=1;next} /^\\\./{f=0} f&&$1==c' "$T/dump.sql" > "$T/items.tsv"
# A Pokemon traded away since the dump belongs to someone else now: it stays theirs, not doubled.
docker exec game-db psql -U "$GAME_DB_USER" -d "$GAME_DB_NAME" -At \
  -c "select id from pokemon where owner_id<>$CHAR" > "$T/others.ids"
awk 'NR==FNR{o[$1]=1;next} !($1 in o)' "$T/others.ids" "$T/pokemon.tsv" > "$T/pokemon.keep"
skipped=$(( $(wc -l < "$T/pokemon.tsv") - $(wc -l < "$T/pokemon.keep") ))
[ "$skipped" = 0 ] || echo "skipping $skipped pokemon that now belong to another character"
mv "$T/pokemon.keep" "$T/pokemon.tsv"
echo "dump holds $(wc -l < "$T/pokemon.tsv") pokemon and $(wc -l < "$T/items.tsv") item stacks for $CHAR"
[ -s "$T/pokemon.tsv" ] || { echo "nothing to restore"; exit 1; }

{
  echo "BEGIN;"
  echo "DELETE FROM pokemon WHERE owner_id=$CHAR;"
  echo "DELETE FROM character_items WHERE character_id=$CHAR;"
  echo "$PH"; cat "$T/pokemon.tsv"; echo '\.'
  echo "$IH"; cat "$T/items.tsv"; echo '\.'
  echo "COMMIT;"
} > "$T/restore.sql"
docker exec -i game-db psql -v ON_ERROR_STOP=1 -U "$GAME_DB_USER" -d "$GAME_DB_NAME" < "$T/restore.sql"
docker exec game-db psql -U "$GAME_DB_USER" -d "$GAME_DB_NAME" -At \
  -c "select 'now: ' || (select count(*) from character_items where character_id=$CHAR) || ' item stacks, ' || (select count(*) from pokemon where owner_id=$CHAR) || ' pokemon'"
echo "restored; starting the game server"
REMOTE
