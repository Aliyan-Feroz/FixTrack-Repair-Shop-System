@echo off
setlocal enabledelayedexpansion
title FixTrack - Repair Shop Management System

echo ============================================================
echo   FixTrack - Repair Shop Management System
echo ============================================================
echo.

cd /d "%~dp0"

:: 1. Validate Java
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
        set "PATH=%JAVA_HOME%\bin;%PATH%"
    ) else if exist "C:\Program Files\Eclipse Adoptium\jdk-25.0.5.10002-hotspot\bin\java.exe" (
        set "PATH=C:\Program Files\Eclipse Adoptium\jdk-25.0.5.10002-hotspot\bin;%PATH%"
    ) else (
        echo [ERROR] Java JDK 21+ or 25 was not found on your system.
        echo Please install OpenJDK 21 or 25 and add it to your PATH:
        echo   https://adoptium.net/temurin/releases/
        echo.
        pause
        exit /b 1
    )
)

:: 2. Validate Maven
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" (
        set "PATH=%MAVEN_HOME%\bin;%PATH%"
    ) else if defined M2_HOME if exist "%M2_HOME%\bin\mvn.cmd" (
        set "PATH=%M2_HOME%\bin;%PATH%"
    ) else if exist "C:\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd" (
        set "PATH=C:\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin;%PATH%"
    ) else (
        echo [ERROR] Apache Maven was not found on your system.
        echo Please install Apache Maven and add it to your PATH:
        echo   https://maven.apache.org/download.cgi
        echo.
        pause
        exit /b 1
    )
)

echo [*] Launching FixTrack Frosted Bento GUI Workbench...
echo.

call mvn javafx:run

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ============================================================
    echo [!] FixTrack closed with exit code: %ERRORLEVEL%
    echo ============================================================
    pause
)
