@echo off
cd /d %~dp0
if not exist build\lindefense call build.bat
java -cp build;lib\acm.jar lindefense.Main %*
