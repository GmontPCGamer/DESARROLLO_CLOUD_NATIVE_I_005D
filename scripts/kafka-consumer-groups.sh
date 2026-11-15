#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
compose=(docker compose -p nexotech-ep3 -f "${KAFKA_COMPOSE_FILE:-$ROOT/docker-compose.yml}")
kafka=("${compose[@]}" exec -T kafka1 kafka-consumer-groups --bootstrap-server kafka1:29092)

action="${1:-list}"
group="${2:-}"
topic="${3:-}"
case "$action" in
  list)
    "${kafka[@]}" --list
    ;;
  inspect)
    [[ -n "$group" ]] || { echo "Uso: $0 inspect GRUPO" >&2; exit 2; }
    "${kafka[@]}" --describe --group "$group"
    ;;
  preview-reset|reset)
    [[ "$group" =~ ^demo\.[A-Za-z0-9._-]+$ && "$topic" =~ ^demo\.[A-Za-z0-9._-]+$ ]] ||
      { echo 'El reinicio de offsets requiere un grupo y un topic demo.*' >&2; exit 2; }
    state="${4:-}"
    mode=--dry-run
    if [[ "$action" == reset ]]; then
      [[ "$state" == --confirm ]] || { echo 'Ejecuta primero preview-reset y luego confirma con --confirm' >&2; exit 2; }
      mode=--execute
    fi
    "${kafka[@]}" --group "$group" --topic "$topic" --reset-offsets --to-earliest "$mode"
    ;;
  *)
    echo "Uso: $0 {list|inspect GRUPO|preview-reset GRUPO TOPIC|reset GRUPO TOPIC --confirm}" >&2
    exit 2
    ;;
esac
