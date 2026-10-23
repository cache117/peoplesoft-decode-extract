@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "APP_JAR=%~dp0peoplesoft-xml-extractor.jar"
set "MINIMUM_JAVA_VERSION=17"
set "JAVA_EXE="
set "JAVAW_EXE="
set "JAVA_VERSION="
set "FIRST_JAVA_EXE="
set "FIRST_JAVA_VERSION="

rem A source checkout can build the JAR on demand. Release packages do not
rem include build.bat, so only try this when the build script is present.
if not exist "%APP_JAR%" if exist "%~dp0build.bat" (
    call "%~dp0build.bat"
    if errorlevel 1 (
        echo.
        echo The PeopleSoft XML Extractor build failed.
        pause
        exit /b 1
    )
)

if not exist "%APP_JAR%" (
    echo The application file could not be found:
    echo   %APP_JAR%
    echo.
    echo Please extract the complete release ZIP into one folder and try again.
    pause
    exit /b 1
)

rem Prefer a future bundled runtime, then JAVA_HOME, then every Java on PATH.
if exist "%~dp0runtime\bin\java.exe" call :consider_java "%~dp0runtime\bin\java.exe"
if defined JAVA_EXE goto java_ready

if defined JAVA_HOME call :consider_java "%JAVA_HOME%\bin\java.exe"
if defined JAVA_EXE goto java_ready

for /f "delims=" %%J in ('where java.exe 2^>nul') do call :consider_java "%%J"
if defined JAVA_EXE goto java_ready

echo PeopleSoft XML Extractor requires Java %MINIMUM_JAVA_VERSION% or later.
echo.
if defined FIRST_JAVA_VERSION (
    echo The first configured Java checked by the launcher was Java !FIRST_JAVA_VERSION!:
    echo   !FIRST_JAVA_EXE!
) else (
    echo No Java installation could be found through JAVA_HOME or PATH.
)
echo.
echo Install Java %MINIMUM_JAVA_VERSION% or later, or update JAVA_HOME to point to it.
echo Java 8 can remain installed for applications that require it.
echo.
echo Download: https://adoptium.net/temurin/releases/?version=%MINIMUM_JAVA_VERSION%
if /i "%~1"=="--check-runtime" exit /b 1
pause
exit /b 1

:java_ready
if /i "%~1"=="--check-runtime" (
    echo Java !JAVA_VERSION! is compatible: !JAVA_EXE!
    exit /b 0
)

start "PeopleSoft XML Extractor" "!JAVAW_EXE!" -jar "%APP_JAR%"
exit /b 0

:consider_java
if defined JAVA_EXE exit /b 0
set "CANDIDATE_EXE=%~1"
if not exist "!CANDIDATE_EXE!" exit /b 0

set "CANDIDATE_VERSION="
rem Read the executable metadata rather than running an incompatible or broken
rem Java shim just to learn its version.
for /f "usebackq delims=" %%V in (`%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe -NoProfile -Command "(Get-Item -LiteralPath $env:CANDIDATE_EXE -ErrorAction Stop).VersionInfo.ProductVersion"`) do if not defined CANDIDATE_VERSION set "CANDIDATE_VERSION=%%V"
if not defined CANDIDATE_VERSION exit /b 0

if not defined FIRST_JAVA_EXE (
    set "FIRST_JAVA_EXE=!CANDIDATE_EXE!"
    set "FIRST_JAVA_VERSION=!CANDIDATE_VERSION!"
)

set "CANDIDATE_MAJOR="
for /f "tokens=1,2 delims=." %%A in ("!CANDIDATE_VERSION!") do (
    set "CANDIDATE_MAJOR=%%A"
    if "%%A"=="1" set "CANDIDATE_MAJOR=%%B"
)
echo(!CANDIDATE_MAJOR!| findstr /r "^[0-9][0-9]*$" >nul
if errorlevel 1 exit /b 0
if !CANDIDATE_MAJOR! LSS %MINIMUM_JAVA_VERSION% exit /b 0

for %%D in ("!CANDIDATE_EXE!") do set "CANDIDATE_DIR=%%~dpD"
if not exist "!CANDIDATE_DIR!javaw.exe" exit /b 0

set "JAVA_EXE=!CANDIDATE_EXE!"
set "JAVAW_EXE=!CANDIDATE_DIR!javaw.exe"
set "JAVA_VERSION=!CANDIDATE_VERSION!"
exit /b 0
