@echo off
chcp 65001 > nul
REM 앱이 모은 캡처(%USERPROFILE%\.baram_macro\dataset\raw)를 labelImg 로 연다.
REM 왼쪽 메뉴에서 저장 형식을 YOLO 로 바꾸고, W 로 박스를 그린 뒤 Ctrl+S, D 로 다음 장.
cd /d %~dp0
call setup.bat
set RAW=%USERPROFILE%\.baram_macro\dataset\raw
.venv\Scripts\labelImg "%RAW%" "%~dp0classes.txt" "%RAW%"
