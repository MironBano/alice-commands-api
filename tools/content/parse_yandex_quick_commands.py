"""Parse quick commands page (no «Алиса» prefix)."""
from __future__ import annotations

from pathlib import Path

from parse_yandex_support import (
    _effect_from_block,
    _extract_phrases_from_block,
    _find_content_root,
    _is_excluded,
    _is_nav_junk,
    _is_weak_phrase,
    _section_title_usable,
    _title_from_effect,
    _title_from_phrase,
)
from categories import CATEGORY_BY_ID
from merge import stable_command_id
from models import ParseResult, ParsedCommand, ParsedCategory

NEEDS_REVIEW = "Требует вычитки"


def parse_quick_commands_html(html_path: Path, *, source_id: str, source_url: str, priority: str = "backup") -> ParseResult:
    from bs4 import BeautifulSoup

    html = html_path.read_text(encoding="utf-8", errors="replace")
    soup = BeautifulSoup(html, "lxml")
    root = _find_content_root(soup)
    category_id = "quick_commands"
    commands: list[ParsedCommand] = []
    seen: set[str] = set()

    section_title = ""
    for element in root.find_all(["h2", "h3", "p", "li"]):
        if _is_excluded(element):
            continue
        if element.name in {"h2", "h3"}:
            section_title = element.get_text(" ", strip=True)
            continue
        text = element.get_text(" ", strip=True)
        if not text or _is_nav_junk(text):
            continue
        for raw in _extract_phrases_from_block(text):
            phrase = raw
            if phrase.lower().startswith("алиса,"):
                phrase = phrase.split(",", 1)[-1].strip()
                phrase = phrase[:1].upper() + phrase[1:] if phrase else phrase
            if _is_weak_phrase(phrase):
                continue
            norm = phrase.lower()
            if not norm or norm in seen:
                continue
            seen.add(norm)
            effect = _effect_from_block(text, phrase, section_title)
            tags = ["quick_commands", "no_alice_word"]
            if effect == NEEDS_REVIEW:
                tags.append("needs_review")
            title = _title_from_effect(section_title) if _section_title_usable(section_title) else _title_from_phrase(phrase)
            if title == NEEDS_REVIEW or len(title) < 4:
                title = _title_from_phrase(phrase)
            commands.append(
                ParsedCommand(
                    id=stable_command_id(category_id, phrase),
                    category_id=category_id,
                    title_ru=title,
                    phrases=[phrase],
                    effect_description_ru=effect,
                    source_url=source_url,
                    requires_alice_word=False,
                    device_types=["station", "phone"],
                    tags=tags,
                    source_priority=priority,
                )
            )

    categories = [CATEGORY_BY_ID[category_id]] if category_id in CATEGORY_BY_ID else []
    return ParseResult(categories=categories, commands=commands, source_id=source_id)
