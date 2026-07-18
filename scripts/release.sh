#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-0.1.0-alpha}"
TAG="v$VERSION"

echo "Preparing release $TAG..."

# Verify clean working directory
if [ -n "$(git status --porcelain)" ]; then
  echo "ERROR: Working directory is not clean. Commit or stash changes first."
  exit 1
fi

# Verify on main/develop branch
BRANCH=$(git branch --show-current)
if [ "$BRANCH" != "main" ] && [ "$BRANCH" != "develop" ]; then
  echo "WARNING: Not on main or develop branch (current: $BRANCH)"
  read -p "Continue anyway? (y/N) " -n 1 -r
  echo
  if [[ ! $REPLY =~ ^[Yy]$ ]]; then exit 1; fi
fi

# Run full test suite
echo "Running tests..."
mvn clean verify -B

# Update POM versions (optional, skip for SNAPSHOT)
if [[ "$VERSION" != *"SNAPSHOT"* ]]; then
  echo "Setting version to $VERSION..."
  mvn versions:set -DnewVersion="$VERSION" -DgenerateBackupPoms=false
  git add -A
  git commit -m "chore: set version to $VERSION"
fi

# Create tag
echo "Creating tag $TAG..."
git tag -a "$TAG" -m "Release $TAG"

echo ""
echo "Release $TAG prepared!"
echo ""
echo "Next steps:"
echo "  git push origin $BRANCH"
echo "  git push origin $TAG"
echo ""
echo "Then create a GitHub release at:"
echo "  https://github.com/open-orchestration/oop/releases/new?tag=$TAG"
