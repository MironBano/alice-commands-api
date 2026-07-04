#!/usr/bin/env python3
"""One-shot: restore staging draft from published v20 + user patches. Not part of runtime."""
from __future__ import annotations

import gzip
import json
import sys
import urllib.request
from pathlib import Path

BASE = "https://staging-api.alicecommands.ru"
STAGING_ICONS = "https://staging-api.alicecommands.ru/icons/v1"

CATEGORY_VISUALS: dict[str, dict[str, str]] = {
    "general": {"icon_url": f"{STAGING_ICONS}/star.svg"},
    "audiobooks": {"icon_url": f"{STAGING_ICONS}/book.svg"},
    "station_settings": {"icon_url": f"{STAGING_ICONS}/speaker.svg"},
    "calls": {"icon_url": f"{STAGING_ICONS}/calls.svg"},
    "alice_plus": {
        "icon_url": f"{STAGING_ICONS}/plus.svg",
        "accent_color": "#C36AEC",
        "accent_color_dark": "#D499F0",
    },
    "kids": {
        "icon_url": f"{STAGING_ICONS}/kids.svg",
        "accent_color": "#DB2777",
        "accent_color_dark": "#F9A8D4",
    },
    "quick_commands": {"icon_url": f"{STAGING_ICONS}/bolt.svg"},
    "obscure": {"icon_url": f"{STAGING_ICONS}/sparkles.svg"},
}

COMMAND_PATCHES: dict[str, dict[str, str]] = {
    "calls_miau": {
        "title_ru": "Мяу!",
        "effect_description_ru": (
            "Алиса ответит «мяу». Нужно: колонка с поддержкой звонков; "
            "настройка в приложении «Дом с Алисой»."
        ),
    },
    "obscure_stroboskop": {
        "effect_description_ru": (
            "Включит эффект stroboscope на подсветке колонки. Нужно: устройство с Алисой."
        ),
    },
    "smart_home_vykliuchi_verkhnii_svet_v_detskoi_cherez": {
        "title_ru": "Выключи свет в детской через 15 минут",
        "effect_description_ru": (
            "Поставит таймер выключения верхнего света в детской через 15 минут. "
            "Нужно: устройство с Алисой; умный свет в «Дом с Алисой»."
        ),
    },
    "smart_home_vykliuchi_videosistemu": {
        "effect_description_ru": (
            "Выключит видеосистему. Нужно: устройство с Алисой; "
            "видеосистема в «Дом с Алисой»."
        ),
    },
    "audiobooks_bitlz": {
        "effect_description_ru": (
            "Включит музыку The Beatles или подберёт похожее. "
            "Нужно: устройство с Алисой; интернет."
        ),
    },
    "obscure_komfort": {
        "effect_description_ru": (
            "Включит режим «Комфорт» на колонке с подсветкой. Нужно: устройство с Алисой."
        ),
    },
    "tv_video_alisa": {
        "effect_description_ru": (
            "Активирует Алису на телевизоре для голосовых команд. "
            "Нужно: ТВ с Алисой или приставка с голосовым управлением."
        ),
    },
}


def main() -> int:
    bundle_gz = Path(sys.argv[1])
    out_json = Path(sys.argv[2])
    data = json.load(gzip.open(bundle_gz, "rt", encoding="utf-8"))

    for cat in data["categories"]:
        patch = CATEGORY_VISUALS.get(cat["id"])
        if patch:
            cat.update(patch)

    by_id = {c["id"]: c for c in data["commands"]}
    for cid, patch in COMMAND_PATCHES.items():
        if cid in by_id:
            by_id[cid].update(patch)

    out_json.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    print(f"patched categories={len(CATEGORY_VISUALS)} commands={len(COMMAND_PATCHES)} -> {out_json}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
