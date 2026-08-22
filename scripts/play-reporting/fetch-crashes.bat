@echo off
setlocal

if not defined GOOGLE_APPLICATION_CREDENTIALS (
    echo [ERROR] GOOGLE_APPLICATION_CREDENTIALS is not set.
    echo.
    echo Set it to the absolute path of the read-only service account JSON key.
    echo Keep the key outside this repository, then reopen this BAT file.
    echo.
    pause
    exit /b 1
)

if not exist "%GOOGLE_APPLICATION_CREDENTIALS%" (
    echo [ERROR] The file specified by GOOGLE_APPLICATION_CREDENTIALS was not found.
    echo.
    pause
    exit /b 1
)

where npm >nul 2>&1
if errorlevel 1 (
    echo [ERROR] npm was not found. Install Node.js first.
    echo.
    pause
    exit /b 1
)

set "SCRIPTS_DIR=%~dp0.."
if not exist "%SCRIPTS_DIR%\node_modules" (
    echo [ERROR] scripts\node_modules was not found.
    echo Run npm ci in the scripts directory first.
    echo.
    pause
    exit /b 1
)

set "HOURS=1440"
set /p "HOURS=Hours to fetch [%HOURS%]: "

set "VERSION_CODE="
set /p "VERSION_CODE=versionCode (leave blank for all versions): "

for /f %%I in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd-HHmmss"') do set "TIMESTAMP=%%I"
set "OUTPUT=play-reporting/output/reports-%TIMESTAMP%.json"
set "COMMAND_ARGS=--hours "%HOURS%" --output "%OUTPUT%""
if defined VERSION_CODE set "COMMAND_ARGS=%COMMAND_ARGS% --version-code "%VERSION_CODE%""

echo.
echo Fetching crash reports from Google Play.
echo Output: %OUTPUT%
echo.

pushd "%SCRIPTS_DIR%"
call npm run fetch-crashes -- %COMMAND_ARGS%
set "EXIT_CODE=%ERRORLEVEL%"
popd

echo.
if "%EXIT_CODE%"=="0" (
    echo Completed.
) else (
    echo [ERROR] Failed with exit code %EXIT_CODE%.
)
echo.
pause
exit /b %EXIT_CODE%
