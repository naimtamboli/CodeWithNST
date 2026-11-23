@echo off
setlocal
set "PROJECT_DIR=%~dp0.."
set "JAR=%PROJECT_DIR%\target\city-weather-1.0.0.jar"

if not exist "%JAR%" (
    echo Application jar not found: "%JAR%"
    echo Build it first with: mvn -DskipTests package
    set "RESULT=1"
    goto finish
)

java -jar "%JAR%" --send-email
set "RESULT=%ERRORLEVEL%"

:finish
if not "%RESULT%"=="0" echo The weather email run failed with exit code %RESULT%.
echo.
pause
exit /b %RESULT%
