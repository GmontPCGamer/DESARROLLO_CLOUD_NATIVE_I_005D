#!/usr/bin/env bash
# Levanta el BFF y los 8 microservicios de NexoTech en segundo plano.
#
# Uso:
#   scripts/start-all.sh                 # perfil local (JWT HS256 de desarrollo)
#   scripts/start-all.sh azuread         # perfil azuread (tokens reales de Microsoft Entra ID)
#   scripts/start-all.sh azuread --no-build
#
# Variables opcionales (perfil azuread):
#   AZURE_TENANT_ID     tenant de Entra (por defecto el del proyecto)
#   AZURE_API_AUDIENCE  client ID de la app "NexoTech Student"
#   APP_ADMIN_USERS     correos (preferred_username) que reciben ROLE_ADMIN, separados por coma
#
# Logs:  .run/logs/<servicio>.log     PIDs: .run/pids/<servicio>.pid
#
# RabbitMQ (avisos de compra): docker compose up -d rabbitmq
#   consola http://localhost:15672  guest / guest
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROFILE="${1:-local}"
BUILD=1
for arg in "$@"; do
  [[ "$arg" == "--no-build" ]] && BUILD=0
done

mkdir -p "$ROOT/.run/logs" "$ROOT/.run/pids"

# Lanza un proceso en su propia sesión para que sobreviva al cierre de la terminal.
detach() {
  local log="$1"; shift
  if command -v setsid >/dev/null 2>&1; then
    setsid "$@" >"$log" 2>&1 < /dev/null &
  elif command -v perl >/dev/null 2>&1; then
    perl -MPOSIX -e 'POSIX::setsid(); exec @ARGV or die "exec: $!"' -- "$@" >"$log" 2>&1 < /dev/null &
  else
    nohup "$@" >"$log" 2>&1 < /dev/null &
  fi
  echo $!
}

export SPRING_PROFILES_ACTIVE="$PROFILE"
export SPRING_RABBITMQ_HOST="${SPRING_RABBITMQ_HOST:-localhost}"
export SPRING_RABBITMQ_PORT="${SPRING_RABBITMQ_PORT:-5672}"
export SPRING_RABBITMQ_USERNAME="${SPRING_RABBITMQ_USERNAME:-guest}"
export SPRING_RABBITMQ_PASSWORD="${SPRING_RABBITMQ_PASSWORD:-guest}"

if ! lsof -iTCP:5672 -sTCP:LISTEN -P -n >/dev/null 2>&1; then
  if command -v docker >/dev/null 2>&1; then
    echo "Levantando RabbitMQ…"
    docker compose -f "$ROOT/docker-compose.yml" up -d rabbitmq || echo "Aviso: no se pudo iniciar RabbitMQ"
  else
    echo "Aviso: RabbitMQ no está en localhost:5672. Los avisos de compra no se encolarán."
  fi
fi
if [[ "$PROFILE" == "azuread" ]]; then
  export AZURE_TENANT_ID="${AZURE_TENANT_ID:-72fd0b5a-8a6a-4cff-89f6-bde961f7e250}"
  export AZURE_API_AUDIENCE="${AZURE_API_AUDIENCE:-097bfd84-a8e3-4232-9048-718f4d648efd}"
  # Correos del equipo que reciben ROLE_ADMIN en el BFF (agrega el tuyo con la variable de entorno).
  export APP_ADMIN_USERS="${APP_ADMIN_USERS:-fe.ardiles@duocuc.cl,p.carvajal@duocuc.cl,mo.huarapil@duocuc.cl}"
  echo "Perfil azuread · tenant=$AZURE_TENANT_ID · audience=$AZURE_API_AUDIENCE"
  echo "Administradores (ROLE_ADMIN): $APP_ADMIN_USERS"
else
  echo "Perfil local · los servicios aceptan JWT HS256 firmados con JWT_LOCAL_SECRET"
fi

# nombre:carpeta:puerto
SERVICES=(
  "catalog-service:services/catalog-service:8081"
  "cart-service:services/cart-service:8082"
  "order-service:services/order-service:8083"
  "notification-service:services/notification-service:8084"
  "inventory-service:services/inventory-service:8085"
  "payment-service:services/payment-service:8086"
  "shipping-service:services/shipping-service:8087"
  "review-service:services/review-service:8088"
  "backend:backend:8080"
)

if [[ $BUILD -eq 1 ]]; then
  echo "Compilando (mvn -q -DskipTests package)…"
  for entry in "${SERVICES[@]}"; do
    IFS=: read -r name dir _ <<<"$entry"
    (cd "$ROOT/$dir" && mvn -q -DskipTests package) &
  done
  wait
fi

for entry in "${SERVICES[@]}"; do
  IFS=: read -r name dir port <<<"$entry"
  pidfile="$ROOT/.run/pids/$name.pid"
  if [[ -f "$pidfile" ]] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then
    echo "· $name ya está corriendo (pid $(cat "$pidfile"))"
    continue
  fi
  if lsof -iTCP:"$port" -sTCP:LISTEN -P -n >/dev/null 2>&1; then
    echo "· puerto $port ocupado por otro proceso; omito $name"
    continue
  fi
  jar="$(ls "$ROOT/$dir"/target/*.jar 2>/dev/null | grep -v '\.original$' | head -1)"
  if [[ -z "$jar" ]]; then
    echo "!! No existe jar para $name. Ejecuta sin --no-build."
    exit 1
  fi
  pid="$(detach "$ROOT/.run/logs/$name.log" java -jar "$jar")"
  echo "$pid" >"$pidfile"
  echo "· $name → :$port (pid $pid)"
done

echo "Esperando health checks…"
for entry in "${SERVICES[@]}"; do
  IFS=: read -r name _ port <<<"$entry"
  for _ in $(seq 1 60); do
    if curl -sf "http://localhost:$port/api/public/health" >/dev/null 2>&1; then
      echo "  ✔ $name (:$port)"
      continue 2
    fi
    sleep 1
  done
  echo "  ✘ $name (:$port) no respondió; revisa .run/logs/$name.log"
done

cat <<EOF

Listo. BFF en http://localhost:8080/api · Frontend: (cd frontend && npm start) → http://localhost:4200
Detener todo: scripts/stop-all.sh
EOF
