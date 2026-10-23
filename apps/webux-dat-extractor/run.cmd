@echo off
setlocal

set "RELEASE_EXE=%~dp0WebUxDatExtractor.exe"
set "SOURCE_EXE=%~dp0src\WebUxDatExtractor\bin\Release\WebUxDatExtractor.exe"
set "APP_EXE="

if exist "%RELEASE_EXE%" (
    set "APP_EXE=%RELEASE_EXE%"
    goto validate
)

rem A source checkout can build the executable on demand. Release packages do
rem not include build.cmd, so only try this when the build script is present.
if not exist "%SOURCE_EXE%" if exist "%~dp0build.cmd" (
    call "%~dp0build.cmd"
    if errorlevel 1 (
        echo.
        echo The PeopleSoft WebUX DAT Extractor build failed.
        pause
        exit /b 1
    )
)

if not exist "%SOURCE_EXE%" (
    echo The application file could not be found:
    echo   %RELEASE_EXE%
    echo.
    echo Please extract the complete release ZIP into one folder and try again.
    pause
    exit /b 1
)

set "APP_EXE=%SOURCE_EXE%"

:validate
for %%D in ("%APP_EXE%") do set "APP_DIR=%%~dpD"
set "MISSING_FILE="
if not exist "%APP_DIR%WebUxDatExtractor.Core.dll" set "MISSING_FILE=WebUxDatExtractor.Core.dll"
if not exist "%APP_DIR%DMSLib.Reader.dll" set "MISSING_FILE=DMSLib.Reader.dll"
if defined MISSING_FILE (
    echo The application package is incomplete. This required file is missing:
    echo   %APP_DIR%%MISSING_FILE%
    echo.
    echo Extract the complete release ZIP into one folder and try again.
    if /i "%~1"=="--check-runtime" exit /b 1
    pause
    exit /b 1
)

reg query "HKLM\SOFTWARE\Microsoft\NET Framework Setup\NDP\v4\Full" /v InstallPath >nul 2>&1
if errorlevel 1 reg query "HKLM\SOFTWARE\WOW6432Node\Microsoft\NET Framework Setup\NDP\v4\Full" /v InstallPath >nul 2>&1
if errorlevel 1 (
    echo PeopleSoft WebUX DAT Extractor requires Microsoft .NET Framework 4.x.
    echo.
    echo Install .NET Framework 4.8 or later, then run this file again.
    echo Download: https://dotnet.microsoft.com/download/dotnet-framework
    if /i "%~1"=="--check-runtime" exit /b 1
    pause
    exit /b 1
)

if /i "%~1"=="--check-runtime" (
    echo The application package and .NET Framework runtime are available.
    exit /b 0
)

start "PeopleSoft WebUX DAT Extractor" "%APP_EXE%"
exit /b 0
