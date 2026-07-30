# -*- coding: utf-8 -*-
"""Walk through new OCR per-question preview + edit + import settings flow."""
from __future__ import annotations

import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ADB = Path(r"D:\Android\Sdk\platform-tools\adb.exe")
PACKAGE = "com.aitrainer.practice"
ACTIVITY = f"{PACKAGE}/.MainActivity"
DUMP_REMOTE = "/sdcard/window_dump.xml"
IMAGE_LOCAL = Path(__file__).resolve().parents[1] / "app/src/test/resources/ocr/single_page_mixed.png"
IMAGE_REMOTE = "/sdcard/Pictures/ocr_preview_flow.png"
DUMP_DIR = Path(__file__).resolve().parent / "dumps"


def adb(*args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [str(ADB), "-s", "emulator-5554", *args],
        capture_output=True,
        text=True,
        check=check,
        encoding="utf-8",
        errors="replace",
    )


def tap(x: int, y: int) -> None:
    adb("shell", "input", "tap", str(x), str(y))


def back() -> None:
    adb("shell", "input", "keyevent", "KEYCODE_BACK")


def swipe(x1: int, y1: int, x2: int, y2: int, duration_ms: int = 350) -> None:
    adb("shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(duration_ms))


def dump_ui(save_name: str | None = None) -> ET.Element:
    if save_name:
        DUMP_DIR.mkdir(parents=True, exist_ok=True)
    adb("shell", "uiautomator", "dump", DUMP_REMOTE)
    xml_text = adb("shell", "cat", DUMP_REMOTE).stdout
    if save_name:
        (DUMP_DIR / save_name).write_text(xml_text, encoding="utf-8")
    if not xml_text.strip().startswith("<?xml"):
        raise RuntimeError(f"Failed to dump UI: {xml_text[:200]}")
    return ET.fromstring(xml_text)


def iter_nodes(root: ET.Element):
    for node in root.iter("node"):
        yield node


def all_texts(root: ET.Element) -> list[str]:
    out: list[str] = []
    for node in iter_nodes(root):
        for key in ("text", "content-desc"):
            value = node.attrib.get(key, "").strip()
            if value:
                out.append(value)
    return out


def find_nodes(root: ET.Element, *, text: str | None = None, desc: str | None = None) -> list[ET.Element]:
    matches: list[ET.Element] = []
    for node in iter_nodes(root):
        node_text = node.attrib.get("text", "")
        node_desc = node.attrib.get("content-desc", "")
        if text is not None and text in node_text:
            matches.append(node)
        elif desc is not None and desc in node_desc:
            matches.append(node)
    return matches


def center(node: ET.Element) -> tuple[int, int]:
    bounds = node.attrib.get("bounds", "")
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    if not m:
        raise ValueError(f"Invalid bounds: {bounds}")
    x1, y1, x2, y2 = map(int, m.groups())
    return (x1 + x2) // 2, (y1 + y2) // 2


def tap_text(root: ET.Element, text: str) -> bool:
    for node in iter_nodes(root):
        if text in node.attrib.get("text", ""):
            x, y = center(node)
            tap(x, y)
            return True
    return False


def tap_desc(root: ET.Element, desc: str) -> bool:
    nodes = find_nodes(root, desc=desc)
    if not nodes:
        return False
    x, y = center(nodes[0])
    tap(x, y)
    return True


def wait_for(predicate, timeout: float = 120.0, interval: float = 1.0) -> ET.Element:
    deadline = time.time() + timeout
    while time.time() < deadline:
        root = dump_ui()
        if predicate(root):
            return root
        time.sleep(interval)
    texts = all_texts(dump_ui())
    raise TimeoutError(f"Timed out. Visible texts: {texts[:30]}")


def is_per_question_preview(root: ET.Element) -> bool:
    return bool(find_nodes(root, text="OCR 逐题预览"))


def is_import_settings(root: ET.Element) -> bool:
    return bool(find_nodes(root, text="OCR 导入设置"))


def is_edit_screen(root: ET.Element) -> bool:
    return bool(find_nodes(root, text="编辑题目"))


def open_settings_gallery_ocr() -> None:
    root = dump_ui()
    if not tap_desc(root, "设置"):
        raise RuntimeError("Settings FAB not found")
    time.sleep(1.2)
    root = dump_ui()
    if not tap_text(root, "相册识别导入"):
        swipe(540, 2000, 540, 1200, 400)
        time.sleep(0.8)
        root = dump_ui()
        if not tap_text(root, "相册识别导入"):
            raise RuntimeError("Gallery OCR button not found")
    time.sleep(1.5)


def pick_first_photo() -> None:
    root = dump_ui()
    for node in iter_nodes(root):
        desc = node.attrib.get("content-desc", "")
        if desc.startswith("Photo taken on") and node.attrib.get("clickable") == "true":
            x, y = center(node)
            tap(x, y)
            return
    tap(177, 817)


def question_counter(root: ET.Element) -> str | None:
    for node in iter_nodes(root):
        t = node.attrib.get("text", "")
        if re.search(r"第 \d+ / \d+ 题", t):
            return t
    return None


def main() -> int:
    print("[1/9] Push OCR test image")
    adb("push", str(IMAGE_LOCAL), IMAGE_REMOTE)
    adb(
        "shell", "am", "broadcast",
        "-a", "android.intent.action.MEDIA_SCANNER_SCAN_FILE",
        "-d", f"file://{IMAGE_REMOTE}",
    )

    print("[2/9] Launch app")
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-n", ACTIVITY)
    time.sleep(2.5)

    print("[3/9] Settings -> Gallery OCR -> pick photo")
    open_settings_gallery_ocr()
    pick_first_photo()

    print("[4/9] Wait per-question preview")
    root = wait_for(is_per_question_preview, timeout=120.0, interval=2.0)
    dump_ui("preview_01_per_question.xml")
    counter = question_counter(root)
    print(f"  preview opened: {counter}")
    print("  texts:", [t for t in all_texts(root) if "题" in t or "OCR" in t][:8])

    print("[5/9] Next / previous navigation")
    if not tap_text(root, "下一题"):
        raise RuntimeError("Next button not found")
    time.sleep(0.8)
    root = dump_ui("preview_02_after_next.xml")
    after_next = question_counter(root)
    print(f"  after next: {after_next}")
    if not tap_text(root, "上一题"):
        raise RuntimeError("Previous button not found")
    time.sleep(0.8)
    root = dump_ui()
    after_prev = question_counter(root)
    print(f"  after prev: {after_prev}")

    print("[6/9] Open full-screen edit and save")
    if not tap_desc(root, "编辑 OCR 题目") and not tap_text(root, "编辑"):
        raise RuntimeError("Edit button not found")
    root = wait_for(is_edit_screen, timeout=15.0, interval=0.8)
    dump_ui("preview_03_edit.xml")
    print("  edit screen opened")
    tap(540, 420)
    time.sleep(0.3)
    adb("shell", "input", "keyevent", "KEYCODE_MOVE_END")
    adb("shell", "input", "text", "OCR-PREVIEW-001")
    time.sleep(0.5)
    back()  # dismiss keyboard
    time.sleep(0.5)
    swipe(540, 1800, 540, 500, 450)
    time.sleep(1.0)
    root = dump_ui()
    if not tap_desc(root, "保存 OCR 编辑") and not tap_text(root, "保存"):
        raise RuntimeError("Save not found")
    root = wait_for(is_per_question_preview, timeout=10.0, interval=0.8)
    dump_ui("preview_04_after_edit.xml")
    print("  returned to preview after save")

    print("[7/9] Open import settings")
    if not tap_text(root, "导入设置"):
        raise RuntimeError("Import settings button not found")
    root = wait_for(is_import_settings, timeout=10.0, interval=0.8)
    dump_ui("preview_05_settings.xml")
    print("  settings opened:", bool(find_nodes(root, text="逐题预览")))

    print("[8/9] Re-open preview from settings")
    preview_btn = None
    for node in iter_nodes(root):
        t = node.attrib.get("text", "")
        if t.startswith("逐题预览"):
            preview_btn = t
            break
    if not preview_btn or not tap_text(root, preview_btn):
        raise RuntimeError("Per-question preview entry not found")
    root = wait_for(is_per_question_preview, timeout=10.0, interval=0.8)
    print("  back to preview:", question_counter(root))
    if not tap_text(root, "导入设置"):
        raise RuntimeError("Import settings button not found on return")
    root = wait_for(is_import_settings, timeout=10.0, interval=0.8)

    print("[9/9] Select policy and import")
    swipe(540, 2000, 540, 1400, 300)
    time.sleep(0.6)
    root = dump_ui()
    if not tap_text(root, "自动重命名"):
        raise RuntimeError("Duplicate policy chip not found")
    time.sleep(0.5)
    root = dump_ui()
    imported = False
    for node in iter_nodes(root):
        t = node.attrib.get("text", "")
        if re.fullmatch(r"导入 \d+ 题", t) or t == "导入":
            x, y = center(node)
            tap(x, y)
            imported = True
            print(f"  tapped: {t}")
            break
    if not imported:
        raise RuntimeError("Import button not found")
    time.sleep(2.5)
    root = wait_for(
        lambda r: bool(find_nodes(r, desc="设置")) and not is_import_settings(r) and not is_per_question_preview(r),
        timeout=20.0,
        interval=1.0,
    )
    dump_ui("preview_06_home.xml")
    print("  home restored:", bool(find_nodes(root, desc="设置")))
    print("OCR preview flow completed successfully.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"FAILED: {exc}", file=sys.stderr)
        raise
