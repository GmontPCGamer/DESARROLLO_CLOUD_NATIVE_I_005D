#!/usr/bin/env bash
# Ejecutar como root en la EC2 de aplicación antes de iniciar offer-service.
set -euo pipefail
umask 077

root=/opt/nexotech
secrets="$root/postgres-offers"
container=nexotech-offers-postgres
mkdir -p "$secrets"
chmod 700 "$secrets"
for secret in postgres-password app-password; do
  if [[ ! -s "$secrets/$secret" ]]; then
    openssl rand -hex 32 > "$secrets/$secret"
  fi
done

if docker container inspect "$container" >/dev/null 2>&1; then
  docker start "$container" >/dev/null
else
  docker run -d --name "$container" --restart unless-stopped \
    --publish 127.0.0.1:15432:5432 \
    --volume nexotech_offers_postgres:/var/lib/postgresql/data \
    --mount "type=bind,src=$secrets/postgres-password,dst=/run/secrets/postgres-password,readonly" \
    --env POSTGRES_PASSWORD_FILE=/run/secrets/postgres-password \
    postgres:17-alpine >/dev/null
fi

ready=0
for attempt in $(seq 1 45); do
  if docker exec "$container" pg_isready -h 127.0.0.1 -q; then ready=1; break; fi
  sleep 2
done
[[ "$ready" == 1 ]] || { echo 'PostgreSQL no respondió' >&2; exit 1; }

app_password="$(cat "$secrets/app-password")"
[[ "$app_password" =~ ^[0-9a-f]{64}$ ]]
{
  echo "SELECT 'CREATE ROLE nexotech_offers LOGIN' WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'nexotech_offers')"
  echo '\gexec'
  printf "ALTER ROLE nexotech_offers PASSWORD '%s';\n" "$app_password"
  echo "SELECT 'CREATE DATABASE nexotech_offers OWNER nexotech_offers' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'nexotech_offers')"
  echo '\gexec'
} | docker exec -i "$container" psql --quiet --username postgres --set ON_ERROR_STOP=1

{
  echo 'OFFER_DB_URL=jdbc:postgresql://127.0.0.1:15432/nexotech_offers'
  echo 'OFFER_DB_USER=nexotech_offers'
  printf 'OFFER_DB_PASSWORD=%s\n' "$app_password"
} > "$secrets/offers.env"

directory=/etc/systemd/system/nexotech-offer-service.service.d
mkdir -p "$directory"
printf '[Service]\nEnvironmentFile=%s/offers.env\n' "$secrets" > "$directory/postgres.conf"
systemctl daemon-reload
echo OFFERS_POSTGRES_READY
