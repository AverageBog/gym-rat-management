#!/usr/bin/env bash
#
# Pre-deployment smoke test for the gym-rat-management Docker image.
#
# Builds the image, exercises the runtime contract (env vars, non-root, SPA +
# API on one port, prod profile, login flow), and tears the container down.
# Exits non-zero on the first failure so it can gate a deploy.
#
# Usage:  ./scripts/test-docker-image.sh

set -euo pipefail

IMAGE_NAME="gym-rat-management:latest"
CONTAINER_NAME="gym-rat-management-smoketest-$$"
HOST_PORT="${HOST_PORT:-18080}"
READY_TIMEOUT_SECONDS=90

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

GREEN=$'\033[0;32m'
RED=$'\033[0;31m'
YELLOW=$'\033[0;33m'
NC=$'\033[0m'

pass() { echo "${GREEN}[PASS]${NC} $1"; }
fail() { echo "${RED}[FAIL]${NC} $1" >&2; exit 1; }
info() { echo "${YELLOW}[....]${NC} $1"; }

cleanup() {
    docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT

# ----------------------------------------------------------------------------
# 1. Build
# ----------------------------------------------------------------------------
info "Building $IMAGE_NAME"
if ! docker build -t "$IMAGE_NAME" . > /tmp/docker-build-$$.log 2>&1; then
    cat /tmp/docker-build-$$.log
    fail "docker build failed"
fi
pass "Image built"

docker images "$IMAGE_NAME" --format '{{.Repository}}:{{.Tag}}' | grep -q "^${IMAGE_NAME}$" \
    || fail "Image $IMAGE_NAME not present after build"
IMAGE_SIZE=$(docker images "$IMAGE_NAME" --format '{{.Size}}')
pass "Image present (size: $IMAGE_SIZE)"

# ----------------------------------------------------------------------------
# 2. Failure mode — must exit non-zero without required env vars
# ----------------------------------------------------------------------------
info "Verifying container fails fast without required env vars"
if docker run --rm --name "${CONTAINER_NAME}-noenv" "$IMAGE_NAME" >/dev/null 2>&1; then
    fail "Container started without JWT_SECRET / CARD_ENCRYPTION_KEY (it must not)"
fi
pass "Container fails fast without required env"

# ----------------------------------------------------------------------------
# 3. Run with proper env
# ----------------------------------------------------------------------------
JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')
CARD_ENCRYPTION_KEY=$(openssl rand -base64 32 | tr -d '\n')

info "Starting container on host port $HOST_PORT"
docker run -d \
    --name "$CONTAINER_NAME" \
    -p "${HOST_PORT}:8080" \
    -e JWT_SECRET="$JWT_SECRET" \
    -e CARD_ENCRYPTION_KEY="$CARD_ENCRYPTION_KEY" \
    "$IMAGE_NAME" >/dev/null

info "Waiting for app readiness (timeout ${READY_TIMEOUT_SECONDS}s)"
elapsed=0
until curl -fsS -o /dev/null "http://localhost:${HOST_PORT}/" 2>/dev/null; do
    if [ "$elapsed" -ge "$READY_TIMEOUT_SECONDS" ]; then
        echo "--- container logs ---"
        docker logs "$CONTAINER_NAME" 2>&1 | tail -50
        fail "Container did not become responsive within ${READY_TIMEOUT_SECONDS}s"
    fi
    sleep 2
    elapsed=$((elapsed + 2))
done
pass "App responsive (took ${elapsed}s)"

# ----------------------------------------------------------------------------
# 4. Prod profile active in logs
# ----------------------------------------------------------------------------
if docker logs "$CONTAINER_NAME" 2>&1 | grep -q '"prod"'; then
    pass "Spring 'prod' profile active"
else
    fail "Expected 'prod' profile in startup logs"
fi

# ----------------------------------------------------------------------------
# 5. Non-root user
# ----------------------------------------------------------------------------
USER_ID=$(docker exec "$CONTAINER_NAME" id -u)
if [ "$USER_ID" = "10001" ]; then
    pass "Container running as non-root (uid 10001)"
else
    fail "Container running as uid $USER_ID, expected 10001"
fi

# ----------------------------------------------------------------------------
# 6. Endpoint smoke tests
# ----------------------------------------------------------------------------
check_status() {
    local url="$1" expected="$2" label="$3"
    local actual
    actual=$(curl -sS -o /dev/null -w '%{http_code}' "$url")
    if [ "$actual" = "$expected" ]; then
        pass "$label ($actual)"
    else
        fail "$label expected $expected, got $actual ($url)"
    fi
}

check_content_type() {
    local url="$1" pattern="$2" label="$3"
    local ct
    ct=$(curl -sS -o /dev/null -w '%{content_type}' "$url")
    if echo "$ct" | grep -qi "$pattern"; then
        pass "$label (content-type: $ct)"
    else
        fail "$label content-type was '$ct', expected to match '$pattern' ($url)"
    fi
}

BASE="http://localhost:${HOST_PORT}"

check_status   "$BASE/"             "200"  "GET / returns SPA"
check_content_type "$BASE/"         "text/html" "GET / serves HTML"
check_status   "$BASE/members"      "200"  "GET /members (SPA route)"
check_status   "$BASE/h2-console"   "404"  "GET /h2-console disabled under prod"
check_status   "$BASE/api/members"  "403"  "GET /api/members requires auth"

# Real asset (discover hashed JS filename from index.html)
ASSET_PATH=$(curl -sS "$BASE/" | grep -oE '/assets/[^"]+\.js' | head -1)
if [ -n "$ASSET_PATH" ]; then
    check_status       "$BASE$ASSET_PATH" "200"        "GET $ASSET_PATH"
    check_content_type "$BASE$ASSET_PATH" "javascript" "Asset served as JS (not the SPA HTML)"
else
    fail "Could not find an /assets/*.js reference in index.html"
fi

# Missing asset must 404, not fall back to SPA
check_status "$BASE/assets/definitely-missing.js" "404" "Missing asset 404s (no SPA fallback)"

# ----------------------------------------------------------------------------
# 7. Login + authenticated request
# ----------------------------------------------------------------------------
LOGIN_BODY='{"email":"admin@gym.com","password":"Admin123!"}'
LOGIN_RESPONSE=$(curl -sS -X POST "$BASE/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "$LOGIN_BODY")
TOKEN=$(echo "$LOGIN_RESPONSE" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
if [ -z "$TOKEN" ]; then
    fail "Login did not return a token. Response: $LOGIN_RESPONSE"
fi
pass "Login returns a JWT (length ${#TOKEN})"

MEMBER_RESPONSE=$(curl -sS -H "Authorization: Bearer $TOKEN" "$BASE/api/members")
MEMBER_COUNT=$(echo "$MEMBER_RESPONSE" | grep -oE '"id":[0-9]+' | wc -l)
if [ "$MEMBER_COUNT" -gt 0 ]; then
    pass "Authenticated GET /api/members returned $MEMBER_COUNT members"
else
    fail "GET /api/members returned no members. Response: $MEMBER_RESPONSE"
fi

echo
echo "${GREEN}All Docker image checks passed.${NC}"
