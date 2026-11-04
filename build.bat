@echo off
REM Compiles all Java sources into .\build (requires JDK 11+ and lib\acm.jar).
cd /d %~dp0
if not exist lib\acm.jar (
  echo lib\acm.jar is missing. See README.md ^(Setup^) for how to obtain it.
  exit /b 1
)
if exist build rmdir /s /q build
mkdir build
dir /s /b src\*.java > build\sources.txt
javac -Xlint:all -Xlint:-serial -encoding UTF-8 -d build -cp lib\acm.jar @build\sources.txt
if errorlevel 1 exit /b 1
echo Build OK
