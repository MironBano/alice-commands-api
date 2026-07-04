#!/usr/bin/env python3
"""Analyze bundle quality metrics for iterative pipeline tuning."""
from __future__ import annotations

import json
import sys
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def analyze(path: Path) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    cmds = data["commands"]
    cats = {c["id"]: c for c in data["categories"]}

    issues: Counter[str] = Counter()
    examples: dict[str, list] = defaultdict(list)

    for c in cmds:
        t = c["title_ru"]
        e = c["effect_description_ru"]
        ph = c["phrases"][0]
        cat = cats[c["category_id"]]["title_ru"]

        def ex(name: str, row: tuple) -> None:
            issues[name] += 1
            if len(examples[name]) < 5:
                examples[name].append(row)

        if e == "Требует вычитки" or "needs_review" in c.get("tags", []):
            ex("needs_review", (cat, t, ph[:55], e[:65]))
        if e.rstrip(".!?").lower() == t.rstrip(".!?").lower():
            ex("title_eq_effect", (cat, t, e))
        if e.lower().startswith("выполнит:") and e.rstrip(".")[11:].strip().lower() == t.rstrip(".!?").lower():
            ex("weak_vypolnit", (cat, t, e, ph[:50]))
        if any(m in e.lower() for m in ("умные колонки от", "яндекс станция", "до покупки", "справочник команд")):
            ex("nav_garbage", (cat, t, e[:80]))
        if len(t) <= 5 and len(c.get("phrases", [])) < 2:
            ex("short_title", (cat, t, ph[:50], e[:50]))
        if len(t) > 70:
            ex("long_title", (cat, t[:75], ph[:45]))
        if "[" in ph or "..." in ph:
            ex("template_phrase", (cat, t, ph))
        if t.lower() in ("голосовые команды", "голосовые команды для алисы"):
            ex("generic_sh_title", (cat, t, ph[:50]))

    dup_titles = Counter((c["category_id"], c["title_ru"]) for c in cmds if len(c["title_ru"]) < 40)
    top_dup = [(cats[cid]["title_ru"], title, n) for (cid, title), n in dup_titles.most_common(8) if n > 3]

    return {
        "total": len(cmds),
        "needs_review": sum(1 for c in cmds if "needs_review" in c.get("tags", [])),
        "multi_phrase": sum(1 for c in cmds if len(c["phrases"]) > 1),
        "issues": dict(issues),
        "examples": {k: v for k, v in examples.items()},
        "top_dup_titles": top_dup,
    }


def main() -> int:
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "seed" / "full-catalog.json"
    report = analyze(path)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
