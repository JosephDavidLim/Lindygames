@echo off
REM Compiles all Java sources into .\build (requires JDK 11-25 and lib\acm.jar).
cd /d %~dp0
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
REM Check for a JDK 11-25 (acm.jar's GraphicsProgram extends JApplet, which JDK 26 removed).
where javac >nul 2>nul || (
  echo javac was not found. Install JDK 21 ^(e.g. "winget install EclipseAdoptium.Temurin.21.JDK"^), open a new terminal, and try again.
  exit /b 1
)
set "LDJV="
for /f "tokens=2" %%v in ('javac -version 2^>^&1') do if not defined LDJV set "LDJV=%%v"
for /f "tokens=1 delims=." %%m in ("%LDJV%") do set "LDJM=%%m"
if "%LDJM%"=="1" set "LDJM=8"
if %LDJM% LSS 11 (
  echo Found JDK %LDJV%, but Lindefense needs JDK 11-25. Install JDK 21 and set JAVA_HOME to it.
  exit /b 1
)
if %LDJM% GTR 25 (
  echo Found JDK %LDJV%, but JDK 26+ removed the Applet API that acm.jar needs. Install JDK 21 and set JAVA_HOME to it.
  exit /b 1
)
if not exist lib\acm.jar (
  echo lib\acm.jar is missing. See README.md ^(Setup^) for how to obtain it.
  exit /b 1
)
if exist build rmdir /s /q build
mkdir build
dir /s /b src\*.java > build\sources.txt
javac --release 11 -Xlint:all -Xlint:-serial -encoding UTF-8 -d build -cp lib\acm.jar @build\sources.txt
if errorlevel 1 exit /b 1
echo Build OK
