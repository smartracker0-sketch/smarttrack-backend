#!/usr/bin/env bash
set -euo pipefail

# ── TrackPro VPS Deploy Script ───────────────────────────────────────────────
# Usage:
#   First run:  ./deploy.sh --init
#   Updates:    ./deploy.sh

COMPOSE="docker compose -f docker-compose.prod.yml"

check_env() {
  if [ ! -f .env ]; then
    echo "❌  .env file not found."
    echo "    Copy .env.example to .env and fill in all values:"
    echo "    cp .env.example .env && nano .env"
    exit 1
  fi
}

install_docker() {
  if ! command -v docker &>/dev/null; then
    echo "🐳  Installing Docker..."
    curl -fsSL https://get.docker.com | sh
    sudo usermod -aG docker "$USER"
    echo "✅  Docker installed. You may need to log out and back in."
  fi
}

obtain_ssl() {
  source .env
  echo "🔒  Obtaining SSL certificate for ${DOMAIN}..."

  # Temporarily replace nginx config with HTTP-only (no ssl_certificate lines)
  # so nginx can start and serve the certbot challenge before certs exist
  cat > /tmp/certbot-bootstrap.conf << 'NGINXEOF'
server {
    listen 80;
    server_name _;
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    location / {
        return 200 'ok';
        add_header Content-Type text/plain;
    }
}
NGINXEOF

  # Mount the bootstrap config over the real one temporarily
  docker run -d --name certbot-nginx \
    -p 80:80 \
    -v /tmp/certbot-bootstrap.conf:/etc/nginx/conf.d/default.conf:ro \
    -v smarttrack-backend_certbot_www:/var/www/certbot \
    nginx:alpine

  echo "⏳  Waiting for bootstrap nginx..."
  sleep 5

  # Run certbot
  docker run --rm \
    -v smarttrack-backend_certbot_www:/var/www/certbot \
    -v smarttrack-backend_certbot_certs:/etc/letsencrypt \
    certbot/certbot certonly --webroot \
    --webroot-path=/var/www/certbot \
    --email "${CERTBOT_EMAIL}" \
    --agree-tos --no-eff-email \
    -d "${DOMAIN}"

  # Stop bootstrap nginx
  docker stop certbot-nginx && docker rm certbot-nginx

  echo "✅  SSL certificate obtained."
}

init() {
  echo "🚀  TrackPro — First-time setup"
  check_env
  install_docker

  echo "🏗️   Building and starting all services..."
  $COMPOSE pull
  $COMPOSE build --no-cache
  $COMPOSE up -d postgres redis mosquitto

  echo "⏳  Waiting for database to be ready..."
  sleep 15

  $COMPOSE up -d backend

  echo "⏳  Waiting for backend to start..."
  sleep 30

  obtain_ssl

  $COMPOSE up -d

  echo ""
  echo "✅  TrackPro is live!"
  source .env
  echo "   API:  https://${DOMAIN}/api/v1/auth/login"
  echo "   TCP:  ${DOMAIN}:5023 (GPS trackers)"
  echo ""
  echo "   Logs:  docker compose -f docker-compose.prod.yml logs -f backend"
}

update() {
  echo "🔄  Updating TrackPro..."
  check_env

  git pull origin main

  $COMPOSE build backend
  $COMPOSE up -d --no-deps backend

  echo "✅  Backend updated and restarted."
  $COMPOSE ps
}

renew_ssl() {
  echo "🔒  Renewing SSL certificates..."
  $COMPOSE run --rm certbot renew
  $COMPOSE exec nginx nginx -s reload
  echo "✅  Certificates renewed."
}

case "${1:-}" in
  --init)   init ;;
  --ssl)    renew_ssl ;;
  "")       update ;;
  *)
    echo "Usage: $0 [--init | --ssl]"
    echo "  (no args)  Pull latest and redeploy backend"
    echo "  --init     First-time full setup"
    echo "  --ssl      Renew SSL certificates"
    exit 1
    ;;
esac
