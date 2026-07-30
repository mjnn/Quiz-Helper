"""Extract JSON assets from app.js for native Kotlin layer."""
import json
import re
from pathlib import Path

ASSETS = Path(__file__).resolve().parents[1] / "AiTrainer/app/src/main/assets"
APP_JS = ASSETS / "app.js"
OUT = ASSETS


def extract_const(name: str, text: str) -> str:
    marker = f"const {name} = "
    start = text.index(marker) + len(marker)
    if text[start] == "[":
        end = text.index("];", start) + 1
    elif text[start] == "{":
        depth = 0
        i = start
        while i < len(text):
            if text[i] == "{":
                depth += 1
            elif text[i] == "}":
                depth -= 1
                if depth == 0:
                    end = i + 1
                    break
            i += 1
        else:
            raise ValueError(f"Unclosed object for {name}")
    else:
        raise ValueError(f"Unsupported const format: {name}")
    return text[start:end]


def main() -> None:
    if not APP_JS.is_file():
        print(f"Skip: {APP_JS.name} not found (native app uses questions.json directly).")
        return
    text = APP_JS.read_text(encoding="utf-8")
    questions = json.loads(extract_const("QUESTIONS", text))
    duplicate_map = json.loads(extract_const("DUPLICATE_ID_MAP", text))
    new_ids = json.loads(extract_const("NEW_QUESTION_IDS", text))

    (OUT / "questions.json").write_text(
        json.dumps(questions, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )
    (OUT / "duplicate_id_map.json").write_text(
        json.dumps(duplicate_map, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )
    (OUT / "new_question_ids.json").write_text(
        json.dumps(new_ids, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )
    print(f"Exported {len(questions)} questions -> {OUT}")


if __name__ == "__main__":
    main()
