"""Tests for phrase-based title/effect inference."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from phrase_effects import improve_effect, infer_from_phrase, is_long_title, title_from_phrase


class PhraseEffectsTest(unittest.TestCase):
    def test_infer_audiobook(self) -> None:
        title, effect = infer_from_phrase("Алиса, включи аудиокнигу 1984")
        self.assertEqual(title, "Включить аудиокнигу 1984")
        self.assertIn("1984", effect)

    def test_long_title_detection(self) -> None:
        long = "Альбом Треки играют по порядку. После завершения альбома"
        self.assertTrue(is_long_title(long))

    def test_improve_weak_vypolnit(self) -> None:
        effect = improve_effect(
            "Алиса, включи Netflix на телевизоре",
            "Видео из интернета",
            "Выполнит: видео из интернета.",
        )
        self.assertNotIn("Выполнит:", effect.lower())

    def test_title_from_phrase_short(self) -> None:
        title = title_from_phrase("Алиса, переключись на AUX")
        self.assertIn("AUX", title)


if __name__ == "__main__":
    unittest.main()
