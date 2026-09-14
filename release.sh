#!/usr/bin/env bash
set -Eeuo pipefail

REPO="filcdev/mergen"
WORKFLOW="developer-release.yml"

if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI (gh) is not installed."
  echo
  echo "You do not need this script."
  echo "Open this page instead:"
  echo "https://github.com/${REPO}/actions/workflows/${WORKFLOW}"
  echo
  echo "Then click: Run workflow"
  exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "GitHub CLI is not authenticated."
  echo "Run: gh auth login"
  exit 1
fi

echo "Starting GitHub-hosted developer release..."
gh workflow run "$WORKFLOW" --repo "$REPO" --ref main

echo
echo "Started."
echo "Build progress:"
echo "https://github.com/${REPO}/actions/workflows/${WORKFLOW}"
echo
echo "When it finishes, the release will be here:"
echo "https://github.com/${REPO}/releases/tag/dev-latest"
