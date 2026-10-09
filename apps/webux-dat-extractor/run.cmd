@echo off
setlocal
pushd "%~dp0"
if exist "WebUxDatExtractor.exe" (
    start "" "WebUxDatExtractor.exe"
    popd
    exit /b 0
)
if not exist "src\WebUxDatExtractor\bin\Release\WebUxDatExtractor.exe" call build.cmd
if errorlevel 1 (
    set "RESULT=%errorlevel%"
    popd
    exit /b %RESULT%
)
start "" "src\WebUxDatExtractor\bin\Release\WebUxDatExtractor.exe"
popd
