@echo off
setlocal
cd /d "%~dp0"

echo ===================================================
echo Starting HealthKart Java GUI Desktop Application...
echo Database: MySQL 8.0 Primary (healthkart_db) + SQLite Fallback
echo ===================================================

set "JAVA_CMD=java"
if exist "C:\Program Files\Java\jdk-26.0.2.1\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-26.0.2.1\bin\java.exe"
) else if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    )
)

if exist "target\healthkart-server-1.0.0.jar" (
    "%JAVA_CMD%" "-Dloader.main=com.healthkart.gui.HealthKartGUI" -cp "target/healthkart-server-1.0.0.jar" org.springframework.boot.loader.launch.PropertiesLauncher
) else if exist "healthkart-java\target\healthkart-server-1.0.0.jar" (
    cd healthkart-java
    "%JAVA_CMD%" "-Dloader.main=com.healthkart.gui.HealthKartGUI" -cp "target/healthkart-server-1.0.0.jar" org.springframework.boot.loader.launch.PropertiesLauncher
) else (
    echo Error: Could not locate healthkart-server-1.0.0.jar
)

pause
