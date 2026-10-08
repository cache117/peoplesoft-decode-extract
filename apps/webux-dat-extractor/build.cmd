@echo off
setlocal
pushd "%~dp0"
set "MSBUILD=C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\MSBuild\Current\Bin\MSBuild.exe"
if not exist "%MSBUILD%" set "MSBUILD=MSBuild.exe"
"%MSBUILD%" WebUxDatExtractor.sln /t:Build /p:Configuration=Release /p:UseSharedCompilation=false /m:1
set "RESULT=%errorlevel%"
popd
exit /b %RESULT%
