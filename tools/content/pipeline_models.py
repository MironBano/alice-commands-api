"""Inventory / editorial / queue models for the content pipeline."""
from __future__ import annotations

from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
from enum import Enum
from typing import Any


def utc_now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


class EditorialStatus(str, Enum):
    APPROVED = "approved"
    PENDING = "pending"
    AI_DRAFT = "ai_draft"
    REJECTED = "rejected"


class QueueEventType(str, Enum):
    NEW_COMMAND = "new_command"
    NEW_PHRASE = "new_phrase"
    GONE_PHRASE = "gone_phrase"
    GONE_COMMAND = "gone_command"
    NEEDS_REVIEW = "needs_review"


class QueueStatus(str, Enum):
    OPEN = "open"
    RESOLVED = "resolved"
    DISMISSED = "dismissed"


@dataclass
class InventoryItem:
    command_id: str
    category_id: str
    phrases: list[str]
    raw_result: str | None
    source_url: str
    section: str | None = None
    requires_alice_word: bool = True
    requires_plus: bool = False
    device_types: list[str] = field(default_factory=lambda: ["station", "phone"])
    source_id: str = ""
    last_seen_at: str = field(default_factory=utc_now)
    deprecated: bool = False

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> InventoryItem:
        return cls(
            command_id=data["command_id"],
            category_id=data["category_id"],
            phrases=list(data.get("phrases") or []),
            raw_result=data.get("raw_result"),
            source_url=data["source_url"],
            section=data.get("section"),
            requires_alice_word=bool(data.get("requires_alice_word", True)),
            requires_plus=bool(data.get("requires_plus", False)),
            device_types=list(data.get("device_types") or ["station", "phone"]),
            source_id=data.get("source_id", ""),
            last_seen_at=data.get("last_seen_at") or utc_now(),
            deprecated=bool(data.get("deprecated", False)),
        )


@dataclass
class EditorialRecord:
    command_id: str
    category_id: str
    title_ru: str
    effect_description_ru: str
    status: str = EditorialStatus.PENDING.value
    approved_at: str | None = None
    notes: str | None = None
    updated_at: str = field(default_factory=utc_now)

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> EditorialRecord:
        return cls(
            command_id=data["command_id"],
            category_id=data["category_id"],
            title_ru=data["title_ru"],
            effect_description_ru=data["effect_description_ru"],
            status=data.get("status", EditorialStatus.PENDING.value),
            approved_at=data.get("approved_at"),
            notes=data.get("notes"),
            updated_at=data.get("updated_at") or utc_now(),
        )

    @property
    def is_approved(self) -> bool:
        return self.status == EditorialStatus.APPROVED.value


@dataclass
class QueueItem:
    id: str
    event_type: str
    command_id: str
    phrase: str | None = None
    category_id: str | None = None
    title_ru: str | None = None
    suggested_effect: str | None = None
    raw_result: str | None = None
    source_url: str | None = None
    status: str = QueueStatus.OPEN.value
    created_at: str = field(default_factory=utc_now)

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> QueueItem:
        return cls(
            id=data["id"],
            event_type=data["event_type"],
            command_id=data["command_id"],
            phrase=data.get("phrase"),
            category_id=data.get("category_id"),
            title_ru=data.get("title_ru"),
            suggested_effect=data.get("suggested_effect"),
            raw_result=data.get("raw_result"),
            source_url=data.get("source_url"),
            status=data.get("status", QueueStatus.OPEN.value),
            created_at=data.get("created_at") or utc_now(),
        )


@dataclass
class InventorySnapshot:
    schema_version: int = 1
    synced_at: str = field(default_factory=utc_now)
    items: list[InventoryItem] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return {
            "schema_version": self.schema_version,
            "synced_at": self.synced_at,
            "items": [item.to_dict() for item in self.items],
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> InventorySnapshot:
        return cls(
            schema_version=int(data.get("schema_version", 1)),
            synced_at=data.get("synced_at") or utc_now(),
            items=[InventoryItem.from_dict(item) for item in data.get("items") or []],
        )


@dataclass
class EditorialStore:
    schema_version: int = 1
    updated_at: str = field(default_factory=utc_now)
    records: dict[str, EditorialRecord] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        return {
            "schema_version": self.schema_version,
            "updated_at": self.updated_at,
            "records": {key: record.to_dict() for key, record in sorted(self.records.items())},
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> EditorialStore:
        records = {
            key: EditorialRecord.from_dict(value)
            for key, value in (data.get("records") or {}).items()
        }
        return cls(
            schema_version=int(data.get("schema_version", 1)),
            updated_at=data.get("updated_at") or utc_now(),
            records=records,
        )


@dataclass
class QueueStore:
    schema_version: int = 1
    updated_at: str = field(default_factory=utc_now)
    items: list[QueueItem] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return {
            "schema_version": self.schema_version,
            "updated_at": self.updated_at,
            "items": [item.to_dict() for item in self.items],
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> QueueStore:
        return cls(
            schema_version=int(data.get("schema_version", 1)),
            updated_at=data.get("updated_at") or utc_now(),
            items=[QueueItem.from_dict(item) for item in data.get("items") or []],
        )
