function adminApp() {
  const HEALTH_INTERVAL_MS = 5 * 60 * 1000;
  const REQUEST_TIMEOUT_MS = 30_000;

  /** Russian labels for analytics event_name. tip — только если название само себя не объясняет. */
  const ANALYTICS_EVENT_LABELS = {
    daily_active: { label: 'Открыл приложение сегодня', tip: 'Считается один раз в день на установку. По этим событиям строится «активность».' },
    session_start: { label: 'Начал сессию' },
    session_end: { label: 'Закончил сессию' },
    app_foreground: { label: 'Вернулся в приложение', tip: 'Часто при каждом возврате из фона — в топе это нормально.' },
    app_background: { label: 'Ушёл в фон' },
    time_in_app_tick: { label: 'Тик «время в приложении»', tip: 'Служебный счётчик, не действие пользователя.' },
    screen_view: { label: 'Открыл экран' },
    ui_click: { label: 'Нажал элемент', tip: 'В «Что нажимали» можно посмотреть, по каким кнопкам кликали.' },
    tab_select: { label: 'Выбрал вкладку' },
    content_sync: { label: 'Скачал каталог', tip: 'Обычно при запуске приложения — в топе это нормально.' },
    command_view: { label: 'Открыл команду', tip: 'Смотрите params.source: откуда открыли (catalog_cod, quick, search, related…).' },
    command_tts: { label: 'Озвучил команду' },
    first_value_tts: { label: 'Первое озвучивание', tip: 'Первый TTS после установки — ключевая ценность.' },
    command_copy: { label: 'Скопировал команду' },
    command_share: { label: 'Поделился командой', tip: 'First-class share; не путать с ui_click.' },
    favorite_add: { label: 'Добавил в избранное' },
    favorite_remove: { label: 'Убрал из избранного' },
    favorite_list_create: { label: 'Создал список избранного' },
    favorite_list_delete: { label: 'Удалил список избранного' },
    search: { label: 'Поиск', tip: 'Текст запроса не сохраняется — только длина и число результатов. Zero-results = results_count=0.' },
    search_result_click: { label: 'Кликнул результат поиска' },
    category_click: { label: 'Выбрал категорию', tip: 'params.featured = true|false (избранные на каталоге vs все).' },
    cod_impression: { label: 'Увидел Команду дня', tip: 'Impression карточки CoD; служебное в топе «Действия».' },
    cod_open: { label: 'Открыл Команду дня' },
    scenario_open: { label: 'Открыл сценарий', tip: 'params.template_id.' },
    smarthome_tab_select: { label: 'Вкладка Умного дома', tip: 'params.tab = commands|templates|devices.' },
    filter_change: { label: 'Сменил фильтр', tip: 'Чипы категории: group_id и/или device_type.' },
    widget_shown: { label: 'Показан виджет', tip: 'Служебное; в топе «Действия» скрыто.' },
    widget_open: { label: 'Открыл из виджета' },
    paywall_view: { label: 'Увидел экран покупки' },
    paywall_dismiss: { label: 'Закрыл экран покупки' },
    pro_gate_shown: { label: 'Увидел ограничение Pro' },
    pro_gate_to_paywall: { label: 'Перешёл с ограничения к покупке' },
    pro_purchase_start: { label: 'Начал покупку Pro' },
    pro_purchase: { label: 'Оплата Pro прошла' },
    pro_activated: { label: 'Pro включился', tip: 'Может быть после покупки или после восстановления покупок.' },
    pro_restore: { label: 'Проверка покупок', tip: 'Приложение само проверяет покупки при каждом запуске. Это не «пользователь нажал Восстановить».' },
    rating_prompt_shown: { label: 'Попросили оценить приложение' },
    rating_prompt_ignored: { label: 'Игнорировал оценку' },
    rating_evaluate_skipped: { label: 'Оценка пропущена (guard)' },
    rating_dismiss: { label: 'Закрыл оценку' },
    rating_star_selected: { label: 'Выбрал оценку' },
    rating_low_feedback_submit: { label: 'Отправил отзыв (низкая оценка)' },
    rating_low_feedback_fail: { label: 'Ошибка отправки отзыва' },
    rating_high_rustore_request: { label: 'Запрос оценки в RuStore' },
    rating_high_rustore_result: { label: 'Результат RuStore-оценки' },
    onboarding_complete: { label: 'Прошёл онбординг' },
    persona_selected: { label: 'Выбрал персону (онбординг)' },
    persona_changed: { label: 'Сменил персону' },
    settings_language_change: { label: 'Сменил язык приложения' },
    settings_tts_language_change: { label: 'Сменил язык озвучки' },
    settings_hide_picks_toggle: { label: 'Переключил «скрыть подборки»' },
    theme_change: { label: 'Сменил тему' },
    font_scale_change: { label: 'Сменил масштаб шрифта' },
    contextual_pick_impression: { label: 'Увидел товар в подборке', tip: 'Impression одного pick за сессию.' },
    contextual_pick_section_shown: { label: 'Увидел блок подборки' },
    contextual_pick_click: { label: 'Кликнул товарную подборку', tip: 'Основной клик по pick (не legacy affiliate_click).' },
    device_pick_click: { label: 'Кликнул pick устройства' },
    affiliate_click: { label: 'Клик legacy affiliate', tip: 'Старые блоки affiliate; picks → contextual_pick_click.' },
    affiliate_no_match: { label: 'Подборка не нашлась' },
    device_guide_detail_open: { label: 'Открыл гайд устройства' },
    device_guide_external_click: { label: 'Клик ссылку из гайда' },
    deeplink_open: { label: 'Открыл deeplink' },
    app_error_non_fatal: { label: 'Ошибка (некритичная)' },
    billing_error: { label: 'Ошибка оплаты' },
    bootstrap_error: { label: 'Ошибка при запуске' },
    review_error: { label: 'Ошибка RuStore Review' },
    ads_error: { label: 'Ошибка рекламы' },
  };

  /** Служебные события — скрываются в топе «Действия пользователей». */
  const ANALYTICS_SYSTEM_EVENT_NAMES = new Set([
    'pro_restore',
    'content_sync',
    'app_foreground',
    'app_background',
    'session_start',
    'session_end',
    'time_in_app_tick',
    'contextual_pick_impression',
    'contextual_pick_section_shown',
    'cod_impression',
    'widget_shown',
    'affiliate_no_match',
    'rating_evaluate_skipped',
  ]);

  const ANALYTICS_FUNNEL_PRESETS = [
    { label: 'Pro', steps: 'paywall_view,pro_purchase_start,pro_activated' },
    { label: 'Поиск', steps: 'search,search_result_click,command_tts' },
    { label: 'TTS', steps: 'daily_active,command_tts,command_copy' },
    { label: 'Подборки', steps: 'contextual_pick_impression,contextual_pick_click' },
    { label: 'First value', steps: 'daily_active,first_value_tts,command_copy' },
    { label: 'Команда дня', steps: 'cod_impression,cod_open,command_tts' },
    { label: 'Сценарии', steps: 'scenario_open,command_tts' },
    { label: 'Виджет', steps: 'widget_shown,widget_open,command_view' },
    { label: 'Engagement', steps: 'daily_active,command_view,command_copy,favorite_add' },
  ];

  const ANALYTICS_BREAKDOWN_PRESETS = [
    { label: 'Кнопки', event: 'ui_click', param: 'element_id', fieldSource: 'params' },
    { label: 'Экраны', event: 'screen_view', param: 'route', fieldSource: 'params' },
    { label: 'Категории команд', event: 'command_view', param: 'category_id', fieldSource: 'params' },
    { label: 'Источник команды', event: 'command_view', param: 'source', fieldSource: 'params' },
    { label: 'Источник TTS', event: 'command_tts', param: 'source', fieldSource: 'params' },
    { label: 'Клик категории', event: 'category_click', param: 'category_id', fieldSource: 'params' },
    { label: 'Таб УД', event: 'smarthome_tab_select', param: 'tab', fieldSource: 'params' },
    { label: 'Placement picks', event: 'contextual_pick_impression', param: 'placement', fieldSource: 'params' },
    { label: 'Pro / free', event: 'daily_active', param: 'is_pro', fieldSource: 'user_properties' },
    { label: 'Персона', event: 'daily_active', param: 'persona', fieldSource: 'user_properties' },
  ];

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
    commandGroups: [],
    allCommandGroups: [],
    commands: [],
    allCommands: [],
    scenarios: [],
    checklist: [],
    affiliate: [],
    deviceGuides: [],
    devicePicks: [],
    smarthomeTab: 'guides',
    deviceGuideForm: null,
    deviceGuideEditing: false,
    devicePickForm: null,
    devicePickEditing: false,
    history: [],
    categoryForm: null,
    categoryEditing: false,
    commandForm: null,
    commandEditing: false,
    commandEditorTab: 'form',
    commandJsonText: '',
    commandSearch: '',
    commandFilter: '',
    groupFilter: 'smart_home',
    groupForm: null,
    groupEditing: false,
    validationWarnings: null,
    iconCatalog: null,
    visualCategories: [],
    selectedCommandIds: [],
    bulkGroupId: '',
    scenarioForm: null,
    scenarioEditing: false,
    affiliateForm: null,
    affiliateEditing: false,
    importMode: 'replace',
    importDraft: null,
    importParseError: '',
    importLoading: false,
    importPreviewLoading: false,
    publishedDiff: null,
    diffFilter: 'all',
    diffNeedsReviewOnly: false,
    pipelineDiffFilter: 'all',
    pipelineDiffNeedsReviewOnly: false,
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
    pipelineDiff: null,
    pipelineDiffLoading: false,
    seedImportLoading: false,
    apiDocs: null,
    apiDocsLoading: false,
    feedbackItems: [],
    feedbackLoading: false,
    feedbackFilter: 'open',
    feedbackSearch: '',
    commandReports: [],
    commandReportsLoading: false,
    commandReportsFilter: 'open',
    commandReportsSearch: '',
    commandOfDay: null,
    commandOfDayForm: null,
    commandOfDayLoading: false,
    analyticsMaxRangeDays: 90,
    analyticsPreset: 7,
    analyticsFrom: '',
    analyticsTo: '',
    analyticsTab: 'overview',
    analyticsEventLabels: ANALYTICS_EVENT_LABELS,
    analyticsFunnelPresets: ANALYTICS_FUNNEL_PRESETS,
    analyticsBreakdownPresets: ANALYTICS_BREAKDOWN_PRESETS,
    analyticsTopEventsMode: 'actions',
    analyticsBreakdownFieldSource: 'params',
    analyticsSummary: null,
    analyticsSummaryLoading: false,
    analyticsSummaryError: '',
    analyticsFunnel: null,
    analyticsFunnelLoading: false,
    analyticsFunnelError: '',
    analyticsFunnelSteps: 'paywall_view,pro_purchase_start,pro_activated',
    analyticsBreakdown: null,
    analyticsBreakdownLoading: false,
    analyticsBreakdownError: '',
    analyticsBreakdownEventName: 'ui_click',
    analyticsBreakdownParam: 'element_id',
    analyticsTrendSeries: 'events',
    analyticsEvents: [],
    analyticsEventsLoading: false,
    analyticsEventsError: '',
    analyticsEventsTotal: 0,
    analyticsEventName: '',
    analyticsInstallId: '',
    analyticsOffset: 0,
    analyticsLimit: 100,
    analyticsRangeError: '',

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
        const res = await fetch(path, { credentials: 'include', signal: ctrl.signal, cache: 'no-store' });
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
      const opts = { method, credentials: 'include', headers: {}, signal: undefined, cache: method === 'GET' ? 'no-store' : 'default' };
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
        this.loadCommandGroups(),
        this.loadAllCommandGroups(),
        this.loadCommands(),
        this.loadAllCommands(),
        this.loadScenarios(),
        this.loadChecklist(),
        this.loadCommandOfDay(),
        this.loadAffiliate(),
        this.loadHistory(),
        this.loadPipeline(),
        this.loadValidationWarnings(),
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
      if (!this.groupFilter && this.categories.length) {
        this.groupFilter = this.categories.find(c => c.id === 'smart_home')?.id || this.categories[0].id;
      }
    },
    async loadCommandGroups() {
      const q = this.groupFilter ? `?category_id=${encodeURIComponent(this.groupFilter)}` : '';
      this.commandGroups = await this.api('/admin/api/command-groups' + q) || [];
    },
    async loadAllCommandGroups() {
      this.allCommandGroups = await this.api('/admin/api/command-groups') || [];
    },
    async loadValidationWarnings() {
      try {
        this.validationWarnings = await this.api('/admin/api/content/validation-warnings');
      } catch {
        this.validationWarnings = null;
      }
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
    async loadSmartHomeDevices() {
      this.deviceGuides = await this.api('/admin/api/smarthome/device-guides') || [];
      this.devicePicks = await this.api('/admin/api/smarthome/device-picks') || [];
    },
    async loadHistory() {
      this.history = await this.api('/admin/api/publish/history') || [];
    },

    /**
     * После любой мутации draft/pipeline/editorial обновляет dashboard и,
     * если pipeline уже загружался или открыт раздел «Контент», пересинхронизирует wizard.
     */
    async refreshAfterDraftMutation(options = {}) {
      const { reloadDiff = true } = options;
      await this.loadDashboard();
      if (this.view === 'content' || this.pipeline !== null) {
        await this.loadPipeline({ reloadDiff, silent: true });
        return;
      }
      if (reloadDiff && this.dashboard?.hasUnpublishedChanges) {
        await this.loadDraftDiff(true);
      } else if (!this.dashboard?.hasUnpublishedChanges) {
        this.pipelineDiff = null;
      }
    },

    async refreshAfterPublishMutation() {
      this.pipelineDiff = null;
      this.publishedDiff = null;
      await Promise.all([this.loadDashboard(), this.loadHistory()]);
      if (this.view === 'content' || this.pipeline !== null) {
        await this.loadPipeline({ reloadDiff: false });
      }
    },

    async loadPipeline(options = {}) {
      const { reloadDiff = true, silent = false } = options;
      if (!this.authenticated) return;
      if (!silent) this.pipelineLoading = true;
      try {
        this.pipeline = await this.api('/admin/api/content/pipeline');
        await this.loadDashboard();
        if (reloadDiff && this.pipeline?.hasUnpublishedChanges) {
          await this.loadDraftDiff(true);
        } else if (!this.pipeline?.hasUnpublishedChanges) {
          this.pipelineDiff = null;
        }
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      } finally {
        this.pipelineLoading = false;
      }
    },

    async loadFeedback(silent = false) {
      if (!this.authenticated) return;
      this.feedbackLoading = true;
      try {
        const params = new URLSearchParams();
        if (this.feedbackFilter) params.set('status', this.feedbackFilter);
        if (this.feedbackSearch.trim()) params.set('search', this.feedbackSearch.trim());
        const qs = params.toString();
        this.feedbackItems = await this.api(`/admin/api/feedback${qs ? `?${qs}` : ''}`) || [];
      } catch (e) {
        if (!silent) this.error = this.networkErrorMessage(e);
      } finally {
        this.feedbackLoading = false;
      }
    },

    async resolveFeedback(item) {
      this.saving = true;
      try {
        await this.api(`/admin/api/feedback/${encodeURIComponent(item.id)}/resolve`, {
          method: 'POST',
          body: {},
        });
        this.showToast('Отзыв закрыт');
        await Promise.all([this.loadFeedback(true), this.loadDashboard()]);
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      } finally {
        this.saving = false;
      }
    },

    async dismissFeedback(item) {
      if (!confirm('Отклонить отзыв?')) return;
      this.saving = true;
      try {
        await this.api(`/admin/api/feedback/${encodeURIComponent(item.id)}/dismiss`, {
          method: 'POST',
          body: {},
        });
        this.showToast('Отзыв отклонён');
        await Promise.all([this.loadFeedback(true), this.loadDashboard()]);
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      } finally {
        this.saving = false;
      }
    },

    formatAnalyticsLocalDate(date) {
      const y = date.getFullYear();
      const m = String(date.getMonth() + 1).padStart(2, '0');
      const d = String(date.getDate()).padStart(2, '0');
      return `${y}-${m}-${d}`;
    },

    ensureAnalyticsDates() {
      if (!this.analyticsFrom || !this.analyticsTo) {
        this.applyAnalyticsPreset(this.analyticsPreset === 'custom' ? 7 : this.analyticsPreset);
      }
    },

    applyAnalyticsPreset(days) {
      this.analyticsPreset = days;
      const to = new Date();
      // Inclusive last N calendar days: today − (N−1) … today → N points.
      const from = new Date(to.getFullYear(), to.getMonth(), to.getDate() - (days - 1));
      this.analyticsTo = this.formatAnalyticsLocalDate(to);
      this.analyticsFrom = this.formatAnalyticsLocalDate(from);
      this.analyticsRangeError = '';
    },

    onAnalyticsCustomDateChange() {
      this.analyticsPreset = 'custom';
      this.analyticsRangeError = '';
    },

    analyticsDateRange() {
      this.ensureAnalyticsDates();
      return { from: this.analyticsFrom, to: this.analyticsTo };
    },

    /** ChronoUnit.DAYS between from and to (exclusive of inclusive count). */
    analyticsSpanDays() {
      const range = this.analyticsDateRange();
      const from = new Date(`${range.from}T00:00:00Z`);
      const to = new Date(`${range.to}T00:00:00Z`);
      return Math.round((to - from) / 86400000);
    },

    /** Inclusive calendar days (= number of daily points). */
    analyticsInclusiveDays() {
      return this.analyticsSpanDays() + 1;
    },

    validateAnalyticsRange() {
      const range = this.analyticsDateRange();
      if (!range.from || !range.to) {
        this.analyticsRangeError = 'Укажите from и to';
        return false;
      }
      if (range.from > range.to) {
        this.analyticsRangeError = 'from должен быть ≤ to';
        return false;
      }
      const inclusive = this.analyticsInclusiveDays();
      if (inclusive > this.analyticsMaxRangeDays) {
        this.analyticsRangeError = `Период не больше ${this.analyticsMaxRangeDays} календарных дней (сейчас ${inclusive})`;
        return false;
      }
      this.analyticsRangeError = '';
      return true;
    },

    openAnalytics(tab = 'overview') {
      this.view = 'analytics';
      this.analyticsTab = tab;
      this.ensureAnalyticsDates();
      this.loadAnalyticsForActiveTab();
    },

    setAnalyticsTab(tab) {
      this.analyticsTab = tab;
      this.loadAnalyticsForActiveTab();
    },

    applyAnalyticsRange() {
      this.analyticsOffset = 0;
      this.loadAnalyticsForActiveTab();
    },

    loadAnalyticsForActiveTab(silent = false) {
      if (!this.validateAnalyticsRange()) return;
      switch (this.analyticsTab) {
        case 'overview':
        case 'trend':
          return this.loadAnalyticsSummary(silent);
        case 'funnel':
          return this.loadAnalyticsFunnel(silent);
        case 'breakdown':
          return this.loadAnalyticsBreakdown(silent);
        case 'events':
          return this.loadAnalyticsEvents(silent);
        case 'help':
          return undefined;
        default:
          return this.loadAnalyticsSummary(silent);
      }
    },

    drillToAnalyticsEvents(eventName) {
      this.analyticsEventName = eventName || '';
      this.analyticsOffset = 0;
      this.setAnalyticsTab('events');
    },

    async loadAnalyticsSummary(silent = false) {
      if (!this.authenticated) return;
      if (!this.validateAnalyticsRange()) return;
      this.analyticsSummaryLoading = true;
      this.analyticsSummaryError = '';
      try {
        const range = this.analyticsDateRange();
        const params = new URLSearchParams({ from: range.from, to: range.to });
        this.analyticsSummary = await this.api(`/admin/api/analytics/summary?${params}`);
      } catch (e) {
        this.analyticsSummary = null;
        if (!silent) this.analyticsSummaryError = this.networkErrorMessage(e);
      } finally {
        this.analyticsSummaryLoading = false;
      }
    },

    async loadAnalyticsFunnel(silent = false) {
      if (!this.authenticated) return;
      if (!this.validateAnalyticsRange()) return;
      this.analyticsFunnelLoading = true;
      this.analyticsFunnelError = '';
      try {
        const range = this.analyticsDateRange();
        const params = new URLSearchParams({ from: range.from, to: range.to });
        if (this.analyticsFunnelSteps.trim()) params.set('steps', this.analyticsFunnelSteps.trim());
        this.analyticsFunnel = await this.api(`/admin/api/analytics/funnel?${params}`);
      } catch (e) {
        this.analyticsFunnel = null;
        if (!silent) this.analyticsFunnelError = this.networkErrorMessage(e);
      } finally {
        this.analyticsFunnelLoading = false;
      }
    },

    async loadAnalyticsBreakdown(silent = false) {
      if (!this.authenticated) return;
      if (!this.validateAnalyticsRange()) return;
      this.analyticsBreakdownLoading = true;
      this.analyticsBreakdownError = '';
      try {
        const range = this.analyticsDateRange();
        const params = new URLSearchParams({
          from: range.from,
          to: range.to,
          event_name: this.analyticsBreakdownEventName.trim() || 'ui_click',
          param: this.analyticsBreakdownParam.trim() || 'element_id',
        });
        if (this.analyticsBreakdownFieldSource === 'user_properties') {
          params.set('field_source', 'user_properties');
        }
        this.analyticsBreakdown = await this.api(`/admin/api/analytics/breakdown?${params}`);
      } catch (e) {
        this.analyticsBreakdown = null;
        if (!silent) this.analyticsBreakdownError = this.networkErrorMessage(e);
      } finally {
        this.analyticsBreakdownLoading = false;
      }
    },

    async loadAnalyticsEvents(silent = false) {
      if (!this.authenticated) return;
      if (!this.validateAnalyticsRange()) return;
      this.analyticsEventsLoading = true;
      this.analyticsEventsError = '';
      try {
        const range = this.analyticsDateRange();
        const params = new URLSearchParams({
          from: range.from,
          to: range.to,
          limit: String(this.analyticsLimit),
          offset: String(this.analyticsOffset),
        });
        if (this.analyticsEventName.trim()) params.set('event_name', this.analyticsEventName.trim());
        if (this.analyticsInstallId.trim()) params.set('install_id', this.analyticsInstallId.trim());
        const result = await this.api(`/admin/api/analytics/events?${params}`);
        this.analyticsEvents = result?.items || [];
        this.analyticsEventsTotal = result?.total || 0;
      } catch (e) {
        this.analyticsEvents = [];
        this.analyticsEventsTotal = 0;
        if (!silent) this.analyticsEventsError = this.networkErrorMessage(e);
      } finally {
        this.analyticsEventsLoading = false;
      }
    },

    analyticsTrendMax() {
      const daily = this.analyticsSummary?.daily || [];
      const key = this.analyticsTrendSeriesKey();
      return Math.max(1, ...daily.map((d) => Number(d[key]) || 0));
    },

    analyticsTrendSeriesKey() {
      if (this.analyticsTrendSeries === 'dau') return 'dau';
      if (this.analyticsTrendSeries === 'new_installs' || this.analyticsTrendSeries === 'unique_installs') {
        return 'new_installs';
      }
      return 'events';
    },

    analyticsBarHeight(point) {
      const value = this.analyticsBarValue(point);
      if (value <= 0) return 0;
      return Math.max(2, Math.round((value / this.analyticsTrendMax()) * 100));
    },

    analyticsBarValue(point) {
      const key = this.analyticsTrendSeriesKey();
      return Number(point?.[key]) || 0;
    },

    analyticsShowBarValues() {
      return (this.analyticsSummary?.daily || []).length <= 31;
    },

    analyticsShowBarLabel(index) {
      const daily = this.analyticsSummary?.daily || [];
      const n = daily.length;
      if (n <= 31) return true;
      if (index === 0 || index === n - 1) return true;
      return index % 7 === 0;
    },

    analyticsEventLabel(eventName) {
      const entry = this.analyticsEventLabels[eventName];
      return entry?.label || eventName;
    },

    analyticsEventTip(eventName) {
      return this.analyticsEventLabels[eventName]?.tip || '';
    },

    analyticsFilteredTopEvents() {
      const rows = this.analyticsSummary?.top_events || [];
      if (this.analyticsTopEventsMode === 'all') return rows;
      return rows.filter((row) => !ANALYTICS_SYSTEM_EVENT_NAMES.has(row.event_name));
    },

    applyAnalyticsFunnelPreset(preset) {
      this.analyticsFunnelSteps = preset.steps;
      this.loadAnalyticsFunnel();
    },

    applyAnalyticsBreakdownPreset(preset) {
      this.analyticsBreakdownEventName = preset.event;
      this.analyticsBreakdownParam = preset.param;
      this.analyticsBreakdownFieldSource = preset.fieldSource || 'params';
      this.loadAnalyticsBreakdown();
    },

    analyticsEventNames() {
      return Object.keys(this.analyticsEventLabels || {});
    },

    /** Only events that need a tip — for the help glossary. */
    analyticsExplainedEventNames() {
      return Object.keys(this.analyticsEventLabels || {}).filter((name) => {
        const tip = this.analyticsEventLabels[name]?.tip;
        return tip && tip.length > 0;
      });
    },

    analyticsPrevPage() {
      if (this.analyticsOffset <= 0) return;
      this.analyticsOffset = Math.max(0, this.analyticsOffset - this.analyticsLimit);
      this.loadAnalyticsEvents();
    },

    analyticsNextPage() {
      if (this.analyticsOffset + this.analyticsLimit >= this.analyticsEventsTotal) return;
      this.analyticsOffset += this.analyticsLimit;
      this.loadAnalyticsEvents();
    },

    shortUuid(value) {
      if (!value || value.length < 12) return value || '';
      return `${value.slice(0, 8)}…${value.slice(-4)}`;
    },

    formatJson(value) {
      try {
        return JSON.stringify(value || {}, null, 2);
      } catch {
        return String(value);
      }
    },

    async loadCommandReports(silent = false) {
      if (!this.authenticated) return;
      this.commandReportsLoading = true;
      try {
        const params = new URLSearchParams();
        if (this.commandReportsFilter) params.set('status', this.commandReportsFilter);
        if (this.commandReportsSearch.trim()) params.set('search', this.commandReportsSearch.trim());
        const qs = params.toString();
        this.commandReports = await this.api(`/admin/api/command-reports${qs ? `?${qs}` : ''}`) || [];
      } catch (e) {
        if (!silent) this.error = this.networkErrorMessage(e);
      } finally {
        this.commandReportsLoading = false;
      }
    },

    async resolveCommandReport(item) {
      this.saving = true;
      try {
        await this.api(`/admin/api/command-reports/${encodeURIComponent(item.id)}/resolve`, {
          method: 'POST',
          body: {},
        });
        this.showToast('Report закрыт');
        await Promise.all([this.loadCommandReports(true), this.loadDashboard()]);
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      } finally {
        this.saving = false;
      }
    },

    async dismissCommandReport(item) {
      if (!confirm(`Отклонить report для ${item.command_id}?`)) return;
      this.saving = true;
      try {
        await this.api(`/admin/api/command-reports/${encodeURIComponent(item.id)}/dismiss`, {
          method: 'POST',
          body: {},
        });
        this.showToast('Report отклонён');
        await Promise.all([this.loadCommandReports(true), this.loadDashboard()]);
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      } finally {
        this.saving = false;
      }
    },

    issueTypeLabel(type) {
      const map = {
        wrong_effect: 'Неверный эффект',
        outdated: 'Устарело',
        phrase_not_working: 'Фраза не работает',
        requires_plus_wrong: 'Неверно про Plus',
        wrong_device: 'Неверное устройство',
        other: 'Другое',
      };
      return map[type] || type;
    },

    async openCommandFromReport(item) {
      this.view = 'commands';
      this.commandFilter = item.category_id || '';
      await this.loadCommands();
      const cmd = this.commands.find(c => c.id === item.command_id) ||
        this.allCommands.find(c => c.id === item.command_id);
      if (cmd) {
        this.editCommand(cmd);
      } else {
        this.showToast(`Команда ${item.command_id} не найдена в draft`);
      }
    },

    changeLabel(change) {
      const map = { added: 'добавлено', changed: 'изменено', removed: 'удалено' };
      return map[change] || change;
    },

    async loadDraftDiff(forceReload = false, options = {}) {
      const { resetFilters = false, showToast = false } = options;
      if (this.pipelineDiff && !forceReload) return;
      this.pipelineDiffLoading = true;
      this.error = '';
      try {
        this.pipelineDiff = await this.api('/admin/api/content/draft-diff');
        if (resetFilters) {
          this.pipelineDiffFilter = 'all';
          this.pipelineDiffNeedsReviewOnly = false;
        }
        if (showToast && this.view !== 'content') {
          this.showToast('Diff draft vs опубликовано загружен — см. раздел «Контент»');
        }
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      } finally {
        this.pipelineDiffLoading = false;
      }
    },

    openAdminGuide() {
      const path = this.pipeline?.guidePath || 'docs/ADMIN-CONTENT-GUIDE.md';
      this.copyText(path);
      this.showToast('Путь к инструкции скопирован — откройте в репозитории или на рабочем столе (ИНСТРУКЦИЯ.md)');
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

    async importSeedFromServer(mode = 'replace') {
      if (mode === 'replace' && !confirm('Replace all заменит весь каталог draft. Продолжить?')) return;
      this.seedImportLoading = true;
      this.error = '';
      try {
        await this.api(`/admin/api/content/import-seed?mode=${mode}`, { method: 'POST', body: {} });
        this.showToast('Seed импортирован — проверьте diff и публикацию');
        this.clearImportFile();
        this.view = 'content';
        await this.refreshAfterDraftMutation({ reloadDiff: true });
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
      this.loadIconCatalog();
      this.categoryForm = {
        id: '', title_ru: '', sort_order: (this.categories.length + 1),
        featured: false, icon_key: '', icon_url: '', accent_color: '', accent_color_dark: '',
        description_ru: '',
        source_url: 'https://alice.yandex.ru/support/ru/station/skills/', device_types: [],
      };
    },
    editCategory(c) {
      this.formError = '';
      this.categoryEditing = true;
      this.loadIconCatalog();
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
        await this.refreshAfterDraftMutation();
        this.showToast('Категория сохранена');
      });
    },
    async deleteCategory(id) {
      if (!confirm(`Удалить категорию ${id}?`)) return;
      try {
        await this.api(`/admin/api/categories/${id}`, { method: 'DELETE' });
        await this.loadCategories();
        await this.refreshAfterDraftMutation();
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
        await this.refreshAfterDraftMutation({ reloadDiff: false });
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      }
    },

    commandsInGroup(groupId) {
      return (this.allCommands || []).filter(c => c.group_id === groupId);
    },
    groupsForCategory(categoryId) {
      return (this.allCommandGroups || []).filter(g => g.category_id === categoryId);
    },
    commandGroupsForFilter() {
      return (this.allCommandGroups || []).filter(g => !this.commandFilter || g.category_id === this.commandFilter);
    },
    toggleCommandSelect(id, checked) {
      if (checked) {
        if (!this.selectedCommandIds.includes(id)) this.selectedCommandIds.push(id);
      } else {
        this.selectedCommandIds = this.selectedCommandIds.filter(x => x !== id);
      }
    },
    async loadIconCatalog(force = false) {
      if (this.iconCatalog && !force) return;
      try {
        this.iconCatalog = await this.api('/admin/api/icons/catalog');
      } catch {
        this.iconCatalog = { icons: [], accent_presets: [] };
      }
    },

    async loadCategoryVisuals(force = false) {
      await Promise.all([
        this.loadCategories(),
        this.loadIconCatalog(force),
      ]);
      this.visualCategories = this.categories.map(c => ({ ...c }));
    },

    applyIconToVisualCategory(c, slug) {
      const icon = (this.iconCatalog?.icons || []).find(i => i.slug === slug);
      if (!icon || !c) return;
      c.icon_url = icon.url;
      if (!c.icon_key) c.icon_key = icon.slug;
    },

    applyPresetToVisualCategory(preset) {
      const c = this.visualCategories[0];
      if (!c || !preset) return;
      c.accent_color = preset.light;
      c.accent_color_dark = preset.dark;
      this.showToast(`Пресет «${preset.name}» — к «${c.title_ru}», нажмите Сохранить`);
    },

    async saveCategoryVisual(c) {
      await this.runSaving(async () => {
        const full = this.categories.find(x => x.id === c.id) || c;
        const body = {
          ...full,
          icon_key: c.icon_key || null,
          icon_url: c.icon_url || null,
          accent_color: c.accent_color || null,
          accent_color_dark: c.accent_color_dark || null,
        };
        await this.api(`/admin/api/categories/${c.id}`, { method: 'PUT', body });
        await this.loadCategories();
        this.visualCategories = this.categories.map(x => ({ ...x }));
        await this.refreshAfterDraftMutation();
        this.showToast(`Оформление «${c.title_ru}» сохранено`);
      });
    },

    applyAccentPreset(target, preset) {
      const form = target === 'category' ? this.categoryForm : this.groupForm;
      if (!form) return;
      form.accent_color = preset.light;
      form.accent_color_dark = preset.dark;
    },

    applyIconFromCatalog(target, icon) {
      const form = target === 'category' ? this.categoryForm : this.groupForm;
      if (!form) return;
      form.icon_url = icon.url;
      if (!form.icon_key) form.icon_key = icon.slug;
    },

    async uploadIconFile(event, target) {
      const file = event.target.files?.[0];
      if (!file) return;
      try {
        const svg = await file.text();
        const slug = file.name.replace(/\.svg$/i, '').toLowerCase().replace(/-/g, '_');
        const result = await this.api('/admin/api/icons/upload', {
          method: 'POST',
          body: { slug, svg },
        });
        const form = target === 'category' ? this.categoryForm : target === 'group' ? this.groupForm : null;
        if (form) {
          form.icon_url = result.icon_url;
          if (!form.icon_key) form.icon_key = result.icon_key;
        }
        this.iconCatalog = null;
        await this.loadIconCatalog(true);
        this.showToast('Иконка загружена');
      } catch (e) {
        this.formError = this.networkErrorMessage(e);
      } finally {
        event.target.value = '';
      }
    },

    toggleGroupVisualInherit() {
      if (!this.groupForm?.inheritVisuals) return;
      this.groupForm.icon_url = '';
      this.groupForm.accent_color = '';
      this.groupForm.accent_color_dark = '';
    },

    hexWithAlpha(hex, alpha) {
      if (!hex || !/^#[0-9A-Fa-f]{6}$/.test(hex)) return `rgba(27,107,90,${alpha})`;
      const r = parseInt(hex.slice(1, 3), 16);
      const g = parseInt(hex.slice(3, 5), 16);
      const b = parseInt(hex.slice(5, 7), 16);
      return `rgba(${r},${g},${b},${alpha})`;
    },

    async bulkAssignGroup() {
      if (!this.selectedCommandIds.length) return;
      try {
        await this.api('/admin/api/commands/bulk-assign-group', {
          method: 'PUT',
          body: { command_ids: this.selectedCommandIds, group_id: this.bulkGroupId || null },
        });
        this.selectedCommandIds = [];
        await Promise.all([this.loadCommands(), this.loadAllCommands(), this.loadValidationWarnings()]);
        await this.refreshAfterDraftMutation();
        this.showToast('Группа назначена');
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      }
    },

    showGroupForm() {
      this.formError = '';
      this.groupEditing = false;
      this.loadIconCatalog();
      this.groupForm = {
        id: '', category_id: this.groupFilter || this.categories[0]?.id || '',
        title_ru: '', sort_order: (this.commandGroups.length + 1) * 10,
        description_ru: '', icon_key: '', icon_url: '', accent_color: '', accent_color_dark: '',
        featured: false, previewIdsText: '', inheritVisuals: true,
      };
    },
    editGroup(g) {
      this.formError = '';
      this.groupEditing = true;
      this.loadIconCatalog();
      const hasOwnVisuals = !!(g.icon_url || g.accent_color || g.accent_color_dark);
      this.groupForm = {
        ...g,
        previewIdsText: (g.preview_command_ids || []).join(', '),
        inheritVisuals: !hasOwnVisuals,
      };
    },
    async saveGroup() {
      await this.runSaving(async () => {
        const f = this.groupForm;
        const body = {
          id: f.id,
          category_id: f.category_id,
          title_ru: f.title_ru,
          sort_order: f.sort_order,
          description_ru: f.description_ru || null,
          icon_key: f.icon_key || null,
          icon_url: f.inheritVisuals ? null : (f.icon_url || null),
          accent_color: f.inheritVisuals ? null : (f.accent_color || null),
          accent_color_dark: f.inheritVisuals ? null : (f.accent_color_dark || null),
          featured: !!f.featured,
          preview_command_ids: f.previewIdsText.split(',').map(s => s.trim()).filter(Boolean),
        };
        if (this.groupEditing) {
          await this.api(`/admin/api/command-groups/${body.id}`, { method: 'PUT', body });
        } else {
          await this.api('/admin/api/command-groups', { method: 'POST', body });
        }
        this.groupForm = null;
        await Promise.all([this.loadCommandGroups(), this.loadAllCommandGroups(), this.loadValidationWarnings()]);
        await this.refreshAfterDraftMutation();
        this.showToast('Группа сохранена');
      });
    },
    async deleteGroup(id) {
      if (!confirm(`Удалить группу ${id}? Команды останутся без группы.`)) return;
      try {
        await this.api(`/admin/api/command-groups/${id}`, { method: 'DELETE' });
        await Promise.all([this.loadCommandGroups(), this.loadAllCommandGroups(), this.loadCommands(), this.loadAllCommands()]);
        await this.refreshAfterDraftMutation();
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      }
    },
    async moveCommandGroup(g, delta) {
      try {
        const sorted = [...this.commandGroups].sort((a, b) => a.sort_order - b.sort_order);
        const idx = sorted.findIndex(x => x.id === g.id);
        const swap = idx + delta;
        if (swap < 0 || swap >= sorted.length) return;
        [sorted[idx], sorted[swap]] = [sorted[swap], sorted[idx]];
        await this.api('/admin/api/command-groups/reorder', {
          method: 'PUT',
          body: { ordered_ids: sorted.map(x => x.id) },
        });
        await this.loadCommandGroups();
        await this.refreshAfterDraftMutation({ reloadDiff: false });
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      }
    },

    filteredCommands() {
      const q = (this.commandSearch || '').trim().toLowerCase();
      if (!q) return this.commands;
      return this.commands.filter((c) => {
        if (c.id.toLowerCase().includes(q)) return true;
        if ((c.title_ru || '').toLowerCase().includes(q)) return true;
        return (c.phrases || []).some((p) => String(p).toLowerCase().includes(q));
      });
    },

    copyCommandJson() {
      let text = this.commandJsonText;
      if (this.commandEditorTab === 'form') {
        text = this.formatCommandJson(this.buildCommandBodyFromForm());
      }
      this.copyText(text);
      this.showToast('JSON команды скопирован — вставьте в seed/catalog-audit-fixed.json (commands[]) или сохраните в draft');
    },

    showCommandForm() {
      this.formError = '';
      this.commandEditing = false;
      this.commandEditorTab = 'form';
      const now = new Date().toISOString();
      this.commandForm = {
        id: '', category_id: this.categories[0]?.id || '', title_ru: '',
        phrasesText: '', effect_description_ru: '', source_url: 'https://alice.yandex.ru/support/ru/station/skills/',
        requires_alice_word: true, requires_plus: false, tagsText: '', updated_at: now,
        group_id: '', sort_order: null, variant_label_ru: '', is_primary_in_group: false, aliasesText: '',
        deviceTypesText: 'station,phone', relatedIdsText: '',
        device_types: ['station', 'phone'], related_command_ids: [], published_at: null,
      };
      this.commandJsonText = this.formatCommandJson(this.buildCommandBodyFromForm());
    },
    editCommand(cmd) {
      this.formError = '';
      this.commandEditing = true;
      this.commandEditorTab = 'form';
      this.commandForm = {
        ...cmd,
        group_id: cmd.group_id || '',
        phrasesText: (cmd.phrases || []).join('\n'),
        tagsText: (cmd.tags || []).join(', '),
        aliasesText: (cmd.search_aliases || []).join(', '),
        deviceTypesText: (cmd.device_types || []).join(','),
        relatedIdsText: (cmd.related_command_ids || []).join(', '),
      };
      this.commandJsonText = this.formatCommandJson(this.normalizeCommandForJson(cmd));
    },
    closeCommandForm() {
      this.commandForm = null;
      this.commandJsonText = '';
      this.formError = '';
      this.commandEditorTab = 'form';
    },
    normalizeCommandForJson(cmd) {
      return {
        id: cmd.id,
        category_id: cmd.category_id,
        title_ru: cmd.title_ru,
        phrases: cmd.phrases || [],
        effect_description_ru: cmd.effect_description_ru,
        requires_alice_word: !!cmd.requires_alice_word,
        requires_plus: !!cmd.requires_plus,
        device_types: cmd.device_types || [],
        related_command_ids: cmd.related_command_ids || [],
        source_url: cmd.source_url,
        published_at: cmd.published_at ?? null,
        updated_at: cmd.updated_at || new Date().toISOString(),
        tags: cmd.tags || [],
        group_id: cmd.group_id || null,
        sort_order: (() => {
          if (cmd.sort_order == null || cmd.sort_order === '') return null;
          const n = Number(cmd.sort_order);
          return Number.isFinite(n) ? n : null;
        })(),
        variant_label_ru: cmd.variant_label_ru || null,
        is_primary_in_group: !!cmd.is_primary_in_group,
        search_aliases: cmd.search_aliases || [],
      };
    },
    formatCommandJson(cmd) {
      return JSON.stringify(this.normalizeCommandForJson(cmd), null, 2);
    },
    buildCommandBodyFromForm() {
      const f = this.commandForm;
      return this.normalizeCommandForJson({
        id: f.id,
        category_id: f.category_id,
        title_ru: f.title_ru,
        phrases: String(f.phrasesText || '').split('\n').map((s) => s.trim()).filter(Boolean),
        effect_description_ru: f.effect_description_ru,
        requires_alice_word: f.requires_alice_word,
        requires_plus: f.requires_plus,
        device_types: this.parseCsvIds(f.deviceTypesText),
        related_command_ids: this.parseCsvIds(f.relatedIdsText),
        source_url: f.source_url,
        published_at: f.published_at ?? null,
        updated_at: new Date().toISOString(),
        tags: this.parseCsvIds(f.tagsText),
        group_id: f.group_id || null,
        sort_order: f.sort_order == null || f.sort_order === '' ? null : Number(f.sort_order),
        variant_label_ru: f.variant_label_ru || null,
        is_primary_in_group: !!f.is_primary_in_group,
        search_aliases: this.parseCsvIds(f.aliasesText),
      });
    },
    applyCommandBodyToForm(body) {
      this.commandForm = {
        ...this.commandForm,
        ...body,
        group_id: body.group_id || '',
        phrasesText: (body.phrases || []).join('\n'),
        tagsText: (body.tags || []).join(', '),
        aliasesText: (body.search_aliases || []).join(', '),
        deviceTypesText: (body.device_types || []).join(','),
        relatedIdsText: (body.related_command_ids || []).join(', '),
      };
    },
    switchCommandEditorTab(tab) {
      this.formError = '';
      if (tab === 'json' && this.commandEditorTab === 'form') {
        this.commandJsonText = this.formatCommandJson(this.buildCommandBodyFromForm());
      } else if (tab === 'form' && this.commandEditorTab === 'json') {
        try {
          const parsed = JSON.parse(this.commandJsonText);
          if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
            throw new Error('Ожидается JSON-объект команды');
          }
          this.applyCommandBodyToForm(this.normalizeCommandForJson(parsed));
        } catch (e) {
          this.formError = `Некорректный JSON: ${e.message || e}`;
          return;
        }
      }
      this.commandEditorTab = tab;
    },
    validateCommandBody(body) {
      if (!body.id || !/^[a-z0-9_]+$/.test(body.id)) return 'id: только a-z, 0-9, _';
      if (!body.category_id) return 'category_id обязателен';
      if (!body.title_ru?.trim()) return 'title_ru обязателен';
      if (!Array.isArray(body.phrases) || body.phrases.length === 0) return 'phrases: нужен хотя бы один элемент';
      if (!body.effect_description_ru?.trim()) return 'effect_description_ru обязателен';
      if (!body.source_url?.trim()) return 'source_url обязателен';
      const allowedDevices = new Set(['station', 'tv', 'phone']);
      if ((body.device_types || []).some((d) => !allowedDevices.has(d))) {
        return 'device_types: только station, tv, phone';
      }
      if (body.sort_order != null && !Number.isFinite(body.sort_order)) {
        return 'sort_order: должно быть число или null';
      }
      return '';
    },
    async saveCommand() {
      await this.runSaving(async () => {
        let body;
        if (this.commandEditorTab === 'json') {
          try {
            body = this.normalizeCommandForJson(JSON.parse(this.commandJsonText));
          } catch (e) {
            this.formError = `Некорректный JSON: ${e.message || e}`;
            return;
          }
        } else {
          body = this.buildCommandBodyFromForm();
        }
        body.updated_at = new Date().toISOString();
        const err = this.validateCommandBody(body);
        if (err) {
          this.formError = err;
          return;
        }
        if (this.commandEditing && body.id !== this.commandForm.id) {
          this.formError = 'Нельзя менять id существующей команды';
          return;
        }
        if (this.commandEditing) {
          await this.api(`/admin/api/commands/${body.id}`, { method: 'PUT', body });
        } else {
          await this.api('/admin/api/commands', { method: 'POST', body });
        }
        this.closeCommandForm();
        await Promise.all([this.loadCommands(), this.loadAllCommands()]);
        await this.refreshAfterDraftMutation();
        this.showToast('Команда сохранена в draft — затем Publish и/или pull-draft.ps1');
      });
    },
    async deleteCommand(id) {
      if (!confirm(`Удалить команду ${id}?`)) return;
      try {
        await this.api(`/admin/api/commands/${id}`, { method: 'DELETE' });
        await Promise.all([this.loadCommands(), this.loadAllCommands()]);
        await this.refreshAfterDraftMutation();
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      }
    },

    async downloadDraftCatalog() {
      try {
        const bundle = await this.api('/admin/api/preview/bundle');
        const text = JSON.stringify(bundle, null, 2);
        const blob = new Blob([text], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'catalog-audit-fixed.json';
        a.style.display = 'none';
        document.body.appendChild(a);
        a.click();
        a.remove();
        setTimeout(() => URL.revokeObjectURL(url), 5000);
        this.showToast('Скачан catalog-audit-fixed.json — положите в seed/ вместо старого файла');
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
        await this.refreshAfterDraftMutation();
        this.showToast('Шаблон сохранён');
      });
    },
    async deleteScenario(id) {
      if (!confirm(`Удалить шаблон ${id}?`)) return;
      try {
        await this.api(`/admin/api/scenario-templates/${id}`, { method: 'DELETE' });
        await this.loadScenarios();
        await this.refreshAfterDraftMutation();
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      }
    },

    async saveChecklist() {
      await this.runSaving(async () => {
        await this.api('/admin/api/checklist-items', { method: 'PUT', body: this.checklist });
        this.showToast('Чеклист сохранён');
        await this.loadChecklist();
        await this.refreshAfterDraftMutation();
      });
    },

    showAffiliateForm() {
      this.formError = '';
      this.affiliateEditing = false;
      this.affiliateForm = {
        id: '', title_ru: '', context_category_id: 'smart_home',
        erid: '', advertiser_name: '', products: [this.blankAffiliateProduct()],
      };
    },
    editAffiliate(b) {
      this.formError = '';
      this.affiliateEditing = true;
      this.affiliateForm = {
        ...b,
        products: (b.products || []).length
          ? (b.products || []).map(p => ({
            title_ru: p.title_ru || '',
            market_url: p.market_url || '',
            price_hint: p.price_hint || '',
          }))
          : [this.blankAffiliateProduct()],
      };
    },
    blankAffiliateProduct() {
      return { title_ru: '', market_url: '', price_hint: '' };
    },
    addAffiliateProduct() {
      if (!this.affiliateForm) return;
      this.affiliateForm.products.push(this.blankAffiliateProduct());
    },
    removeAffiliateProduct(idx) {
      if (!this.affiliateForm || this.affiliateForm.products.length <= 1) return;
      this.affiliateForm.products.splice(idx, 1);
    },
    trimOrNull(value) {
      const text = (value || '').trim();
      return text || null;
    },
    normalizeAffiliateProducts(products) {
      const normalized = (products || []).map((p, idx) => {
        const title = (p.title_ru || '').trim();
        const url = (p.market_url || '').trim();
        const price = (p.price_hint || '').trim();
        if (!title) throw new Error(`Товар #${idx + 1}: укажите название`);
        if (!url) throw new Error(`Товар #${idx + 1}: укажите партнёрскую ссылку`);
        let parsed;
        try {
          parsed = new URL(url);
        } catch {
          throw new Error(`Товар #${idx + 1}: ссылка должна быть корректным URL`);
        }
        if (parsed.protocol !== 'https:') throw new Error(`Товар #${idx + 1}: ссылка должна начинаться с https://`);
        return { title_ru: title, market_url: url, price_hint: price || null };
      });
      if (!normalized.length) throw new Error('Добавьте хотя бы один товар с партнёрской ссылкой');
      return normalized;
    },
    async loadCommandOfDay() {
      this.commandOfDayLoading = true;
      try {
        const data = await this.api('/admin/api/command-of-day');
        if (!data) return;
        this.commandOfDay = data;
        const s = data.settings || {};
        this.commandOfDayForm = {
          mode: s.mode || 'auto',
          command_id: s.command_id || '',
          auto_category_id: s.auto_category_id || (this.categories[0]?.id || ''),
          auto_seed: s.auto_seed || 31,
        };
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      } finally {
        this.commandOfDayLoading = false;
      }
    },
    async saveCommandOfDay() {
      await this.runSaving(async () => {
        const f = this.commandOfDayForm;
        if (!f) throw new Error('Форма не загружена');
        const body = {
          mode: f.mode,
          auto_seed: f.auto_seed || 31,
        };
        if (f.mode === 'manual') {
          body.command_id = (f.command_id || '').trim();
        } else {
          body.auto_category_id = f.auto_category_id;
        }
        this.commandOfDay = await this.api('/admin/api/command-of-day', { method: 'PUT', body });
        const s = this.commandOfDay.settings || {};
        this.commandOfDayForm = {
          mode: s.mode,
          command_id: s.command_id || '',
          auto_category_id: s.auto_category_id || '',
          auto_seed: s.auto_seed || 31,
        };
        this.toast = 'Команда дня сохранена в draft';
      });
    },
    async publishCommandOfDay() {
      if (!confirm('Опубликовать команду дня в live bundle? App получит новую content_version с обновлённым command_of_day.')) return;
      this.loading = true;
      try {
        const r = await this.api('/admin/api/command-of-day/publish', { method: 'POST', body: {} });
        this.showToast(`Команда дня опубликована (v${r.contentVersion})`);
        await Promise.all([this.loadCommandOfDay(), this.loadDashboard()]);
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      } finally {
        this.loading = false;
      }
    },
    async saveAffiliate() {
      await this.runSaving(async () => {
        const f = this.affiliateForm;
        const body = {
          id: (f.id || '').trim(),
          title_ru: (f.title_ru || '').trim(),
          context_category_id: this.trimOrNull(f.context_category_id),
          erid: this.trimOrNull(f.erid),
          advertiser_name: this.trimOrNull(f.advertiser_name),
          products: this.normalizeAffiliateProducts(f.products),
        };
        if (this.affiliateEditing) {
          await this.api(`/admin/api/affiliate-blocks/${body.id}`, { method: 'PUT', body });
        } else {
          await this.api('/admin/api/affiliate-blocks', { method: 'POST', body });
        }
        this.affiliateForm = null;
        await this.loadAffiliate();
        await this.refreshAfterDraftMutation();
        this.showToast('Партнёрский блок сохранён');
      });
    },
    async deleteAffiliate(id) {
      if (!confirm(`Удалить affiliate блок ${id}?`)) return;
      try {
        await this.api(`/admin/api/affiliate-blocks/${id}`, { method: 'DELETE' });
        await this.loadAffiliate();
        await this.refreshAfterDraftMutation();
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      }
    },

    showDeviceGuideForm() {
      this.formError = '';
      this.deviceGuideEditing = false;
      this.deviceGuideForm = {
        id: '', title_ru: '', summary_ru: '', capabilities_ru: '', setup_ru: '',
        setup_steps_text: '', related_devices_ru: '', related_device_ids_text: '',
        command_device_filter_id: '', image_url: '', action_url: '', sort_order: 10,
      };
    },
    editDeviceGuide(g) {
      this.formError = '';
      this.deviceGuideEditing = true;
      this.deviceGuideForm = {
        ...g,
        setup_steps_text: (g.setup_steps_ru || []).join('\n'),
        related_device_ids_text: (g.related_device_ids || []).join(','),
        command_device_filter_id: g.command_device_filter_id || '',
      };
    },
    showDevicePickForm() {
      this.formError = '';
      this.devicePickEditing = false;
      this.devicePickForm = {
        id: '', title_ru: '', description_ru: '',
        image_url: '', action_url: '', cta_ru: 'Смотреть цену', sort_order: 10, priority: 0,
        placements_text: 'smart_home_devices', tags_text: '', device_types_text: '',
        category_ids_text: 'smart_home', guide_ids_text: '', scenario_template_ids_text: '',
        command_ids_text: '', command_group_ids_text: '',
        erid: '', advertiser_name: '', disclosure_ru: '', max_impressions_per_session: null,
      };
    },
    editDevicePick(p) {
      this.formError = '';
      this.devicePickEditing = true;
      this.devicePickForm = {
        ...p,
        cta_ru: p.cta_ru || '',
        priority: p.priority || 0,
        placements_text: (p.placements || []).join(','),
        tags_text: (p.tags || []).join(','),
        device_types_text: (p.device_types || []).join(','),
        category_ids_text: (p.category_ids || []).join(','),
        guide_ids_text: (p.guide_ids || []).join(','),
        scenario_template_ids_text: (p.scenario_template_ids || []).join(','),
        command_ids_text: (p.command_ids || []).join(','),
        command_group_ids_text: (p.command_group_ids || []).join(','),
        erid: p.erid || '',
        advertiser_name: p.advertiser_name || '',
        disclosure_ru: p.disclosure_ru || '',
        max_impressions_per_session: p.max_impressions_per_session ?? null,
      };
    },
    parseLines(text) {
      return (text || '').split('\n').map(s => s.trim()).filter(Boolean);
    },
    parseCsvIds(text) {
      return String(text || '').split(/[,\n]/).map((s) => s.trim()).filter(Boolean);
    },
    async uploadDeviceImage(event, formKey) {
      const file = event.target.files?.[0];
      if (!file || !this[formKey]) return;
      const slugField = this[formKey].id?.trim();
      if (!slugField) {
        this.formError = 'Сначала укажите ID (slug для файла)';
        return;
      }
      const reader = new FileReader();
      reader.onload = async () => {
        try {
          const dataUrl = reader.result;
          const res = await this.api('/admin/api/smarthome/upload-image', {
            method: 'POST',
            body: {
              slug: slugField,
              image_base64: dataUrl,
            },
          });
          if (res?.image_url) {
            this[formKey].image_url = res.image_url;
            this.showToast('Картинка загружена');
          }
        } catch (e) {
          this.formError = this.networkErrorMessage(e);
        }
      };
      reader.readAsDataURL(file);
    },
    async saveDeviceGuide() {
      await this.runSaving(async () => {
        const f = this.deviceGuideForm;
        const body = {
          id: f.id.trim(),
          title_ru: f.title_ru.trim(),
          summary_ru: f.summary_ru.trim(),
          capabilities_ru: f.capabilities_ru.trim(),
          setup_ru: f.setup_ru.trim(),
          setup_steps_ru: this.parseLines(f.setup_steps_text),
          related_devices_ru: this.trimOrNull(f.related_devices_ru),
          related_device_ids: this.parseCsvIds(f.related_device_ids_text),
          command_device_filter_id: this.trimOrNull(f.command_device_filter_id),
          image_url: this.trimOrNull(f.image_url),
          action_url: f.action_url.trim(),
          sort_order: Number(f.sort_order) || 0,
        };
        if (this.deviceGuideEditing) {
          await this.api(`/admin/api/smarthome/device-guides/${body.id}`, { method: 'PUT', body });
        } else {
          await this.api('/admin/api/smarthome/device-guides', { method: 'POST', body });
        }
        this.deviceGuideForm = null;
        await this.loadSmartHomeDevices();
        this.showToast('Guide сохранён');
      });
    },
    async deleteDeviceGuide(id) {
      if (!confirm(`Удалить guide ${id}?`)) return;
      await this.api(`/admin/api/smarthome/device-guides/${id}`, { method: 'DELETE' });
      await this.loadSmartHomeDevices();
    },
    async saveDevicePick() {
      await this.runSaving(async () => {
        const f = this.devicePickForm;
        const body = {
          id: f.id.trim(),
          title_ru: f.title_ru.trim(),
          description_ru: this.trimOrNull(f.description_ru),
          price_hint_ru: null,
          image_url: this.trimOrNull(f.image_url),
          action_url: f.action_url.trim(),
          cta_ru: this.trimOrNull(f.cta_ru),
          sort_order: Number(f.sort_order) || 0,
          priority: Number(f.priority) || 0,
          placements: this.parseCsvIds(f.placements_text),
          tags: this.parseCsvIds(f.tags_text),
          device_types: this.parseCsvIds(f.device_types_text),
          category_ids: this.parseCsvIds(f.category_ids_text),
          guide_ids: this.parseCsvIds(f.guide_ids_text),
          scenario_template_ids: this.parseCsvIds(f.scenario_template_ids_text),
          command_ids: this.parseCsvIds(f.command_ids_text),
          command_group_ids: this.parseCsvIds(f.command_group_ids_text),
          erid: this.trimOrNull(f.erid),
          advertiser_name: this.trimOrNull(f.advertiser_name),
          disclosure_ru: this.trimOrNull(f.disclosure_ru),
          starts_at: this.trimOrNull(f.starts_at),
          ends_at: this.trimOrNull(f.ends_at),
          max_impressions_per_session: f.max_impressions_per_session ? Number(f.max_impressions_per_session) : null,
        };
        if (this.devicePickEditing) {
          await this.api(`/admin/api/smarthome/device-picks/${body.id}`, { method: 'PUT', body });
        } else {
          await this.api('/admin/api/smarthome/device-picks', { method: 'POST', body });
        }
        this.devicePickForm = null;
        await this.loadSmartHomeDevices();
        this.showToast('Pick сохранён');
      });
    },
    async deleteDevicePick(id) {
      if (!confirm(`Удалить pick ${id}?`)) return;
      await this.api(`/admin/api/smarthome/device-picks/${id}`, { method: 'DELETE' });
      await this.loadSmartHomeDevices();
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
      if (!confirm('Опубликовать текущий draft как live bundle? Пользователи app получат новую версию.')) return;
      this.loading = true;
      try {
        const r = await this.api('/admin/api/publish', { method: 'POST', body: {} });
        this.showToast(`Опубликовано v${r.contentVersion}`);
        await this.refreshAfterPublishMutation();
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      } finally {
        this.loading = false;
      }
    },

    async rollback(version) {
      if (!confirm(`Откатить live bundle на v${version}? Текущий draft не изменится.`)) return;
      try {
        await this.api('/admin/api/publish/rollback', { method: 'POST', body: { content_version: version } });
        this.showToast(`Откат на v${version}`);
        await this.refreshAfterPublishMutation();
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
        command_groups: (parsed.command_groups || []).length,
        commands: (parsed.commands || []).length,
        scenario_templates: (parsed.scenario_templates || []).length,
        checklist_items: (parsed.checklist_items || []).length,
      };
      const preview = { counts, schemaVersion: parsed.schema_version ?? null };
      if (this.importMode === 'merge') {
        preview.mergeDiff = {
          categories: this.diffImportIds(parsed.categories, this.categories),
          command_groups: this.diffImportIds(parsed.command_groups, this.allCommandGroups),
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
        { label: 'Группы команд', value: c.command_groups },
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
        command_groups: 'Группы',
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
      return this.diffSections(this.publishedDiff);
    },

    diffSections(diff) {
      if (!diff) return [];
      return [
        { key: 'commands', label: 'Команды', section: diff.commands },
        { key: 'command_groups', label: 'Группы команд', section: diff.command_groups },
        { key: 'categories', label: 'Категории', section: diff.categories },
        { key: 'scenario_templates', label: 'Шаблоны', section: diff.scenario_templates },
        { key: 'checklist_items', label: 'Чеклист', section: diff.checklist_items },
      ];
    },

    filteredDiffItems(section, context = 'import') {
      if (!section?.items) return [];
      const filter = context === 'pipeline' ? this.pipelineDiffFilter : this.diffFilter;
      const needsReviewOnly = context === 'pipeline' ? this.pipelineDiffNeedsReviewOnly : this.diffNeedsReviewOnly;
      let items = section.items;
      if (filter !== 'all') {
        items = items.filter((i) => i.change === filter);
      }
      if (needsReviewOnly) {
        items = items.filter((i) => (i.tags || []).includes('needs_review'));
      }
      return items;
    },

    pipelineDiffIsEmptyCatalog() {
      const s = this.pipelineDiff?.summary;
      if (!s) return false;
      return s.added === 0 && s.changed === 0 && s.removed === 0;
    },

    diffFieldEntries(item) {
      if (!item?.field_diffs) return [];
      return Object.entries(item.field_diffs).map(([field, diff]) => ({ field, old: diff.old, new: diff.new }));
    },

    publishedDiffSummaryText() {
      return this.diffSummaryText(this.publishedDiff);
    },

    diffSummaryText(diff) {
      const s = diff?.summary;
      if (!s) return '';
      const base = diff.base_content_version != null
        ? `сравнение с опубликованным v${diff.base_content_version}`
        : `сравнение с ${diff.base}`;
      return `${base}: +${s.added} / ~${s.changed} / −${s.removed}`;
    },

    pipelineDiffHasVisibleItems() {
      if (!this.pipelineDiff) return false;
      return this.diffSections(this.pipelineDiff).some(
        (sec) => this.filteredDiffItems(sec.section, 'pipeline').length > 0,
      );
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
      if (parsed.records && Array.isArray(parsed.records) && parsed.records.some((r) => r?.edit?.title_ru != null)) {
        this.importDraft = { name: file.name, sizeBytes: file.size, text: null, parsed: null, preview: null };
        this.importParseError = 'Это устаревший editorial JSON. Используйте content bundle или правьте команды во вкладке «Команды».';
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
        this.showToast('Bundle импортирован — проверьте diff и публикацию');
        this.clearImportFile();
        this.view = 'content';
        await this.refreshAfterDraftMutation({ reloadDiff: true });
      } catch (e) {
        this.error = this.networkErrorMessage(e);
      } finally {
        this.importLoading = false;
      }
    },
  };
}
