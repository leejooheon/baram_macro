@echo off
REM Gradle run script for release build

:: 사용자 이름 변수 설정
set USERNAME=%USERNAME%

:: 작업 디렉터리로 이동
cd C:\Users\%USERNAME%\IdeaProjects\baram_macro
echo Changing directory to %CD%

:: Gradle 빌드 실행
echo Starting Gradle build with release buildType...
.\gradlew run -PbuildType=release

:: 완료 메시지
echo Build finished.
pause