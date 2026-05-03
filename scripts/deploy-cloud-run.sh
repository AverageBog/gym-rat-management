#!/usr/bin/env bash
#
# Deploy gym-rat-management to Google Cloud Run.
#
# Sequence: local pre-flight (test-docker-image.sh) -> build & tag for
# Artifact Registry -> push -> gcloud run deploy -> live smoke test.
# Exits non-zero on the first failure so it can gate a deployment pipeline.
#
# Required env: GCP_PROJECT
# Optional env: GCP_REGION (default us-central1), AR_REPO (default
#   gym-rat-management), SERVICE_NAME (default gym-rat-management),
#   RUNTIME_SA (default gym-rat-runtime@${GCP_PROJECT}.iam.gserviceaccount.com),
#   DEPLOY_SKIP_LOCAL_TESTS=1 to skip the pre-flight (emergency redeploys only).
#
# Usage:  GCP_PROJECT=my-project ./scripts/deploy-cloud-run.sh
#         (or drop config in .env/cloud-run-vars; this script auto-sources it)

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# Auto-load local deploy config if present, so devs don't have to remember
# `set -a; source ...; set +a` before each run. The file is gitignored.
if [ -f ".env/cloud-run-vars" ]; then
    set -a
    # shellcheck disable=SC1091
    source ".env/cloud-run-vars"
    set +a
fi

GCP_REGION="${GCP_REGION:-us-central1}"
AR_REPO="${AR_REPO:-gym-rat-management}"
SERVICE_NAME="${SERVICE_NAME:-gym-rat-management}"

GREEN=$'\033[0;32m'
RED=$'\033[0;31m'
YELLOW=$'\033[0;33m'
NC=$'\033[0m'

pass() { echo "${GREEN}[PASS]${NC} $1"; }
fail() { echo "${RED}[FAIL]${NC} $1" >&2; exit 1; }
info() { echo "${YELLOW}[....]${NC} $1"; }

# ----------------------------------------------------------------------------
# 1. Config & prerequisites
# ----------------------------------------------------------------------------
if [ -z "${GCP_PROJECT:-}" ]; then
    fail "GCP_PROJECT is required. Example: GCP_PROJECT=my-project-id $0"
fi

RUNTIME_SA="${RUNTIME_SA:-gym-rat-runtime@${GCP_PROJECT}.iam.gserviceaccount.com}"

command -v gcloud >/dev/null 2>&1 || fail "gcloud not found on PATH. Install the Google Cloud SDK."
command -v docker >/dev/null 2>&1 || fail "docker not found on PATH."
docker info >/dev/null 2>&1 || fail "Cannot reach the Docker daemon. Is Docker running?"

ACTIVE_ACCOUNT=$(gcloud auth list --filter=status:ACTIVE --format='value(account)' 2>/dev/null | head -1)
if [ -z "$ACTIVE_ACCOUNT" ]; then
    fail "No active gcloud account. Run: gcloud auth login"
fi
pass "gcloud authenticated as $ACTIVE_ACCOUNT"

GIT_SHA=$(git rev-parse --short HEAD 2>/dev/null || echo "untagged")
AR_HOST="${GCP_REGION}-docker.pkg.dev"
AR_PATH="${AR_HOST}/${GCP_PROJECT}/${AR_REPO}/${SERVICE_NAME}"
SHA_TAG="${AR_PATH}:${GIT_SHA}"
LATEST_TAG="${AR_PATH}:latest"

info "Project:    $GCP_PROJECT"
info "Region:     $GCP_REGION"
info "Service:    $SERVICE_NAME"
info "Runtime SA: $RUNTIME_SA"
info "Image:      $SHA_TAG"

# ----------------------------------------------------------------------------
# 2. Local pre-flight
# ----------------------------------------------------------------------------
if [ "${DEPLOY_SKIP_LOCAL_TESTS:-0}" = "1" ]; then
    info "DEPLOY_SKIP_LOCAL_TESTS=1 — skipping local pre-flight (emergency mode)"
else
    info "Running local pre-flight: scripts/test-docker-image.sh"
    if ! "$PROJECT_ROOT/scripts/test-docker-image.sh"; then
        fail "Local pre-flight failed. Fix and retry, or set DEPLOY_SKIP_LOCAL_TESTS=1 to override."
    fi
    pass "Local pre-flight passed"
