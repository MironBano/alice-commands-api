"""Extract commands from Yandex support HTML pages."""
from __future__ import annotations

import re
from pathlib import Path

from bs4 import BeautifulSoup, Tag

from categories import CATEGORY_BY_ID
from effect_templates import humanize_table_effect
from merge import stable_command_id
from models import ParseResult, ParsedCommand, ParsedCategory

ALICE_PREFIX_RE = re.compile(r"^[\s«\"]*(?:алиса|яндекс)[,\s]+", re.IGNORECASE)
QUOTE_RE = re.compile(r"[«\"]([^»\"]{2,120})[»\"]")
ALICE_IN_QUOTE_RE = re.compile(
    r"[«\"]([^»\"]*(?:\bалиса\b|\bяндекс\b)[^»\"]*)[»\"]",
    re.IGNORECASE,
)
NEEDS_REVIEW = "Требует вычитки"

EXCLUDED_TAGS = frozenset({"nav", "aside", "header", "footer", "script", "style"})
EXCLUDED_CLASS_FRAGMENTS = (
    "dc-toc",
    "desktop-navigation",
    "dc-doc-layout__left",
    "dc-doc-layout__right",
)

NAV_JUNK_RE = re.compile(
    r"^(?:умные колонки|до покупки|с\s+чего начать|справочник команд|"
    r"развлечения|общение|для\s+детей|алиса\s+плюс|центр умного дома|"
    r"мультиподписка|неочевидные навыки|настройки|яндекс\s+станция)\b",
    re.IGNORECASE,
)

GENERIC_SECTION_RE = re.compile(
    r"^(?:аудиокниги|музыка|радио|настройки|команды|справочник|"
    r"фильмы|телевизор|баланс|кешбэк)\b",
    re.IGNORECASE,
)

WEAK_PHRASES = frozenset(
    {
        "алиса, везде",
        "алиса, стоп",
        "алиса",
        "алиса, битлз",
    }
)

GENERIC_SECTION_TITLES = frozenset(
    {
        "голосовые команды",
        "голосовые команды для алисы",
    }
)

NAV_GARBAGE_MARKERS = (
    "умные колонки от",
    "яндекс станция",
    "до покупки",
)

TABLE_HEADER_MARKERS = ("результат", "команда")


def _contains_nav_garbage(text: str) -> bool:
    lower = text.lower()
    return any(marker in lower for marker in NAV_GARBAGE_MARKERS)


def _section_title_usable(title: str) -> bool:
    cleaned = title.strip()
    if cleaned.lower() in GENERIC_SECTION_TITLES:
        return False
    if len(cleaned) < 12:
        return False
    if GENERIC_SECTION_RE.match(cleaned):
        return False
    return True


def _is_weak_phrase(phrase: str) -> bool:
    norm = phrase.strip().lower()
    if norm in WEAK_PHRASES:
        return True
    body = ALICE_PREFIX_RE.sub("", phrase).strip(" ,.")
    if not body:
        return True
    if len(body.split()) == 1 and len(body) <= 4:
        return True
    return False


def _effect_is_weak(effect: str) -> bool:
    if effect == NEEDS_REVIEW:
        return True
    core = effect.rstrip(".!?").strip()
    if len(core) < 15 and " " not in core:
        return True
    return len(effect) < 12


def _clean_phrase(text: str) -> str:
    text = text.strip().strip("«»\"'")
    text = re.sub(r"\s+", " ", text)
    if not text:
        return text
    lower = text.lower()
    if not lower.startswith("алиса") and not lower.startswith("яндекс"):
        if re.match(r"^[a-zа-яё0-9\[]", text, re.IGNORECASE):
            text = f"Алиса, {text[0].lower()}{text[1:]}" if text else text
    return text


def _title_from_phrase(phrase: str) -> str:
    cleaned = ALICE_PREFIX_RE.sub("", phrase).strip(" ,.")
    cleaned = cleaned[:1].upper() + cleaned[1:] if cleaned else phrase
    return cleaned[:80]


def _title_from_table_result(raw: str) -> str:
    text = re.sub(r"\s+", " ", raw.strip())
    if "/" in text:
        text = text.split("/")[0].strip()
    for stop in (" Треки ", " После ", " Вы можете ", ". "):
        idx = text.find(stop)
        if idx > 8:
            text = text[:idx].strip()
    return _title_from_effect(text)


def _should_skip_page(source_url: str, source_id: str) -> bool:
    blob = f"{source_url} {source_id}".lower()
    return any(
        token in blob
        for token in (
            "alice-controls",
            "/lamp/",
            "/socket/",
            "/ir-remote/",
        )
    )


def _title_from_effect(effect: str) -> str:
    text = effect.strip().strip("«»")
    text = re.sub(r"\s+", " ", text)
    if not text:
        return NEEDS_REVIEW
    if len(text) <= 80:
        return text[:1].upper() + text[1:]
    cut = text[:80]
    if " " in cut:
        cut = cut.rsplit(" ", 1)[0]
    return cut[:1].upper() + cut[1:]


