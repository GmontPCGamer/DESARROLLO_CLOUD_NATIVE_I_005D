#!/usr/bin/env bash
# Release EC2 de la actividad: Rabbit existente + ZooKeeper, Kafka y Kafka UI.
set -euo pipefail
umask 077
RELEASE="$(cd "$(dirname "$0")" && pwd)"
ROOT=/opt/nexotech
[[ "$RELEASE" == "$ROOT/releases/"* ]] || { echo 'Directorio de release inválido'; exit 1; }
cd "$RELEASE"
sha256sum -c SHA256SUMS
source "$ROOT/env.sh"
dnf install -y docker java-17-amazon-corretto-headless >/dev/null 2>&1
systemctl enable --now docker >/dev/null
if ! docker compose version >/dev/null 2>&1; then
  mkdir -p /usr/local/lib/docker/cli-plugins
  cd /usr/local/lib/docker/cli-plugins
  curl -fsSL https://github.com/docker/compose/releases/download/v2.39.4/docker-compose-linux-x86_64 -o docker-compose-linux-x86_64
  curl -fsSL https://github.com/docker/compose/releases/download/v2.39.4/docker-compose-linux-x86_64.sha256 -o docker-compose.sha256
  sha256sum -c docker-compose.sha256
  mv docker-compose-linux-x86_64 docker-compose
  chmod 755 docker-compose
  cd "$RELEASE"
fi
export KAFKA_UI_USERNAME="${SPRING_RABBITMQ_USERNAME:?}"
export KAFKA_UI_PASSWORD="${SPRING_RABBITMQ_PASSWORD:?}"
COMPOSE=(docker compose -p nexotech-ep3 -f "$RELEASE/docker-compose.yml")
"${COMPOSE[@]}" pull zookeeper kafka1 kafka2 kafka-ui >/dev/null 2>&1
SERVICES=(catalog cart order notification inventory payment shipping review offer sales-analytics kafka-admin rabbit-admin backend)
# Se detiene sólo esta aplicación y después de descargar todas las imágenes.
for svc in "${SERVICES[@]}"; do
  name="$svc-service"; [[ "$svc" == backend ]] && name=backend
  systemctl stop "nexotech-$name" 2>/dev/null || true
done
"${COMPOSE[@]}" up -d --wait --wait-timeout 180 zookeeper kafka1 kafka2
mkdir -p "$ROOT/data"
chown ec2-user:ec2-user "$ROOT/data"
chmod 700 "$ROOT/data"
# Crea los dos topics de negocio y sus DLT con tres particiones y dos réplicas.
for topic in nexotech.review-created nexotech.review-created.DLT nexotech.order-confirmed nexotech.order-confirmed.DLT; do
  retention=604800000
  [[ "$topic" == nexotech.review-created ]] && retention=-1
  [[ "$topic" == *.DLT ]] && retention=1209600000
  "${COMPOSE[@]}" exec -T kafka1 kafka-topics --bootstrap-server kafka1:29092 --create --if-not-exists \
    --topic "$topic" --partitions 3 --replication-factor 2 --config cleanup.policy=delete --config retention.ms="$retention"
done
# Migra el historial del Kafka anterior sin borrar su contenedor ni sus volúmenes.
if [[ ! -f "$ROOT/data/migration-kraft-to-zookeeper.done" ]] && docker container inspect nexotech-kafka >/dev/null 2>&1; then
  docker start nexotech-kafka >/dev/null
  for topic in nexotech.review-created nexotech.review-created.DLT; do
    if ! docker exec nexotech-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list | grep -Fxq "$topic"; then continue; fi
    count="$(docker exec nexotech-kafka /opt/kafka/bin/kafka-get-offsets.sh --bootstrap-server localhost:9092 --topic "$topic" | awk -F: '{n+=$3} END{print n+0}')"
    if [[ "$count" -gt 0 ]]; then
      file="$ROOT/data/migration-$topic.txt"
      docker exec nexotech-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
        --topic "$topic" --from-beginning --max-messages "$count" --timeout-ms 10000 \
        --property print.key=true --property key.separator=$'\t' > "$file" 2> "$file.log"
      [[ "$(wc -l < "$file")" -eq "$count" ]] || { echo 'Exportación incompleta; Kafka anterior conservado'; exit 1; }
      "${COMPOSE[@]}" exec -T kafka1 kafka-console-producer --bootstrap-server kafka1:29092 --topic "$topic" \
        --property parse.key=true --property key.separator=$'\t' --producer-property acks=all < "$file"
      echo "MIGRATED $topic $count"
    fi
  done
  touch "$ROOT/data/migration-kraft-to-zookeeper.done"
  docker stop nexotech-kafka >/dev/null