fi

# ----------------------------------------------------------------------------
# 3. Build & tag for Artifact Registry
# ----------------------------------------------------------------------------
info "Building $SHA_TAG"
docker build -t "$SHA_TAG" -t "$LATEST_TAG" . > /tmp/cloud-run-build-$$.log 2>&1 || {
    cat /tmp/cloud-run-build-$$.log
    fail "docker build failed"
}
pass "Image built and tagged for Artifact Registry"

# ----------------------------------------------------------------------------
# 4. Push to Artifact Registry
# ----------------------------------------------------------------------------
info "Configuring docker auth for $AR_HOST"
gcloud auth configure-docker "$AR_HOST" --quiet >/dev/null 2>&1 \
    || fail "gcloud auth configure-docker failed"

info "Pushing $SHA_TAG"
docker push "$SHA_TAG" >/dev/null || fail "docker push of $SHA_TAG failed"
docker push "$LATEST_TAG" >/dev/null || fail "docker push of $LATEST_TAG failed"
pass "Pushed $SHA_TAG and :latest"

# ----------------------------------------------------------------------------
# 5. Deploy to Cloud Run
# ----------------------------------------------------------------------------
# Best-effort pre-resolve the URL so we can pin CORS to the exact origin on
# this deploy. Empty on first-ever deploy — the prod profile's `https://*.a.run.app`
# fallback covers that case so login still works, then subsequent deploys tighten.
EXISTING_URL=$(gcloud run services describe "$SERVICE_NAME" \
    --project "$GCP_PROJECT" \
    --region "$GCP_REGION" \
    --format='value(status.url)' 2>/dev/null || true)

ENV_VARS="SPRING_PROFILES_ACTIVE=prod"
if [ -n "$EXISTING_URL" ]; then
    ENV_VARS="${ENV_VARS},APP_CORS_ALLOWED_ORIGIN_PATTERNS=${EXISTING_URL}"
    info "Pinning CORS origin to $EXISTING_URL"
else
    info "First deploy — CORS will fall back to https://*.a.run.app until next run"
fi

info "Deploying $SERVICE_NAME to Cloud Run in $GCP_REGION"
gcloud run deploy "$SERVICE_NAME" \
    --project "$GCP_PROJECT" \
    --image "$SHA_TAG" \
    --region "$GCP_REGION" \
    --platform managed \
    --service-account "$RUNTIME_SA" \
    --allow-unauthenticated \
    --port 8080 \
    --memory 1Gi \
    --cpu 1 \
    --cpu-boost \
    --min-instances 0 \
    --max-instances 3 \
    --set-env-vars "$ENV_VARS" \
    --set-secrets "JWT_SECRET=jwt-secret:latest,CARD_ENCRYPTION_KEY=card-encryption-key:latest,ADMIN_SEED_PASSWORD=admin-seed-password:latest" \
    --quiet \
    || fail "gcloud run deploy failed"
pass "Deploy command completed"

SERVICE_URL=$(gcloud run services describe "$SERVICE_NAME" \
    --project "$GCP_PROJECT" \
    --region "$GCP_REGION" \
    --format='value(status.url)')
[ -n "$SERVICE_URL" ] || fail "Could not resolve service URL after deploy"
pass "Service URL: $SERVICE_URL"

# ----------------------------------------------------------------------------
# 6. Live smoke test
# ----------------------------------------------------------------------------
# Pull the seeded admin password from Secret Manager so the smoke can do the
# authed checks. Best-effort: if the SA running this script can't read it, we
# still run the unauthed contract checks.
ADMIN_SEED_PASSWORD=$(gcloud secrets versions access latest \
    --secret=admin-seed-password \
    --project="$GCP_PROJECT" 2>/dev/null || true)
export ADMIN_SEED_PASSWORD

info "Running live smoke test against $SERVICE_URL"
if ! "$PROJECT_ROOT/scripts/test-cloud-run-deployment.sh" "$SERVICE_URL"; then
    fail "Live smoke test failed against $SERVICE_URL"
fi

echo
echo "${GREEN}Deployment successful: $SERVICE_URL${NC}"
