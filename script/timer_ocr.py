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
    "파력무참", "투명", "주술마도", "부활",
]

DIGITS = "0123456789"
SECONDS_SUFFIX = "초"
# 이름이 정확히 읽히지 않은 줄은 신뢰도 0.5 이상만 남긴다. 잡음은 0.3 아래, 실제 글자는 0.2 이상으로 나왔다
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
        mask = gray > max(150, median + 80)
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
        keep[i] = True
    return keep[labels]


def split_lines(mask, min_height=8):
    lines = []
    for y0, y1 in _runs(mask.sum(axis=1), min_height):
        xs = np.where(mask[y0:y1].sum(axis=0) > 0)[0]
        lines.append((y0, y1, int(xs[0]), int(xs[-1]) + 1))
    return lines


def split_words(mask, y0, y1, x0, x1):
    """줄 높이의 35%보다 넓은 공백을 단어 경계로 본다."""
    glyphs = _runs(mask[y0:y1, x0:x1].sum(axis=0))
    gap = (y1 - y0) * 0.35
    words = [glyphs[0]]
    for g in glyphs[1:]:
        if g[0] - words[-1][1] < gap:
            words[-1][1] = g[1]
        else:
            words.append(g)
    return [(x0 + a, x0 + b) for a, b in words]


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
        self.names = list(names or DEFAULT_NAMES)
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
        crop = self._crop(clean, y0, y1, x0, x1)
        text, conf = self._recognize("name", crop)
        best = difflib.get_close_matches(text, self.names, n=1, cutoff=0.5)
        if best:
            return best[0], text, conf
        # 알려진 이름이 아니면 글자 제한 없이 다시 읽는다
        free, free_conf = self._recognize("free", crop)
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
                exact = raw in self.names
                if confidence < MIN_CONFIDENCE or (not exact and confidence < 0.5):
                    continue  # 빈 패널 무늬, 잘린 글자, 게임 배경 같은 잡음
                if seconds is None and not _looks_like_name(name, name_conf, self.names):
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


def _looks_like_name(name, confidence, names):
    """초를 못 읽은 줄은 알려진 이름이거나, 한글이 2자 이상이고 신뢰도가 높을 때만 남긴다."""
    if name in names:
        return True
    hangul = sum(1 for ch in name if "가" <= ch <= "힣")
    return hangul >= 2 and confidence >= 0.5


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
