#!/usr/bin/env bash
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

if [[ -f ".release.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source ".release.env"
  set +a
fi

REPO="${GITHUB_REPO:-filcdev/mergen}"
TAG="${RELEASE_TAG:-dev-latest}"
TITLE="${RELEASE_TITLE:-Mergen – latest developer build}"
IOS_EXPORT_METHOD="${IOS_EXPORT_METHOD:-development}"
IOS_MARKETING_VERSION="${IOS_MARKETING_VERSION:-1.0.0}"
BUILD_IOS="${BUILD_IOS:-auto}"
PUBLISH="${PUBLISH:-1}"
ALLOW_DIRTY="${ALLOW_DIRTY:-0}"

DIST="$ROOT/dist"
BUILD_ROOT="$ROOT/.release-build"
IOS_DERIVED="$BUILD_ROOT/ios-derived"
IOS_ARCHIVE="$BUILD_ROOT/Mergen.xcarchive"
IOS_EXPORT="$BUILD_ROOT/ios-export"
EXPORT_OPTIONS="$BUILD_ROOT/ExportOptions.plist"
NOTES="$BUILD_ROOT/release-notes.md"

usage() {
  cat <<'EOF'
Usage:
  ./release.sh
  ./release.sh --local
  ./release.sh --android-only
  ./release.sh --ios-method development
  ./release.sh --ios-method ad-hoc

Options:
  --local           Build artifacts only; do not publish GitHub Release.
  --android-only    Skip all iOS builds.
  --ios-method X    Xcode export method, normally development or ad-hoc.
  -h, --help        Show this help.

Optional .release.env:
  GITHUB_REPO=filcdev/mergen
  APPLE_TEAM_ID=XXXXXXXXXX
  IOS_BUNDLE_ID=hu.petrik.filcapp.FilcappXXXXXXXXXX
  IOS_EXPORT_METHOD=development
  BUILD_IOS=auto
  PUBLISH=1
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --local)
      PUBLISH=0
      shift
      ;;
    --android-only)
      BUILD_IOS=0
      shift
      ;;
    --ios-method)
      IOS_EXPORT_METHOD="${2:?Missing value after --ios-method}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 2
      ;;
  esac
done

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "ERROR: Required command not found: $1" >&2
    exit 1
  fi
}

echo "============================================================"
echo "MERGEN DEVELOPER RELEASE"
echo "============================================================"

require_cmd git

if [[ "$ALLOW_DIRTY" != "1" ]] && [[ -n "$(git status --porcelain)" ]]; then
  echo "ERROR: Working tree is not clean."
  echo "Commit/stash changes first, or set ALLOW_DIRTY=1."
  git status --short
  exit 1
fi

BRANCH="$(git branch --show-current)"
SHA="$(git rev-parse HEAD)"
SHORT_SHA="$(git rev-parse --short=8 HEAD)"
BUILT_AT="$(date -u +"%Y-%m-%d %H:%M UTC")"
BUILD_NUMBER="$(( $(date +%s) / 60 ))"
VERSION_NAME="1.0.0-dev.${BUILD_NUMBER}"

rm -rf "$DIST" "$BUILD_ROOT"
mkdir -p "$DIST" "$BUILD_ROOT"

echo
echo "Branch:       $BRANCH"
echo "Commit:       $SHORT_SHA"
echo "Build number: $BUILD_NUMBER"
echo "Version:      $VERSION_NAME"
echo

echo "[1/5] Building Android developer APK..."
if [[ -x "./gradlew" ]]; then
  ./gradlew \
    :composeApp:assembleDebug \
    -PmergenVersionCode="$BUILD_NUMBER" \
    -PmergenVersionName="$VERSION_NAME"
elif [[ -f "./gradlew.bat" ]]; then
  cmd.exe /c gradlew.bat \
    :composeApp:assembleDebug \
    -PmergenVersionCode="$BUILD_NUMBER" \
    -PmergenVersionName="$VERSION_NAME"
else
  echo "ERROR: Gradle wrapper not found." >&2
  exit 1
fi

ANDROID_APK="composeApp/build/outputs/apk/debug/composeApp-debug.apk"
if [[ ! -f "$ANDROID_APK" ]]; then
  echo "ERROR: Android APK not found after build: $ANDROID_APK" >&2
  exit 1
