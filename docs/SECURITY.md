# Security — alice-commands-api

---

## 1. Threat model (v1.0)

| Угроза | Mitigation |
| ------ | ---------- |
| Admin brute force | Rate limit login; strong password |
| Session hijack | HttpOnly cookie; Secure; SameSite=Lax |
| Public API abuse | CDN cache; optional Cloudflare rate limit |
| SQL injection | Exposed parameterized queries |
| Secret leak | `.env` not in git; GitHub secrets for CI |
| MITM | HTTPS only prod/staging |

**Out of scope v1.0:** DDoS enterprise-grade, WAF rules beyond Cloudflare free.

---

## 2. Admin credentials

| Rule | |
| ---- | -- |
| Storage | `ADMIN_PASSWORD` bcrypt hash in env **or** hash in DB |
| Rotation | Manual при компрометации |
| Sharing | Solo — один operator |
| Default | Запрещён `change-me` в prod (startup check) |

---

## 3. Session

- Server-side session ID in cookie `alice_admin_session`
- TTL: 24h sliding
- Logout invalidates session

---

## 4. Public API

- No auth required (read-only catalog)
- No user PII collected
- AppMetrica — on client only

---

## 5. 152-ФЗ

- Backend не хранит данные пользователей app
- Admin username — служебная учётка оператора
- Policy app описывает sync с API (AliceCommands in-app privacy)

---

## 6. Affiliate compliance

- ERID + «Реклама» — validated before publish (warn if missing)
- CPA links only https

---

## 7. VPS hardening (checklist)

- [ ] SSH key only, no password root
- [ ] ufw: 22, 80, 443
- [ ] fail2ban ssh
- [ ] Auto security updates
- [ ] PostgreSQL listen localhost only

---

## 8. Secrets in repo

**Never commit:** `.env`, passwords, SESSION_SECRET, DB passwords.

Use `.env.example` as template.

---

*См. [BACKEND-REQUIREMENTS.md](BACKEND-REQUIREMENTS.md) B04*
