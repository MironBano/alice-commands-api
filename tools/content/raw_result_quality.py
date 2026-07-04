"""Decide whether support raw_result is good enough for auto-suggest."""
from __future__ import annotations

import re

from effect_templates import NEEDS_REVIEW

NAV_MARKERS = (
    "умные колонки от",
    "яндекс станция",
    "до покупки",
    "справочник команд",
    "развлечения послушать",
)

WEAK_PREFIXES = (
    "алиса выполнит",
    "алиса выполнит команду",
    "выполнит команду",
)


def is_nav_garbage(text: str) -> bool:
    lower = text.lower()
    return any(marker in lower for marker in NAV_MARKERS)


def is_weak_raw_result(raw: str | None, phrase: str = "") -> bool:
    if not raw or raw.strip() == NEEDS_REVIEW:
        return True
    text = raw.strip()
    if len(text) < 8:
        return True
    lower = text.lower()
    if is_nav_garbage(text):
        return True
    if any(lower.startswith(prefix) for prefix in WEAK_PREFIXES):
        return True
    if phrase and text.lower().strip(" .") == phrase.lower().strip(" ."):
        return True
    if re.fullmatch(r"алиса[,\s]+.+", lower) and len(text) < 30:
        return True
    return False


def suggest_title_from_phrase(phrase: str) -> str:
    body = re.sub(r"^алиса[,\s]+", "", phrase.strip(), flags=re.I).strip()
    if not body:
        return phrase.strip()[:60]
    body = body[0].upper() + body[1:]
    if len(body) > 60:
        body = body[:57] + "…"
    return body


def suggest_effect(raw_result: str | None, phrase: str, *, fallback: str | None = None) -> str:
    if raw_result and not is_weak_raw_result(raw_result, phrase):
        text = raw_result.strip()
        if not text.endswith("."):
            text += "."
        return text[0].upper() + text[1:] if text else text
    if fallback and not is_weak_raw_result(fallback, phrase):
        text = fallback.strip()
        if not text.endswith("."):
            text += "."
        return text[0].upper() + text[1:] if text else text
    return NEEDS_REVIEW
