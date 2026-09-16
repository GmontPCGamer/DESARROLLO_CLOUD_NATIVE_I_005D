#!/usr/bin/env bash
# Build Angular prod, lo publica con nginx en la EC2 y obtiene certificado Let's Encrypt (sslip.io).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/terraform"
IP="$(terraform output -raw ec2_public_ip)"
FE_URL="$(terraform output -raw frontend_url)"
API_URL="$(terraform output -raw url_deploy)"
HOST="${IP//./-}.sslip.io"
KEY="${NEXOTECH_SSH_KEY:-$HOME/.ssh/vockey.pem}"
SSH=(ssh -i "$KEY" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ec2-user@"$IP")
SCP=(scp -i "$KEY" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null)

ENV_FILE="$ROOT/frontend/src/environments/environment.ts"
cat >"$ENV_FILE" <<EOF
export const environment = {
  production: true,
  msal: {
    clientId: '097bfd84-a8e3-4232-9048-718f4d648efd',
    authority: 'https://login.microsoftonline.com/72fd0b5a-8a6a-4cff-89f6-bde961f7e250',
    apiScope: 'api://nexotech-student-api/access_as_user',
    redirectUri: '${FE_URL}/auth',
    postLogoutRedirectUri: '${FE_URL}',
  },
  backendApiUrl: '${API_URL}/api',
};
EOF

echo "==> Build Angular → API=$API_URL  FE=$FE_URL"
export PATH="/opt/homebrew/opt/node@24/bin:$PATH"
cd "$ROOT/frontend"
npx ng build --configuration production

DIST="$ROOT/frontend/dist/frontend/browser"
[[ -d "$DIST" ]] || DIST="$ROOT/frontend/dist/frontend"

echo "==> Subiendo dist a EC2…"
"${SSH[@]}" 'sudo mkdir -p /var/www/nexotech && sudo chown ec2-user:ec2-user /var/www/nexotech'
rsync -az -e "ssh -i $KEY -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null" \
  "$DIST"/ ec2-user@"$IP":/var/www/nexotech/

echo "==> Instalando nginx + certbot…"
"${SSH[@]}" 'sudo dnf install -y nginx certbot python3-certbot-nginx >/dev/null'

"${SSH[@]}" "sudo tee /etc/nginx/conf.d/nexotech.conf >/dev/null" <<NGINX
server {
  listen 80;
  server_name ${HOST} ${IP};
  root /var/www/nexotech;
  index index.html;

  location / {
    try_files \$uri \$uri/ /index.html;
  }
}
NGINX

"${SSH[@]}" 'sudo systemctl enable nginx && sudo systemctl restart nginx'

echo "==> Certificado Let's Encrypt (sslip.io)…"
"${SSH[@]}" "sudo certbot --nginx -d ${HOST} --non-interactive --agree-tos --register-unsafely-without-email --redirect || true"

cat <<MSG

Frontend:  ${FE_URL}
API GW:    ${API_URL}

Entra ID — agrega YA estas URIs en la app NexoTech Student:
  Redirect URI (SPA):  ${FE_URL}/auth
  Logout URI:          ${FE_URL}

MSG
