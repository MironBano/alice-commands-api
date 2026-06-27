"""Intermediate models for content pipeline."""
from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any


@dataclass
class ParsedCategory:
    id: str
    title_ru: str
    sort_order: int
    source_url: str
    description_ru: str | None = None
    icon_key: str | None = None
    featured: bool = False
    device_types: list[str] = field(default_factory=list)


@dataclass
class ParsedCommand:
    id: str
    category_id: str
    title_ru: str
    phrases: list[str]
    effect_description_ru: str
    source_url: str
    requires_alice_word: bool = True
    requires_plus: bool = False
    device_types: list[str] = field(default_factory=lambda: ["station", "phone"])
    related_command_ids: list[str] = field(default_factory=list)
    tags: list[str] = field(default_factory=list)
    source_priority: str = "primary"
    updated_at: str = field(default_factory=lambda: datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"))


@dataclass
class ParsedScenario:
    id: str
    title_ru: str
    source_url: str
    trigger_ru: str | None = None
    actions_ru: list[str] = field(default_factory=list)
    example_phrases: list[str] = field(default_factory=list)
    audience: str | None = None
    deep_link_hint: str | None = None


@dataclass
class ParsedChecklistItem:
    id: str
    order: int
    command_id: str
    hint_ru: str | None = None


@dataclass
class ParseResult:
    categories: list[ParsedCategory] = field(default_factory=list)
    commands: list[ParsedCommand] = field(default_factory=list)
    scenarios: list[ParsedScenario] = field(default_factory=list)
    checklist_items: list[ParsedChecklistItem] = field(default_factory=list)
    source_id: str = ""
    errors: list[str] = field(default_factory=list)


def command_to_bundle_dict(cmd: ParsedCommand) -> dict[str, Any]:
    return {
        "id": cmd.id,
        "category_id": cmd.category_id,
        "title_ru": cmd.title_ru,
        "phrases": cmd.phrases,
        "effect_description_ru": cmd.effect_description_ru,
        "requires_alice_word": cmd.requires_alice_word,
        "requires_plus": cmd.requires_plus,
        "device_types": cmd.device_types,
        "related_command_ids": cmd.related_command_ids,
        "source_url": cmd.source_url,
        "updated_at": cmd.updated_at,
        "tags": cmd.tags,
    }


def category_to_bundle_dict(cat: ParsedCategory) -> dict[str, Any]:
    data: dict[str, Any] = {
        "id": cat.id,
        "title_ru": cat.title_ru,
        "sort_order": cat.sort_order,
        "source_url": cat.source_url,
    }
    if cat.description_ru:
        data["description_ru"] = cat.description_ru
    if cat.icon_key:
        data["icon_key"] = cat.icon_key
    if cat.featured:
        data["featured"] = True
    if cat.device_types:
        data["device_types"] = cat.device_types
    return data
