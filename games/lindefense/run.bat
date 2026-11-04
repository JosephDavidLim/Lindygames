@echo off
cd /d %~dp0
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
if not exist build\lindefense call build.bat
java %JAVA_OPTS% -cp build;lib\acm.jar lindefense.Main %*
