@ECHO OFF
SETLOCAL

SET BASE_DIR=%~dp0
SET WRAPPER_DIR=%BASE_DIR%\.mvn\wrapper
SET PROPS_FILE=%WRAPPER_DIR%\maven-wrapper.properties
SET JAR_FILE=%WRAPPER_DIR%\maven-wrapper.jar

IF NOT EXIST "%JAR_FILE%" (
  FOR /F "usebackq tokens=1,* delims==" %%A IN ("%PROPS_FILE%") DO (
    IF "%%A"=="wrapperUrl" SET WRAPPER_URL=%%B
  )
  IF "%WRAPPER_URL%"=="" (
    ECHO Missing wrapperUrl in %PROPS_FILE%
    EXIT /B 1
  )
  IF NOT EXIST "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
  powershell -NoProfile -Command "Invoke-WebRequest -UseBasicParsing '%WRAPPER_URL%' -OutFile '%JAR_FILE%'" || EXIT /B 1
)

java %MAVEN_OPTS% -classpath "%JAR_FILE%" -Dmaven.multiModuleProjectDirectory="%BASE_DIR%" org.apache.maven.wrapper.MavenWrapperMain %*
