"""Editorial store — human-approved titles and effects."""
from __future__ import annotations

import json
from pathlib import Path

from command_bank import build_command_bank
from merge import stable_command_id
from pipeline_models import EditorialRecord, EditorialStatus, EditorialStore, utc_now
from pipeline_paths import EDITORIAL_JSON
from raw_result_quality import suggest_effect, suggest_title_from_phrase


def load_editorial(path: Path = EDITORIAL_JSON) -> EditorialStore:
    if not path.exists():
        return bootstrap_editorial(path)
    data = json.loads(path.read_text(encoding="utf-8"))
    return EditorialStore.from_dict(data)


def save_editorial(store: EditorialStore, path: Path = EDITORIAL_JSON) -> None:
    store.updated_at = utc_now()
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(store.to_dict(), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def bootstrap_editorial(path: Path = EDITORIAL_JSON) -> EditorialStore:
    """Seed editorial from command_bank — all approved."""
    if path.exists():
        data = json.loads(path.read_text(encoding="utf-8"))
        existing = EditorialStore.from_dict(data)
        if existing.records:
            return existing

    records: dict[str, EditorialRecord] = {}
    now = utc_now()
    for cmd in build_command_bank():
        command_id = cmd.id or stable_command_id(cmd.category_id, cmd.phrases[0])
        records[command_id] = EditorialRecord(
            command_id=command_id,
            category_id=cmd.category_id,
            title_ru=cmd.title_ru,
            effect_description_ru=cmd.effect_description_ru,
            status=EditorialStatus.APPROVED.value,
            approved_at=now,
            updated_at=now,
        )
    store = EditorialStore(records=records)
    save_editorial(store, path)
    return store


def ensure_editorial_for_inventory(
    store: EditorialStore,
    *,
    command_id: str,
    category_id: str,
    phrase: str,
    raw_result: str | None,
    parsed_title: str | None = None,
    parsed_effect: str | None = None,
) -> EditorialRecord:
    existing = store.records.get(command_id)
    if existing is not None:
        return existing

    title = parsed_title or suggest_title_from_phrase(phrase)
    effect = suggest_effect(raw_result, phrase, fallback=parsed_effect)
    status = EditorialStatus.APPROVED.value if effect != "Требует вычитки" else EditorialStatus.PENDING.value
    record = EditorialRecord(
        command_id=command_id,
        category_id=category_id,
        title_ru=title,
        effect_description_ru=effect,
        status=status,
        approved_at=utc_now() if status == EditorialStatus.APPROVED.value else None,
    )
    store.records[command_id] = record
    return record


def approve_record(
    store: EditorialStore,
    command_id: str,
    *,
    title_ru: str | None = None,
    effect_description_ru: str | None = None,
) -> EditorialRecord | None:
    record = store.records.get(command_id)
    if record is None:
        return None
    if title_ru:
        record.title_ru = title_ru
    if effect_description_ru:
        record.effect_description_ru = effect_description_ru
    record.status = EditorialStatus.APPROVED.value
    record.approved_at = utc_now()
    record.updated_at = utc_now()
    return record


def reject_record(store: EditorialStore, command_id: str) -> EditorialRecord | None:
    record = store.records.get(command_id)
    if record is None:
        return None
    record.status = EditorialStatus.REJECTED.value
    record.updated_at = utc_now()
    return record
