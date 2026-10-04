import os
import cv2
import pyautogui
import numpy as np
from flask import Flask, request, jsonify

from timer_ocr import TimerOcr

app = Flask(__name__)
# CUDA가 있으면 GPU, 없으면 CPU(OCR_THREADS 개 스레드, 기본 2)로 돈다
timer_ocr = TimerOcr()
reader = timer_ocr.ko

full_screenshot = pyautogui.screenshot()
full_screen = cv2.cvtColor(np.array(full_screenshot), cv2.COLOR_RGB2GRAY)
screen_height, screen_width = full_screen.shape[:2]
target_width, target_height = 1920, 1080
scale_x = screen_width / target_width
scale_y = screen_height / target_height

def _decode_upload():
    file = request.files.get('file')
    if not file:
        return None
    data = np.frombuffer(file.read(), dtype=np.uint8)
    return cv2.imdecode(data, cv2.IMREAD_COLOR)


@app.route('/ocr/', methods=['POST'])
def read_image():
    image = _decode_upload()
    if image is not None:
        try:
            with timer_ocr.lock:
                result = reader.readtext(image, allowlist='0123456789')
            texts = [text[1] for text in result] or ["error"]
            return jsonify({"result": texts})
        except (IndexError, ValueError):
            print("error")

    return jsonify({"result": ["",""]})


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

@app.route('/conversation/king/', methods=['GET'])
def read_string():
    x, y, w, h = 800, 300, 900, 800
    screenshot = pyautogui.screenshot(region=(x,y,w,h))
    game_screen = cv2.cvtColor(np.array(screenshot), cv2.COLOR_RGB2GRAY)
    result = reader.readtext(game_screen)

    ocr_data = []
    for entry in result:
        bbox, text, confidence = entry
        x_min, y_min = map(int, bbox[0])  # 좌상단 좌표
        x_max, y_max = map(int, bbox[2])  # 우하단 좌표
        w, h = x_max - x_min, y_max - y_min  # 너비, 높이

        scaled_x = (x + x_min) / scale_x
        scaled_y = (y + y_min) / scale_y
        scaled_w = w / scale_x
        scaled_h = h / scale_y

        model = {
            "text": text,
            "confidence": confidence,
            "position": {"x": scaled_x, "y": scaled_y },
        }
        ocr_data.append(model)
    print(ocr_data)
    return jsonify({"result": ocr_data})

@app.route('/find/king/', methods=['GET'])
def find_king():
    screenshot = pyautogui.screenshot()
    game_screen = cv2.cvtColor(np.array(screenshot), cv2.COLOR_RGB2GRAY)
    npc_template = cv2.imread("king3.png", 0)

    if npc_template is None:
        print("템플릿 이미지를 불러올 수 없습니다.")
        return

    result = cv2.matchTemplate(game_screen, npc_template, cv2.TM_CCOEFF_NORMED)

    # 🔹 매칭된 영역 중 최고 점수 좌표 찾기
    min_val, max_val, min_loc, max_loc = cv2.minMaxLoc(result)

    x, y = max_loc
    w, h = npc_template.shape[::-1]

    scaled_x = x / scale_x
    scaled_y = y / scale_y
    scaled_w = w / scale_x
    scaled_h = h / scale_y

    result_json = {
        "text": "nothing",
        "confidence": max_val,
        "position": {
            "x": scaled_x - 20,
            "y": scaled_y + 20
        }
    }

    return jsonify({"result": result_json})

# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    # debug=True는 리로더가 프로세스를 하나 더 띄워 모델을 두 번 올리므로 끈다
    app.run(host='0.0.0.0', port=5001, debug=False, threaded=True)
    # find_king()
    # read_string()