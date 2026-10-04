"""
스킬 쿨타임(우상단 검은 박스)과 버프 목록(우측 양피지 패널)을 읽는 OCR 엔진.

EasyOCR의 글자 영역 검출(CRAFT)을 매번 돌리면 CPU를 많이 쓰고, 버프 패널처럼
줄 간격이 좁은 곳에서는 두 줄을 한 덩어리로 잡아 인식이 깨진다.
게임 글꼴은 배경과 대비가 뚜렷한 비트맵 글꼴이라 아래처럼 처리한다.

1. 배경 밝기로 글자 색을 판단해서 이진화하고, 작은 점과 테두리는 지운다.
2. 가로 투영으로 줄을 나누고, 줄 안에서 넓은 공백으로 "이름 / 숫자초"를 나눈다.
3. 이름은 한글 인식기(알려진 이름 글자로 제한), 숫자는 영문 인식기(숫자만)로 읽는다.
4. 같은 비트맵 조각(이름, 숫자 한 글자)은 결과를 캐시해서 한 번만 인식한다.
5. 영역 그림이 직전과 같으면 이전 결과를 그대로 돌려준다.
"""
import difflib
import hashlib
import os
import re
import threading
import time
from collections import OrderedDict

import cv2
import numpy as np

DEFAULT_NAMES = [
    "호체주술", "보호", "무장", "금강불체", "혼마술", "헬파이어", "공력증강",
    "저주", "마비", "절망", "중독", "삼매진화", "지폭지술", "백호의희원",
    "파력무참", "투명", "주술마도", "부활", "마기지체", "노도성황",
]

# 목록에 없는 이름도 읽히지만, 여기 있으면 더 정확하다. 한 줄에 하나씩 추가한다.
NAMES_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "names.txt")


def load_names():
    names = list(DEFAULT_NAMES)
    if os.path.exists(NAMES_FILE):
        with open(NAMES_FILE, encoding="utf-8") as f:
            for line in f:
                name = line.strip()
                if name and not name.startswith("#") and name not in names:
                    names.append(name)
    return names

DIGITS = "0123456789"
SECONDS_SUFFIX = "초"
MIN_CONFIDENCE = 0.15


class _Lru:
    def __init__(self, size):
        self.size = size
        self.data = OrderedDict()

    def get(self, key):
        if key in self.data:
            self.data.move_to_end(key)
            return self.data[key]
        return None

    def put(self, key, value):
        self.data[key] = value
        self.data.move_to_end(key)
        while len(self.data) > self.size:
            self.data.popitem(last=False)


def _runs(profile, min_len=1):
    """profile에서 0이 아닌 구간 [start, end) 목록."""
    out, start = [], None
    for i, v in enumerate(list(profile) + [0]):
        if v > 0 and start is None:
            start = i
        elif v <= 0 and start is not None:
            if i - start >= min_len:
                out.append([start, i])
            start = None
    return out


def binarize(gray):
    """글자 픽셀이 True인 마스크. 어두운 배경이면 밝은 글자, 밝은 배경이면 어두운 글자로 본다."""
    median = float(np.median(gray))
    if median < 100:
        # 게임 글자는 거의 순백(255)이고, 겹쳐 보이는 메뉴 글자는 180대라 높게 자른다
        mask = gray > max(210, median + 80)
    else:
        mask = gray < min(70, median - 50)

    count, labels, stats, _ = cv2.connectedComponentsWithStats(mask.astype(np.uint8), 8)
    height, width = mask.shape
    keep = np.zeros(count, dtype=bool)
    for i in range(1, count):
        x, y, w, h, area = stats[i]
        if area < 12:
            continue  # 양피지 얼룩, 점
        if h > height * 0.6 or w > width * 0.6:
            continue  # 패널 테두리
        if x == 0 or x + w >= width:
            continue  # 영역 좌우 끝에 걸린 무늬 조각 (글자는 영역 안쪽에 있다)
        keep[i] = True
    return keep[labels]


