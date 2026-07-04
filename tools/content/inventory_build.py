"""Build inventory snapshot from Yandex support parsers (facts only)."""
from __future__ import annotations

import re
from pathlib import Path

from command_bank import build_command_bank
from fetch import fetch_all, load_config
from merge import normalize_phrase, stable_command_id
from models import ParseResult, ParsedCommand
from parse_yandex_quick_commands import parse_quick_commands_html
from parse_yandex_smart_home import parse_pilot_json, parse_smart_home
from parse_yandex_support import parse_html
from pipeline_models import InventoryItem, InventorySnapshot, utc_now
from postprocess import is_nav_garbage, is_template_phrase
from yandex_discovery import resolve_sources

ROOT = Path(__file__).resolve().parents[2]

DROP_PHRASES = frozenset({"алиса", "алиса,", "быстрые", "голосовые команды"})


def _should_drop_command(cmd: ParsedCommand) -> bool:
    if is_nav_garbage(cmd.effect_description_ru) or is_nav_garbage(cmd.title_ru):
        return True
    if any(is_template_phrase(p) for p in cmd.phrases):
        return True
    if not cmd.phrases:
        return True
    body = normalize_phrase(cmd.phrases[0])
    if body in DROP_PHRASES:
        return True
    if len(cmd.title_ru) > 100:
        return True
    return False


def _command_to_inventory(cmd: ParsedCommand, *, source_id: str, section: str | None = None) -> InventoryItem:
    raw = cmd.raw_result if cmd.raw_result is not None else cmd.effect_description_ru
    return InventoryItem(
        command_id=cmd.id or stable_command_id(cmd.category_id, cmd.phrases[0]),
        category_id=cmd.category_id,
        phrases=list(dict.fromkeys(cmd.phrases)),
        raw_result=raw,
        source_url=cmd.source_url,
        section=section,
        requires_alice_word=cmd.requires_alice_word,
        requires_plus=cmd.requires_plus,
        device_types=list(cmd.device_types or ["station", "phone"]),
        source_id=source_id,
        last_seen_at=cmd.updated_at or utc_now(),
        deprecated=False,
    )


def _merge_inventory_items(items: list[InventoryItem]) -> list[InventoryItem]:
    by_id: dict[str, InventoryItem] = {}
    phrase_index: dict[str, str] = {}

    for item in items:
        if _should_drop_inventory(item):
            continue
        norm = normalize_phrase(item.phrases[0])
        if norm in phrase_index and phrase_index[norm] != item.command_id:
            existing = by_id[phrase_index[norm]]
            merged = list(dict.fromkeys(existing.phrases + item.phrases))
            existing.phrases = merged
            if not existing.raw_result and item.raw_result:
                existing.raw_result = item.raw_result
            if item.last_seen_at > existing.last_seen_at:
                existing.last_seen_at = item.last_seen_at
                existing.source_url = item.source_url
            continue

        existing = by_id.get(item.command_id)
        if existing is None:
            by_id[item.command_id] = item
            phrase_index[norm] = item.command_id
            continue

        merged_phrases = list(dict.fromkeys(existing.phrases + item.phrases))
        existing.phrases = merged_phrases
        if not existing.raw_result and item.raw_result:
            existing.raw_result = item.raw_result
        if item.last_seen_at > existing.last_seen_at:
            existing.last_seen_at = item.last_seen_at
            existing.source_url = item.source_url
        phrase_index[norm] = existing.command_id

    return sorted(by_id.values(), key=lambda x: (x.category_id, x.command_id))


def _should_drop_inventory(item: InventoryItem) -> bool:
    if not item.phrases:
        return True
    if any(is_template_phrase(p) for p in item.phrases):
        return True
    body = normalize_phrase(item.phrases[0])
    if body in DROP_PHRASES:
        return True
    if item.raw_result and is_nav_garbage(item.raw_result):
        return False  # keep if phrases are valid; raw may be junk but editorial fixes it
    return False


def _bank_inventory_items() -> list[InventoryItem]:
    items: list[InventoryItem] = []
    for cmd in build_command_bank():
        items.append(
            _command_to_inventory(
                cmd,
                source_id="command_bank",
            )
        )
    return items


def run_inventory_parsers(*, skip_fetch: bool = False, force_fetch: bool = False) -> tuple[InventorySnapshot, list[str]]:
    config = load_config()
    sources = resolve_sources(config, skip_discovery=skip_fetch)
    paths = fetch_all(force=force_fetch, sources=sources) if not skip_fetch else {}

    if skip_fetch:
        cache = Path(__file__).resolve().parent / "cache"
        for src in sources:
            sid = src["id"]
            src_dir = cache / sid
            if src_dir.exists():
                files = list(src_dir.glob("*.html")) + list(src_dir.glob("*.json"))
                if files:
                    paths[sid] = files[0]

    inventory_items: list[InventoryItem] = []
    inventory_items.extend(_bank_inventory_items())
    errors: list[str] = []

    for src in sources:
        sid = src["id"]
        url = src["url"]
        priority = src.get("priority", "backup")
        parser = src.get("parser", "yandex_support")
        category_id = src.get("category_id", "general")

        if url.startswith("file://"):
            local = ROOT / url.replace("file://", "")
            if parser == "pilot_json":
                result = parse_pilot_json(local, source_id=sid)
                for cmd in result.commands:
                    if _should_drop_command(cmd):
                        continue
                    inventory_items.append(_command_to_inventory(cmd, source_id=sid))
            continue

        path = paths.get(sid)
        if path is None or not path.exists():
            errors.append(f"Missing cache for {sid}")
            continue

        try:
            if parser == "yandex_quick_commands":
                result = parse_quick_commands_html(path, source_id=sid, source_url=url, priority=priority)
            elif parser == "yandex_smart_home":
                result = parse_smart_home(path, source_id=sid, source_url=url, priority=priority)
            else:
                result = parse_html(
                    path,
                    source_id=sid,
                    category_id=category_id,
                    source_url=url,
                    priority=priority,
                )
            for cmd in result.commands:
                if _should_drop_command(cmd):
                    continue
                inventory_items.append(_command_to_inventory(cmd, source_id=sid))
            errors.extend(result.errors)
        except Exception as exc:  # noqa: BLE001
            errors.append(f"{sid}: {exc}")

    merged = _merge_inventory_items(inventory_items)
    snapshot = InventorySnapshot(synced_at=utc_now(), items=merged)
    return snapshot, errors


def inventory_from_pilot_json(path: Path) -> list[InventoryItem]:
    """Convert pilot JSON bundle commands to inventory items (for baseline seed)."""
    import json

    data = json.loads(path.read_text(encoding="utf-8"))
    items: list[InventoryItem] = []
    for cmd in data.get("commands") or []:
        phrases = list(cmd.get("phrases") or [])
        if not phrases:
            continue
        items.append(
            InventoryItem(
                command_id=cmd["id"],
                category_id=cmd["category_id"],
                phrases=phrases,
                raw_result=cmd.get("effect_description_ru"),
                source_url=cmd.get("source_url", ""),
                requires_alice_word=bool(cmd.get("requires_alice_word", True)),
                requires_plus=bool(cmd.get("requires_plus", False)),
                device_types=list(cmd.get("device_types") or ["station", "phone"]),
                source_id="pilot_json",
            )
        )
    return items