fi

cp "$ANDROID_APK" "$DIST/Mergen-android-dev.apk"

ASSETS=(
  "$DIST/Mergen-android-dev.apk"
)

IOS_STATUS="Not built (run release.sh on macOS for iOS artifacts)."

if [[ "$BUILD_IOS" != "0" ]] && [[ "$(uname -s)" == "Darwin" ]]; then
  require_cmd xcodebuild
  require_cmd xcrun
  require_cmd ditto

  echo "[2/5] Building iOS Simulator artifact..."

  rm -rf "$IOS_DERIVED"
  xcodebuild \
    -project "iosApp/iosApp.xcodeproj" \
    -scheme "iosApp" \
    -configuration Debug \
    -sdk iphonesimulator \
    -destination "generic/platform=iOS Simulator" \
    -derivedDataPath "$IOS_DERIVED" \
    CODE_SIGNING_ALLOWED=NO \
    CURRENT_PROJECT_VERSION="$BUILD_NUMBER" \
    MARKETING_VERSION="$IOS_MARKETING_VERSION" \
    build

  SIM_APP="$(
    find "$IOS_DERIVED/Build/Products" \
      -type d \
      -name "*.app" \
      -path "*Debug-iphonesimulator*" \
      -print \
      -quit
  )"

  if [[ -z "$SIM_APP" || ! -d "$SIM_APP" ]]; then
    echo "ERROR: iOS Simulator .app not found after build." >&2
    exit 1
  fi

  ditto \
    -c \
    -k \
    --sequesterRsrc \
    --keepParent \
    "$SIM_APP" \
    "$DIST/Mergen-ios-simulator.zip"

  ASSETS+=("$DIST/Mergen-ios-simulator.zip")
  IOS_STATUS="Simulator build included."

  if [[ -n "${APPLE_TEAM_ID:-}" ]]; then
    echo "[3/5] Building signed iOS archive..."

    IOS_BUNDLE_ID="${IOS_BUNDLE_ID:-hu.petrik.filcapp.Filcapp${APPLE_TEAM_ID}}"

    rm -rf "$IOS_ARCHIVE" "$IOS_EXPORT"

    xcodebuild \
      -project "iosApp/iosApp.xcodeproj" \
      -scheme "iosApp" \
      -configuration Release \
      -destination "generic/platform=iOS" \
      -archivePath "$IOS_ARCHIVE" \
      TEAM_ID="$APPLE_TEAM_ID" \
      DEVELOPMENT_TEAM="$APPLE_TEAM_ID" \
      PRODUCT_BUNDLE_IDENTIFIER="$IOS_BUNDLE_ID" \
      CURRENT_PROJECT_VERSION="$BUILD_NUMBER" \
      MARKETING_VERSION="$IOS_MARKETING_VERSION" \
      -allowProvisioningUpdates \
      archive

    cat > "$EXPORT_OPTIONS" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>method</key>
  <string>${IOS_EXPORT_METHOD}</string>
  <key>signingStyle</key>
  <string>automatic</string>
  <key>teamID</key>
  <string>${APPLE_TEAM_ID}</string>
  <key>destination</key>
  <string>export</string>
  <key>manageAppVersionAndBuildNumber</key>
  <false/>
</dict>
</plist>
EOF

    mkdir -p "$IOS_EXPORT"

    if xcodebuild \
      -exportArchive \
      -archivePath "$IOS_ARCHIVE" \
      -exportPath "$IOS_EXPORT" \
      -exportOptionsPlist "$EXPORT_OPTIONS" \
      -allowProvisioningUpdates
    then
      IPA="$(
        find "$IOS_EXPORT" \
          -maxdepth 1 \
          -type f \
          -name "*.ipa" \
          -print \
          -quit
      )"

      if [[ -n "$IPA" && -f "$IPA" ]]; then
        cp "$IPA" "$DIST/Mergen-ios-${IOS_EXPORT_METHOD}.ipa"
        ASSETS+=("$DIST/Mergen-ios-${IOS_EXPORT_METHOD}.ipa")
        IOS_STATUS="Simulator + signed ${IOS_EXPORT_METHOD} IPA included."
      else
        echo "WARNING: xcodebuild export succeeded but no IPA was found."
        IOS_STATUS="Simulator build included; IPA was not found after export."
      fi
    else
      echo "WARNING: Signed IPA export failed."
      echo "The Simulator build will still be published."
      IOS_STATUS="Simulator build included; signed IPA export failed."
    fi
  else
    echo
    echo "INFO: APPLE_TEAM_ID is not configured."
    echo "      iOS Simulator artifact was built, but no installable device IPA."
    echo
  fi