fi
"${COMPOSE[@]}" up -d kafka-ui
[[ -f "$RELEASE/env.previous.sh" ]] || cp -p "$ROOT/env.sh" "$RELEASE/env.previous.sh"
chmod 600 "$RELEASE/env.previous.sh"
cat >> "$ROOT/env.sh" <<'ENV'
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:19092,localhost:19094
export APP_KAFKA_REPLICAS=2
export OFFERS_URL=http://127.0.0.1:8091
export ANALYTICS_URL=http://127.0.0.1:8092
export KAFKA_ADMIN_URL=http://127.0.0.1:8094
export APP_DATA_DIR=/opt/nexotech/data
export APP_CORS_ORIGINS=https://ourd5f7qr1.execute-api.us-east-1.amazonaws.com,http://localhost:4200,https://18-211-7-130.sslip.io
export AZURE_API_AUDIENCE=f7d7e5dd-430c-4adb-9348-9ecd974b220c,api://nexotech-demo-api,097bfd84-a8e3-4232-9048-718f4d648efd,api://nexotech-student-api
ENV
chown ec2-user:ec2-user "$ROOT/env.sh"
chmod 600 "$ROOT/env.sh"
if [[ -L "$ROOT/current" && "$(readlink "$ROOT/current")" != "$RELEASE" ]]; then readlink "$ROOT/current" > "$RELEASE/previous-release.txt"; fi
ln -sfn "$RELEASE" "$ROOT/current"
chmod 755 "$ROOT/releases" "$RELEASE" "$RELEASE/jars"
chmod 644 "$RELEASE"/jars/*.jar
for svc in "${SERVICES[@]}"; do
  name="$svc-service"; [[ "$svc" == backend ]] && name=backend
  address=127.0.0.1; [[ "$svc" == backend ]] && address=0.0.0.0
  cat > "/etc/systemd/system/nexotech-$name.service" <<UNIT
[Unit]
Description=NexoTech $name
After=network-online.target docker.service
Wants=network-online.target
[Service]
Type=simple
User=ec2-user
WorkingDirectory=$ROOT
Environment=SERVER_ADDRESS=$address
Environment=SERVER_TOMCAT_THREADS_MAX=12
Environment=SERVER_TOMCAT_THREADS_MIN_SPARE=2
ExecStart=/bin/bash -c 'source $ROOT/env.sh; unset JAVA_TOOL_OPTIONS; exec /usr/bin/java -Xms32m -Xmx128m -XX:ActiveProcessorCount=2 -jar $ROOT/current/jars/$name.jar'
Restart=on-failure
RestartSec=10
TimeoutStopSec=30
StandardOutput=journal
StandardError=journal
[Install]
WantedBy=multi-user.target
UNIT
done
systemctl daemon-reload
PORTS=(8081 8082 8083 8084 8085 8086 8087 8088 8091 8092 8094 8090 8080)
for index in "${!SERVICES[@]}"; do
  svc="${SERVICES[$index]}";name="$svc-service";[[ "$svc" == backend ]] && name=backend
  systemctl enable "nexotech-$name" >/dev/null
  systemctl start "nexotech-$name"
  port="${PORTS[$index]}";ok=0
  for _ in $(seq 1 90); do
    if curl -fsS --max-time 2 "http://127.0.0.1:$port/api/public/health" >/dev/null 2>&1; then ok=1;break;fi
    sleep 2
  done
  [[ "$ok" == 1 ]] || { echo "Health falló en $name"; exit 1; }
  echo "HEALTH_OK $name"
done
if [[ -d "$RELEASE/frontend" ]]; then
  [[ -d "$RELEASE/frontend.previous" ]] || cp -a /var/www/nexotech "$RELEASE/frontend.previous"
  cp -a "$RELEASE/frontend/." /var/www/nexotech/
  chmod -R a+rX /var/www/nexotech
fi
bash "$RELEASE/configure-public-access.sh"
curl -fsS http://127.0.0.1/web/index.html >/dev/null
echo DEPLOY_EP3_OK
