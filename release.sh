#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

if [ $# -lt 1 ]; then
  echo "Usage: $0 <major|minor|patch>"
  exit 1
fi

RELEASE_TYPE="$(echo "$1" | tr '[:upper:]' '[:lower:]')"

# Determine current version from latest git tag or pom.xml
LATEST_TAG="$(git tag -l 'v[0-9]*.[0-9]*.[0-9]*' | sort -V | tail -n 1)"

if [ -n "${LATEST_TAG}" ]; then
  CURRENT_VERSION="${LATEST_TAG#v}"
else
  POM_VERSION="$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null || echo "0.0.0")"
  CURRENT_VERSION="${POM_VERSION%-SNAPSHOT}"
fi

# Parse major, minor, patch numbers
IFS='.' read -r MAJOR MINOR PATCH <<< "${CURRENT_VERSION}"
PATCH="${PATCH%%-*}"
MAJOR="${MAJOR:-0}"
MINOR="${MINOR:-0}"
PATCH="${PATCH:-0}"

case "${RELEASE_TYPE}" in
  major)
    MAJOR=$((MAJOR + 1))
    MINOR=0
    PATCH=0
    ;;
  minor)
    MINOR=$((MINOR + 1))
    PATCH=0
    ;;
  patch)
    PATCH=$((PATCH + 1))
    ;;
  *)
    echo "Error: Invalid release type '${RELEASE_TYPE}'." >&2
    echo "Usage: $0 <major|minor|patch>" >&2
    exit 1
    ;;
esac

NEW_VERSION="${MAJOR}.${MINOR}.${PATCH}"
NEW_TAG="v${NEW_VERSION}"

if git rev-parse -q --verify "refs/tags/${NEW_TAG}" >/dev/null; then
  echo "Error: Git tag '${NEW_TAG}' already exists." >&2
  exit 1
fi

echo "Current version: ${CURRENT_VERSION}"
echo "Bumping version to: ${NEW_VERSION} (${NEW_TAG})"

# Update version in pom.xml
./mvnw -q versions:set -DnewVersion="${NEW_VERSION}" -DgenerateBackupPoms=false

# Stage and commit pom.xml
git add pom.xml
if ! git diff --cached --quiet; then
  git commit -m "chore(release): Release ${NEW_VERSION}"
fi

# Create annotated tag
git tag -a "${NEW_TAG}" -m "Release ${NEW_TAG}"

echo ""
echo "Release ${NEW_TAG} prepared successfully."
echo "Auto-push is disabled. To trigger CI, push manually when ready:"
echo "  git push && git push origin ${NEW_TAG}"
