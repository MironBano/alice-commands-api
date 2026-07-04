# Database — alice-commands-api

**DBMS:** PostgreSQL 16 · **Migrations:** Flyway (`V1__init.sql` … `V6__command_of_day.sql`)

| Migration | Таблицы |
| --------- | ------- |
| V1 | Draft content, manifest, publish_history, admin_sessions |
| V2 | `inventory_items`, `editorial_records`, `content_queue` — content pipeline |
| V3 | `user_feedback`, `command_reports`, `public_submission_attempts` — app feedback |
| V4 | `command_groups` + group fields on `commands` |
| V5 | `icon_url`, `accent_color`, `accent_color_dark` on `categories` + `command_groups` |
| V6 | `command_of_day_settings` — editorial singleton (manual/auto, FK → commands/categories) |

---

## 1. Принцип

- **Draft** таблицы (`categories`, `commands`, …) — mutable, редактируются admin
- **Published** state — files (`content_vN.json.gz`) + row `current_manifest` + affiliate snapshot на диске
- Publish читает draft → валидирует schema → пишет bundle

---

## 2. ERD (упрощённо)

```mermaid
erDiagram
  categories ||--o{ command_groups : contains
  categories ||--o{ commands : contains
  command_groups ||--o{ commands : groups
  commands ||--o{ checklist_items : referenced_by
  publish_history ||--|| current_manifest : tracks

  categories {
    text id PK
    text title_ru
    text title_kk
    int sort_order
    bool featured
    text icon_key
    text icon_url
    varchar accent_color
    varchar accent_color_dark
    text description_ru
    text source_url
    text[] device_types
    timestamptz updated_at
  }

  command_groups {
    text id PK
    text category_id FK
    text title_ru
    text description_ru
    int sort_order
    text icon_key
    text icon_url
    varchar accent_color
    varchar accent_color_dark
    bool featured
    text[] preview_command_ids
    timestamptz updated_at
  }

  commands {
    text id PK
    text category_id FK
    text group_id FK
    int sort_order
    text variant_label_ru
    bool is_primary_in_group
    text[] search_aliases
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
    bigint bundle_size_bytes
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

  login_attempts {
    text ip_address
    timestamptz attempted_at
  }

  inventory_items {
    text command_id PK
    text category_id
    jsonb phrases
    text source_url
    bool deprecated
  }

  editorial_records {
    text command_id PK
    text title_ru
    text status
  }

  content_queue {
    text id PK
    text event_type
    text command_id
    text status
  }

  user_feedback {
    text id PK
    text message
    int rating
    text status
  }

  command_reports {
    text id PK
    text command_id
    text issue_type
    text status
  }
```

---

## 3. Indexes

```sql
CREATE INDEX idx_command_groups_category_sort ON command_groups(category_id, sort_order);
CREATE INDEX idx_commands_group_sort ON commands(group_id, sort_order);
CREATE INDEX idx_commands_category ON commands(category_id);
CREATE INDEX idx_commands_tags ON commands USING GIN(tags);
CREATE INDEX idx_categories_sort ON categories(sort_order);
CREATE INDEX idx_login_attempts_ip ON login_attempts(ip_address, attempted_at);
CREATE INDEX idx_checklist_order ON checklist_items(item_order);
CREATE INDEX idx_editorial_status ON editorial_records(status);
CREATE INDEX idx_content_queue_status ON content_queue(status);
CREATE INDEX idx_user_feedback_status ON user_feedback(status);
CREATE INDEX idx_command_reports_status ON command_reports(status);
```

---

## 4. Publish state

| Table / storage | Role |
| --------------- | ---- |
| `current_manifest` | Pointer to live bundle (single active row) |
| `publish_history` | Audit + rollback source (last 5 on disk) |
| `storage/bundles/` | `content_v{N}.json.gz` files |
| `storage/manifest/` | Affiliate snapshot для public endpoint |

Rollback: update `current_manifest` to previous `content_version` where bundle file still exists.

---

## 5. Seed / import

| Файл | Назначение |
| ---- | ---------- |
| `seed/smart-home-groups-v2.json` | Pilot Умный дом + command_groups (первый dev/staging publish) |
| `seed/full-catalog.json` | Output `tools/content/build_bundle.py` (schema v2) |

Import через admin UI или `POST /admin/api/import/json?mode=merge|replace`.

---

## 6. Backup

- `pg_dump` weekly (cron on VPS)
- Bundle files в storage backup или regenerable через re-publish из draft

---

## 7. Local dev

```yaml
# docker-compose.yml
POSTGRES_DB: alice_commands
POSTGRES_USER: alice
POSTGRES_PASSWORD: alice_dev
```

Flyway применяется автоматически при старте Ktor.

---

*Field mapping → [schema/content-bundle.schema.json](../schema/content-bundle.schema.json)*
