# Infrastructure — alice-commands-api

**Провайдер:** Selectel · **Аккаунт:** rybak.m.yu@gmail.com (ID 626746)  
**Секреты:** только в панели Selectel и `/opt/alice-api/.env` — **не коммитить в git**.

---

## 1. VPS (staging)

| Параметр | Значение |
| -------- | -------- |
| Имя в панели | Miron |
| Hostname | `miron` |
| Публичный IP | `161.104.46.92` |
| Регион | Moscow / ru-2c |
| OS | Ubuntu 24.04 LTS |
| vCPU / RAM / Disk | 2 / 2 GB / 40 GB |
| UUID | `dbe8ef96-b328-4dea-913a-3de50faaecc8` |

**SSH:**

```powershell
ssh -i $env:USERPROFILE\.ssh\id_ed25519_selectel root@161.104.46.92
```

| Файл | Назначение |
| ---- | ---------- |
| `C:\Users\rybak\.ssh\id_ed25519_selectel` | Приватный ключ |
| `C:\Users\rybak\.ssh\id_ed25519_selectel.pub` | Публичный ключ на VPS |

Пароль root — резерв в панели Selectel → Miron → «Подключение по SSH».

**Пути на сервере:**

| Путь | Назначение |
| ---- | ---------- |
| `/opt/alice-api/app/` | Ktor installDist |
| `/opt/alice-api/.env` | Runtime env |
| `/opt/alice-api/deploy/` | nginx, systemd |
| `/opt/alice-api/storage/bundles/` | Published bundles |
| `/opt/alice-api/storage/manifest/` | Manifest |
| `/var/log/alice-api/app.log` | Логи |

**Services:** `systemctl status alice-api nginx` · nginx config: `/etc/nginx/sites-available/alice-api` (HTTPS Let's Encrypt + proxy → `:8080`).

---

## 2. DNS и доступ из России

**Домен:** `alicecommands.ru` · NS: **Cloudflare** (`kaiser.ns.cloudflare.com`, `angelina.ns.cloudflare.com`).

### Рабочая конфигурация (2026-06)

| Type | Name | Content | Proxy |
| ---- | ---- | ------- | ----- |
| A | `staging-api` | `161.104.46.92` | **DNS only** (серое облако) |

**Проверка:**

```powershell
nslookup staging-api.alicecommands.ru 8.8.8.8   # → 161.104.46.92
curl https://staging-api.alicecommands.ru/health # → {"status":"ok"}
```

Предупреждения Cloudflare («Proxying required», missing www/root/email) — **можно игнорировать** для API subdomain.

### Почему нельзя Cloudflare Proxy (оранжевое облако) для API

С июня 2025 российские ISP **дросселируют** трафик через Cloudflare CDN (~16 KB/ответ). Симптомы: сайт/API только с VPN, `ERR_CONNECTION_RESET`, таймауты.

**VPS Selectel доступен из РФ напрямую** — API и admin должны резолвиться на `161.104.46.92` без CF proxy.

Для prod: запись **`api`** → A `161.104.46.92`, тоже **DNS only**.

### Скрипт смены DNS (опционально)

```powershell
# scripts\.env: CF_API_TOKEN=...
.\scripts\cloudflare-dns-direct.ps1
```

### Альтернатива: DNS в Selectel

Если уйти с Cloudflare NS — делегировать на `a/b/c/d.ns.selectel.ru` у регистратора. Зона в Selectel: ID `c797cd8c-47f8-4279-a044-418c6a105441`. Статус «не делегирована» в Selectel **нормален**, пока NS у Cloudflare.

---

## 3. URLs

| Среда | URL |
| ----- | --- |
| Staging API | https://staging-api.alicecommands.ru |
| Admin | https://staging-api.alicecommands.ru/admin |
| Manifest | https://staging-api.alicecommands.ru/v1/content/manifest |
| Health / Ready | `/health`, `/ready` |

**Admin login:** `ADMIN_USERNAME` из `/opt/alice-api/.env` (staging: `miron`).

**Android staging:** `CONTENT_API_BASE_URL=https://staging-api.alicecommands.ru`

---

## 4. Операции

### Деплой backend

```powershell
Copy-Item scripts\.env.example scripts\.env   # SSH_KEY_PATH, SSH_HOST
.\scripts\deploy-staging.ps1
```

`installDist` → scp → nginx reload → `systemctl restart alice-api`.

### Content pipeline

```powershell
.\scripts\update-content.ps1    # build → validate → push-draft → verify
```

Или в admin: **Контент** → import seed (если `CONTENT_SEED_PATH` на сервере) → **Publish**.

### Логи и диагностика

```bash
ssh -i ~/.ssh/id_ed25519_selectel root@161.104.46.92
journalctl -u alice-api -n 100 --no-pager
tail -100 /var/log/alice-api/app.log
curl -sS localhost:8080/ready
```

---

## 5. Локальные env (gitignored)

| Файл | Назначение |
| ---- | ---------- |
| `scripts/.env` | `STAGING_API_URL`, admin creds, `SSH_*`, `CF_API_TOKEN` |
| `/opt/alice-api/.env` | Runtime VPS |
| `.env` | Local dev |

Шаблоны: `scripts/.env.example`, `deploy/.env.staging.example`, `.env.example`.

---

*См. [DEPLOYMENT.md](DEPLOYMENT.md), [RUNBOOK-PUBLISH.md](RUNBOOK-PUBLISH.md), [CONTENT-UPDATE.md](CONTENT-UPDATE.md)*
