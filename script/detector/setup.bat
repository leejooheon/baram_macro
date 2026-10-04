@echo off
chcp 65001 > nul
REM 몹 탐지 학습 환경 (최초 1회). OCR 서버와 따로 .venv 를 만든다
cd /d %~dp0
set PYTHONUTF8=1

if not exist .venv (
    py -3.12 -m venv .venv 2> nul || py -3.11 -m venv .venv 2> nul || py -3.10 -m venv .venv 2> nul || python -m venv .venv
    .venv\Scripts\python -m pip install --upgrade pip
    .venv\Scripts\python -m pip install torch torchvision --index-url https://download.pytorch.org/whl/cpu
    .venv\Scripts\python -m pip install -r requirements.txt
)
