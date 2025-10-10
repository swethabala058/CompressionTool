@echo off
REM Batch file to run the CompressionTool with both UI and Backend

REM Change to the script directory
cd /d "%~dp0"

REM Set Maven path
set MAVEN_HOME=%~dp0maven\apache-maven-3.8.8
set PATH=%MAVEN_HOME%\bin;%PATH%

echo Starting Compression Tool - Backend and UI...

REM Start Spring Boot backend in background
start "Spring Boot Backend" cmd /c "mvn spring-boot:run"

REM Wait a moment for backend to start
timeout /t 5 /nobreak > nul

REM Run the JavaFX UI
mvn clean javafx:run

pause
