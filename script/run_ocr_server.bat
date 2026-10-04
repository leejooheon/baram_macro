@echo off
chcp 65001 > nul
REM OCR 서버 실행 (최초 1회 venv 생성 및 의존성 설치)
cd /d %~dp0
set PYTHONUTF8=1

if not exist .venv (
    REM easyocr/torch 는 python 3.10~3.12 가 필요하다. py 런처로 3.12 를 먼저 찾는다
    py -3.12 -m venv .venv 2> nul || py -3.11 -m venv .venv 2> nul || py -3.10 -m venv .venv 2> nul || python -m venv .venv
    .venv\Scripts\python -m pip install --upgrade pip
    .venv\Scripts\python -m pip install torch torchvision --index-url https://download.pytorch.org/whl/cpu
    .venv\Scripts\python -m pip install -r requirements.txt
)

REM 예전에 만든 .venv 에는 몹 탐지용 ultralytics 가 없으니 한 번 깔아 준다
.venv\Scripts\python -c "import ultralytics" 2> nul || .venv\Scripts\python -m pip install ultralytics

REM CPU 스레드 수. 높이면 빨라지지만 게임과 CPU를 나눠 쓴다
if "%OCR_THREADS%"=="" set OCR_THREADS=2

.venv\Scripts\python script.py
pause
