@echo off
REM 스킬 쿨타임 / 버프 OCR 모니터 실행 (OCR 서버는 run_ocr_server.bat 으로 먼저 띄워 주세요)
cd /d %~dp0..
call gradlew.bat run -PmainClass=OcrMonitorKt
pause
