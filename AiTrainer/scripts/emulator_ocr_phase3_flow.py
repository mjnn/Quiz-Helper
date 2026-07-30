# -*- coding: utf-8 -*-
"""Automate OCR Phase 3 flow: batch gallery OCR, add images, export JSON, crop entry."""
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
IMAGE_REMOTE_A = "/sdcard/Pictures/ocr_phase3_a.png"
IMAGE_REMOTE_B = "/sdcard/Pictures/ocr_phase3_b.png"
IMAGE_REMOTE_C = "/sdcard/Pictures/ocr_phase3_c.png"
DUMP_DIR = Path(__file__).resolve().parent / "dumps"


def adb(*args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [str(ADB), *args],
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
    match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    if not match:
        raise ValueError(f"Invalid bounds: {bounds}")
    x1, y1, x2, y2 = map(int, match.groups())
    return (x1 + x2) // 2, (y1 + y2) // 2


def tap_text(root: ET.Element, text: str, *, exact: bool = False) -> bool:
    for node in iter_nodes(root):
        node_text = node.attrib.get("text", "")
        matched = node_text == text if exact else text in node_text
        if matched:
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


def wait_for(predicate, timeout: float = 90.0, interval: float = 1.0) -> ET.Element:
    deadline = time.time() + timeout
    while time.time() < deadline:
        root = dump_ui()
        if predicate(root):
            return root
        time.sleep(interval)
    raise TimeoutError("Timed out waiting for UI condition")


def processed_image_label(root: ET.Element) -> str | None:
    for node in iter_nodes(root):
        text = node.attrib.get("text", "")
        if re.fullmatch(r"已处理 \d+ 张图片", text):
            return text
    return None


def photo_picker_nodes(root: ET.Element) -> list[ET.Element]:
    photos: list[ET.Element] = []
    for node in iter_nodes(root):
        desc = node.attrib.get("content-desc", "")
        if desc.startswith("Photo taken on") and node.attrib.get("clickable") == "true":
            photos.append(node)
    return photos


def is_photo_picker(root: ET.Element) -> bool:
    packages = {node.attrib.get("package", "") for node in iter_nodes(root)}
    return "com.google.android.providers.media.module" in packages or bool(photo_picker_nodes(root))


def dismiss_external_overlays(root: ET.Element) -> bool:
    if tap_desc(root, "Cancel"):
        return True
    if is_photo_picker(root):
        back()
        return True
    packages = {node.attrib.get("package", "") for node in iter_nodes(root)}
    if packages and PACKAGE not in packages:
        back()
        return True
    return False


def ensure_app_home() -> ET.Element:
    for _ in range(4):
        root = dump_ui()
        if find_nodes(root, desc="设置"):
            break
        dismiss_external_overlays(root)
        time.sleep(0.8)
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-n", ACTIVITY)
    time.sleep(2.5)
    return wait_for(lambda r: bool(find_nodes(r, desc="设置")), timeout=20.0, interval=0.8)


def parse_bounds(node: ET.Element) -> tuple[int, int, int, int]:
    match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
    if not match:
        raise ValueError(f"Invalid bounds: {node.attrib.get('bounds')}")
    return tuple(map(int, match.groups()))


def tap_settings_ocr_button(root: ET.Element, keyword: str) -> bool:
    text_nodes = [
        node
        for node in iter_nodes(root)
        if keyword in node.attrib.get("text", "") and "OCR" in node.attrib.get("text", "")
    ]
    if not text_nodes:
        return False
    _, ty1, _, ty2 = parse_bounds(text_nodes[0])
    text_center_y = (ty1 + ty2) // 2
    best: ET.Element | None = None
    best_area = 10**18
    for node in iter_nodes(root):
        if node.attrib.get("clickable") != "true":
            continue
        x1, y1, x2, y2 = parse_bounds(node)
        if y1 <= text_center_y <= y2 and (x2 - x1) >= 500:
            area = (x2 - x1) * (y2 - y1)
            if area < best_area:
                best = node
                best_area = area
    target = best if best is not None else text_nodes[0]
    x, y = center(target)
    tap(x, y)
    return True


def open_settings() -> None:
    root = dump_ui()
    if not tap_desc(root, "设置"):
        raise RuntimeError("Settings FAB not found")
    time.sleep(1.2)


def open_settings_button(keyword: str, *, max_swipes: int = 3) -> None:
    open_settings()
    for attempt in range(max_swipes + 1):
        root = dump_ui()
        if tap_settings_ocr_button(root, keyword):
            time.sleep(2.5)
            return
        if attempt < max_swipes:
            swipe(540, 2100, 540, 900, 450)
            time.sleep(0.8)
    raise RuntimeError(f"Settings OCR button not found: {keyword}")


def pick_multiple_photos(count: int) -> None:
    root = wait_for(
        lambda r: is_photo_picker(r) and len(photo_picker_nodes(r)) >= count,
        timeout=45.0,
        interval=1.0,
    )
    photos = photo_picker_nodes(root)
    for photo in photos[:count]:
        x, y = center(photo)
        tap(x, y)
        time.sleep(0.7)
    root = dump_ui()
    add_label = f"Add ({count})"
    if not tap_text(root, add_label, exact=True):
        for node in iter_nodes(root):
            if node.attrib.get("text", "").startswith("Add ("):
                x, y = center(node)
                tap(x, y)
                break
        else:
            raise RuntimeError(f"Photo picker confirm button not found: {add_label}")
    time.sleep(1.0)


def pick_single_photo() -> None:
    root = wait_for(
        lambda r: is_photo_picker(r) and bool(photo_picker_nodes(r)),
        timeout=45.0,
        interval=1.0,
    )
    photos = photo_picker_nodes(root)
    x, y = center(photos[0])
    tap(x, y)
    time.sleep(2.0)


def push_test_images() -> None:
    for remote in (IMAGE_REMOTE_A, IMAGE_REMOTE_B, IMAGE_REMOTE_C):
        adb("push", str(IMAGE_LOCAL), remote)
        adb(
            "shell",
            "am",
            "broadcast",
            "-a",
            "android.intent.action.MEDIA_SCANNER_SCAN_FILE",
            "-d",
            f"file://{remote}",
        )


def is_crop_screen(root: ET.Element) -> bool:
    for node in iter_nodes(root):
        resource_id = node.attrib.get("resource-id", "")
        if "cropImageView" in resource_id or "CropOverlayView" in resource_id:
            return True
    packages = {node.attrib.get("package", "") for node in iter_nodes(root)}
    if "com.canhub.cropper" in packages:
        return True
    texts = all_texts(root)
    return any(
        marker in text
        for text in texts
        for marker in ("Crop image", "Crop", "Rotate", "裁剪")
    )


def is_export_chooser(root: ET.Element) -> bool:
    if find_nodes(root, text="导出 OCR JSON"):
        return True
    texts = all_texts(root)
    return any(marker in texts for marker in ("Nearby", "Drive", "Bluetooth", "分享"))


def main() -> int:
    print("[1/12] Push OCR test images")
    push_test_images()

    print("[2/12] Launch app")
    root = ensure_app_home()
    dump_ui("phase3_01_home.xml")
    print("  home hints:", [t for t in all_texts(root) if "题" in t][:5])

    print("[3/12] Settings -> Batch gallery OCR")
    open_settings_button("批量")
    pick_multiple_photos(2)

    print("[4/12] Wait batch OCR preview")
    root = wait_for(
        lambda r: processed_image_label(r) == "已处理 2 张图片"
        and (bool(find_nodes(r, text="OCR 逐题预览")) or bool(find_nodes(r, text="OCR 导入设置"))),
        timeout=120.0,
        interval=2.0,
    )
    dump_ui("phase3_02_batch_preview.xml")
    print(f"  batch preview: {processed_image_label(root)}")

    print("[5/12] Continue add one image")
    if not tap_desc(root, "继续添加 OCR 图片") and not tap_text(root, "继续添加图片"):
        swipe(540, 1200, 540, 1800, 300)
        time.sleep(0.6)
        root = dump_ui()
        if not tap_desc(root, "继续添加 OCR 图片") and not tap_text(root, "继续添加图片"):
            raise RuntimeError("Continue add images button not found")
    time.sleep(1.5)
    pick_multiple_photos(1)
    root = wait_for(
        lambda r: processed_image_label(r) == "已处理 3 张图片",
        timeout=120.0,
        interval=2.0,
    )
    dump_ui("phase3_03_after_add_image.xml")
    print(f"  merged preview: {processed_image_label(root)}")

    print("[6/12] Export JSON")
    if not tap_desc(root, "导出 OCR JSON") and not tap_text(root, "导出 JSON"):
        swipe(540, 1200, 540, 1800, 300)
        time.sleep(0.6)
        root = dump_ui()
        if not tap_desc(root, "导出 OCR JSON") and not tap_text(root, "导出 JSON"):
            raise RuntimeError("Export JSON button not found")
    root = wait_for(is_export_chooser, timeout=15.0, interval=0.8)
    dump_ui("phase3_04_export_chooser.xml")
    print("  export chooser markers:", [t for t in all_texts(root) if "导出" in t or "Drive" in t][:8])
    back()
    time.sleep(1.0)
    root = wait_for(
        lambda r: bool(find_nodes(r, text="OCR 逐题预览")) or bool(find_nodes(r, text="OCR 导入设置")),
        timeout=10.0,
        interval=0.8,
    )
    print("  returned to OCR flow after export")

    print("[7/12] Cancel OCR import")
    if not tap_text(root, "导入设置"):
        if not tap_text(root, "取消"):
            raise RuntimeError("Cancel/import settings not found")
    else:
        root = wait_for(lambda r: bool(find_nodes(r, text="OCR 导入设置")), timeout=10.0, interval=0.8)
        if not tap_text(root, "取消"):
            raise RuntimeError("Cancel button not found on import settings")
    time.sleep(1.5)
    root = wait_for(
        lambda r: bool(find_nodes(r, desc="设置"))
        and not find_nodes(r, text="OCR 逐题预览")
        and not find_nodes(r, text="OCR 导入设置"),
        timeout=15.0,
        interval=0.8,
    )
    dump_ui("phase3_05_home_after_cancel.xml")
    print("  home restored after cancel")

    print("[8/12] Settings -> Crop OCR entry")
    open_settings_button("裁剪")
    pick_single_photo()

    print("[9/12] Wait crop screen")
    root = wait_for(is_crop_screen, timeout=30.0, interval=1.0)
    dump_ui("phase3_06_crop_screen.xml")
    pkgs = sorted({node.attrib.get("package", "") for node in iter_nodes(root)})
    print("  crop packages:", pkgs)

    print("[10/12] Leave crop screen")
    for _ in range(4):
        root = dump_ui()
        if find_nodes(root, desc="设置"):
            break
        if is_crop_screen(root) or is_photo_picker(root):
            back()
            time.sleep(0.8)
            continue
        dismiss_external_overlays(root)
        time.sleep(0.8)

    print("[11/12] Verify app home")
    root = wait_for(
        lambda r: bool(find_nodes(r, desc="设置"))
        and not find_nodes(r, text="OCR 逐题预览")
        and not find_nodes(r, text="OCR 导入设置"),
        timeout=20.0,
        interval=0.8,
    )
    dump_ui("phase3_07_home_final.xml")
    print("  final home ok:", bool(find_nodes(root, desc="设置")))

    print("[12/12] Done")
    print("OCR Phase 3 emulator flow completed successfully.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"FAILED: {exc}", file=sys.stderr)
        raise
