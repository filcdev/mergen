@echo off
setlocal enabledelayedexpansion

echo.
echo === Mergen ktlint Windows fix ===
echo.

if not exist gradlew.bat (
    echo HIBA: A scriptet a Mergen repo gyokerebol futtasd.
    exit /b 1
)

if not exist composeApp (
    echo HIBA: Nem talalom a composeApp mappat.
    exit /b 1
)

if not exist .editorconfig (
    echo HIBA: Nem talalom a .editorconfig fajlt.
    exit /b 1
)

set KTLINT_VERSION=1.1.1
set TOOLS_DIR=.local-tools
set KTLINT_FILE=%TOOLS_DIR%\ktlint-%KTLINT_VERSION%

if not exist "%TOOLS_DIR%" mkdir "%TOOLS_DIR%"

if not exist "%KTLINT_FILE%" (
    echo Ktlint %KTLINT_VERSION% letoltese...
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "Invoke-WebRequest -Uri 'https://github.com/pinterest/ktlint/releases/download/1.1.1/ktlint' -OutFile '%KTLINT_FILE%'"
    if errorlevel 1 (
        echo HIBA: Nem sikerult letolteni a ktlint-et.
        exit /b 1
    )
)

echo.
echo Java verzio:
java -version
if errorlevel 1 (
    echo HIBA: A java parancs nem erheto el.
    exit /b 1
)

echo.
echo 1/3 Ktlint automatikus formatazas...
java -jar "%KTLINT_FILE%" --format "**/*.kt" "**/*.kts" "!composeApp/src/commonMain/kotlin/hu/petrik/filcapp/api/**"
if errorlevel 1 (
    echo.
    echo HIBA: A ktlint --format hibara futott.
    exit /b 1
)

echo.
echo 2/3 Ktlint ellenorzes...
java -jar "%KTLINT_FILE%" "**/*.kt" "**/*.kts" "!composeApp/src/commonMain/kotlin/hu/petrik/filcapp/api/**"
if errorlevel 1 (
    echo.
    echo Maradt lint hiba. Masold be ide a kimenetet.
    exit /b 1
)

echo.
echo 3/3 Android debug build...
call gradlew.bat :composeApp:assembleDebug
if errorlevel 1 (
    echo.
    echo A lint mar jo, de a build hibara futott.
    echo Masold be ide a build hibat.
    exit /b 1
)

echo.
echo ========================================
echo KESZ - ktlint OK es assembleDebug OK.
echo ========================================
echo.
echo Most ellenorizd:
echo   git status
echo   git diff
echo.
endlocal
