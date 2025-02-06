import os
import uuid
import easyocr
import cv2
import pyautogui
import numpy as np
from flask import Flask, request, jsonify
import matplotlib.pyplot as plt

app = Flask(__name__)
reader = easyocr.Reader(['ko', 'en'], gpu=True)

full_screenshot = pyautogui.screenshot()
full_screen = cv2.cvtColor(np.array(full_screenshot), cv2.COLOR_RGB2GRAY)
screen_height, screen_width = full_screen.shape[:2]
target_width, target_height = 1920, 1080
scale_x = screen_width / target_width
scale_y = screen_height / target_height

@app.route('/ocr/', methods=['POST'])
def read_image():
    file = request.files.get('file')
    if file:
        random_filename = f"{uuid.uuid4()}.png"
        try:
            file.save(random_filename)
            result = reader.readtext(random_filename, allowlist ='0123456789')
            print(result)
            texts = [text[1] for text in result] or ["error"]
            model = jsonify({"result": texts})
            return model
        except (IndexError, ValueError):
            print("error")
        finally:
            if os.path.exists(random_filename):
                os.remove(random_filename)

    return jsonify({"result": ["",""]})

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
    top_left = max_loc  # 가장 일치하는 좌표
    w, h = npc_template.shape[::-1]  # 템플릿 크기 가져오기
    bottom_right = (top_left[0] + w, top_left[1] + h)

    game_screen_bgr = cv2.cvtColor(game_screen, cv2.COLOR_GRAY2BGR)
    cv2.rectangle(game_screen_bgr, top_left, bottom_right, (255, 0, 0), 2)

    plt.figure(figsize=(10, 5))
    plt.imshow(game_screen_bgr, cmap="gray")
    plt.title("Best Match (Template Matching)")
    plt.axis("off")
    plt.show()

# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)
    # find_king()
    # read_string()