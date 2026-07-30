from pathlib import Path

from PIL import Image

SRC = Path(
    r"C:\Users\svw\.cursor\projects\d-cursor-project-ai-trainer-android-tool\assets"
    r"\c__Users_svw_AppData_Roaming_Cursor_User_workspaceStorage_empty-window_images_image-5c7a7931-"
    r"91b4-4f2c-9a75-da3953af6d1f.png"
)
RES = Path(r"D:\cursor_project\ai_trainer_android_tool\AiTrainer\app\src\main\res")
ASSETS = Path(r"D:\cursor_project\ai_trainer_android_tool\AiTrainer\app\src\main\assets")
ASSETS.mkdir(parents=True, exist_ok=True)

im = Image.open(SRC).convert("RGBA")
w, h = im.size
crop = im.crop((0, 0, w - 36, h - 36))
size = min(crop.size)
left = (crop.size[0] - size) // 2
top = (crop.size[1] - size) // 2
square = crop.crop((left, top, left + size, top + size))

master = RES / "drawable-nodpi" / "app_logo.png"
master.parent.mkdir(parents=True, exist_ok=True)
square.save(master, optimize=True)
square.save(ASSETS / "app_logo.png", optimize=True)

legacy = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
foreground = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}

for folder, px in legacy.items():
    out = RES / folder
    out.mkdir(parents=True, exist_ok=True)
    icon = square.resize((px, px), Image.Resampling.LANCZOS)
    icon.save(out / "ic_launcher.png", optimize=True)
    icon.save(out / "ic_launcher_round.png", optimize=True)

for folder, px in foreground.items():
    out = RES / folder
    out.mkdir(parents=True, exist_ok=True)
    inner = int(px * 0.84)
    fg = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    resized = square.resize((inner, inner), Image.Resampling.LANCZOS)
    offset = (px - inner) // 2
    fg.paste(resized, (offset, offset), resized)
    fg.save(out / "ic_launcher_foreground.png", optimize=True)

print("Done:", RES)
