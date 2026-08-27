# Archive — исторические снимки каталога

**Не использовать в скриптах и deploy.** Только ручной emergency rollback.

| Файл | Описание |
| --- | --- |
| `full-catalog-legacy-v32.json` | Legacy pipeline output (~approved subset, schema v2) |
| `full-catalog-verify-v1.json` | Старый schema v1 smoke |
| `catalog-audit-fixed-REFERENCE-v43.json` | Эталон v43 (764 cmd), byte-parity checkpoint |
| `full-catalog-published*.json.gz` | Опубликованные bundle с VPS (gzip) |
| `v25.json.gz` | Ранний snapshot |

**Канон для релиза:** [`../catalog-audit-fixed.json`](../catalog-audit-fixed.json) (885 команд).
