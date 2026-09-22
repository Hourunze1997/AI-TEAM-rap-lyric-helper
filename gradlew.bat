@rem Self-contained Gradle wrapper for Windows
@echo off
setlocal

set APP_VERSION=8.2
set GRADLE_USER_HOME=%USERPROFILE%\.gradle
set GRADLE_DIST_DIR=%GRADLE_USER_HOME%\wrapper\dists
set GRADLE_DIST_PATH=%GRADLE_DIST_DIR%\gradle-%APP_VERSION%-bin\gradle-%APP_VERSION%

if not exist "%GRADLE_DIST_PATH%" (
    mkdir "%GRADLE_DIST_DIR%\gradle-%APP_VERSION%-bin" 2>nul
    echo Downloading Gradle %APP_VERSION%...
    powershell -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%APP_VERSION%-bin.zip' -OutFile '%GRADLE_DIST_DIR%\gradle-%APP_VERSION%-bin.zip'"
    echo Extracting...
    powershell -Command "Expand-Archive -Path '%GRADLE_DIST_DIR%\gradle-%APP_VERSION%-bin.zip' -DestinationPath '%GRADLE_DIST_DIR%\gradle-%APP_VERSION%-bin' -Force"
)

"%GRADLE_DIST_PATH%\bin\gradle.bat" %*
