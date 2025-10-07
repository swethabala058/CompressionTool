@echo off
REM Batch file to run the CompressionTool JavaFX application using Maven

REM Set Maven path
set PATH=%PATH%;maven\apache-maven-3.8.8\bin

REM Run the application using Maven
mvn clean javafx:run

pause