#!/usr/bin/env bash
# One-time setup on a fresh Ubuntu 24.04 server. Run as root:
#   curl -fsSL https://raw.githubusercontent.com/<you>/artisticView-service/main/deploy/setup-server.sh -o setup.sh
#   DOMAIN=artisticview.in bash setup.sh
# (or copy this file over with scp). Afterwards finish Jenkins setup in the browser.
set -euo pipefail

DOMAIN="${DOMAIN:?set DOMAIN, e.g. DOMAIN=artisticview.in}"
STACK=/opt/artisticview
REPO_RAW="${REPO_RAW:-}"   # optional raw GitHub base URL for deploy/ files; otherwise files next to this script are used

echo "==> Docker"
if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sh
fi
systemctl enable --now docker

echo "==> Firewall (SSH, HTTP, HTTPS only)"
if command -v ufw >/dev/null; then
  ufw allow OpenSSH && ufw allow 80/tcp && ufw allow 443/tcp && ufw allow 443/udp
  ufw --force enable
fi

echo "==> Swap (Gradle + Jenkins need headroom on small servers)"
if ! swapon --show | grep -q swapfile; then
  fallocate -l 4G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

echo "==> Stack folder $STACK"
mkdir -p "$STACK/jenkins"
here="$(cd "$(dirname "$0")" && pwd)"
for f in docker-compose.yml jenkins/Dockerfile jenkins/plugins.txt; do
  if [ -n "$REPO_RAW" ]; then curl -fsSL "$REPO_RAW/deploy/$f" -o "$STACK/$f"; else cp "$here/$f" "$STACK/$f"; fi
done

if [ ! -f "$STACK/.env" ]; then
  rand() { openssl rand -hex "$1"; }
  cat > "$STACK/.env" <<ENV
PUBLIC_URL=https://$DOMAIN
SITE_ADDRESS=$DOMAIN, www.$DOMAIN
JENKINS_ADDRESS=jenkins.$DOMAIN
DB_USER=artisticview
DB_PASSWORD=$(rand 24)
REDIS_PASSWORD=$(rand 24)
JWT_SECRET=$(rand 48)
SUPER_ADMIN_EMAIL=
DOCKER_GID=$(getent group docker | cut -d: -f3)
ENV
  chmod 600 "$STACK/.env"
  echo "    wrote $STACK/.env with fresh secrets"
fi

echo "==> Database, Redis and Jenkins"
cd "$STACK"
docker compose --profile ci up -d --build postgres redis jenkins

echo
echo "Done. Next:"
echo "  1. DNS: A records for $DOMAIN, www.$DOMAIN and jenkins.$DOMAIN -> $(curl -fs https://api.ipify.org || echo '<server ip>')"
echo "  2. Jenkins admin password:  docker exec artisticview-jenkins cat /var/jenkins_home/secrets/initialAdminPassword"
echo "     Until the web container runs, reach Jenkins with:  ssh -L 8081:localhost:8081 root@<server>  (see deploy/README.md)"
echo "  3. Create the two pipeline jobs (backend first, then frontend)."
