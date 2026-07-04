"""One-off inventory dump for manual migration (read-only)."""
import json
import sys
from pathlib import Path

cat = sys.argv[1] if len(sys.argv) > 1 else "calls"
src = Path(r"C:\Users\rybak\Downloads\editorial-export-all (2).json")
d = json.loads(src.read_text(encoding="utf-8"))
recs = [r for r in d["records"] if r["category_id"] == cat]
out = Path(__file__).parent / f"_cat-{cat}.json"
out.write_text(json.dumps(recs, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"{cat}: {len(recs)} -> {out}")
