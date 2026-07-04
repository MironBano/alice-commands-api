#!/usr/bin/env python3
"""Optional AI drafts for open queue items (OPENAI_API_KEY in env)."""
from __future__ import annotations

import json
import os
import sys
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from editorial_store import load_editorial, save_editorial
from inventory_sync import load_queue, save_queue
from pipeline_models import EditorialStatus, QueueEventType, QueueStatus, utc_now
from raw_result_quality import is_weak_raw_result, suggest_title_from_phrase


def _openai_draft(phrase: str, raw_result: str | None, category_id: str) -> tuple[str, str]:
    api_key = os.environ.get("OPENAI_API_KEY", "").strip()
    if not api_key:
        raise RuntimeError("OPENAI_API_KEY not set in environment")

    prompt = (
        f"Категория: {category_id}\n"
        f"Фраза пользователя колонке: {phrase}\n"
        f"Сырой результат с support: {raw_result or '(нет)'}\n\n"
        "Верни JSON: {\"title_ru\": \"до 5 слов\", \"effect_description_ru\": \"одно предложение, что сделает Алиса\"}"
    )
    body = json.dumps(
        {
            "model": os.environ.get("OPENAI_MODEL", "gpt-4o-mini"),
            "messages": [
                {"role": "system", "content": "Ты редактор справочника голосовых команд Яндекс Алисы. Только JSON."},
                {"role": "user", "content": prompt},
            ],
            "temperature": 0.3,
        }
    ).encode("utf-8")
    req = urllib.request.Request(
        "https://api.openai.com/v1/chat/completions",
        data=body,
        headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    content = data["choices"][0]["message"]["content"]
    start = content.find("{")
    end = content.rfind("}") + 1
    parsed = json.loads(content[start:end])
    return parsed["title_ru"], parsed["effect_description_ru"]


def main() -> int:
    queue = load_queue()
    editorial = load_editorial()
    updated = 0
    for item in queue.items:
        if item.status != QueueStatus.OPEN.value:
            continue
        if item.event_type not in {QueueEventType.NEW_COMMAND.value, QueueEventType.NEEDS_REVIEW.value}:
            continue
        phrase = item.phrase or ""
        if not is_weak_raw_result(item.suggested_effect or item.raw_result, phrase):
            continue
        try:
            title, effect = _openai_draft(phrase, item.raw_result, item.category_id or "general")
        except Exception as exc:  # noqa: BLE001
            print(f"Skip {item.command_id}: {exc}", file=sys.stderr)
            continue
        item.title_ru = title
        item.suggested_effect = effect
        record = editorial.records.get(item.command_id)
        if record is None:
            from editorial_store import ensure_editorial_for_inventory

            ensure_editorial_for_inventory(
                editorial,
                command_id=item.command_id,
                category_id=item.category_id or "general",
                phrase=phrase,
                raw_result=item.raw_result,
                parsed_title=title,
                parsed_effect=effect,
            )
            record = editorial.records[item.command_id]
        record.title_ru = title
        record.effect_description_ru = effect
        record.status = EditorialStatus.AI_DRAFT.value
        record.updated_at = utc_now()
        updated += 1
        print(f"AI draft: {item.command_id} -> {title}")

    save_queue(queue)
    save_editorial(editorial)
    print(f"Updated {updated} queue item(s). Review in admin or approve manually.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
