"""Parse quick commands page (no «Алиса» prefix)."""
from __future__ import annotations

from pathlib import Path

from parse_yandex_support import _clean_phrase, _effect_from_context, _extract_phrases_from_text, _title_from_phrase
from categories import CATEGORY_BY_ID
from merge import stable_command_id
from models import ParseResult, ParsedCommand, ParsedCategory


def parse_quick_commands_html(html_path: Path, *, source_id: str, source_url: str, priority: str = "backup") -> ParseResult:
    from bs4 import BeautifulSoup

    html = html_path.read_text(encoding="utf-8", errors="replace")
    soup = BeautifulSoup(html, "lxml")
    main = soup.find("article") or soup.find("main") or soup.body or soup
    category_id = "quick_commands"
    commands: list[ParsedCommand] = []
    seen: set[str] = set()

    for block in main.find_all(["p", "li", "div"]):
        text = block.get_text(" ", strip=True)
        phrases = _extract_phrases_from_text(text)
        for raw in phrases:
            phrase = raw
            if phrase.lower().startswith("алиса,"):
                phrase = phrase.split(",", 1)[-1].strip()
                phrase = phrase[:1].upper() + phrase[1:] if phrase else phrase
            norm = phrase.lower()
            if not norm or norm in seen:
                continue
            seen.add(norm)
            effect = _effect_from_context(text, phrase)
            tags = ["quick_commands", "no_alice_word"]
            if effect == "Требует вычитки":
                tags.append("needs_review")
            commands.append(
                ParsedCommand(
                    id=stable_command_id(category_id, phrase),
                    category_id=category_id,
                    title_ru=_title_from_phrase(phrase),
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
