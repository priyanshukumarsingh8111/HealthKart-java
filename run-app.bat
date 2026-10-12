@echo off
setlocal
cd /d "%~dp0"

if exist "healthkart-java" (
    cd healthkart-java
)

echo ==================================================================
echo   HealthKart - Full Healthcare, Pharmacy ^& Teleconsultation System
echo ==================================================================
echo Web Interface:   http://localhost:3000
echo Login / Demo:    http://localhost:3000/pages/login.html
echo Database:        http://localhost:3000/pages/database.html
echo ==================================================================

set "JAVA_CMD=java"
if exist "C:\Program Files\Java\jdk-26.0.2.1\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-26.0.2.1\bin\java.exe"
) else if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    )
)

start "" http://localhost:3000

if exist "target\healthkart-server-1.0.0.jar" (
    "%JAVA_CMD%" -jar "target\healthkart-server-1.0.0.jar"
) else (
    echo Error: Could not locate healthkart-server-1.0.0.jar
)

pause

