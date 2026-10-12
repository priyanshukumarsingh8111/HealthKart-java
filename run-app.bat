@echo off
setlocal
cd /d "%~dp0"

echo =====================================================
echo Starting HealthKart (HTML & CSS Frontend + JDBC)...
echo Web Interface: http://localhost:3000
echo Diagnostics:   http://localhost:3000/pages/database.html
echo Database:      MySQL 8.0 Primary (healthkart_db) + SQLite Fallback
echo =====================================================

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
) else if exist "healthkart-java\target\healthkart-server-1.0.0.jar" (
    cd healthkart-java
    "%JAVA_CMD%" -jar "target\healthkart-server-1.0.0.jar"
) else (
    echo Error: Could not locate healthkart-server-1.0.0.jar
)

pause
