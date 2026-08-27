# Production cutover — alice-commands-api

**as_of:** 2026-07-13  
**Статус:** **✅ ЗАВЕРШЁН** — prod LIVE, приложение в RuStore на `https://api.alicecommands.ru`  
**Операционный канон:** **[PRODUCTION.md](PRODUCTION.md)** (staging → verify → copy → verify)

---

## 1. Итог cutover

| Компонент | Staging | Prod |
| --------- | ------- | ---- |
| API URL | `https://staging-api.alicecommands.ru` ✅ | `https://api.alicecommands.ru` ✅ |
| CDN icons | зеркало + `cdn.alicecommands.ru` ✅ | `https://cdn.alicecommands.ru` ✅ |
| БД | `alice_commands_staging` | `alice_commands` ✅ |
| Deploy | `deploy-staging.ps1` ✅ | `deploy-prod.ps1` ✅ |
| Verify | `verify-staging.ps1` ✅ | `verify-prod.ps1` ✅ |
| Content sync | — | `copy-staging-to-prod.ps1` ✅ |
| Android app default | staging (debug) | **prod** (release) ✅ |

**Prod снимок (2026-07-13):** `content_version` **10**, **885** команд, **13** категорий, **80** групп, smarthome **35+36**, `verify-prod.ps1` OK.

**Topology:** один VPS, staging `:8080` / prod `:8081`, отдельные `storage` / `storage-prod`.

---

## 2. Staging vs prod (что меняется)

| | Staging | Prod |
| - | ------- | ---- |
| `APP_ENV` | `staging` | `prod` |
| PostgreSQL | `alice_commands_staging` | `alice_commands` |
| Ktor port | `8080` (`alice-api`) | `8081` (`alice-api-prod`) |
| Public API | `staging-api.alicecommands.ru` | `api.alicecommands.ru` |
| `ICON_PUBLIC_BASE_URL` | `https://staging-api.alicecommands.ru` | `https://cdn.alicecommands.ru` |
| Назначение | QA, черновики, эксперименты | **Пользователи в production** |

---

## 3. Чеклист cutover (архив)

### 3.1 DNS и TLS

- [x] Cloudflare A `api`, `cdn`, `staging-api` → VPS (DNS only)
- [x] certbot `api.alicecommands.ru` + `cdn.alicecommands.ru`

### 3.2 VPS / backend

- [x] `deploy/nginx-prod.conf`, `alice-api-prod.service`, `.env.prod`
- [x] `deploy-prod.ps1` — `alice-api-prod` :8081
- [x] `GET https://api.alicecommands.ru/health` → 200

### 3.3 Контент

- [x] Staging `verify-staging.ps1` green
- [x] `copy-staging-to-prod.ps1` — bundle + smarthome
- [x] Manifest: 13 categories, 885 commands
- [x] `verify-prod.ps1` green
- [x] CDN icons smoke OK

### 3.4 Android app

- [x] Release → `https://api.alicecommands.ru`
- [x] Первый пользовательский релиз (RuStore)

### 3.5 Affiliate / ERID

`erid` / `advertiser_name` — опционально (политика v1.0). См. `SECURITY.md` §8.

---

## 4. Команды (после cutover — routine)

```powershell
.\scripts\verify-staging.ps1
.\scripts\copy-staging-to-prod.ps1
.\scripts\verify-prod.ps1
```

Backend: `deploy-staging.ps1` → smoke → `deploy-prod.ps1`.

Подробно: **[PRODUCTION.md](PRODUCTION.md)**.

---

## 5. Откат

| Симптом | Действие |
| ------- | -------- |
| Плохой publish | Admin prod → **Rollback** |
| Smarthome рассинхрон | `copy-staging-to-prod.ps1` |
| Prod API down | `journalctl -u alice-api-prod`; redeploy предыдущего коммита |

---

*Cutover завершён 2026-07-13. Дальнейшие изменения — только по [PRODUCTION.md](PRODUCTION.md).*
