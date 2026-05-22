@echo off
echo ===================================================
echo   Online Examination System - Build and Run
echo ===================================================

REM Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found. Please install JDK 11 or higher.
    pause & exit /b 1
)

REM Create output dirs
if not exist "out\classes" mkdir "out\classes"

echo [1/3] Compiling Java sources...
dir /s /b src\main\java\*.java > sources.txt

javac -cp "lib\sqlite-jdbc.jar" -d "out\classes" -source 11 -target 11 @sources.txt

if errorlevel 1 (
    echo [ERROR] Compilation failed! Check the errors above.
    del sources.txt
    pause & exit /b 1
)
del sources.txt
echo     Compilation successful!

echo [2/3] Copying resources...
xcopy /s /q /y "src\main\resources\static" "out\classes\static\" >nul 2>&1

echo [3/3] Starting server...
echo.
echo   Open your browser at: http://localhost:8080
echo   Admin login: admin / admin123
echo.
java -cp "out\classes;lib\sqlite-jdbc.jar" com.examportal.server.ExamServer

pause
