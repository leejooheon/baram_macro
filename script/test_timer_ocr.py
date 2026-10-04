"""
샘플 캡처로 쿨타임/버프 인식을 확인한다.
    python test_timer_ocr.py
samples/*.png 는 2560x1600 화면에서 실제 픽셀로 잡은 캡처이고, buff_150.png 는 배율 150%로 줄인 캡처다.
"""
import os
import cv2

from timer_ocr import TimerOcr

HERE = os.path.dirname(os.path.abspath(__file__))
EXPECTED = {
    "cooldown.png": [("호체주술", 243)],
    # 쿨타임 여러 줄 (합성 이미지)
    "cooldown_multi.png": [("호체주술", 243), ("호체주술", 49), ("보호", 178), ("무장", 179)],
    "buff.png": [("호체주술", 49), ("보호", 178), ("무장", 179)],
    "buff_150.png": [("호체주술", 49), ("보호", 178), ("무장", 179)],
}

if __name__ == "__main__":
    ocr = TimerOcr()
    failed = 0
    for name, expected in EXPECTED.items():
        result = ocr.read(cv2.imread(os.path.join(HERE, "samples", name)))
        got = [(line["name"], line["seconds"]) for line in result["lines"]]
        ok = got == expected
        failed += not ok
        print(f"{'OK  ' if ok else 'FAIL'} {name} {result['elapsed_ms']}ms {got}")
    raise SystemExit(failed)
