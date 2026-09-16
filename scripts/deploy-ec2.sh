#!/usr/bin/env bash
# Sube jars por SCP y levanta los 9 servicios Spring Boot en la EC2.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/terraform"
IP="$(terraform output -raw ec2_public_ip)"
FE_URL="$(terraform output -raw frontend_url)"
KEY="${NEXOTECH_SSH_KEY:-$HOME/.ssh/vockey.pem}"
SSH=(ssh -i "$KEY" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=20 ec2-user@"$IP")
SCP=(scp -i "$KEY" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null)

echo "==> EC2=$IP  Frontend=$FE_URL"

echo "==> Esperando SSH…"
for i in $(seq 1 40); do
  if "${SSH[@]}" 'echo ok' >/dev/null 2>&1; then break; fi
  sleep 5
done

echo "==> Instalando Java…"
"${SSH[@]}" 'sudo dnf install -y java-17-amazon-corretto-headless >/dev/null'
"${SSH[@]}" 'sudo mkdir -p /opt/nexotech/jars /opt/nexotech/logs && sudo chown -R ec2-user:ec2-user /opt/nexotech'

echo "==> Subiendo jars (SCP)…"
"${SCP[@]}" "$ROOT/backend/target/backend-0.0.1-SNAPSHOT.jar" ec2-user@"$IP":/opt/nexotech/jars/backend.jar
for svc in catalog cart order notification inventory payment shipping review; do
  "${SCP[@]}" "$ROOT/services/${svc}-service/target/${svc}-service-0.0.1-SNAPSHOT.jar" \
    ec2-user@"$IP":/opt/nexotech/jars/${svc}-service.jar
done

"${SSH[@]}" "cat >/opt/nexotech/env.sh" <<EOF
export SPRING_PROFILES_ACTIVE=azuread
export AZURE_TENANT_ID=72fd0b5a-8a6a-4cff-89f6-bde961f7e250
export AZURE_API_AUDIENCE=097bfd84-a8e3-4232-9048-718f4d648efd
export APP_ADMIN_USERS=fe.ardiles@duocuc.cl
export APP_CORS_ORIGINS=${FE_URL}
export JAVA_TOOL_OPTIONS="-Xms64m -Xmx192m"
export CATALOG_URL=http://127.0.0.1:8081
export CART_URL=http://127.0.0.1:8082
export ORDERS_URL=http://127.0.0.1:8083
export NOTIFICATIONS_URL=http://127.0.0.1:8084
export INVENTORY_URL=http://127.0.0.1:8085
export PAYMENTS_URL=http://127.0.0.1:8086
export SHIPPING_URL=http://127.0.0.1:8087
export REVIEWS_URL=http://127.0.0.1:8088
EOF

"${SSH[@]}" 'bash -s' <<'REMOTE'
set -eux
if [[ -f /opt/nexotech/pids.txt ]]; then
  while read -r pid; do kill "$pid" 2>/dev/null || true; done < /opt/nexotech/pids.txt || true
fi
: > /opt/nexotech/pids.txt
# shellcheck disable=SC1091
source /opt/nexotech/env.sh

start_one() {
  local name="$1" jar="$2"
  nohup java -jar "/opt/nexotech/jars/$jar" >"/opt/nexotech/logs/$name.log" 2>&1 &
  echo $! >> /opt/nexotech/pids.txt
  echo "started $name pid $!"
}

start_one catalog-service catalog-service.jar
start_one cart-service cart-service.jar
start_one order-service order-service.jar
start_one notification-service notification-service.jar
start_one inventory-service inventory-service.jar
start_one payment-service payment-service.jar
start_one shipping-service shipping-service.jar
start_one review-service review-service.jar
sleep 12
start_one backend backend.jar

for i in $(seq 1 90); do
  if curl -sf http://127.0.0.1:8080/api/public/health >/dev/null; then
    echo BFF_OK
    exit 0
  fi
  sleep 2
done
echo BFF_FAIL
tail -n 80 /opt/nexotech/logs/backend.log || true
exit 1
REMOTE

echo "==> Health público desde internet:"
curl -sS -w "\nHTTP %{http_code}\n" "http://$IP:8080/api/public/health" | tail -5
