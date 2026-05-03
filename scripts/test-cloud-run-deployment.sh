#!/usr/bin/env bash
#
# Smoke-test a deployed gym-rat-management instance against its public URL.
#
# Use after `deploy-cloud-run.sh` (which calls this automatically) or
# standalone to verify an existing deployment without redeploying.
#
# Usage:  ./scripts/test-cloud-run-deployment.sh https://your-service-xyz-uc.a.run.app

set -euo pipefail

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <service-url>" >&2
    exit 2
fi

URL="${1%/}"   # strip trailing slash if present

GREEN=$'\033[0;32m'
RED=$'\033[0;31m'
YELLOW=$'\033[0;33m'
NC=$'\033[0m'

pass() { echo "${GREEN}[PASS]${NC} $1"; }
fail() { echo "${RED}[FAIL]${NC} $1" >&2; exit 1; }
info() { echo "${YELLOW}[....]${NC} $1"; }

check_status() {
    local target="$1" expected="$2" label="$3"
    local actual
    actual=$(curl -sS -o /dev/null -w '%{http_code}' --max-time 30 "$target")
    if [ "$actual" = "$expected" ]; then
        pass "$label ($actual)"
    else
        fail "$label expected $expected, got $actual ($target)"
    fi
}

check_content_type() {
    local target="$1" pattern="$2" label="$3"
    local ct
    ct=$(curl -sS -o /dev/null -w '%{content_type}' --max-time 30 "$target")
    if echo "$ct" | grep -qi "$pattern"; then
        pass "$label (content-type: $ct)"
    else
        fail "$label content-type was '$ct', expected to match '$pattern' ($target)"
    fi
}

info "Smoke testing $URL"

# Cold-start tolerant: warm the URL before timing-sensitive checks.
info "Warming the service (cold-start tolerant, up to 60s)"
elapsed=0
until curl -fsS -o /dev/null --max-time 30 "$URL/" 2>/dev/null; do
    if [ "$elapsed" -ge 60 ]; then
        fail "Service did not respond at $URL/ within 60s"
    fi
    sleep 3
    elapsed=$((elapsed + 3))
done
pass "Service responsive (took ${elapsed}s)"

# ----------------------------------------------------------------------------
# Endpoint contract
# ----------------------------------------------------------------------------
check_status       "$URL/"             "200"       "GET / returns SPA"
check_content_type "$URL/"             "text/html" "GET / serves HTML"
check_status       "$URL/members"      "200"       "GET /members (SPA route)"
check_status       "$URL/h2-console"   "404"       "GET /h2-console disabled under prod"
check_status       "$URL/api/members"  "403"       "GET /api/members requires auth"

ASSET_PATH=$(curl -sS --max-time 30 "$URL/" | grep -oE '/assets/[^"]+\.js' | head -1)
if [ -n "$ASSET_PATH" ]; then
    check_status       "$URL$ASSET_PATH" "200"        "GET $ASSET_PATH"
    check_content_type "$URL$ASSET_PATH" "javascript" "Asset served as JS (not the SPA HTML)"
else
    fail "Could not find an /assets/*.js reference in index.html"
fi

check_status "$URL/assets/definitely-missing.js" "404" "Missing asset 404s (no SPA fallback)"

# ----------------------------------------------------------------------------
# Login + authenticated request
# ----------------------------------------------------------------------------
# The seeded admin password lives in Secret Manager — `deploy-cloud-run.sh` exports
# it before invoking this script. When run standalone without it set, skip the
# authed checks rather than fail (still useful for quick contract checks).
if [ -z "${ADMIN_SEED_PASSWORD:-}" ]; then
    info "ADMIN_SEED_PASSWORD not set — skipping authenticated checks. To enable:"
    info "  ADMIN_SEED_PASSWORD=\$(gcloud secrets versions access latest --secret=admin-seed-password) $0 $URL"
else
    LOGIN_BODY=$(printf '{"email":"admin@gym.com","password":"%s"}' "$ADMIN_SEED_PASSWORD")
    LOGIN_RESPONSE=$(curl -sS --max-time 30 -X POST "$URL/api/auth/login" \
        -H 'Content-Type: application/json' \
        -d "$LOGIN_BODY")
    TOKEN=$(echo "$LOGIN_RESPONSE" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
    if [ -z "$TOKEN" ]; then
        fail "Login did not return a token. Response: $LOGIN_RESPONSE"
    fi
    pass "Login returns a JWT (length ${#TOKEN})"

    MEMBER_RESPONSE=$(curl -sS --max-time 30 -H "Authorization: Bearer $TOKEN" "$URL/api/members")
    MEMBER_COUNT=$(echo "$MEMBER_RESPONSE" | grep -oE '"id":[0-9]+' | wc -l)
    if [ "$MEMBER_COUNT" -gt 0 ]; then
        pass "Authenticated GET /api/members returned $MEMBER_COUNT members"
    else
        fail "GET /api/members returned no members. Response: $MEMBER_RESPONSE"
    fi
fi

echo
echo "${GREEN}Live deployment smoke passed: $URL${NC}"
