@echo off
cd /d %~dp0
call build.bat
java -cp build;lib\acm.jar lindefense.test.HeadlessTests
