@echo off
if not exist "%~dp0peoplesoft-xml-extractor.jar" call "%~dp0build.bat"
start "PeopleSoft XML Extractor" javaw -jar "%~dp0peoplesoft-xml-extractor.jar"
