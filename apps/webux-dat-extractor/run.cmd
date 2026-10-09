@echo off
setlocal

set "RELEASE_EXE=%~dp0WebUxDatExtractor.exe"
set "SOURCE_EXE=%~dp0src\WebUxDatExtractor\bin\Release\WebUxDatExtractor.exe"

if exist "%RELEASE_EXE%" (
    start "PeopleSoft WebUX DAT Extractor" "%RELEASE_EXE%"
    exit /b 0
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

start "PeopleSoft WebUX DAT Extractor" "%SOURCE_EXE%"
