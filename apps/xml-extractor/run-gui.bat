@echo off
setlocal

set "APP_JAR=%~dp0peoplesoft-xml-extractor.jar"
set "JAVA_COMMAND=javaw.exe"

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

where javaw.exe >nul 2>&1
if errorlevel 1 (
    if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" (
        set "JAVA_COMMAND=%JAVA_HOME%\bin\javaw.exe"
    ) else (
        echo Java 17 or later could not be found.
        echo.
        echo Install Java 17 or later, then run this file again.
        pause
        exit /b 1
    )
)

start "PeopleSoft XML Extractor" "%JAVA_COMMAND%" -jar "%APP_JAR%"
