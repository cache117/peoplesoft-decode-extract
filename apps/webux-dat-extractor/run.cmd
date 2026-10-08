@echo off
setlocal
pushd "%~dp0"
if not exist "src\WebUxDatExtractor\bin\Release\WebUxDatExtractor.exe" call build.cmd
if errorlevel 1 exit /b %errorlevel%
start "" "src\WebUxDatExtractor\bin\Release\WebUxDatExtractor.exe"
popd
