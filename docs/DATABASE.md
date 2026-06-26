# Database — alice-commands-api

**DBMS:** PostgreSQL 16 · **Migrations:** Flyway

---

## 1. Принцип

- Все **draft** таблицы — mutable, редактируются admin
- **Published** state — только files (`content_vN.json.gz`) + row `current_manifest`
- Publish читает draft → пишет bundle

---

## 2. ERD (упрощённо)

```mermaid
erDiagram
  categories ||--o{ commands : contains
  commands ||--o{ checklist_items : referenced_by
  publish_history ||--|| current_manifest : tracks

  categories {
    text id PK
    text title_ru
    text title_kk
    int sort_order
    bool featured
    text icon_key
    text description_ru
    text source_url
    text[] device_types
    timestamptz updated_at
  }

  commands {
    text id PK
    text category_id FK
    text title_ru
    jsonb phrases
    text effect_description_ru
    bool requires_alice_word
    bool requires_plus
    text[] device_types
    text[] related_command_ids
    text source_url
    timestamptz published_at
    timestamptz updated_at
    text[] tags
  }

  scenario_templates {
    text id PK
    text title_ru
    text trigger_ru
    jsonb actions_ru
    jsonb example_phrases
    text audience
    text deep_link_hint
    text source_url
  }

  checklist_items {
    text id PK
    int item_order
    text command_id FK
    text hint_ru
  }

  affiliate_blocks {
    text id PK
    text context_category_id
    text title_ru
    text erid
    text advertiser_name
    jsonb products
    timestamptz updated_at
  }

  current_manifest {
    int content_version PK
    text bundle_path
    text bundle_sha256
    timestamptz published_at
    text min_app_version
    int schema_version
  }

  publish_history {
    bigint id PK
    int content_version
    text bundle_sha256
    text admin_username
    timestamptz published_at
    text notes
  }

  admin_sessions {
    text id PK
    timestamptz expires_at
  }
```

---

## 3. Indexes

```sql
CREATE INDEX idx_commands_category ON commands(category_id);
CREATE INDEX idx_commands_tags ON commands USING GIN(tags);
CREATE INDEX idx_categories_sort ON categories(sort_order);
```

---

## 4. Publish state

| Table | Role |
| ----- | ---- |
| `current_manifest` | Single row (or versioned) — pointer to live bundle |
| `publish_history` | Audit + rollback source |

Rollback: update `current_manifest` to previous `content_version` where bundle file still exists.

---

## 5. Seed

Import from [`seed/import-smart-home.json`](../seed/import-smart-home.json) via admin import or Flyway seed script (dev only).

---

## 6. Backup

- `pg_dump` weekly (cron on VPS)
- Bundle files included in storage backup or regenerable from DB via re-publish

---

*Field mapping → [schema/content-bundle.schema.json](../schema/content-bundle.schema.json)*