def _normalize_effect(text: str) -> str:
    effect = re.sub(r"\s+", " ", text.strip())
    if not effect:
        return NEEDS_REVIEW
    if effect[-1] not in ".!?":
        effect += "."
    return effect[:500]


def _find_content_root(soup: BeautifulSoup) -> Tag:
    for selector in (".dc-doc-page__body", "main.dc-doc-page__content", "article", "main"):
        node = soup.select_one(selector)
        if node is not None:
            return node
    return soup.body or soup


def _is_excluded(element: Tag) -> bool:
    for parent in element.parents:
        if not isinstance(parent, Tag):
            continue
        if parent.name in EXCLUDED_TAGS:
            return True
        classes = " ".join(parent.get("class") or [])
        if any(fragment in classes for fragment in EXCLUDED_CLASS_FRAGMENTS):
            return True
    return False


def _is_nav_junk(text: str) -> bool:
    cleaned = re.sub(r"\s+", " ", text.strip())
    if not cleaned or len(cleaned) < 3:
        return True
    if NAV_JUNK_RE.match(cleaned):
        return True
    if len(cleaned) > 180 and cleaned.count(" ") > 15:
        return True
    return False


def _split_command_alternatives(text: str) -> list[str]:
    parts = [p.strip() for p in re.split(r"\s*/\s*", text) if p.strip()]
    return parts or [text.strip()]


def _cell_phrases(cell: Tag) -> list[str]:
    phrases: list[str] = []
    paragraphs = cell.find_all("p")
    if not paragraphs:
        paragraphs = [cell]
    for block in paragraphs:
        raw = block.get_text(" ", strip=True)
        if not raw:
            continue
        for part in _split_command_alternatives(raw):
            phrase = _clean_phrase(part)
            if phrase and not _is_nav_junk(phrase):
                phrases.append(phrase)
    return phrases


def _is_command_table(table: Tag) -> bool:
    rows = table.find_all("tr")
    if len(rows) < 2:
        return False
    header = rows[0].get_text(" ", strip=True).lower()
    return all(marker in header for marker in TABLE_HEADER_MARKERS)


def _parse_command_tables(
    root: Tag,
    *,
    category_id: str,
    source_url: str,
    priority: str,
    requires_alice_word: bool,
    devices: list[str],
    seen_phrases: set[str],
) -> list[ParsedCommand]:
    commands: list[ParsedCommand] = []

    for table in root.find_all("table"):
        if _is_excluded(table) or not _is_command_table(table):
            continue
        for row in table.find_all("tr")[1:]:
            cells = row.find_all(["td", "th"])
            if len(cells) < 2:
                continue
            effect_raw = cells[0].get_text(" ", strip=True)
            if not effect_raw or _is_nav_junk(effect_raw) or _contains_nav_garbage(effect_raw):
                continue
            title = _title_from_table_result(effect_raw)

            row_phrases: list[str] = []
            for phrase in _cell_phrases(cells[1]):
                if _is_weak_phrase(phrase):
                    continue
                norm = phrase.lower()
                if norm in seen_phrases:
                    continue
                row_phrases.append(phrase)

            if not row_phrases:
                continue

            effect = humanize_table_effect(effect_raw, phrase=row_phrases[0])

            for norm in (p.lower() for p in row_phrases):
                seen_phrases.add(norm)

            primary = row_phrases[0]
            requires_alice = requires_alice_word and primary.lower().startswith("алиса")
            commands.append(
                ParsedCommand(
                    id=stable_command_id(category_id, primary),
                    category_id=category_id,
                    title_ru=title,
                    phrases=row_phrases,
                    effect_description_ru=effect,
                    raw_result=effect_raw or None,
                    source_url=source_url,
                    requires_alice_word=requires_alice,
                    device_types=devices,
                    tags=[category_id],
                    source_priority=priority,
                )
            )
    return commands


