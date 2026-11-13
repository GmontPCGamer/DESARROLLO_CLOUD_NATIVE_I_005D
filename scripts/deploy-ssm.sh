#!/usr/bin/env bash
# Requiere sesión AWS CLI vigente; no guarda credenciales en el paquete.
# Uso: ARTIFACT_BUCKET=... INSTANCE_ID=... bash scripts/deploy-ssm.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REGION="${AWS_REGION:-us-east-1}"
INSTANCE_ID="${INSTANCE_ID:?Indica INSTANCE_ID de la EC2 de aplicación}"
ARTIFACT_BUCKET="${ARTIFACT_BUCKET:?Indica ARTIFACT_BUCKET existente y privado}"
RELEASE_ID="$(date -u +%Y%m%dT%H%M%SZ)"
STAGE="$ROOT/.run/releases/$RELEASE_ID"
mkdir -p "$STAGE/jars"
cp "$ROOT/backend/target/backend-0.0.1-SNAPSHOT.jar" "$STAGE/jars/backend.jar"
for svc in catalog cart order notification inventory payment shipping review offer sales-analytics kafka-admin rabbit-admin; do
  cp "$ROOT/services/$svc-service/target/$svc-service-0.0.1-SNAPSHOT.jar" "$STAGE/jars/$svc-service.jar"
done
cp "$ROOT/scripts/install-release-ec2.sh" "$STAGE/install-release-ec2.sh"
cp "$ROOT/scripts/configure-public-access.sh" "$STAGE/configure-public-access.sh"
cp "$ROOT/docker-compose.yml" "$STAGE/docker-compose.yml"
mkdir -p "$STAGE/rabbitmq"
cp "$ROOT/rabbitmq/local-cluster.conf" "$STAGE/rabbitmq/local-cluster.conf"
# Incluir interfaz sólo cuando existe una compilación validada.
if [[ -f "$ROOT/frontend/dist/frontend/browser/index.html" ]]; then
  cp -R "$ROOT/frontend/dist/frontend/browser" "$STAGE/frontend"
fi
(cd "$STAGE" && find jars -name '*.jar' -exec shasum -a 256 {} \; > SHA256SUMS)
python3 - "$STAGE" <<'PACK'
import sys,tarfile
from pathlib import Path
stage=Path(sys.argv[1])
def normalize(info):
    info.uid=info.gid=0
    info.uname=info.gname='root'
    info.mode=0o755 if info.isdir() else 0o644
    return info
with tarfile.open(str(stage)+'.tar.gz','w:gz',format=tarfile.USTAR_FORMAT) as archive:
    archive.add(stage,arcname='.',filter=normalize)
PACK
OBJECT="releases/$RELEASE_ID/release.tar.gz"
aws s3 cp "$STAGE.tar.gz" "s3://$ARTIFACT_BUCKET/$OBJECT" --sse AES256 --region "$REGION" --only-show-errors
# JSON construido con argumentos estructurados; el script remoto no contiene secretos.
python3 - "$ARTIFACT_BUCKET" "$OBJECT" "$RELEASE_ID" "$REGION" > "$STAGE/ssm-parameters.json" <<'PY'
import json,shlex,sys
bucket,obj,release,region=sys.argv[1:]
path='/opt/nexotech/releases/'+release
commands=['set -eu','umask 077','mkdir -p '+shlex.quote(path),
    'aws s3 cp '+shlex.quote('s3://'+bucket+'/'+obj)+' '+shlex.quote(path+'/release.tar.gz')+' --region '+shlex.quote(region)+' --only-show-errors',
    'tar -xzf '+shlex.quote(path+'/release.tar.gz')+' -C '+shlex.quote(path),
    'cd '+shlex.quote(path)+' && bash install-release-ec2.sh']
print(json.dumps({'commands':['\n'.join(commands)],'executionTimeout':['1800']}))
PY
COMMAND_ID="$(aws ssm send-command --region "$REGION" --instance-ids "$INSTANCE_ID" \
  --document-name AWS-RunShellScript --parameters "file://$STAGE/ssm-parameters.json" \
  --query Command.CommandId --output text)"
printf 'RELEASE_ID=%s\nCOMMAND_ID=%s\n' "$RELEASE_ID" "$COMMAND_ID"
