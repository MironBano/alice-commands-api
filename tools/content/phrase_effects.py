"""Infer user-facing title and effect from command phrases."""
from __future__ import annotations

import re

ALICE_PREFIX_RE = re.compile(r"^[\s«\"]*(?:алиса|яндекс)[,\s]+", re.IGNORECASE)

PHRASE_RULES: list[tuple[re.Pattern[str], str, str]] = [
    (re.compile(r"^включи\s+аудиокнигу\s+(.+)$", re.I), "Включить аудиокнигу \\1", "Найдёт и включит аудиокнигу «\\1»."),
    (re.compile(r"^включи\s+(.+)\s+на\s+телевизор", re.I), "Включить \\1 на ТВ", "Откроет \\1 на телевизоре."),
    (re.compile(r"^включи\s+(.+)\s+везде$", re.I), "Включить \\1 везде", "Запустит \\1 на всех колонках в режиме «Играй везде»."),
    (re.compile(r"^включи\s+музыку\s+везде$", re.I), "Музыка везде", "Включит музыку на всех колонках."),
    (re.compile(r"^включи\s+(.+)$", re.I), "Включить \\1", "Запустит \\1."),
    (re.compile(r"^выключи\s+(.+)$", re.I), "Выключить \\1", "Выключит \\1."),
    (re.compile(r"^поставь\s+таймер", re.I), "Установить таймер", "Запустит таймер на указанное время."),
    (re.compile(r"^удали\s+таймер", re.I), "Удалить таймер", "Удалит установленный таймер."),
    (re.compile(r"^сколько\s+осталось\s+до\s+таймера", re.I), "Статус таймера", "Скажет, сколько осталось до срабатывания таймера."),
    (re.compile(r"^поставь\s+будильник", re.I), "Установить будильник", "Создаст будильник на указанное время."),
    (re.compile(r"^какой\s+прогноз\s+погоды", re.I), "Прогноз погоды", "Расскажет прогноз погоды."),
    (re.compile(r"^какая\s+погода", re.I), "Погода", "Сообщит текущую погоду."),
    (re.compile(r"^который\s+час", re.I), "Текущее время", "Сообщит текущее время."),
    (re.compile(r"^сколько\s+времени", re.I), "Текущее время", "Сообщит текущее время."),
    (re.compile(r"^громче$", re.I), "Громче", "Увеличит громкость."),
    (re.compile(r"^тише$", re.I), "Тише", "Уменьшит громкость."),
    (re.compile(r"^стоп$", re.I), "Стоп", "Остановит текущее действие."),
    (re.compile(r"^продолж", re.I), "Продолжить", "Продолжит воспроизведение."),
    (re.compile(r"^переключись\s+на\s+aux", re.I), "Переключить на AUX", "Переключит вход колонки на AUX."),
    (re.compile(r"^позвони", re.I), "Позвонить", "Инициирует звонок."),
    (re.compile(r"^останови\s+музыку\s+в\s+(.+)$", re.I), "Остановить музыку в \\1", "Остановит воспроизведение в комнате «\\1»."),
]

LONG_TITLE_MARKERS = (
    " после ",
    " вы можете ",
    " треки ",
    " воспроизвод",
    " включается ",
    " если ",
    " при ",
)


def phrase_body(phrase: str) -> str:
    return ALICE_PREFIX_RE.sub("", phrase).strip(" ,.")


def title_from_phrase(phrase: str, *, max_len: int = 60) -> str:
    inferred = infer_from_phrase(phrase)
    if inferred and inferred[0]:
        return inferred[0][:max_len]
    body = phrase_body(phrase)
    if not body:
        return phrase[:max_len]
    title = body[:1].upper() + body[1:]
    return title[:max_len]


def infer_from_phrase(phrase: str) -> tuple[str | None, str | None]:
    body = phrase_body(phrase)
    if not body or len(body) < 3:
        return None, None
    for pattern, title_tpl, effect_tpl in PHRASE_RULES:
        match = pattern.match(body)
        if not match:
            continue
        groups = match.groups()
        title = title_tpl
        effect = effect_tpl
        for idx, group in enumerate(groups, start=1):
            repl = group.strip()
            title = title.replace(f"\\{idx}", repl)
            effect = effect.replace(f"\\{idx}", repl)
        return title[:80], effect[:500]

    words = body.split()
    if len(words) == 1 and len(words[0]) >= 3 and words[0].isalpha():
        word = words[0]
        return f"Включить {word.lower()}", f"Запустит музыку или контент по запросу «{word}»."

    return None, None


def is_long_title(title: str) -> bool:
    if len(title) > 60:
        return True
    lower = title.lower()
    return any(marker in lower for marker in LONG_TITLE_MARKERS)


def is_weak_effect(effect: str, title: str = "") -> bool:
    if not effect or effect == "Требует вычитки":
        return True
    lower = effect.lower()
    if lower.startswith("выполнит:"):
        rest = effect.split(":", 1)[-1].strip().rstrip(".!?").lower()
        if not rest or rest == title.strip().rstrip(".!?").lower():
            return True
    t = title.strip().rstrip(".!?").lower()
    e = effect.strip().rstrip(".!?").lower()
    return bool(t and t == e)


def improve_effect(phrase: str, title: str, effect: str) -> str:
    if not is_weak_effect(effect, title):
        return effect
    inferred = infer_from_phrase(phrase)
    if inferred[1] and not is_weak_effect(inferred[1], title):
        return inferred[1]
    from effect_templates import humanize_table_effect

    candidate = humanize_table_effect(title, phrase=phrase)
    if not is_weak_effect(candidate, title):
        return candidate
    body = phrase_body(phrase)
    if body:
        return f"Алиса выполнит команду «{body[:70]}»."
    return effect
