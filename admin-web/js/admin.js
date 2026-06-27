function adminApp() {
  const HEALTH_INTERVAL_MS = 5 * 60 * 1000;
  const REQUEST_TIMEOUT_MS = 30_000;

  return {
    authenticated: false,
    loading: false,
    saving: false,
    error: '',
    formError: '',
    toast: '',
    view: 'dashboard',
    loginForm: { username: '', password: '' },
    dashboard: null,
    categories: [],
    commands: [],
    allCommands: [],
    scenarios: [],
    checklist: [],
    affiliate: [],
    history: [],
    categoryForm: null,
    categoryEditing: false,
    commandForm: null,
    commandEditing: false,
    commandFilter: '',
    scenarioForm: null,
    scenarioEditing: false,
    affiliateForm: null,
    affiliateEditing: false,
    importMode: 'merge',
    importDraft: null,
    importParseError: '',
    importLoading: false,
    importPreviewLoading: false,
    publishedDiff: null,
    diffFilter: 'all',
    diffNeedsReviewOnly: false,
    previewJson: null,
    serverStatus: {
      online: null,
      ready: null,
      database: null,
      storage: null,
      lastCheck: null,
      checking: false,
      error: '',
    },
    healthTimer: null,
    pipeline: null,
    pipelineLoading: false,
    seedImportLoading: false,
    apiDocs: null,
    apiDocsLoading: false,

    async init() {
      this.startHealthPolling();
      try {
        const ok = await this.api('/admin/api/dashboard', { redirectOn401: false });
        if (ok !== null) {
          this.authenticated = true;
          this.dashboard = ok;
          await this.loadAll();
        }
      } catch {
        // session check failed — stay on login
      }
    },

    startHealthPolling() {
      this.checkServerHealth();
      if (this.healthTimer) clearInterval(this.healthTimer);
      this.healthTimer = setInterval(() => this.checkServerHealth(), HEALTH_INTERVAL_MS);
    },

    async checkServerHealth() {
      this.serverStatus.checking = true;
      try {
        const health = await this.fetchJson('/health', { timeout: 10_000, allowError: false });
        this.serverStatus.online = health?.status === 'ok';
        const ready = await this.fetchJson('/ready', { timeout: 10_000, allowError: true });
        this.serverStatus.ready = ready?.status === 'ready';
        this.serverStatus.database = ready?.database ?? null;
        this.serverStatus.storage = ready?.storage ?? null;
        this.serverStatus.error = '';
      } catch (e) {
        this.serverStatus.online = false;
        this.serverStatus.ready = false;
        this.serverStatus.error = this.networkErrorMessage(e);
      } finally {
        this.serverStatus.lastCheck = new Date().toISOString();
        this.serverStatus.checking = false;
      }
    },

    serverStatusLabel() {
      if (this.serverStatus.checking && this.serverStatus.online === null) return 'Проверка…';
      if (!this.serverStatus.online) return 'Сервер недоступен';
      if (!this.serverStatus.ready) return 'Сервер работает, но не готов (DB/storage)';
      return 'Сервер OK';
    },

    serverStatusClass() {
      if (this.serverStatus.online === null) return 'status-unknown';
      if (!this.serverStatus.online) return 'status-down';
      if (!this.serverStatus.ready) return 'status-degraded';
      return 'status-ok';
    },

    formatLastCheck(iso) {
      if (!iso) return '—';
      try {
        return new Date(iso).toLocaleString('ru-RU');
      } catch {
        return iso;
      }
    },

    networkErrorMessage(err) {
      const msg = (err?.message || '').toLowerCase();
      if (err?.name === 'AbortError' || msg.includes('abort')) {
        return 'Таймаут соединения — проверьте VPN/сеть и попробуйте снова';
      }
      if (msg.includes('failed to fetch') || msg.includes('network') || msg.includes('load failed')) {
        return 'Нет связи с сервером (ERR_CONNECTION_RESET / сеть). Попробуйте без VPN или с другим VPN-сервером';
      }
      return err?.message || 'Ошибка сети';
    },

    async fetchJson(path, { timeout = REQUEST_TIMEOUT_MS, allowError = false } = {}) {
      const ctrl = new AbortController();
      const timer = setTimeout(() => ctrl.abort(), timeout);
      try {
        const res = await fetch(path, { credentials: 'include', signal: ctrl.signal });
        const ct = res.headers.get('content-type') || '';
        const body = ct.includes('application/json') ? await res.json().catch(() => ({})) : {};
        if (!res.ok && !(allowError && body?.status)) {
          const e = new Error(body.message || res.statusText);
          e.status = res.status;
          e.code = body.error;
          throw e;
        }
        return body;
      } finally {
        clearTimeout(timer);
      }
    },

    async api(path, { method = 'GET', body, raw = false, redirectOn401 = true, timeout = REQUEST_TIMEOUT_MS } = {}) {
      const opts = { method, credentials: 'include', headers: {}, signal: undefined };
      const ctrl = new AbortController();
      opts.signal = ctrl.signal;
      const timer = setTimeout(() => ctrl.abort(), timeout);
      if (body !== undefined) {
        if (raw) {
          opts.body = body;
          opts.headers['Content-Type'] = 'application/json';
        } else {
          opts.body = JSON.stringify(body);
          opts.headers['Content-Type'] = 'application/json';
        }
      }
      try {
        const res = await fetch(path, opts);
        if (res.status === 401 && redirectOn401) {
          this.authenticated = false;
          return null;
        }
        if (!res.ok) {
          const err = await res.json().catch(() => ({}));
          const e = new Error(err.message || res.statusText);
          e.status = res.status;
          e.code = err.error;
          throw e;
        }
        if (res.status === 204) return null;
        const ct = res.headers.get('content-type') || '';
        if (ct.includes('application/json')) return res.json();
        return res.text();
      } catch (e) {
        if (e.name === 'AbortError') {
          const te = new Error('Таймаут запроса');
          te.name = 'AbortError';
          throw te;
        }
        throw e;
      } finally {
        clearTimeout(timer);
      }
    },

    showToast(msg) {
      this.toast = msg;
      setTimeout(() => { this.toast = ''; }, 3000);
    },

    async runSaving(fn) {
      this.saving = true;
      this.formError = '';
      try {
        await fn();
      } catch (e) {
        this.formError = this.networkErrorMessage(e);
        if (e.status && e.message) this.formError = e.message;
        throw e;
      } finally {
        this.saving = false;
      }
    },

    async login() {
      this.loading = true;
      this.error = '';
      try {
        await this.api('/admin/api/login', { method: 'POST', body: this.loginForm, timeout: 60_000 });
        this.authenticated = true;
        await this.loadAll();
      } catch (e) {
        if (e.status === 429 || e.code === 'rate_limited') {
          this.error = 'Слишком много неудачных попыток. Подождите 15 минут или сбросьте login_attempts в dev.';
        } else if (e.status === 401) {
          this.error = 'Неверный логин или пароль';
        } else {
          this.error = this.networkErrorMessage(e);
        }
      } finally {
        this.loading = false;
      }
    },

    async logout() {
      try {
        await this.api('/admin/api/logout', { method: 'POST' });
      } catch { /* ignore */ }
      if (this.healthTimer) clearInterval(this.healthTimer);
      this.authenticated = false;
    },

    async loadAll() {
      await Promise.all([
        this.loadDashboard(),
        this.loadCategories(),
        this.loadCommands(),
        this.loadAllCommands(),
        this.loadScenarios(),
        this.loadChecklist(),
        this.loadAffiliate(),
        this.loadHistory(),
        this.loadPipeline(),
      ]);
    },

    async loadDashboard() {
      try {
        this.dashboard = await this.api('/admin/api/dashboard');
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      }
    },
    async loadCategories() {
      this.categories = await this.api('/admin/api/categories') || [];
    },
    async loadCommands() {
      const q = this.commandFilter ? `?category_id=${encodeURIComponent(this.commandFilter)}` : '';
      this.commands = await this.api('/admin/api/commands' + q) || [];
    },
    async loadAllCommands() {
      this.allCommands = await this.api('/admin/api/commands') || [];
    },
    async loadScenarios() {
      this.scenarios = await this.api('/admin/api/scenario-templates') || [];
    },
    async loadChecklist() {
      this.checklist = await this.api('/admin/api/checklist-items') || [];
    },
    async loadAffiliate() {
      this.affiliate = await this.api('/admin/api/affiliate-blocks') || [];
    },
    async loadHistory() {
      this.history = await this.api('/admin/api/publish/history') || [];
    },

    async loadPipeline() {
      if (!this.authenticated) return;
      this.pipelineLoading = true;
      try {
        this.pipeline = await this.api('/admin/api/content/pipeline');
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      } finally {
        this.pipelineLoading = false;
      }
    },

    async loadApiDocs() {
      if (this.apiDocs) return;
      this.apiDocsLoading = true;
      try {
        this.apiDocs = await this.api('/admin/api/docs');
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      } finally {
        this.apiDocsLoading = false;
      }
    },

    async importSeedFromServer(mode = 'merge') {
      if (mode === 'replace' && !confirm('Replace all заменит весь каталог draft. Продолжить?')) return;
      this.seedImportLoading = true;
      this.error = '';
      try {
        await this.api(`/admin/api/content/import-seed?mode=${mode}`, { method: 'POST', body: {} });
        this.showToast('Seed импортирован в draft — проверьте Import/Publish');
        await this.loadAll();
        this.view = 'import';
      } catch (e) {
        this.error = e.message || this.networkErrorMessage(e);
      } finally {
        this.seedImportLoading = false;
      }
    },

    copyText(text) {
      navigator.clipboard?.writeText(text).then(() => this.showToast('Скопировано')).catch(() => {
        this.showToast('Не удалось скопировать');
      });
    },

    showCategoryForm() {
      this.formError = '';
      this.categoryEditing = false;
      this.categoryForm = {
        id: '', title_ru: '', sort_order: (this.categories.length + 1),
        featured: false, icon_key: '', description_ru: '',
        source_url: 'https://alice.yandex.ru/support/ru/station/skills/', device_types: [],
      };
    },
    editCategory(c) {
      this.formError = '';
      this.categoryEditing = true;
      this.categoryForm = { ...c };
    },
    async saveCategory() {
      await this.runSaving(async () => {
        const c = this.categoryForm;
        if (this.categoryEditing) {
          await this.api(`/admin/api/categories/${c.id}`, { method: 'PUT', body: c });
        } else {
          await this.api('/admin/api/categories', { method: 'POST', body: c });
        }
        this.categoryForm = null;
        await this.loadCategories();
        this.showToast('Категория сохранена');
      });
    },
    async deleteCategory(id) {
      if (!confirm(`Удалить категорию ${id}?`)) return;
      try {
        await this.api(`/admin/api/categories/${id}`, { method: 'DELETE' });
        await this.loadCategories();
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      }
    },
    async moveCategory(c, delta) {
      try {
        const sorted = [...this.categories].sort((a, b) => a.sort_order - b.sort_order);
        const idx = sorted.findIndex(x => x.id === c.id);
        const swap = idx + delta;
        if (swap < 0 || swap >= sorted.length) return;
        [sorted[idx], sorted[swap]] = [sorted[swap], sorted[idx]];
        await this.api('/admin/api/categories/reorder', {
          method: 'PUT',
          body: { ordered_ids: sorted.map(x => x.id) },
        });
        await this.loadCategories();
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      }
    },

    showCommandForm() {
      this.formError = '';
      this.commandEditing = false;
      const now = new Date().toISOString();
      this.commandForm = {
        id: '', category_id: this.categories[0]?.id || '', title_ru: '',
        phrasesText: '', effect_description_ru: '', source_url: 'https://alice.yandex.ru/support/ru/station/skills/',
        requires_alice_word: true, requires_plus: false, tagsText: '', updated_at: now,
      };
    },
    editCommand(cmd) {
      this.formError = '';
      this.commandEditing = true;
      this.commandForm = {
        ...cmd,
        phrasesText: (cmd.phrases || []).join('\n'),
        tagsText: (cmd.tags || []).join(', '),
      };
    },
    async saveCommand() {
      await this.runSaving(async () => {
        const f = this.commandForm;
        const body = {
          id: f.id,
          category_id: f.category_id,
          title_ru: f.title_ru,
          phrases: f.phrasesText.split('\n').map(s => s.trim()).filter(Boolean),
          effect_description_ru: f.effect_description_ru,
          requires_alice_word: f.requires_alice_word,
          requires_plus: f.requires_plus,
          device_types: f.device_types || [],
          related_command_ids: f.related_command_ids || [],
          source_url: f.source_url,
          updated_at: new Date().toISOString(),
          tags: f.tagsText.split(',').map(s => s.trim()).filter(Boolean),
        };
        if (this.commandEditing) {
          await this.api(`/admin/api/commands/${body.id}`, { method: 'PUT', body });
        } else {
          await this.api('/admin/api/commands', { method: 'POST', body });
        }
        this.commandForm = null;
        await Promise.all([this.loadCommands(), this.loadAllCommands(), this.loadDashboard()]);
        this.showToast('Команда сохранена');
      });
    },
    async deleteCommand(id) {
      if (!confirm(`Удалить команду ${id}?`)) return;
      try {
        await this.api(`/admin/api/commands/${id}`, { method: 'DELETE' });
        await Promise.all([this.loadCommands(), this.loadAllCommands()]);
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      }
    },

    showScenarioForm() {
      this.formError = '';
      this.scenarioEditing = false;
      this.scenarioForm = {
        id: '', title_ru: '', trigger_ru: '', actionsText: '', examplesText: '',
        audience: 'all', source_url: 'https://alice.yandex.ru/support/ru/smart-home/scenarios/create',
      };
    },
    editScenario(s) {
      this.formError = '';
      this.scenarioEditing = true;
      this.scenarioForm = {
        ...s,
        actionsText: (s.actions_ru || []).join('\n'),
        examplesText: (s.example_phrases || []).join('\n'),
      };
    },
    async saveScenario() {
      await this.runSaving(async () => {
        const f = this.scenarioForm;
        const body = {
          id: f.id,
          title_ru: f.title_ru,
          trigger_ru: f.trigger_ru,
          actions_ru: f.actionsText.split('\n').map(s => s.trim()).filter(Boolean),
          example_phrases: f.examplesText.split('\n').map(s => s.trim()).filter(Boolean),
          audience: f.audience,
          deep_link_hint: f.deep_link_hint,
          source_url: f.source_url,
        };
        if (this.scenarioEditing) {
          await this.api(`/admin/api/scenario-templates/${body.id}`, { method: 'PUT', body });
        } else {
          await this.api('/admin/api/scenario-templates', { method: 'POST', body });
        }
        this.scenarioForm = null;
        await this.loadScenarios();
        this.showToast('Шаблон сохранён');
      });
    },
    async deleteScenario(id) {
      if (!confirm(`Удалить шаблон ${id}?`)) return;
      try {
        await this.api(`/admin/api/scenario-templates/${id}`, { method: 'DELETE' });
        await this.loadScenarios();
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      }
    },

    async saveChecklist() {
      await this.runSaving(async () => {
        await this.api('/admin/api/checklist-items', { method: 'PUT', body: this.checklist });
        this.showToast('Чеклист сохранён');
        await this.loadChecklist();
      });
    },

    showAffiliateForm() {
      this.formError = '';
      this.affiliateEditing = false;
      this.affiliateForm = {
        id: '', title_ru: '', context_category_id: 'smart_home',
        erid: '', advertiser_name: '', productsText: '',
      };
    },
    editAffiliate(b) {
      this.formError = '';
      this.affiliateEditing = true;
      this.affiliateForm = {
        ...b,
        productsText: (b.products || []).map(p => `${p.title_ru}|${p.market_url}|${p.price_hint || ''}`).join('\n'),
      };
    },
    parseProducts(text) {
      return text.split('\n').map(line => line.trim()).filter(Boolean).map(line => {
        const [title_ru, market_url, price_hint] = line.split('|');
        return { title_ru: title_ru?.trim(), market_url: market_url?.trim(), price_hint: price_hint?.trim() || null };
      }).filter(p => p.title_ru && p.market_url);
    },
    async saveAffiliate() {
      await this.runSaving(async () => {
        const f = this.affiliateForm;
        const body = {
          id: f.id,
          title_ru: f.title_ru,
          context_category_id: f.context_category_id,
          erid: f.erid,
          advertiser_name: f.advertiser_name,
          products: this.parseProducts(f.productsText),
        };
        if (this.affiliateEditing) {
          await this.api(`/admin/api/affiliate-blocks/${body.id}`, { method: 'PUT', body });
        } else {
          await this.api('/admin/api/affiliate-blocks', { method: 'POST', body });
        }
        this.affiliateForm = null;
        await this.loadAffiliate();
        this.showToast('Affiliate блок сохранён');
      });
    },
    async deleteAffiliate(id) {
      if (!confirm(`Удалить affiliate блок ${id}?`)) return;
      try {
        await this.api(`/admin/api/affiliate-blocks/${id}`, { method: 'DELETE' });
        await this.loadAffiliate();
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      }
    },

    async previewBundle() {
      try {
        const bundle = await this.api('/admin/api/preview/bundle');
        const text = JSON.stringify(bundle, null, 2);
        const blob = new Blob([text], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'preview-bundle.json';
        a.style.display = 'none';
        document.body.appendChild(a);
        a.click();
        a.remove();
        setTimeout(() => URL.revokeObjectURL(url), 5000);
        this.previewJson = text;
        this.showToast('Preview: скачан preview-bundle.json');
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      }
    },

    async doPublish() {
      if (!confirm('Опубликовать контент для всех пользователей app?')) return;
      this.loading = true;
      try {
        const r = await this.api('/admin/api/publish', { method: 'POST', body: {} });
        this.showToast(`Опубликовано v${r.contentVersion}`);
        await this.loadAll();
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      } finally {
        this.loading = false;
      }
    },

    async rollback(version) {
      if (!confirm(`Откатить на v${version}?`)) return;
      try {
        await this.api('/admin/api/publish/rollback', { method: 'POST', body: { content_version: version } });
        this.showToast(`Откат на v${version}`);
        await this.loadAll();
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      }
    },

    formatBytes(bytes) {
      if (bytes < 1024) return `${bytes} B`;
      if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
      return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    },

    diffImportIds(incoming, current) {
      const currentIds = new Set((current || []).map((x) => x.id));
      const add = [];
      const update = [];
      for (const item of incoming || []) {
        if (!item?.id) continue;
        if (currentIds.has(item.id)) update.push(item.id);
        else add.push(item.id);
      }
      return { add, update, addCount: add.length, updateCount: update.length };
    },

    buildImportPreview(parsed) {
      const counts = {
        categories: (parsed.categories || []).length,
        commands: (parsed.commands || []).length,
        scenario_templates: (parsed.scenario_templates || []).length,
        checklist_items: (parsed.checklist_items || []).length,
      };
      const preview = { counts, schemaVersion: parsed.schema_version ?? null };
      if (this.importMode === 'merge') {
        preview.mergeDiff = {
          categories: this.diffImportIds(parsed.categories, this.categories),
          commands: this.diffImportIds(parsed.commands, this.allCommands),
          scenario_templates: this.diffImportIds(parsed.scenario_templates, this.scenarios),
          checklist_items: this.diffImportIds(parsed.checklist_items, this.checklist),
        };
      }
      return preview;
    },

    importSummaryRows() {
      const c = this.importDraft?.preview?.counts;
      if (!c) return [];
      return [
        { label: 'Категории', value: c.categories },
        { label: 'Команды', value: c.commands },
        { label: 'Шаблоны сценариев', value: c.scenario_templates },
        { label: 'Чеклист', value: c.checklist_items },
      ];
    },

    importDiffBlocks() {
      const d = this.importDraft?.preview?.mergeDiff;
      if (!d) return [];
      const labels = {
        categories: 'Категории',
        commands: 'Команды',
        scenario_templates: 'Шаблоны',
        checklist_items: 'Чеклист',
      };
      return Object.entries(labels).map(([key, label]) => {
        const block = d[key];
        const total = block.addCount + block.updateCount;
        const parts = [];
        if (block.addCount) parts.push(`+${block.addCount} новых`);
        if (block.updateCount) parts.push(`${block.updateCount} обновится`);
        return {
          label, total, text: parts.join(', ') || '—',
          addIds: block.add, updateIds: block.update,
        };
      });
    },

    formatIdList(ids, max = 8) {
      if (!ids?.length) return '';
      if (ids.length <= max) return ids.join(', ');
      return `${ids.slice(0, max).join(', ')} … (+${ids.length - max})`;
    },

    importDiffHasChanges() {
      return this.importDiffBlocks().some((b) => b.total > 0);
    },

    refreshImportPreview() {
      if (!this.importDraft?.parsed) return;
      this.importDraft.preview = this.buildImportPreview(this.importDraft.parsed);
    },

    clearImportFile() {
      this.importDraft = null;
      this.importParseError = '';
      this.publishedDiff = null;
      this.diffFilter = 'all';
      this.diffNeedsReviewOnly = false;
      document.querySelectorAll('.import-file-picker input[type="file"]').forEach((el) => { el.value = ''; });
    },

    async loadPublishedDiff(text) {
      this.importPreviewLoading = true;
      this.publishedDiff = null;
      try {
        this.publishedDiff = await this.api('/admin/api/import/preview', {
          method: 'POST',
          body: text,
          raw: true,
        });
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      } finally {
        this.importPreviewLoading = false;
      }
    },

    publishedDiffSections() {
      if (!this.publishedDiff) return [];
      return [
        { key: 'commands', label: 'Команды', section: this.publishedDiff.commands },
        { key: 'categories', label: 'Категории', section: this.publishedDiff.categories },
        { key: 'scenario_templates', label: 'Шаблоны', section: this.publishedDiff.scenario_templates },
        { key: 'checklist_items', label: 'Чеклист', section: this.publishedDiff.checklist_items },
      ];
    },

    filteredDiffItems(section) {
      if (!section?.items) return [];
      let items = section.items;
      if (this.diffFilter !== 'all') {
        items = items.filter((i) => i.change === this.diffFilter);
      }
      if (this.diffNeedsReviewOnly) {
        items = items.filter((i) => (i.tags || []).includes('needs_review'));
      }
      return items;
    },

    diffFieldEntries(item) {
      if (!item?.field_diffs) return [];
      return Object.entries(item.field_diffs).map(([field, diff]) => ({ field, old: diff.old, new: diff.new }));
    },

    publishedDiffSummaryText() {
      const s = this.publishedDiff?.summary;
      if (!s) return '';
      const base = this.publishedDiff.base_content_version != null
        ? `vs published v${this.publishedDiff.base_content_version}`
        : `vs ${this.publishedDiff.base}`;
      return `${base}: +${s.added} / ~${s.changed} / −${s.removed}`;
    },

    async selectImportFile(ev) {
      const file = ev.target.files?.[0];
      if (!file) return;
      this.importParseError = '';
      this.error = '';
      let text;
      let parsed;
      try {
        text = await file.text();
        parsed = JSON.parse(text);
      } catch {
        this.importDraft = { name: file.name, sizeBytes: file.size, text: null, parsed: null, preview: null };
        this.importParseError = 'Некорректный JSON — проверьте синтаксис файла';
        return;
      }
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        this.importDraft = { name: file.name, sizeBytes: file.size, text: null, parsed: null, preview: null };
        this.importParseError = 'Ожидается JSON-объект формата content bundle';
        return;
      }
      const hasContent = ['categories', 'commands', 'scenario_templates', 'checklist_items']
        .some((k) => Array.isArray(parsed[k]) && parsed[k].length > 0);
      if (!hasContent) {
        this.importDraft = { name: file.name, sizeBytes: file.size, text: null, parsed: null, preview: null };
        this.importParseError = 'Файл пустой: нужен хотя бы один непустой массив (categories, commands, …)';
        return;
      }
      const preview = this.buildImportPreview(parsed);
      this.importDraft = { name: file.name, sizeBytes: file.size, text, parsed, preview };
      await this.loadPublishedDiff(text);
    },

    async doImport() {
      if (!this.importDraft?.text || this.importParseError) return;
      if (this.importMode === 'replace') {
        const d = this.dashboard?.draft || {};
        const msg = `Replace all заменит каталог bundle (${d.categoriesCount || 0} кат., ${d.commandsCount || 0} команд, ${d.scenarioTemplatesCount || 0} шаблонов, ${d.checklistItemsCount || 0} чеклиста). Affiliate не затрагивается. Продолжить?`;
        if (!confirm(msg)) return;
      }
      this.importLoading = true;
      this.error = '';
      try {
        await this.api(`/admin/api/import/json?mode=${this.importMode}`, {
          method: 'POST', body: this.importDraft.text, raw: true, timeout: 120_000,
        });
        this.showToast('Import выполнен — проверьте draft и сделайте Publish');
        this.clearImportFile();
        await this.loadAll();
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      } finally {
        this.importLoading = false;
      }
    },
  };
}
