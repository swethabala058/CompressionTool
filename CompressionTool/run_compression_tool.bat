@echo off
REM Batch file to run the CompressionTool with both UI and Backend

REM Change to the script directory
cd /d "%~dp0"

REM Set Maven path
set MAVEN_HOME=%~dp0maven\apache-maven-3.8.8
set PATH=%MAVEN_HOME%\bin;%PATH%

echo Starting Compression Tool - Backend and UI...

REM Start Spring Boot backend in background
start "Spring Boot Backend" cmd /k "set MAVEN_HOME=%MAVEN_HOME% && set PATH=%PATH% && %MAVEN_HOME%\bin\mvn.cmd spring-boot:run"

REM Wait for backend to start (increased to 10 seconds for reliability)
timeout /t 10 /nobreak > nul

REM Run the JavaFX UI
%MAVEN_HOME%\bin\mvn.cmd clean javafx:run

pause
