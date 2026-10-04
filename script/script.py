import cv2
import numpy as np
from flask import Flask, request, jsonify

from timer_ocr import TimerOcr

app = Flask(__name__)
# CUDA가 있으면 GPU, 없으면 CPU(OCR_THREADS 개 스레드, 기본 2)로 돈다
timer_ocr = TimerOcr()

def _decode_upload():
    file = request.files.get('file')
    if not file:
        return None
    data = np.frombuffer(file.read(), dtype=np.uint8)
    return cv2.imdecode(data, cv2.IMREAD_COLOR)


@app.route('/ocr/timers/', methods=['POST'])
def read_timers():
    """쿨타임 박스 / 버프 패널 캡처 이미지를 받아 '이름 N초' 줄 목록을 돌려준다."""
    image = _decode_upload()
    if image is None:
        return jsonify({"error": "file is required"}), 400
    return jsonify(timer_ocr.read(image))


@app.route('/health/', methods=['GET'])
def health():
    import torch
    return jsonify({
        "gpu": timer_ocr.gpu,
        "threads": torch.get_num_threads(),
        "names": timer_ocr.names,
    })

# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    # debug=True는 리로더가 프로세스를 하나 더 띄워 모델을 두 번 올리므로 끈다
    app.run(host='0.0.0.0', port=5001, debug=False, threaded=True)
