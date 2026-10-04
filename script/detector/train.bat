@echo off
chcp 65001 > nul
REM 라벨링한 캡처로 몹 탐지 모델을 학습한다. 결과: %USERPROFILE%\.baram_macro\detector\train\weights\best.pt
REM 사용법: train.bat [에폭 수, 기본 100]
cd /d %~dp0
call setup.bat
set PYTHONUTF8=1
set EPOCHS=%1
if "%EPOCHS%"=="" set EPOCHS=100

.venv\Scripts\python prepare_dataset.py || goto :end
.venv\Scripts\yolo detect train data="%USERPROFILE%\.baram_macro\dataset\data.yaml" model=yolov8n.pt epochs=%EPOCHS% imgsz=960 batch=8 workers=2 project="%USERPROFILE%\.baram_macro\detector" name=train exist_ok=True
:end
pause