else
  echo "[2/5] iOS skipped on this machine."
fi

cat > "$DIST/Mergen-iOS-README.txt" <<EOF
Mergen iOS developer build
==========================

Commit: ${SHA}
Built:  ${BUILT_AT}

SIMULATOR
---------
Mergen-ios-simulator.zip is for the iOS Simulator on macOS only.

Example:
1. Start an iPhone Simulator.
2. Unzip Mergen-ios-simulator.zip.
3. Run:
   xcrun simctl install booted ./Filcapp.app
4. Launch the app from the Simulator.

DEVICE IPA
----------
A Mergen-ios-development.ipa or Mergen-ios-ad-hoc.ipa is only installable
on devices allowed by the Apple provisioning profile.

For development/ad-hoc testing:
- the device must be registered in the Apple Developer team/profile
- Developer Mode must be enabled on the iPhone/iPad
- install with Xcode Device Hub or Apple Configurator

An arbitrary IPA cannot be installed on every iPhone just because the file
is downloadable from GitHub.

EU WEB DISTRIBUTION
-------------------
Apple Web Distribution is a separate notarized distribution path. It requires
Apple Developer Program enrollment, Apple authorization, App Store Connect
configuration and a registered distribution website. This developer release
script does not pretend to replace that process.
EOF

ASSETS+=("$DIST/Mergen-iOS-README.txt")

cat > "$NOTES" <<EOF
## Mergen latest developer build

This is the moving **developer build** of Mergen.

- Branch: \`${BRANCH}\`
- Commit: \`${SHA}\`
- Built: ${BUILT_AT}
- Android version: \`${VERSION_NAME}\` (\`${BUILD_NUMBER}\`)
- iOS: ${IOS_STATUS}

### Android

Download **Mergen-android-dev.apk** and install it on the Android device.

### iOS Simulator

If **Mergen-ios-simulator.zip** is attached, unzip it on a Mac and install it
to a booted Simulator with:

\`\`\`bash
xcrun simctl install booted ./Filcapp.app
\`\`\`

### iPhone / iPad

If a signed **.ipa** is attached, it is a development/ad-hoc build and only
works on devices included in the Apple provisioning setup.

Enable **Developer Mode** on the device and install the IPA with Xcode Device
Hub or Apple Configurator.

See **Mergen-iOS-README.txt** in the release assets.
EOF

if [[ "$PUBLISH" == "1" ]]; then
  echo "[4/5] Publishing GitHub dev-latest release..."
  require_cmd gh

  if ! gh auth status >/dev/null 2>&1; then
    echo "ERROR: GitHub CLI is not authenticated."
    echo "Run: gh auth login"
    exit 1
  fi

  # dev-latest is intentionally a moving developer tag.
  if gh release view "$TAG" --repo "$REPO" >/dev/null 2>&1; then
    gh release delete "$TAG" --repo "$REPO" --yes
  fi

  # Delete an old moving tag if it still exists after release deletion.
  git push origin ":refs/tags/$TAG" >/dev/null 2>&1 || true
  git tag -d "$TAG" >/dev/null 2>&1 || true

  gh release create "$TAG" \
    "${ASSETS[@]}" \
    --repo "$REPO" \
    --target "$SHA" \
    --title "$TITLE" \
    --prerelease \
    --notes-file "$NOTES"

  echo "[5/5] Release published."
  echo
  echo "Release page:"
  echo "  https://github.com/${REPO}/releases/tag/${TAG}"
  echo
  echo "Stable Android download:"
  echo "  https://github.com/${REPO}/releases/download/${TAG}/Mergen-android-dev.apk"
else
  echo "[4/5] Publish skipped (--local / PUBLISH=0)."
  echo "[5/5] Local artifacts are ready."
fi

echo
echo "Artifacts:"
for asset in "${ASSETS[@]}"; do
  echo "  ${asset#$ROOT/}"
done

echo
echo "DONE."
