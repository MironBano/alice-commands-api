"""Merge parsed content from multiple sources."""
from __future__ import annotations

import re
from collections import defaultdict

from slugify import slugify

from models import ParseResult, ParsedCommand, ParsedCategory, ParsedScenario, ParsedChecklistItem

PRIORITY_RANK = {"primary": 0, "backup": 1, "pilot": 2}


def normalize_phrase(phrase: str) -> str:
    text = phrase.strip().lower()
    text = re.sub(r"^алиса[,\s]+", "", text)
    text = re.sub(r"\s+", " ", text)
    return text


def stable_command_id(category_id: str, primary_phrase: str) -> str:
    norm = normalize_phrase(primary_phrase)
    slug = slugify(norm, separator="_", max_length=40) or "cmd"
    return f"{category_id}_{slug}"[:64]


def merge_results(results: list[ParseResult]) -> ParseResult:
    categories: dict[str, ParsedCategory] = {}
    commands: dict[str, ParsedCommand] = {}
    phrase_index: dict[str, str] = {}
    scenarios: dict[str, ParsedScenario] = {}
    checklist: dict[str, ParsedChecklistItem] = {}
    errors: list[str] = []

    sorted_results = sorted(
        results,
        key=lambda r: PRIORITY_RANK.get(
            next((c.source_priority for c in r.commands[:1]), "backup"),
            99,
        ),
    )

    for result in sorted_results:
        errors.extend(result.errors)
        for cat in result.categories:
            existing = categories.get(cat.id)
            if existing is None or cat.source_url:
                categories[cat.id] = cat

        for cmd in result.commands:
            norm = normalize_phrase(cmd.phrases[0])
            if norm in phrase_index and phrase_index[norm] != cmd.id:
                existing_id = phrase_index[norm]
                existing = commands[existing_id]
                merged_phrases = list(dict.fromkeys(existing.phrases + cmd.phrases))
                existing.phrases = merged_phrases
                if existing.effect_description_ru in ("", "Требует вычитки") and cmd.effect_description_ru:
                    existing.effect_description_ru = cmd.effect_description_ru
                    existing.tags = [t for t in existing.tags if t != "needs_review"]
                for tag in cmd.tags:
                    if tag not in existing.tags:
                        existing.tags.append(tag)
                continue

            existing = commands.get(cmd.id)
            if existing is None:
                commands[cmd.id] = cmd
                phrase_index[norm] = cmd.id
                continue

            rank_new = PRIORITY_RANK.get(cmd.source_priority, 99)
            rank_old = PRIORITY_RANK.get(existing.source_priority, 99)
            winner, loser = (cmd, existing) if rank_new < rank_old else (existing, cmd)
            merged_phrases = list(dict.fromkeys(winner.phrases + loser.phrases))
            winner.phrases = merged_phrases
            if winner.effect_description_ru in ("", "Требует вычитки") and loser.effect_description_ru:
                winner.effect_description_ru = loser.effect_description_ru
            commands[winner.id] = winner
            phrase_index[norm] = winner.id

        for sc in result.scenarios:
            scenarios[sc.id] = sc
        for item in result.checklist_items:
            checklist[item.id] = item

    return ParseResult(
        categories=list(categories.values()),
        commands=list(commands.values()),
        scenarios=list(scenarios.values()),
        checklist_items=sorted(checklist.values(), key=lambda x: x.order),
        errors=errors,
    )


def assign_missing_ids(commands: list[ParsedCommand]) -> None:
    seen: set[str] = set()
    for cmd in commands:
        if not cmd.id or cmd.id in seen:
            cmd.id = stable_command_id(cmd.category_id, cmd.phrases[0])
        base = cmd.id
        n = 2
        while cmd.id in seen:
            cmd.id = f"{base}_{n}"
            n += 1
        seen.add(cmd.id)
