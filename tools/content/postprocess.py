"""Polish, group and filter parsed commands before bundle export."""
from __future__ import annotations

import re

from effect_templates import NEEDS_REVIEW, humanize_table_effect
from merge import PRIORITY_RANK, stable_command_id
from models import ParsedCommand
from phrase_effects import (
    improve_effect,
    infer_from_phrase,
    is_long_title,
    is_weak_effect,
    title_from_phrase,
)

NAV_GARBAGE_MARKERS = (
    "умные колонки от",
    "яндекс станция",
    "до покупки",
    "с чего начать",
    "справочник команд",
    "развлечения послушать",
)

GENERIC_TITLES = frozenset(
    {
        "голосовые команды",
        "голосовые команды для алисы",
        "алиса",
        "быстрые",
    }
)

DROP_TITLE_PHRASES = frozenset({"алиса", "быстрые"})


def is_nav_garbage(text: str) -> bool:
    lower = text.lower()
    return any(marker in lower for marker in NAV_GARBAGE_MARKERS)


def is_template_phrase(phrase: str) -> bool:
    return "[" in phrase or "..." in phrase or "{" in phrase


def is_title_effect_duplicate(title: str, effect: str) -> bool:
    return is_weak_effect(effect, title)


def effect_quality(effect: str, title: str = "") -> int:
    if not effect or effect == NEEDS_REVIEW:
        return 0
    if is_nav_garbage(effect):
        return 0
    if is_weak_effect(effect, title):
        return 1
    score = min(len(effect), 120)
    if effect.endswith("."):
        score += 5
    if " " in effect:
        score += 10
    if effect.lower().startswith("алиса выполнит"):
        score += 3
    return score


def should_drop(cmd: ParsedCommand) -> bool:
    if is_nav_garbage(cmd.effect_description_ru) or is_nav_garbage(cmd.title_ru):
        return True
    if any(is_template_phrase(p) for p in cmd.phrases):
        return True
    if cmd.title_ru.strip().lower() in DROP_TITLE_PHRASES:
        return True
    if len(cmd.title_ru) > 100:
        return True
    if _normalize_title_key(cmd.title_ru) in GENERIC_TITLES:
        return True
    body = cmd.phrases[0].strip().lower()
    if body in {"алиса", "алиса,", "быстрые"}:
        return True
    return False


def _normalize_title_key(title: str) -> str:
    return re.sub(r"\s+", " ", title.strip().lower())[:100]


def _pick_title(cmd: ParsedCommand) -> str:
    title = cmd.title_ru.strip()
    phrase = cmd.phrases[0]
    if len(title) <= 5:
        inferred = infer_from_phrase(phrase)
        if inferred[0]:
            return inferred[0]
        return title_from_phrase(phrase)
    if _normalize_title_key(title) in GENERIC_TITLES or is_long_title(title):
        return title_from_phrase(phrase)
    inferred = infer_from_phrase(phrase)
    if inferred[0] and title.lower() == phrase_body_lower(phrase):
        return inferred[0]
    return title


def phrase_body_lower(phrase: str) -> str:
    from phrase_effects import phrase_body

    return phrase_body(phrase).lower()


def polish_command(cmd: ParsedCommand) -> ParsedCommand:
    cmd.title_ru = _pick_title(cmd)
    primary = cmd.phrases[0]
    cmd.effect_description_ru = improve_effect(primary, cmd.title_ru, cmd.effect_description_ru)
    if is_title_effect_duplicate(cmd.title_ru, cmd.effect_description_ru):
        cmd.effect_description_ru = humanize_table_effect(cmd.title_ru, phrase=primary)
    if effect_quality(cmd.effect_description_ru, cmd.title_ru) <= 1:
        if NEEDS_REVIEW not in cmd.tags:
            cmd.tags = [*cmd.tags, "needs_review"]
    elif "needs_review" in cmd.tags and effect_quality(cmd.effect_description_ru, cmd.title_ru) >= 10:
        cmd.tags = [t for t in cmd.tags if t != "needs_review"]
    return cmd


def group_commands_by_title(commands: list[ParsedCommand]) -> list[ParsedCommand]:
    groups: dict[tuple[str, str], ParsedCommand] = {}

    ordered = sorted(commands, key=lambda c: PRIORITY_RANK.get(c.source_priority, 99))
    for cmd in ordered:
        key = (cmd.category_id, _normalize_title_key(cmd.title_ru))
        existing = groups.get(key)
        if existing is None:
            groups[key] = cmd
            continue

        merged_phrases = list(dict.fromkeys(existing.phrases + cmd.phrases))
        existing.phrases = merged_phrases

        if effect_quality(cmd.effect_description_ru, cmd.title_ru) > effect_quality(
            existing.effect_description_ru, existing.title_ru
        ):
            existing.effect_description_ru = cmd.effect_description_ru
        elif is_title_effect_duplicate(existing.title_ru, existing.effect_description_ru) and not is_title_effect_duplicate(
            cmd.title_ru, cmd.effect_description_ru
        ):
            existing.effect_description_ru = cmd.effect_description_ru

        if PRIORITY_RANK.get(cmd.source_priority, 99) < PRIORITY_RANK.get(existing.source_priority, 99):
            existing.source_priority = cmd.source_priority
            existing.source_url = cmd.source_url

        for tag in cmd.tags:
            if tag not in existing.tags:
                existing.tags.append(tag)

    result: list[ParsedCommand] = []
    for cmd in groups.values():
        cmd.id = stable_command_id(cmd.category_id, cmd.phrases[0])
        if effect_quality(cmd.effect_description_ru, cmd.title_ru) >= 10:
            cmd.tags = [t for t in cmd.tags if t != "needs_review"]
        elif "needs_review" not in cmd.tags:
            cmd.tags = [*cmd.tags, "needs_review"]
        result.append(cmd)
    return result


def postprocess_commands(commands: list[ParsedCommand]) -> list[ParsedCommand]:
    polished: list[ParsedCommand] = []
    for cmd in commands:
        cmd = polish_command(cmd)
        if should_drop(cmd):
            continue
        polished.append(cmd)
    return group_commands_by_title(polished)
