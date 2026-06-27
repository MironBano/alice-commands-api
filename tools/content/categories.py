"""Launch categories checklist (FEATURES.md §5)."""
from models import ParsedCategory

YANDEX_SKILLS_BASE = "https://alice.yandex.ru/support/ru/station/skills/"

LAUNCH_CATEGORIES: list[ParsedCategory] = [
    ParsedCategory("general", "Основные команды", 1, f"{YANDEX_SKILLS_BASE}", "Общие команды для колонки и телефона", "star", True, ["station", "phone"]),
    ParsedCategory("music", "Музыка и радио", 2, f"{YANDEX_SKILLS_BASE}music/", "Воспроизведение музыки, радио и подкастов", "music", True, ["station", "phone"]),
    ParsedCategory("audiobooks", "Аудиокниги", 3, f"{YANDEX_SKILLS_BASE}", "Слушать аудиокниги и главы", "book", False, ["station", "phone"]),
    ParsedCategory("tv_video", "Фильмы и ТВ", 4, f"{YANDEX_SKILLS_BASE}tv/", "Управление телевизором и видеосервисами", "tv", True, ["station", "tv"]),
    ParsedCategory("quick_answers", "Быстрые ответы", 5, f"{YANDEX_SKILLS_BASE}weather/", "Погода, факты, переводы", "cloud", False, ["station", "phone"]),
    ParsedCategory("timers", "Часы и таймеры", 6, f"{YANDEX_SKILLS_BASE}timers/", "Будильники, таймеры и напоминания", "timer", True, ["station", "phone"]),
    ParsedCategory("smart_home", "Умный дом", 7, f"{YANDEX_SKILLS_BASE}smart-home/", "Свет, розетки, сценарии и камеры", "home_iot", True, ["station", "phone"]),
    ParsedCategory("station_settings", "Настройки колонки", 8, f"{YANDEX_SKILLS_BASE}", "Громкость, эквалайзер, режимы колонки", "speaker", False, ["station"]),
    ParsedCategory("calls", "Звонки и радионяня", 9, f"{YANDEX_SKILLS_BASE}calls/", "Звонки и режим радионяни", "phone_call", False, ["station", "phone"]),
    ParsedCategory("kids", "Дети и игры", 10, f"{YANDEX_SKILLS_BASE}kids/", "Детские игры и сказки", "child", False, ["station"]),
    ParsedCategory("alice_plus", "Алиса Плюс", 11, f"{YANDEX_SKILLS_BASE}plus/", "Команды с подпиской Плюс", "plus", False, ["station", "phone"]),
    ParsedCategory("quick_commands", "Быстрые команды", 12, "https://alice.yandex.ru/support/ru/station/start/quick-commands", "Команды без слова «Алиса»", "bolt", True, ["station", "phone"]),
    ParsedCategory("obscure", "Неочевидные команды", 13, f"{YANDEX_SKILLS_BASE}", "Подсветка, эмбиент, скрытые возможности", "sparkles", False, ["station"]),
]

CATEGORY_BY_ID = {c.id: c for c in LAUNCH_CATEGORIES}
