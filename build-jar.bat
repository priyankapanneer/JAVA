@echo off
echo ===================================================
echo   ExamPortal - Build Deployable JAR
echo ===================================================

java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found. Please install JDK 11+.
    pause & exit /b 1
)

if not exist "out\classes" mkdir "out\classes"

echo [1/4] Compiling Java sources...
dir /s /b src\main\java\*.java > sources.txt
javac -cp "lib\sqlite-jdbc.jar" -d "out\classes" -source 11 -target 11 @sources.txt
if errorlevel 1 (
    echo [ERROR] Compilation failed!
    del sources.txt
    pause & exit /b 1
)
del sources.txt
echo     Done!

echo [2/4] Copying static resources...
xcopy /s /q /y "src\main\resources\static" "out\classes\static\" >nul

echo [3/4] Extracting SQLite JDBC into classes...
cd out\classes
jar xf ..\..\lib\sqlite-jdbc.jar
cd ..\..

echo [4/4] Creating fat JAR...
jar --create --file=examportal.jar --main-class=com.examportal.server.ExamServer -C out\classes .

echo.
echo ===================================================
echo   SUCCESS! examportal.jar is ready.
echo   Run with:  java -jar examportal.jar
echo   Then open: http://localhost:8080
echo ===================================================
pause
