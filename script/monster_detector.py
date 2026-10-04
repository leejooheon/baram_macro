"""
게임 화면에서 몹(monster)과 내 캐릭터(me)를 찾는 YOLO 탐지기.

모델은 script/detector/train.bat 으로 학습한 결과(~/.baram_macro/detector/train/weights/best.pt)를 쓴다.
모델 파일이 없거나 ultralytics 가 안 깔려 있으면 available=False 로 두고, 서버의 다른 기능은 그대로 돈다.
다시 학습해서 모델 파일이 바뀌면 다음 요청 때 새로 읽는다.
"""
import os
import threading
import time

DEFAULT_MODEL = os.path.join(os.path.expanduser("~"), ".baram_macro", "detector", "train", "weights", "best.pt")
IMAGE_SIZE = 960
MIN_CONFIDENCE = 0.4


class MonsterDetector:
    def __init__(self, model_path=None):
        self.model_path = model_path or os.environ.get("DETECTOR_MODEL", DEFAULT_MODEL)
        self.model = None
        self.loaded_mtime = None
        self.error = None
        self.lock = threading.Lock()

    def _ensure_model(self):
        if not os.path.exists(self.model_path):
            self.model, self.loaded_mtime = None, None
            self.error = "모델 파일이 없어요 (train.bat 으로 학습)"
            return None
        mtime = os.path.getmtime(self.model_path)
        if self.model is not None and mtime == self.loaded_mtime:
            return self.model
        try:
            from ultralytics import YOLO
        except ImportError:
            self.error = "ultralytics 가 안 깔려 있어요"
            return None
        self.model = YOLO(self.model_path)
        self.loaded_mtime = mtime
        self.error = None
        return self.model

    def status(self):
        with self.lock:
            model = self._ensure_model()
        return {"available": model is not None, "model": self.model_path, "error": self.error}

    def detect(self, image, conf=MIN_CONFIDENCE):
        """BGR 이미지에서 찾은 물체 목록. box 는 받은 이미지 픽셀 기준 [x, y, w, h]. 모델이 없으면 None."""
        started = time.perf_counter()
        with self.lock:
            model = self._ensure_model()
            if model is None:
                return None
            result = model.predict(image, imgsz=IMAGE_SIZE, conf=conf, verbose=False)[0]
        objects = []
        for xyxy, cls, score in zip(result.boxes.xyxy.tolist(), result.boxes.cls.tolist(), result.boxes.conf.tolist()):
            x1, y1, x2, y2 = (int(round(v)) for v in xyxy)
            objects.append({
                "label": result.names[int(cls)],
                "confidence": round(float(score), 3),
                "box": [x1, y1, x2 - x1, y2 - y1],
            })
        return {"objects": objects, "elapsed_ms": round((time.perf_counter() - started) * 1000, 1)}
