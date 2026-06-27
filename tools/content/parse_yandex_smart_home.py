"""Smart home parser — merges pilot JSON scenarios + support page."""
from __future__ import annotations

import json
from pathlib import Path

from parse_yandex_support import parse_html
from models import ParseResult, ParsedScenario, ParsedChecklistItem


def parse_pilot_json(json_path: Path, *, source_id: str) -> ParseResult:
    data = json.loads(json_path.read_text(encoding="utf-8"))
    from models import ParsedCategory, ParsedCommand

    categories = [
        ParsedCategory(
            c["id"], c["title_ru"], c["sort_order"], c["source_url"],
            c.get("description_ru"), c.get("icon_key"), c.get("featured", False), c.get("device_types", []),
        )
        for c in data.get("categories", [])
    ]
    commands = [
        ParsedCommand(
            id=c["id"],
            category_id=c["category_id"],
            title_ru=c["title_ru"],
            phrases=c["phrases"],
            effect_description_ru=c["effect_description_ru"],
            source_url=c["source_url"],
            requires_alice_word=c.get("requires_alice_word", True),
            requires_plus=c.get("requires_plus", False),
            device_types=c.get("device_types", ["station", "phone"]),
            related_command_ids=c.get("related_command_ids", []),
            tags=c.get("tags", []),
            source_priority="primary",
            updated_at=c.get("updated_at", ""),
        )
        for c in data.get("commands", [])
    ]
    scenarios = [
        ParsedScenario(
            id=s["id"],
            title_ru=s["title_ru"],
            source_url=s["source_url"],
            trigger_ru=s.get("trigger_ru"),
            actions_ru=s.get("actions_ru", []),
            example_phrases=s.get("example_phrases", []),
            audience=s.get("audience"),
            deep_link_hint=s.get("deep_link_hint"),
        )
        for s in data.get("scenario_templates", [])
    ]
    checklist = [
        ParsedChecklistItem(id=i["id"], order=i["order"], command_id=i["command_id"], hint_ru=i.get("hint_ru"))
        for i in data.get("checklist_items", [])
    ]
    return ParseResult(categories, commands, scenarios, checklist, source_id)


def parse_smart_home(html_path: Path, *, source_id: str, source_url: str, priority: str) -> ParseResult:
    page = parse_html(
        html_path,
        source_id=source_id,
        category_id="smart_home",
        source_url=source_url,
        priority=priority,
        device_types=["station", "phone", "tv"],
    )
    return page
