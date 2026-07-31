#!/usr/bin/env bash

set -euo pipefail

ssh -i ~/.ssh/id_ed25519 -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_HOST} << 'EOF'
  cd ./community
  sudo docker compose pull
  sudo docker compose up -d
EOF