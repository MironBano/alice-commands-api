"""Tests for postprocess pipeline."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from models import ParsedCommand
from postprocess import group_commands_by_title, postprocess_commands, should_drop


def _cmd(
    category_id: str,
    title: str,
    phrase: str,
    effect: str,
    *,
    priority: str = "primary",
) -> ParsedCommand:
    return ParsedCommand(
        id=f"{category_id}_{phrase[:8]}",
        category_id=category_id,
        title_ru=title,
        phrases=[phrase],
        effect_description_ru=effect,
        source_url="https://example.com",
        tags=[category_id],
        source_priority=priority,
    )


class PostprocessTest(unittest.TestCase):
    def test_group_by_title_merges_phrases(self) -> None:
        a = _cmd("timers", "Управлять таймером", "Алиса, удали таймер", "Покажет оставшееся время.")
        b = _cmd("timers", "Управлять таймером", "Алиса, какие таймеры установлены?", "Управлять таймером.")
        grouped = group_commands_by_title([a, b])
        self.assertEqual(len(grouped), 1)
        self.assertEqual(len(grouped[0].phrases), 2)
        self.assertFalse(
            grouped[0].effect_description_ru.rstrip(".").lower()
            == grouped[0].title_ru.rstrip(".").lower()
        )

    def test_drop_nav_garbage(self) -> None:
        bad = _cmd(
            "music",
            "Азербайджан",
            "Алиса, азербайджан",
            "Яндекс Станция Умные колонки от Яндекса До покупки",
        )
        self.assertTrue(should_drop(bad))

    def test_postprocess_fixes_title_effect_duplicate(self) -> None:
        dup = _cmd("timers", "Удалить таймер", "Алиса, удали таймер", "Удалить таймер.")
        out = postprocess_commands([dup])
        self.assertEqual(len(out), 1)
        self.assertNotEqual(out[0].effect_description_ru.rstrip("."), out[0].title_ru.rstrip("."))


if __name__ == "__main__":
    unittest.main()
