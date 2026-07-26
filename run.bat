@echo off
REM DiskInsight - compile and run
cd /d "%~dp0"

REM put mysql-connector-j-*.jar in lib\ to enable the database (optional)
set CP=out
if exist lib\*.jar (
  for %%j in (lib\*.jar) do call set CP=%%CP%%;%%j
)

echo Compiling...
if not exist out mkdir out
javac -d out src\diskinsight\*.java
if errorlevel 1 goto :error

echo Starting DiskInsight...
java -cp "%CP%" diskinsight.DiskInsightApp
goto :eof

:error
echo.
echo Compilation failed. Check that the JDK is installed and javac is on your PATH.
pause
