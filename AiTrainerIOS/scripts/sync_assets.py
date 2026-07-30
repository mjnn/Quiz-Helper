#!/usr/bin/env python3
"""Sync question bank assets from Android to iOS Resources/."""
from __future__ import annotations

import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID_ASSETS = ROOT.parent / "AiTrainer" / "app" / "src" / "main" / "assets"
IOS_RESOURCES = ROOT / "Resources"

FILES = [
    "questions.json",
    "new_question_ids.json",
    "duplicate_id_map.json",
]


def main() -> int:
    IOS_RESOURCES.mkdir(parents=True, exist_ok=True)
    for name in FILES:
        src = ANDROID_ASSETS / name
        dst = IOS_RESOURCES / name
        if not src.exists():
            raise SystemExit(f"Missing Android asset: {src}")
        shutil.copy2(src, dst)
        print(f"Copied {name} ({dst.stat().st_size} bytes)")
    print("Asset sync complete.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
