#!/usr/bin/env bash
# Dashboards privados: no abre puertos públicos ni modifica security groups.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REGION="${AWS_REGION:-us-east-1}"
APP_INSTANCE="${INSTANCE_ID:-i-0e3ea93394a16686d}"
RABBIT_INSTANCE="${RABBIT_INSTANCE_ID:-i-0f8aae5246e5e7513}"
PLUGIN="${SESSION_MANAGER_PLUGIN:-$(command -v session-manager-plugin || true)}"
if [[ -z "$PLUGIN" ]]; then
  [[ "$(uname -s)" == Darwin ]] || { echo 'Instala el complemento oficial Session Manager de AWS o define SESSION_MANAGER_PLUGIN'; exit 1; }
  tools="$ROOT/.run/monitoring-tools"
  mkdir -p "$tools"
  platform=mac; [[ "$(uname -m)" == arm64 ]] && platform=mac_arm64
  curl -fsSL "https://s3.amazonaws.com/session-manager-downloads/plugin/latest/$platform/sessionmanager-bundle.zip" -o "$tools/bundle.zip"
  python3 - "$tools" <<'PY'
import sys,zipfile
from pathlib import Path
root=Path(sys.argv[1])
with zipfile.ZipFile(root/'bundle.zip') as archive:
    for info in archive.infolist():
        target=(root/info.filename).resolve()
        if not target.is_relative_to(root.resolve()):raise ValueError('Ruta de archivo inválida')
    archive.extractall(root)
(root/'sessionmanager-bundle/bin/session-manager-plugin').chmod(0o755)
PY
  PLUGIN="$tools/sessionmanager-bundle/bin/session-manager-plugin"
fi
export PATH="$(dirname "$PLUGIN"):$PATH"
aws ssm start-session --region "$REGION" --target "$APP_INSTANCE" \
  --document-name AWS-StartPortForwardingSession \
  --parameters '{"portNumber":["8093"],"localPortNumber":["18093"]}' &
kafka_pid=$!
aws ssm start-session --region "$REGION" --target "$RABBIT_INSTANCE" \
  --document-name AWS-StartPortForwardingSession \
  --parameters '{"portNumber":["15672"],"localPortNumber":["18072"]}' &
rabbit_pid=$!
trap 'kill "$kafka_pid" "$rabbit_pid" 2>/dev/null || true' EXIT INT TERM
printf '\nKafka UI: http://127.0.0.1:18093/dev/kafka/\nRabbitMQ: http://127.0.0.1:18072/\nUsa las credenciales RabbitMQ existentes. Mantén esta terminal abierta.\n'
wait "$kafka_pid" "$rabbit_pid"
