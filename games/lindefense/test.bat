@echo off
cd /d %~dp0
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
call build.bat
java -cp build;lib\acm.jar lindefense.test.HeadlessTests
