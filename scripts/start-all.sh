#!/usr/bin/env bash
# Levanta el BFF y los 12 microservicios de NexoTech en segundo plano.
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
# RabbitMQ (avisos): http://localhost:15672 · Kafka (eventos y ofertas): localhost:9092
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
export SPRING_RABBITMQ_USERNAME="${SPRING_RABBITMQ_USERNAME:-nexotech}"
export SPRING_RABBITMQ_PASSWORD="${SPRING_RABBITMQ_PASSWORD:-local-dev-only}"
export SPRING_KAFKA_BOOTSTRAP_SERVERS="${SPRING_KAFKA_BOOTSTRAP_SERVERS:-localhost:19092,localhost:19094}"

export APP_KAFKA_REPLICAS="${APP_KAFKA_REPLICAS:-2}"

if [[ "${START_LOCAL_BROKERS:-true}" == "true" ]]; then
  command -v docker >/dev/null 2>&1 || { echo "Docker no está disponible; configura START_LOCAL_BROKERS=false para brokers externos"; exit 1; }
  echo "Levantando Kafka y RabbitMQ…"
  docker compose -f "$ROOT/docker-compose.yml" up -d --wait --wait-timeout 120 zookeeper kafka1 kafka2 kafka-ui rabbitmq rabbitmq2
else
  echo "Usando brokers externos configurados por variables de entorno"
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
  "offer-service:services/offer-service:8091"
  "sales-analytics-service:services/sales-analytics-service:8092"
  "kafka-admin-service:services/kafka-admin-service:8094"
  "rabbit-admin-service:services/rabbit-admin-service:8090"
  "backend:backend:8080"
)

if [[ $BUILD -eq 1 ]]; then
  echo "Instalando contrato de mensajería…"
  (cd "$ROOT/services/messaging-contract" && mvn -q install)
  echo "Compilando (mvn -q -DskipTests package)…"
  build_pids=()
  for entry in "${SERVICES[@]}"; do
    IFS=: read -r name dir _ <<<"$entry"
    (cd "$ROOT/$dir" && mvn -q package) &
    build_pids+=("$!")
  done
  build_failed=0
  for build_pid in "${build_pids[@]}"; do
    wait "$build_pid" || build_failed=1
  done
  [[ "$build_failed" -eq 0 ]] || { echo "Falló la compilación o las pruebas"; exit 1; }
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

failed=0
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
  failed=1
  echo "  ✘ $name (:$port) no respondió; revisa .run/logs/$name.log"
done

[[ "$failed" -eq 0 ]] || exit 1

cat <<EOF

Listo. BFF en http://localhost:8080/api · Frontend: (cd frontend && npm start) → http://localhost:4200
Ofertas Kafka: http://localhost:8080/api/public/offers · detener servicios Java: scripts/stop-all.sh
EOF
