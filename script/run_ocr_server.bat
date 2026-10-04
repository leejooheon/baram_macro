@echo off
REM OCR 서버 실행 (최초 1회 venv 생성 및 의존성 설치)
cd /d %~dp0

if not exist .venv (
    python -m venv .venv
    .venv\Scripts\python -m pip install --upgrade pip
    .venv\Scripts\python -m pip install torch torchvision --index-url https://download.pytorch.org/whl/cpu
    .venv\Scripts\python -m pip install -r requirements.txt
)

REM CPU 스레드 수. 높이면 빨라지지만 게임과 CPU를 나눠 쓴다
if "%OCR_THREADS%"=="" set OCR_THREADS=2

.venv\Scripts\python script.py
pause
