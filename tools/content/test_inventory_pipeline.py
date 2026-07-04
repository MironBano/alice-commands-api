"""Tests for inventory sync and editorial pipeline."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from editorial_store import EditorialStore, bootstrap_editorial
from inventory_sync import sync_inventory
from pipeline_models import EditorialRecord, EditorialStatus, InventoryItem, InventorySnapshot


class InventorySyncTest(unittest.TestCase):
    def test_new_command_creates_queue_item(self) -> None:
        baseline = InventorySnapshot(items=[])
        snapshot = InventorySnapshot(
            items=[
                InventoryItem(
                    command_id="music_test",
                    category_id="music",
                    phrases=["Алиса, включи джаз"],
                    raw_result="Алиса выполнит команду включи джаз",
                    source_url="https://example.com/music",
                    source_id="test",
                )
            ]
        )
        editorial = EditorialStore(records={})
        queue, editorial, stats = sync_inventory(snapshot, baseline=baseline, editorial=editorial)
        self.assertEqual(stats["new_command"], 1)
        self.assertEqual(len(queue.items), 1)
        self.assertEqual(queue.items[0].event_type, "new_command")

    def test_approved_editorial_not_requeued_on_resync(self) -> None:
        item = InventoryItem(
            command_id="music_test",
            category_id="music",
            phrases=["Алиса, включи джаз"],
            raw_result="Алиса выполнит команду включи джаз",
            source_url="https://example.com/music",
            source_id="test",
        )
        baseline = InventorySnapshot(items=[item])
        snapshot = InventorySnapshot(items=[item])
        editorial = EditorialStore(
            records={
                "music_test": EditorialRecord(
                    command_id="music_test",
                    category_id="music",
                    title_ru="Джаз",
                    effect_description_ru="Включит джаз.",
                    status=EditorialStatus.APPROVED.value,
                    approved_at="2026-01-01T00:00:00Z",
                )
            }
        )
        queue, _, stats = sync_inventory(snapshot, baseline=baseline, editorial=editorial)
        open_items = [q for q in queue.items if q.status == "open"]
        self.assertEqual(len(open_items), 0)
        self.assertGreaterEqual(stats["unchanged"], 1)


if __name__ == "__main__":
    unittest.main()
