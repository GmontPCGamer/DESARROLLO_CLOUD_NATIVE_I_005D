#!/usr/bin/env bash
# Se ejecuta en EC2; conserva la configuración privada fuera del repositorio.
set -euo pipefail
umask 077
ROOT=/opt/nexotech
source "$ROOT/env.sh"
if [[ ! -f "$ROOT/nginx-public.before-ep3.conf" && -f /etc/nginx/conf.d/nexotech-public.conf ]]; then
  cp /etc/nginx/conf.d/nexotech-public.conf "$ROOT/nginx-public.before-ep3.conf"
fi
cat > /etc/nginx/conf.d/nexotech-public.conf <<'NGINX'
server {
  listen 80;
  server_name 18.211.7.130;
  root /var/www/nexotech;
  index index.html;
  location = / { return 302 https://ourd5f7qr1.execute-api.us-east-1.amazonaws.com/dev/web/; }
  location /web/ {
    rewrite ^/web/(.*)$ /$1 break;
    try_files $uri $uri/ /index.html;
    add_header Cache-Control "no-cache";
  }
  location / { try_files $uri $uri/ /index.html; }
}
NGINX
chmod 644 /etc/nginx/conf.d/nexotech-public.conf
nginx -t
systemctl reload nginx
