#!/usr/bin/env bash
set -Eeuo pipefail

REPO="filcdev/mergen"
WORKFLOW="dev-release-build.yml"

if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI (gh) is not installed."
  echo "Run: https://cli.github.com"
  exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "GitHub CLI is not authenticated."
  echo "Run: gh auth login"
  exit 1
fi

if [[ "$(git rev-parse --abbrev-ref HEAD)" != "main" ]]; then
  echo "WARNING: not on 'main' (current: $(git rev-parse --abbrev-ref HEAD))."
fi

if [[ -n "$(git status --porcelain)" ]]; then
  echo "WARNING: working tree is dirty."
fi

echo "This creates a production release (tag + published GitHub release),"
echo "which triggers dev-release-build.yml to build and sign both platforms."
echo

printf 'Do you want to release it? [y/N] '
read -r ANSWER
case "$ANSWER" in
  y|Y|yes) ;;
  *)
    echo "Aborted."
    exit 1
    ;;
esac

# Auto-derive the next version from the latest released tag.
# Override by passing a tag explicitly, e.g. ./release-prod.sh v0.2.0
if [[ -n "${1:-}" ]]; then
  TAG="$1"
else
  # Latest version-shaped release (prereleases like dev-latest excluded).
  LATEST="$(gh release list --repo "$REPO" --exclude-drafts --exclude-pre-releases --limit 100 \
    --json tagName --jq '.[].tagName' \
    | grep -E '^v?[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$' \
    | sed 's/^v//' \
    | sort -t. -k1,1n -k2,2n -k3,3n \
    | tail -n1)" || true

  if [[ -z "$LATEST" ]]; then
    echo "Error: no existing version release found."
    echo "Pass one explicitly, e.g.: ./release-prod.sh v0.1.0"
    exit 1
  fi

  IFS=. read -r MAJ MIN PAT <<< "$LATEST"
  MAJ=$((10#$MAJ)); MIN=$((10#$MIN)); PAT=$((10#$PAT))
  PAT=$((PAT + 1))
  if (( PAT > 999 )); then
    PAT=0
    MIN=$((MIN + 1))
    if (( MIN > 999 )); then
      MIN=0
      MAJ=$((MAJ + 1))
    fi
  fi
  if (( MAJ > 999 )); then
    echo "Error: version overflow (MAJOR > 999); pass a tag manually." >&2
    exit 1
  fi
  TAG="v${MAJ}.${MIN}.${PAT}"
  echo "Latest release: v${LATEST} -> next: ${TAG}"
fi

if [[ ! "$TAG" =~ ^v?[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
  echo "Error: invalid release tag '$TAG'."
  echo "Must match v?MAJOR.MINOR.PATCH, each component 0-999."
  exit 1
fi

if [[ -n "$(git ls-remote --tags origin "refs/tags/$TAG")" ]]; then
  echo "Error: tag '$TAG' already exists on origin."
  exit 1
fi

gh release create "$TAG" \
  --repo "$REPO" \
  --target main \
  --title "Mergen $TAG" \
  --generate-notes

echo
echo "Release published: https://github.com/${REPO}/releases/tag/${TAG}"
echo
echo "dev-release-build.yml is now building + signing both platforms"
echo "(Android AAB + iOS IPA as artifacts)."
echo
echo "Watch it:"
echo "  gh run list --repo ${REPO} --workflow ${WORKFLOW}"
echo "  gh run watch <run-id> --repo ${REPO}"
echo "  https://github.com/${REPO}/actions/workflows/${WORKFLOW}"
echo
echo "Store upload is manual. Once the build succeeds, push Android to Google Play:"
echo "  gh workflow run dev-release.yml --repo ${REPO} -f platform=android"
echo
echo "  (also: -f platform=ios or -f platform=both, or the Actions UI:"
echo "   'Dev release to stores' -> Run workflow)"
echo
echo "Play Console reminder: promote the build from 'internal' to production by hand."
