"""Build publishable full-catalog.json from inventory + approved editorial."""
from __future__ import annotations

from datetime import datetime, timezone

from categories import LAUNCH_CATEGORIES
from models import category_to_bundle_dict
from pipeline_models import EditorialStatus, EditorialStore, InventorySnapshot
from raw_result_quality import is_weak_raw_result


def build_catalog_bundle(
    inventory: InventorySnapshot,
    editorial: EditorialStore,
    *,
    include_pending: bool = False,
) -> dict:
    inv_index = {item.command_id: item for item in inventory.items}
    commands: list[dict] = []
    now = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    for command_id, record in sorted(editorial.records.items()):
        if record.status == EditorialStatus.REJECTED.value:
            continue
        if record.status != EditorialStatus.APPROVED.value and not include_pending:
            continue
        if is_weak_raw_result(record.effect_description_ru, record.title_ru):
            if not include_pending:
                continue

        item = inv_index.get(command_id)
        phrases = list(item.phrases) if item else []
        if not phrases:
            continue

        source_url = item.source_url if item else ""
        requires_alice = item.requires_alice_word if item else True
        requires_plus = item.requires_plus if item else False
        device_types = list(item.device_types) if item else ["station", "phone"]

        tags: list[str] = [record.category_id]
        if record.status == EditorialStatus.PENDING.value:
            tags.append("needs_review")
        if item and item.deprecated:
            tags.append("deprecated")

        commands.append(
            {
                "id": command_id,
                "category_id": record.category_id,
                "title_ru": record.title_ru,
                "phrases": phrases,
                "effect_description_ru": record.effect_description_ru,
                "requires_alice_word": requires_alice,
                "requires_plus": requires_plus,
                "device_types": device_types,
                "related_command_ids": [],
                "source_url": source_url,
                "updated_at": record.updated_at or now,
                "tags": tags,
            }
        )

    categories = {c.id: c for c in LAUNCH_CATEGORIES}
    return {
        "schema_version": 1,
        "content_version": 0,
        "published_at": "1970-01-01T00:00:00Z",
        "min_app_version": "1.0",
        "categories": [category_to_bundle_dict(c) for c in sorted(categories.values(), key=lambda x: x.sort_order)],
        "commands": sorted(commands, key=lambda x: (x["category_id"], x["id"])),
        "scenario_templates": [],
        "checklist_items": [],
    }


def catalog_stats(bundle: dict) -> dict[str, int]:
    cmds = bundle.get("commands") or []
    return {
        "categories": len(bundle.get("categories") or []),
        "commands": len(cmds),
        "approved_in_catalog": len(cmds),
        "needs_review_in_catalog": sum(1 for c in cmds if "needs_review" in c.get("tags", [])),
    }
