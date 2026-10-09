@echo off
chcp 65001 > nul
REM OCR 서버 실행 (최초 1회 venv 생성 및 의존성 설치)
cd /d %~dp0
set PYTHONUTF8=1

where uv > nul 2>&1 || (
    echo uv 가 필요합니다. 설치: winget install --id=astral-sh.uv -e
    pause
    exit /b 1
)

if not exist .venv (
    REM easyocr/torch 는 python 3.10~3.12 가 필요하다. 3.12 가 없으면 uv 가 받아 온다
    uv venv --python 3.12 .venv
    uv pip install --python .venv torch torchvision --index-url https://download.pytorch.org/whl/cpu
    uv pip install --python .venv -r requirements.txt
)

REM CPU 스레드 수. 높이면 빨라지지만 게임과 CPU를 나눠 쓴다
if "%OCR_THREADS%"=="" set OCR_THREADS=2

.venv\Scripts\python script.py
pause
