@echo off
chcp 65001 > nul
REM 학습한 모델로 사진(또는 폴더)을 돌려 박스를 그린 결과를 저장한다.
REM 사용법: predict.bat [사진이나 폴더, 기본은 검증용 사진들]
cd /d %~dp0
call setup.bat
set SOURCE=%1
if "%SOURCE%"=="" set SOURCE=%USERPROFILE%\.baram_macro\dataset\images\val
.venv\Scripts\yolo detect predict model="%USERPROFILE%\.baram_macro\detector\train\weights\best.pt" source="%SOURCE%" imgsz=960 conf=0.4 device=cpu project="%USERPROFILE%\.baram_macro\detector" name=predict exist_ok=True
explorer "%USERPROFILE%\.baram_macro\detector\predict"
pause
