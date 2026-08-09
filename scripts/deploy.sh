#!/usr/bin/env bash

set -euo pipefail

: "${SERVER_USER:?SERVER_USER is required}"
: "${SERVER_HOST:?SERVER_HOST is required}"
: "${BACKEND_IMAGE_TAG:?BACKEND_IMAGE_TAG is required}"

if [[ ! "$BACKEND_IMAGE_TAG" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Invalid backend image tag: $BACKEND_IMAGE_TAG" >&2
  exit 1
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_SOURCE="$SCRIPT_DIR/../deploy/compose.yaml"
SSH_OPTIONS=(
  -i "$HOME/.ssh/id_ed25519"
  -o StrictHostKeyChecking=no
)
REMOTE="${SERVER_USER}@${SERVER_HOST}"

scp "${SSH_OPTIONS[@]}" \
  "$COMPOSE_SOURCE" \
  "$REMOTE:~/community/compose.yaml.next"

ssh "${SSH_OPTIONS[@]}" "$REMOTE" \
  bash -s -- "$BACKEND_IMAGE_TAG" <<'REMOTE_SCRIPT'
set -euo pipefail

image_tag="$1"
cd "$HOME/community"

current_compose_file=""
for candidate in compose.yaml docker-compose.yaml docker-compose.yml; do
  if [[ -f "$candidate" ]]; then
    current_compose_file="$candidate"
    break
  fi
done

previous_image_id="$(
  sudo docker inspect community-backend --format '{{.Image}}' 2>/dev/null \
    || true
)"

printf 'BACKEND_IMAGE_TAG=%s\n' "$image_tag" > deploy.env.next

compose_next() {
  sudo docker compose \
    --file compose.yaml.next \
    --env-file deploy.env.next \
    "$@"
}

rollback() {
  if [[ -n "$current_compose_file" \
      && -f deploy.env \
      && -n "$(grep 'BACKEND_IMAGE_TAG' "$current_compose_file" || true)" ]]; then
    echo "Rolling back to the previous SHA deployment..." >&2
    sudo docker compose \
      --file "$current_compose_file" \
      --env-file deploy.env \
      up --detach --wait --wait-timeout 180 --force-recreate backend nginx
    return
  fi

  if [[ -n "$current_compose_file" && -n "$previous_image_id" ]]; then
    echo "Rolling back the first SHA deployment to the previous image..." >&2
    sudo docker image tag "$previous_image_id" wns1628/community-be:latest
    sudo docker compose \
      --file "$current_compose_file" \
      up --detach --pull never --force-recreate backend nginx
    return
  fi

  echo "No previous deployment is available for rollback." >&2
}

compose_next config >/dev/null
compose_next pull backend

if ! compose_next up \
    --detach \
    --wait \
    --wait-timeout 180 \
    --force-recreate \
    backend nginx; then
  compose_next logs --tail=200 backend || true
  rollback
  rm -f compose.yaml.next deploy.env.next
  exit 1
fi

actual_revision="$(
  sudo docker inspect community-backend \
    --format '{{ index .Config.Labels "org.opencontainers.image.revision" }}'
)"

if [[ "$actual_revision" != "$image_tag" ]]; then
  echo "Revision mismatch: expected=$image_tag actual=$actual_revision" >&2
  compose_next logs --tail=200 backend || true
  rollback
  rm -f compose.yaml.next deploy.env.next
  exit 1
fi

if [[ -n "$current_compose_file" ]]; then
  cp "$current_compose_file" compose.previous.yaml
fi
if [[ -f deploy.env ]]; then
  cp deploy.env deploy.previous.env
fi

mv compose.yaml.next compose.yaml
mv deploy.env.next deploy.env

echo "Deployment completed: $image_tag"
REMOTE_SCRIPT
