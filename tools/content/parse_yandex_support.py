"""Extract commands from Yandex support HTML pages."""
from __future__ import annotations

import re
from pathlib import Path

from bs4 import BeautifulSoup

from categories import CATEGORY_BY_ID
from merge import stable_command_id
from models import ParseResult, ParsedCommand, ParsedCategory

ALICE_PREFIX_RE = re.compile(r"^[\s«\"]*(?:алиса|яндекс)[,\s]+", re.IGNORECASE)
QUOTE_RE = re.compile(r"[«\"]([^»\"]{3,120})[»\"]")
NEEDS_REVIEW = "Требует вычитки"


def _clean_phrase(text: str) -> str:
    text = text.strip().strip("«»\"'")
    text = re.sub(r"\s+", " ", text)
    if not text:
        return text
    lower = text.lower()
    if not lower.startswith("алиса") and not lower.startswith("яндекс"):
        if re.match(r"^[a-zа-яё0-9]", text, re.IGNORECASE):
            text = f"Алиса, {text[0].lower()}{text[1:]}" if text else text
    return text


def _title_from_phrase(phrase: str) -> str:
    cleaned = ALICE_PREFIX_RE.sub("", phrase).strip(" ,.")
    cleaned = cleaned[:1].upper() + cleaned[1:] if cleaned else phrase
    return cleaned[:80]


def _extract_phrases_from_text(text: str) -> list[str]:
    phrases: list[str] = []
    for match in QUOTE_RE.finditer(text):
        raw = match.group(1).strip()
        if len(raw) < 3:
            continue
        phrase = _clean_phrase(raw)
        if phrase and phrase not in phrases:
            phrases.append(phrase)
    for line in text.splitlines():
        line = line.strip()
        if not line or len(line) > 150:
            continue
        if "алиса" in line.lower() or line.startswith("«"):
            for m in QUOTE_RE.finditer(line):
                phrase = _clean_phrase(m.group(1))
                if phrase and phrase not in phrases:
                    phrases.append(phrase)
            if line.lower().startswith("алиса,"):
                phrase = _clean_phrase(line)
                if phrase and phrase not in phrases:
                    phrases.append(phrase)
    return phrases


def _effect_from_context(element_text: str, phrase: str) -> str:
    text = element_text.strip()
    if len(text) > 40 and phrase not in text:
        return text[:500]
    return NEEDS_REVIEW


def parse_html(
    html_path: Path,
    *,
    source_id: str,
    category_id: str,
    source_url: str,
    priority: str = "primary",
    requires_alice_word: bool = True,
    device_types: list[str] | None = None,
) -> ParseResult:
    devices = device_types or ["station", "phone"]
    html = html_path.read_text(encoding="utf-8", errors="replace")
    soup = BeautifulSoup(html, "lxml")
    main = soup.find("article") or soup.find("main") or soup.body or soup

    commands: list[ParsedCommand] = []
    seen_phrases: set[str] = set()

    for block in main.find_all(["p", "li", "h2", "h3", "div"]):
        text = block.get_text(" ", strip=True)
        if not text or len(text) < 8:
            continue
        phrases = _extract_phrases_from_text(text)
        for phrase in phrases:
            norm = phrase.lower()
            if norm in seen_phrases:
                continue
            seen_phrases.add(norm)
            requires_alice = requires_alice_word and norm.startswith("алиса")
            cmd_id = stable_command_id(category_id, phrase)
            effect = _effect_from_context(text, phrase)
            tags = [category_id]
            if effect == NEEDS_REVIEW:
                tags.append("needs_review")
            commands.append(
                ParsedCommand(
                    id=cmd_id,
                    category_id=category_id,
                    title_ru=_title_from_phrase(phrase),
                    phrases=[phrase],
                    effect_description_ru=effect,
                    source_url=source_url,
                    requires_alice_word=requires_alice,
                    device_types=devices,
                    tags=tags,
                    source_priority=priority,
                )
            )

    categories: list[ParsedCategory] = []
    if category_id in CATEGORY_BY_ID:
        categories.append(CATEGORY_BY_ID[category_id])

    return ParseResult(categories=categories, commands=commands, source_id=source_id)
