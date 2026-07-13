@REM Maven Wrapper script for Windows
@echo off
set MAVEN_PROJECTBASEDIR=%~dp0
set MAVEN_CMD_LINE_ARGS=%*
set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9
if exist "%MAVEN_HOME%\bin\mvn.cmd" (
    "%MAVEN_HOME%\bin\mvn.cmd" %*
) else (
    echo Maven not found. Please install Maven or download Maven Wrapper.
    echo Run: mvn wrapper:wrapper
    exit /b 1
)