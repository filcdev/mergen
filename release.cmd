@echo off
setlocal

where gh >nul 2>nul
if errorlevel 1 (
    echo GitHub CLI ^(gh^) is not installed.
    echo.
    echo Open:
    echo https://github.com/filcdev/mergen/actions/workflows/developer-release.yml
    echo.
    echo Then click: Run workflow
    pause
    exit /b 1
)

gh auth status >nul 2>nul
if errorlevel 1 (
    echo GitHub CLI is not authenticated.
    echo Run:
    echo   gh auth login
    pause
    exit /b 1
)

echo Starting GitHub-hosted release...
echo.
echo Android outputs:
echo   - Mergen-android-dev.apk
echo   - Mergen-android-play.aab
echo.

gh workflow run developer-release.yml ^
    --repo filcdev/mergen ^
    --ref main ^
    -f build_play_aab=true

if errorlevel 1 (
    echo.
    echo Could not start the workflow.
    pause
    exit /b 1
)

echo.
echo Started.
echo.
echo Build progress:
echo https://github.com/filcdev/mergen/actions/workflows/developer-release.yml
echo.
echo Release:
echo https://github.com/filcdev/mergen/releases/tag/dev-latest
echo.
echo Google Play file:
echo Mergen-android-play.aab
echo.

start "" "https://github.com/filcdev/mergen/actions/workflows/developer-release.yml"
pause