def split_lines(mask, min_height=14):
    """
    가로 투영으로 줄을 나눈다. 줄 높이의 중앙값을 기준으로
    - 많이 낮은 덩어리(패널 무늬 조각)는 버리고
    - 두 줄 이상이 무늬 조각 때문에 붙은 덩어리는 줄 높이 단위로 다시 나눈다.
    """
    runs = _runs(mask.sum(axis=1), min_height)
    if not runs:
        return []
    typical = float(np.median([y1 - y0 for y0, y1 in runs]))
    rows = []
    for y0, y1 in runs:
        height = y1 - y0
        if height < typical * 0.6:
            continue
        count = max(1, int(round(height / typical))) if height > typical * 1.5 else 1
        for k in range(count):
            rows.append((y0 + height * k // count, y0 + height * (k + 1) // count))

    lines = []
    for y0, y1 in rows:
        xs = np.where(mask[y0:y1].sum(axis=0) > 0)[0]
        if len(xs):
            lines.append((y0, y1, int(xs[0]), int(xs[-1]) + 1))
    return lines


def split_words(mask, y0, y1, x0, x1):
    """
    줄을 [이름, 숫자초] 두 조각으로 나눈다. 이름과 숫자 사이 공백이 줄에서 가장 넓다
    ('1'은 폭이 좁아서 숫자 사이 공백도 꽤 넓게 보이므로, 고정 기준보다 '가장 넓은 공백'이 안전하다).
    """
    glyphs = _runs(mask[y0:y1, x0:x1].sum(axis=0))
    if len(glyphs) < 2:
        return [(x0 + glyphs[0][0], x0 + glyphs[0][1])] if glyphs else []
    gaps = [glyphs[i + 1][0] - glyphs[i][1] for i in range(len(glyphs) - 1)]
    widest = max(range(len(gaps)), key=lambda i: gaps[i])
    if gaps[widest] < (y1 - y0) * 0.4:
        return [(x0 + glyphs[0][0], x0 + glyphs[-1][1])]
    return [
        (x0 + glyphs[0][0], x0 + glyphs[widest][1]),
        (x0 + glyphs[widest + 1][0], x0 + glyphs[-1][1]),
    ]


def parse_line(text):
    m = re.search(r"(\d+)\s*초", text)
    if not m:
        return text.strip(), None
    return text[:m.start()].strip(), int(m.group(1))


class TimerOcr:
    def __init__(self, names=None, gpu=None, threads=None):
        import torch
        import easyocr

        threads = threads or int(os.environ.get("OCR_THREADS", "2"))
        torch.set_num_threads(threads)
        cv2.setNumThreads(1)
        if gpu is None:
            gpu = torch.cuda.is_available()
        self.gpu = gpu
        self.names = list(names or load_names())
        self.name_allowlist = "".join(sorted(set("".join(self.names))))
        self.ko = easyocr.Reader(["ko", "en"], gpu=gpu, verbose=False)
        self.en = easyocr.Reader(["en"], gpu=gpu, verbose=False)
        self.lock = threading.Lock()
        self.piece_cache = _Lru(2048)
        self.frame_cache = _Lru(64)

    # ---------- 조각 인식 ----------

    def _crop(self, clean, y0, y1, x0, x1):
        h, w = clean.shape
        crop = clean[max(0, y0 - 4):min(h, y1 + 4), max(0, x0 - 4):min(w, x1 + 4)]
        return cv2.copyMakeBorder(crop, 10, 10, 10, 10, cv2.BORDER_CONSTANT, value=255)

    def _recognize(self, kind, crop):
        key = (kind, crop.shape, hashlib.blake2b(crop.tobytes(), digest_size=16).digest())
        hit = self.piece_cache.get(key)
        if hit is not None:
            return hit
        if kind == "digits":
            res = self.en.recognize(crop, allowlist=DIGITS)
        elif kind == "name":
            res = self.ko.recognize(crop, allowlist=self.name_allowlist)
        else:
            res = self.ko.recognize(crop)
        text = "".join(r[1] for r in res).replace(" ", "")
        conf = float(min((r[2] for r in res), default=0.0))
        value = (text, conf)
        self.piece_cache.put(key, value)
        return value

    def _read_name(self, clean, y0, y1, x0, x1):
        """
        알려진 이름 글자로 제한해서 먼저 읽고(작은 글꼴에서 더 정확), 확실하지 않으면 제한 없이 다시 읽는다.
        목록에 없는 이름(예: 새 마법)은 제한 없이 읽은 글자를 그대로 쓴다.
        """
        crop = self._crop(clean, y0, y1, x0, x1)
        text, conf = self._recognize("name", crop)
        if conf >= 0.5:
            if text in self.names:
                return text, text, conf
            best = difflib.get_close_matches(text, self.names, n=1, cutoff=0.75)
            if best:
                return best[0], text, conf

        free, free_conf = self._recognize("free", crop)
        best = difflib.get_close_matches(free, self.names, n=1, cutoff=0.75)
        if best:
            return best[0], free, free_conf
        return free, free, free_conf

    def _read_seconds(self, mask, clean, y0, y1, x0, x1):
        """'243초' 조각에서 마지막 글자(초)를 떼고, 숫자를 한 글자씩 읽는다.
        숫자 글자 모양은 몇 개 안 되므로 캐시가 금방 채워지고, 그 뒤로는 인식기를 거의 돌리지 않는다."""
        line = mask[y0:y1]
        runs = _runs(line[:, x0:x1].sum(axis=0))
        if len(runs) < 2:
            return None, 0.0
        glyphs = []
        for a, b in runs[:-1]:
            glyphs += _split_glyph(line, x0 + a, x0 + b, y1 - y0)
        digits, conf = "", 1.0
        for a, b in glyphs:
            crop = cv2.copyMakeBorder(clean[y0:y1, a:b], 10, 10, 10, 10, cv2.BORDER_CONSTANT, value=255)
            text, c = self._recognize("digits", crop)
            if not text:
                return None, 0.0
            if len(text) > 1:
                c *= 0.5
            digits += text[0]
            conf = min(conf, c)
        return int(digits), conf

    # ---------- 공개 API ----------

    def read(self, image):
        """
        image: BGR 또는 그레이 numpy 배열 (쿨타임 박스나 버프 패널을 잘라낸 것).
        반환: {"lines": [{"name", "raw", "seconds", "confidence", "box": [x, y, w, h]}], "elapsed_ms", "cached"}
        """
        started = time.perf_counter()
        gray = image if image.ndim == 2 else cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        mask = binarize(gray)
        frame_key = (mask.shape, hashlib.blake2b(np.packbits(mask).tobytes(), digest_size=16).digest())

        with self.lock:
            cached = self.frame_cache.get(frame_key)
            if cached is not None:
                return {"lines": cached, "elapsed_ms": _ms(started), "cached": True}

            clean = np.where(mask, 0, 255).astype(np.uint8)
            lines = []
            for y0, y1, x0, x1 in split_lines(mask):
                words = split_words(mask, y0, y1, x0, x1)
                seconds, sec_conf = (None, 1.0)
                name_words = words
                if len(words) >= 2:
                    seconds, sec_conf = self._read_seconds(mask, clean, y0, y1, *words[-1])
                    if seconds is not None:
                        name_words = words[:-1]
                name, raw, name_conf = self._read_name(clean, y0, y1, name_words[0][0], name_words[-1][1])
                confidence = min(name_conf, sec_conf)
                # 'N초'가 없는 줄, 한글이 없는 줄, 신뢰도가 너무 낮은 줄은 패널 무늬나 잘린 글자 같은 잡음이다
                if seconds is None or not _is_name(name) or confidence < MIN_CONFIDENCE:
                    continue
                text = f"{raw} {seconds}{SECONDS_SUFFIX}" if seconds is not None else raw
                lines.append({
                    "name": name,
                    "raw": text,
                    "seconds": seconds,
                    "confidence": round(confidence, 3),
                    "box": [x0, y0, x1 - x0, y1 - y0],
                })
            self.frame_cache.put(frame_key, lines)
        return {"lines": lines, "elapsed_ms": _ms(started), "cached": False}


def _is_name(text):
    """마법 이름은 한글 2글자 이상이고 다른 문자가 섞이지 않는다"""
    return len(text) >= 2 and all("가" <= ch <= "힣" for ch in text)


def _split_glyph(line_mask, start, end, height):
    """붙어 있는 숫자(예: '79')를 숫자 폭(줄 높이의 약 45%) 기준으로 나눈다."""
    width = end - start
    count = max(1, int(round(width / (height * 0.45))))
    if count == 1:
        return [(start, end)]
    cols = line_mask.sum(axis=0)
    out, left = [], start
    for k in range(1, count):
        center = start + int(width * k / count)
        lo, hi = max(left + 1, center - 3), min(end - 1, center + 4)
        cut = lo + int(np.argmin(cols[lo:hi]))
        out.append((left, cut))
        left = cut
    out.append((left, end))
    return out


def _ms(started):
    return round((time.perf_counter() - started) * 1000, 1)
