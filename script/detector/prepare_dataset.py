"""
라벨링한 캡처(raw 폴더의 png + 같은 이름의 YOLO txt)를 학습용/검증용으로 나눠
YOLO 학습 폴더 구조와 data.yaml 을 만든다.

라벨 파일이 없는 사진은 아직 라벨링 안 한 것으로 보고 건너뛴다.
몹이 하나도 없는 화면은 라벨링 도구에서 박스 없이 저장하면 빈 txt 가 생기고, 배경 사진으로 학습에 쓰인다.
"""
import os
import random
import shutil
import sys

ROOT = os.path.join(os.path.expanduser("~"), ".baram_macro", "dataset")
RAW = os.path.join(ROOT, "raw")
CLASSES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "classes.txt")
VAL_RATIO = 0.15
SEED = 42


def main():
    with open(CLASSES, encoding="utf-8") as f:
        names = [line.strip() for line in f if line.strip()]

    if not os.path.isdir(RAW):
        sys.exit(f"캡처 폴더가 없어요: {RAW}")

    pairs = []
    for file in sorted(os.listdir(RAW)):
        stem, ext = os.path.splitext(file)
        label = os.path.join(RAW, stem + ".txt")
        if ext.lower() == ".png" and os.path.exists(label):
            pairs.append((os.path.join(RAW, file), label))

    if len(pairs) < 10:
        sys.exit(f"라벨링된 사진이 {len(pairs)}장뿐이에요. 최소 10장, 가능하면 300장 이상 라벨링해 주세요.")

    random.Random(SEED).shuffle(pairs)
    val_count = max(1, int(len(pairs) * VAL_RATIO))
    splits = {"val": pairs[:val_count], "train": pairs[val_count:]}

    for split, items in splits.items():
        for kind in ("images", "labels"):
            folder = os.path.join(ROOT, kind, split)
            shutil.rmtree(folder, ignore_errors=True)
            os.makedirs(folder)
        for image, label in items:
            shutil.copy2(image, os.path.join(ROOT, "images", split))
            shutil.copy2(label, os.path.join(ROOT, "labels", split))

    with open(os.path.join(ROOT, "data.yaml"), "w", encoding="utf-8") as f:
        f.write(f"path: {ROOT.replace(os.sep, '/')}\n")
        f.write("train: images/train\n")
        f.write("val: images/val\n")
        f.write("names:\n")
        for i, name in enumerate(names):
            f.write(f"  {i}: {name}\n")

    empty = sum(1 for _, label in pairs if os.path.getsize(label) == 0)
    print(f"학습 {len(splits['train'])}장, 검증 {len(splits['val'])}장 (몹 없는 배경 사진 {empty}장)")


if __name__ == "__main__":
    main()