def _extract_phrases_from_block(text: str) -> list[str]:
    phrases: list[str] = []
    for match in ALICE_IN_QUOTE_RE.finditer(text):
        phrase = _clean_phrase(match.group(1))
        if phrase and phrase not in phrases:
            phrases.append(phrase)

    if text.lower().startswith("алиса,"):
        phrase = _clean_phrase(text)
        if phrase and phrase not in phrases:
            phrases.append(phrase)

    if not phrases and "«" not in text and "»" not in text:
        stripped = text.strip()
        if 8 <= len(stripped) <= 120 and not _is_nav_junk(stripped):
            lower = stripped.lower()
            if lower.startswith("алиса") or re.match(
                r"^(?:включи|выключи|поставь|запусти|останов|продолж|скажи|"
                r"удали|врубай|давай|сколько|какие|какой|какая|переключ|"
                r"покажи|найди|включите|выключите)\b",
                lower,
            ):
                phrase = _clean_phrase(stripped)
                if phrase and phrase not in phrases:
                    phrases.append(phrase)

    if re.search(r"\bкомандами\b", text, re.IGNORECASE):
        return phrases

    for match in QUOTE_RE.finditer(text):
        inner = match.group(1).strip()
        if len(inner) < 4 or len(inner) > 40:
            continue
        if "алиса" in inner.lower():
            continue
        if inner.lower() in {"stations-list", "multiroom-activate"}:
            continue
        if not re.search(r"[а-яёa-z]", inner, re.I):
            continue
        phrase = _clean_phrase(inner)
        if phrase and phrase not in phrases and not _is_nav_junk(phrase) and not _is_weak_phrase(phrase):
            phrases.append(phrase)

    return phrases


def _effect_from_block(block_text: str, phrase: str, section_title: str = "") -> str:
    if _section_title_usable(section_title):
        effect = humanize_table_effect(section_title)
        if not _effect_is_weak(effect) and not _contains_nav_garbage(effect):
            return effect

    text = block_text.strip()
    remainder = ALICE_IN_QUOTE_RE.sub("", text)
    remainder = QUOTE_RE.sub("", remainder)
    remainder = re.sub(r"\s+", " ", remainder).strip(" .—–-:;")
    if len(remainder) >= 20 and phrase.lower() not in remainder.lower() and not _contains_nav_garbage(remainder):
        effect = _normalize_effect(remainder)
        if not _effect_is_weak(effect):
            return effect

    if len(text) > 40 and phrase.lower() not in text.lower() and not _is_nav_junk(text) and not _contains_nav_garbage(text):
        effect = _normalize_effect(text)
        if not _effect_is_weak(effect):
            return effect

    return NEEDS_REVIEW


def _section_blocks(root: Tag) -> list[tuple[str, list[Tag]]]:
    sections: list[tuple[str, list[Tag]]] = []
    current_title = ""
    current_blocks: list[Tag] = []

    for element in root.find_all(["h2", "h3", "p", "li", "table"]):
        if _is_excluded(element):
            continue
        if element.name in {"h2", "h3"}:
            if current_blocks:
                sections.append((current_title, current_blocks))
            current_title = element.get_text(" ", strip=True)
            current_blocks = []
            continue
        if element.name == "table":
            continue
        current_blocks.append(element)

    if current_blocks:
        sections.append((current_title, current_blocks))
    return sections


def _parse_quote_blocks(
    root: Tag,
    *,
    category_id: str,
    source_url: str,
    priority: str,
    requires_alice_word: bool,
    devices: list[str],
    seen_phrases: set[str],
) -> list[ParsedCommand]:
    commands: list[ParsedCommand] = []

    for section_title, blocks in _section_blocks(root):
        for block in blocks:
            text = block.get_text(" ", strip=True)
            if not text or len(text) < 8 or _is_nav_junk(text):
                continue
            phrases = _extract_phrases_from_block(text)
            for phrase in phrases:
                if _is_weak_phrase(phrase):
                    continue
                norm = phrase.lower()
                if norm in seen_phrases:
                    continue
                seen_phrases.add(norm)
                effect = _effect_from_block(text, phrase, section_title)
                tags = [category_id]
                if effect == NEEDS_REVIEW or _effect_is_weak(effect):
                    effect = NEEDS_REVIEW
                    tags.append("needs_review")
                requires_alice = requires_alice_word and norm.startswith("алиса")
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
                        requires_alice_word=requires_alice,
                        device_types=devices,
                        tags=tags,
                        source_priority=priority,
                    )
                )
    return commands


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
    if _should_skip_page(source_url, source_id):
        return ParseResult(categories=[], commands=[], source_id=source_id)

    devices = device_types or ["station", "phone"]
    html = html_path.read_text(encoding="utf-8", errors="replace")
    soup = BeautifulSoup(html, "lxml")
    root = _find_content_root(soup)

    seen_phrases: set[str] = set()
    common = {
        "category_id": category_id,
        "source_url": source_url,
        "priority": priority,
        "requires_alice_word": requires_alice_word,
        "devices": devices,
        "seen_phrases": seen_phrases,
    }
    commands = _parse_command_tables(root, **common)
    commands.extend(_parse_quote_blocks(root, **common))

    categories: list[ParsedCategory] = []
    if category_id in CATEGORY_BY_ID:
        categories.append(CATEGORY_BY_ID[category_id])

    return ParseResult(categories=categories, commands=commands, source_id=source_id)


# Re-exported helpers for quick-commands parser
def _extract_phrases_from_text(text: str) -> list[str]:
    return _extract_phrases_from_block(text)


def _effect_from_context(element_text: str, phrase: str) -> str:
    return _effect_from_block(element_text, phrase)
