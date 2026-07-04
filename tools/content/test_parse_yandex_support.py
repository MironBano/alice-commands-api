"""Tests for Yandex support HTML parser."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from parse_yandex_support import parse_html

FIXTURES = Path(__file__).resolve().parent / "fixtures"
CACHE = Path(__file__).resolve().parent / "cache"


class ParseYandexSupportTest(unittest.TestCase):
    def test_timer_table_parsing(self) -> None:
        result = parse_html(
            FIXTURES / "timer_table.html",
            source_id="test_timer",
            category_id="timers",
            source_url="https://alice.yandex.ru/support/ru/station/skills/timer",
        )
        self.assertGreaterEqual(len(result.commands), 3)
        phrases = {c.phrases[0].lower() for c in result.commands}
        self.assertIn("алиса, поставь таймер на 10 минут", phrases)
        self.assertIn("алиса, удали таймер", phrases)
        sample = next(c for c in result.commands if "10 минут" in c.phrases[0])
        self.assertEqual(sample.title_ru, "Установить таймер")
        self.assertIn("таймер", sample.effect_description_ru.lower())
        self.assertNotEqual(sample.effect_description_ru.rstrip("."), sample.title_ru.rstrip("."))
        self.assertGreaterEqual(len(sample.phrases), 2)
        self.assertTrue(all("умные колонки" not in c.effect_description_ru.lower() for c in result.commands))

    def test_multiroom_skips_nav_and_uses_sections(self) -> None:
        result = parse_html(
            FIXTURES / "multiroom_quotes.html",
            source_id="test_multiroom",
            category_id="music",
            source_url="https://alice.yandex.ru/support/ru/station/multiroom",
        )
        phrases = {c.phrases[0].lower() for c in result.commands}
        self.assertIn("алиса, включи музыку везде", phrases)
        self.assertIn("алиса, останови музыку в гостиной", phrases)
        self.assertNotIn("алиса, везде", phrases)
        self.assertNotIn("алиса, сначала", phrases)
        self.assertNotIn("алиса, дальше", phrases)
        self.assertTrue(all("развлечения послушать" not in c.effect_description_ru.lower() for c in result.commands))

    def test_cached_timer_page_if_present(self) -> None:
        cached = CACHE / "yandex_support_ru_station_skills_timer" / "support_ru_station_skills_timer.html"
        if not cached.exists():
            self.skipTest("cache not available")
        result = parse_html(
            cached,
            source_id="cached_timer",
            category_id="timers",
            source_url="https://alice.yandex.ru/support/ru/station/skills/timer",
        )
        self.assertGreaterEqual(len(result.commands), 4)
        phrase_total = sum(len(c.phrases) for c in result.commands)
        self.assertGreaterEqual(phrase_total, 8)
        needs_review = sum(1 for c in result.commands if "needs_review" in c.tags)
        self.assertEqual(needs_review, 0)


if __name__ == "__main__":
    unittest.main()
