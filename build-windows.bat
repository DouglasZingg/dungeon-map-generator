@echo off
setlocal
title Dungeon Map Generator Builder

cd /d "%~dp0"

set "JDK_BIN=C:\Program Files\Java\jdk-21.0.11\bin"
set "JAVAC=%JDK_BIN%\javac.exe"
set "JAR=%JDK_BIN%\jar.exe"
set "JPACKAGE=%JDK_BIN%\jpackage.exe"
set "APP_NAME=Dungeon Map Generator"
set "JAR_NAME=DungeonGenerator.jar"
set "ICON_ICO=%~dp0icon.ico"
set "ICON_PNG=%~dp0assets\icon.png"

echo.
echo ==========================================
echo Building %APP_NAME%
echo ==========================================
echo.

if not exist "%JAVAC%" (
    echo Could not find javac at: "%JAVAC%"
    pause
    exit /b 1
)

if not exist "%JAR%" (
    echo Could not find jar at: "%JAR%"
    pause
    exit /b 1
)

if not exist "%JPACKAGE%" (
    echo Could not find jpackage at: "%JPACKAGE%"
    pause
    exit /b 1
)

if not exist "%ICON_ICO%" (
    echo Could not find icon.ico at: "%ICON_ICO%"
    pause
    exit /b 1
)

if not exist "%ICON_PNG%" (
    echo Could not find window icon at: "%ICON_PNG%"
    pause
    exit /b 1
)

echo Cleaning old build output...
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
mkdir build
mkdir dist

echo.
echo Compiling Java source...
pushd source
"%JAVAC%" -d ..\build *.java
if errorlevel 1 (
    popd
    echo.
    echo Compilation failed.
    pause
    exit /b 1
)
popd

echo.
echo Copying app resources...
copy "%ICON_PNG%" build\icon.png >nul

echo.
echo Creating runnable JAR...
pushd build
"%JAR%" cfe "%JAR_NAME%" Main *.class icon.png
if errorlevel 1 (
    popd
    echo.
    echo JAR creation failed.
    pause
    exit /b 1
)
popd

echo.
echo Creating executable app image...
"%JPACKAGE%" ^
  --name "%APP_NAME%" ^
  --input build ^
  --main-jar "%JAR_NAME%" ^
  --main-class Main ^
  --type app-image ^
  --dest dist ^
  --icon "%ICON_ICO%"

if errorlevel 1 (
    echo.
    echo Executable build failed.
    pause
    exit /b 1
)

echo.
echo ==========================================
echo Build complete!
echo ==========================================
echo.
echo EXE location:
echo dist\%APP_NAME%\%APP_NAME%.exe
echo.
echo If Windows still shows an old icon, rename the app folder or restart Explorer.
echo.
pause
