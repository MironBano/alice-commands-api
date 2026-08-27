# Deployment — alice-commands-api

**Prod:** **LIVE** (2026-07-13) — `api.alicecommands.ru` · ops: [PRODUCTION.md](PRODUCTION.md)  
**Бюджет:** ~625 ₽/мес · **Staging + prod:** Selectel VPS

---

## 1. Topology

```
Android / Admin browser
        │
        ▼
  Cloudflare DNS (DNS only для api.* — без CDN proxy)
        │
        ▼
  Selectel VPS (161.104.46.92)
    nginx :443 (Let's Encrypt)
        ├── staging-api → proxy :8080 + static /icons/
        └── cdn → static /icons/v1/ only (после DNS + cert)
        │
        ▼
    Ktor :8080
        ├── PostgreSQL (localhost)
        ├── /opt/alice-api/storage/bundles/
        └── /opt/alice-api/storage/icons/v1/
```

**Важно для РФ:** subdomain `staging-api` / `api` — **DNS only** (серое облако CF). Orange cloud ломает доступ без VPN. См. [INFRASTRUCTURE.md](INFRASTRUCTURE.md) §2.

Local dev: Ktor на хосте + PostgreSQL в Docker (`docker compose up -d`).

---

## 2. VPS

**Текущий staging:** Selectel Miron, `161.104.46.92` — **[INFRASTRUCTURE.md](INFRASTRUCTURE.md)** (SSH, DNS, URLs, логи).

| Параметр | Staging (факт) |
| -------- | -------------- |
| Провайдер | Selectel |
| RAM / CPU | 2 GB / 2 vCPU |
| OS | Ubuntu 24.04 LTS |
| Домен | `staging-api.alicecommands.ru` |

---

## 3. Артефакты (`deploy/`)

| Файл | Назначение |
| ---- | ---------- |
| `deploy/.env.staging.example` | Шаблон `/opt/alice-api/.env` |
| `deploy/alice-api.service` | systemd unit |
| `deploy/nginx-staging.conf` | HTTPS + proxy → `:8080`, static `/icons/`, 64m body |
| `deploy/nginx-prod.conf` | HTTPS + proxy → `:8081` (`alice-api-prod`), prod API |
| `deploy/alice-api-prod.service` | systemd unit prod (`EnvironmentFile=.env.prod`) |
| `deploy/.env.prod.example` | Шаблон `/opt/alice-api/.env.prod` |
| `deploy/nginx-cdn.conf` | HTTPS vhost `cdn.alicecommands.ru` → static icons |
| `deploy/nginx-cdn-bootstrap.conf` | HTTP-only bootstrap до certbot |
| `deploy/remote-setup.sh` | Bootstrap VPS (Java 21, PG, nginx, certbot, ufw) |

---

## 4. Деплой backend (routine)

```powershell
Copy-Item scripts\.env.example scripts\.env
# SSH_KEY_PATH, SSH_HOST=root@161.104.46.92, CF_API_TOKEN
.\scripts\deploy-staging.ps1
```

**Production (отдельный сервис :8081, staging не трогаем):**

```powershell
.\scripts\deploy-prod.ps1
.\scripts\copy-staging-to-prod.ps1
.\scripts\verify-prod.ps1
```

См. [PROD-CUTOVER.md](PROD-CUTOVER.md).

Вручную: `gradlew :server:installDist` → scp → `systemctl restart alice-api`.

Первичная настройка VPS (один раз):

```bash
sudo bash /opt/alice-api/deploy/remote-setup.sh staging-api.alicecommands.ru
```

---

## 5. Cloudflare

| Задача | Настройка |
| ------ | --------- |
| DNS | NS Cloudflare для `alicecommands.ru` |
| API records | A → VPS IP, **DNS only** |
| CDN / cache | **Не использовать** для API (throttle в РФ) |
| Предупреждения CF | www/root/email — игнорировать для API-only setup |

Скрипты: `scripts/cloudflare-dns-direct.ps1`, `scripts/setup-cdn.ps1` (нужен `CF_API_TOKEN` в `scripts/.env`).

**Иконки:** отдельный subdomain `cdn` — тот же VPS, **DNS only** (не CF proxy). На staging иконки доступны и через `staging-api.../icons/v1/` (зеркало).

---

## 6. Staging vs prod

| | Staging | Prod |
| - | ------- | ---- |
| `APP_ENV` | `staging` | `prod` |
| DB | `alice_commands_staging` | `alice_commands` |
| URL | `https://staging-api.alicecommands.ru` | `https://api.alicecommands.ru` |
| Icons URL | `https://staging-api.../icons/v1/` | `https://cdn.alicecommands.ru/icons/v1/` |
| `ICON_PUBLIC_BASE_URL` | `https://staging-api.alicecommands.ru` | `https://cdn.alicecommands.ru` |
| DNS | A → VPS, DNS only | A → VPS, DNS only (+ `cdn` A) |
| Admin | `/admin` | `/admin` |

---

## 7. CI

[`.github/workflows/validate-content.yml`](../.github/workflows/validate-content.yml) — validate content on PR/push. Deploy VPS — manual / `deploy-staging.ps1`.

Content publish — через admin **Publish** или `update-content.ps1` → review → Publish.

---

## 8. Мониторинг

| Check | URL / команда |
| ----- | ------------- |
| Liveness | `GET /health` |
| Readiness | `GET /ready` (503 если DB/storage down) |
| Admin health bar | polling каждые 5 мин в UI |
| Logs | `/var/log/alice-api/app.log`, `journalctl -u alice-api` |

UptimeRobot: мониторить `/health` на **прямом URL** (не через CF proxy).

---

## 9. Бюджет

| Статья | ₽/мес |
| ------ | ----- |
| VPS Selectel 2GB | ~600 |
| Домен | ~25 |
| Cloudflare DNS | 0 |
| **Итого** | **~625** |

---

*См. [INFRASTRUCTURE.md](INFRASTRUCTURE.md), [SECURITY.md](SECURITY.md), [RUNBOOK-PUBLISH.md](RUNBOOK-PUBLISH.md), [PROD-CUTOVER.md](PROD-CUTOVER.md)*
