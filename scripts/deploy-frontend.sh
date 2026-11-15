#!/usr/bin/env bash
# Publica Angular en la EC2 existente mediante SSM y conserva la versión anterior.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REGION="${AWS_REGION:-us-east-1}"
INSTANCE_ID="${INSTANCE_ID:-i-0e3ea93394a16686d}"
BUCKET="${ARTIFACT_BUCKET:-nexotech-jars-7717}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
STAGE="$ROOT/.run/frontend/$STAMP"
mkdir -p "$STAGE"
(cd "$ROOT/frontend" && npm exec --yes --package=node@24 -- node node_modules/@angular/cli/bin/ng.js build --configuration production)
python3 - "$ROOT/frontend/dist/frontend/browser" "$STAGE/frontend.tar.gz" <<'PY'
import sys,tarfile
def normalize(info):
    info.uid=info.gid=0
    info.uname=info.gname='root'
    return info
with tarfile.open(sys.argv[2],'w:gz',format=tarfile.USTAR_FORMAT) as archive:
    archive.add(sys.argv[1],arcname='.',filter=normalize)
PY
OBJECT="frontend/$STAMP/frontend.tar.gz"
aws s3 cp "$STAGE/frontend.tar.gz" "s3://$BUCKET/$OBJECT" --sse AES256 --region "$REGION" --only-show-errors
python3 - "$BUCKET" "$OBJECT" "$STAMP" "$REGION" > "$STAGE/parameters.json" <<'PY'
import json,shlex,sys
bucket,obj,stamp,region=sys.argv[1:]
local='/opt/nexotech/frontend-'+stamp+'.tar.gz'
commands=['set -eu','umask 077',
 'aws s3 cp '+shlex.quote('s3://'+bucket+'/'+obj)+' '+shlex.quote(local)+' --region '+shlex.quote(region)+' --only-show-errors',
 'cp -a /var/www/nexotech '+shlex.quote('/opt/nexotech/frontend-before-'+stamp),
 'tar -xzf '+shlex.quote(local)+' -C /var/www/nexotech',
 'chmod -R a+rX /var/www/nexotech',
 'curl -fsS http://127.0.0.1/web/index.html >/dev/null','echo FRONTEND_OK']
print(json.dumps({'commands':['\n'.join(commands)]}))
PY
aws ssm send-command --region "$REGION" --instance-ids "$INSTANCE_ID" --document-name AWS-RunShellScript \
  --parameters "file://$STAGE/parameters.json" --query Command.CommandId --output text
printf 'Web: https://ourd5f7qr1.execute-api.us-east-1.amazonaws.com/dev/web/\n'
