"""Diff inventory baseline vs snapshot and build editorial work queue."""
from __future__ import annotations

import json
import uuid
from pathlib import Path

from merge import normalize_phrase
from pipeline_models import (
    EditorialStore,
    InventoryItem,
    InventorySnapshot,
    QueueEventType,
    QueueItem,
    QueueStatus,
    QueueStore,
    utc_now,
)
from pipeline_paths import INVENTORY_BASELINE, INVENTORY_SNAPSHOT, QUEUE_JSON
from editorial_store import ensure_editorial_for_inventory, load_editorial, save_editorial
from raw_result_quality import is_weak_raw_result, suggest_effect, suggest_title_from_phrase


def load_snapshot(path: Path = INVENTORY_SNAPSHOT) -> InventorySnapshot:
    if not path.exists():
        return InventorySnapshot(items=[])
    return InventorySnapshot.from_dict(json.loads(path.read_text(encoding="utf-8")))


def load_baseline(path: Path = INVENTORY_BASELINE) -> InventorySnapshot:
    if not path.exists():
        return InventorySnapshot(items=[])
    return InventorySnapshot.from_dict(json.loads(path.read_text(encoding="utf-8")))


def save_snapshot(snapshot: InventorySnapshot, path: Path = INVENTORY_SNAPSHOT) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(snapshot.to_dict(), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def save_baseline(snapshot: InventorySnapshot, path: Path = INVENTORY_BASELINE) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(snapshot.to_dict(), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def _phrase_sets(items: list[InventoryItem]) -> dict[str, set[str]]:
    result: dict[str, set[str]] = {}
    for item in items:
        result[item.command_id] = {normalize_phrase(p) for p in item.phrases}
    return result


def _index_by_id(items: list[InventoryItem]) -> dict[str, InventoryItem]:
    return {item.command_id: item for item in items}


def sync_inventory(
    snapshot: InventorySnapshot,
    *,
    baseline: InventorySnapshot | None = None,
    editorial: EditorialStore | None = None,
) -> tuple[QueueStore, EditorialStore, dict[str, int]]:
    baseline = baseline or load_baseline()
    editorial = editorial or load_editorial()

    base_index = _index_by_id(baseline.items)
    snap_index = _index_by_id(snapshot.items)
    base_phrases = _phrase_sets(baseline.items)
    snap_phrases = _phrase_sets(snapshot.items)

    queue_items: list[QueueItem] = []
    stats = {
        "new_command": 0,
        "new_phrase": 0,
        "gone_phrase": 0,
        "gone_command": 0,
        "needs_review": 0,
        "unchanged": 0,
    }

    for command_id, item in snap_index.items():
        if command_id not in base_index:
            phrase = item.phrases[0]
            suggested = suggest_effect(item.raw_result, phrase)
            ensure_editorial_for_inventory(
                editorial,
                command_id=command_id,
                category_id=item.category_id,
                phrase=phrase,
                raw_result=item.raw_result,
            )
            record = editorial.records.get(command_id)
            if record and record.is_approved:
                stats["unchanged"] += 1
                continue
            stats["new_command"] += 1
            queue_items.append(
                QueueItem(
                    id=str(uuid.uuid4()),
                    event_type=QueueEventType.NEW_COMMAND.value,
                    command_id=command_id,
                    phrase=phrase,
                    category_id=item.category_id,
                    title_ru=suggest_title_from_phrase(phrase),
                    suggested_effect=suggested,
                    raw_result=item.raw_result,
                    source_url=item.source_url,
                )
            )
            continue

        new_norms = snap_phrases[command_id] - base_phrases.get(command_id, set())
        for norm in sorted(new_norms):
            phrase = next(p for p in item.phrases if normalize_phrase(p) == norm)
            stats["new_phrase"] += 1
            queue_items.append(
                QueueItem(
                    id=str(uuid.uuid4()),
                    event_type=QueueEventType.NEW_PHRASE.value,
                    command_id=command_id,
                    phrase=phrase,
                    category_id=item.category_id,
                    source_url=item.source_url,
                )
            )

        record = editorial.records.get(command_id)
        if record and record.status == "approved":
            stats["unchanged"] += 1
        elif record and is_weak_raw_result(item.raw_result, item.phrases[0]):
            stats["needs_review"] += 1
            queue_items.append(
                QueueItem(
                    id=str(uuid.uuid4()),
                    event_type=QueueEventType.NEEDS_REVIEW.value,
                    command_id=command_id,
                    phrase=item.phrases[0],
                    category_id=item.category_id,
                    title_ru=record.title_ru,
                    suggested_effect=suggest_effect(item.raw_result, item.phrases[0]),
                    raw_result=item.raw_result,
                    source_url=item.source_url,
                )
            )
        else:
            stats["unchanged"] += 1

    for command_id, item in base_index.items():
        if command_id not in snap_index:
            stats["gone_command"] += 1
            queue_items.append(
                QueueItem(
                    id=str(uuid.uuid4()),
                    event_type=QueueEventType.GONE_COMMAND.value,
                    command_id=command_id,
                    category_id=item.category_id,
                    phrase=item.phrases[0] if item.phrases else None,
                    source_url=item.source_url,
                )
            )
            continue

        gone_norms = base_phrases.get(command_id, set()) - snap_phrases.get(command_id, set())
        for norm in sorted(gone_norms):
            phrase = next(p for p in item.phrases if normalize_phrase(p) == norm)
            stats["gone_phrase"] += 1
            queue_items.append(
                QueueItem(
                    id=str(uuid.uuid4()),
                    event_type=QueueEventType.GONE_PHRASE.value,
                    command_id=command_id,
                    phrase=phrase,
                    category_id=item.category_id,
                    source_url=item.source_url,
                )
            )

    store = QueueStore(updated_at=utc_now(), items=_merge_queue(load_queue().items, queue_items))
    save_editorial(editorial)
    save_queue(store)
    return store, editorial, stats


def save_queue(store: QueueStore, path: Path = QUEUE_JSON) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(store.to_dict(), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def load_queue(path: Path = QUEUE_JSON) -> QueueStore:
    if not path.exists():
        return QueueStore(items=[])
    return QueueStore.from_dict(json.loads(path.read_text(encoding="utf-8")))


def _merge_queue(previous: list[QueueItem], new_items: list[QueueItem]) -> list[QueueItem]:
    closed_keys = {
        (item.event_type, item.command_id, item.phrase or "")
        for item in previous
        if item.status != QueueStatus.OPEN.value
    }
    merged: list[QueueItem] = [item for item in previous if item.status != QueueStatus.OPEN.value]
    for item in new_items:
        key = (item.event_type, item.command_id, item.phrase or "")
        if key in closed_keys:
            continue
        merged.append(item)
    return merged


def resolve_queue_item(
    queue: QueueStore,
    editorial: EditorialStore,
    item_id: str,
    *,
    action: str,
    title_ru: str | None = None,
    effect_description_ru: str | None = None,
) -> bool:
    item = next((q for q in queue.items if q.id == item_id and q.status == QueueStatus.OPEN.value), None)
    if item is None:
        return False

    if action == "approve":
        from editorial_store import approve_record

        ensure_editorial_for_inventory(
            editorial,
            command_id=item.command_id,
            category_id=item.category_id or "general",
            phrase=item.phrase or "",
            raw_result=item.raw_result,
            parsed_title=item.title_ru,
            parsed_effect=item.suggested_effect,
        )
        approve_record(
            editorial,
            item.command_id,
            title_ru=title_ru or item.title_ru,
            effect_description_ru=effect_description_ru or item.suggested_effect,
        )
    elif action == "dismiss":
        pass
    elif action == "reject":
        from editorial_store import reject_record

        reject_record(editorial, item.command_id)

    item.status = QueueStatus.RESOLVED.value if action == "approve" else QueueStatus.DISMISSED.value
    save_editorial(editorial)
    save_queue(queue)
    return True
