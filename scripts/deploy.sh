#!/usr/bin/env bash

set -euo pipefail

ssh -i ~/.ssh/id_ed25519 -o StrictHostKeyChecking=no ${{ secrets.SERVER_USER }}@${{ secrets.SERVER_HOST }} << 'EOF'
  cd /home/${{ secrets.SERVER_USER }}/backend
  docker compose pull
  docker compose up -d
EOF