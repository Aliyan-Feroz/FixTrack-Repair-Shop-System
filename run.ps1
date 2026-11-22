# FixTrack - Repair Shop Management System PowerShell Launcher
$Host.UI.RawUI.WindowTitle = "FixTrack - Repair Shop Management System"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  FixTrack - Repair Shop Management System Launcher" -ForegroundColor White
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

# 1. Validate Java
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
        $env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
    } elseif (Test-Path "C:\Program Files\Eclipse Adoptium\jdk-25.0.5.10002-hotspot\bin\java.exe") {
        $env:PATH = "C:\Program Files\Eclipse Adoptium\jdk-25.0.5.10002-hotspot\bin;" + $env:PATH
    } else {
        Write-Error "Java JDK 21+ or 25 was not found in PATH or JAVA_HOME."
        Write-Host "Please install OpenJDK 21 or 25 from https://adoptium.net" -ForegroundColor Yellow
        Read-Host "Press Enter to exit..."
        exit 1
    }
}

# 2. Validate Maven
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    if ($env:MAVEN_HOME -and (Test-Path "$env:MAVEN_HOME\bin\mvn.cmd")) {
        $env:PATH = "$env:MAVEN_HOME\bin;" + $env:PATH
    } elseif ($env:M2_HOME -and (Test-Path "$env:M2_HOME\bin\mvn.cmd")) {
        $env:PATH = "$env:M2_HOME\bin;" + $env:PATH
    } elseif (Test-Path "C:\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd") {
        $env:PATH = "C:\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin;" + $env:PATH
    } else {
        Write-Error "Apache Maven was not found in PATH or MAVEN_HOME."
        Write-Host "Please install Apache Maven from https://maven.apache.org" -ForegroundColor Yellow
        Read-Host "Press Enter to exit..."
        exit 1
    }
}

Write-Host "[*] Launching FixTrack Frosted Bento GUI Workbench..." -ForegroundColor Cyan
Write-Host ""

& mvn javafx:run

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[!] FixTrack exited with code $LASTEXITCODE" -ForegroundColor Red
    Read-Host "Press Enter to exit..."
}
