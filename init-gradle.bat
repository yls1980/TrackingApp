@echo off
echo Initializing Gradle Wrapper...

REM Download gradle-wrapper.jar if not exists
if not exist gradle\wrapper\gradle-wrapper.jar (
    echo Downloading gradle-wrapper.jar...
    powershell -Command "& { Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle/wrapper/gradle-wrapper.jar' }"
)

echo Gradle Wrapper initialized!
echo.
echo Now run: gradlew.bat clean build
pause


