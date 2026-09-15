#!/usr/bin/env bash
# Detiene los procesos levantados por scripts/start-all.sh
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PIDS="$ROOT/.run/pids"

if [[ ! -d "$PIDS" ]]; then
  echo "No hay procesos registrados."
  exit 0
fi

for pidfile in "$PIDS"/*.pid; do
  [[ -e "$pidfile" ]] || continue
  name="$(basename "$pidfile" .pid)"
  pid="$(cat "$pidfile")"
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid" && echo "· $name detenido (pid $pid)"
  else
    echo "· $name ya no estaba corriendo"
  fi
  rm -f "$pidfile"
done
