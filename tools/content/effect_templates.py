"""Turn Yandex table «Результат» cells into user-facing effect descriptions."""
from __future__ import annotations

import re

NEEDS_REVIEW = "Требует вычитки"

TABLE_EFFECT_RULES: list[tuple[re.Pattern[str], str]] = [
    (re.compile(r"^установить\s+таймер", re.I), "Запустит таймер на указанное время."),
    (re.compile(r"^управлять\s+таймер", re.I), "Покажет оставшееся время, поставит таймер на паузу или удалит его."),
    (re.compile(r"^удалить\s+таймер", re.I), "Удалит установленный таймер."),
    (re.compile(r"^выключить\s+таймер", re.I), "Остановит сигнал активного таймера."),
    (re.compile(r"^установить\s+будиль", re.I), "Создаст будильник на указанное время."),
    (re.compile(r"^управлять\s+будиль", re.I), "Покажет, изменит или отключит будильники."),
    (re.compile(r"^установить\s+напоминан", re.I), "Создаст напоминание на указанное время."),
    (re.compile(r"остановить.*пауз", re.I), "Остановит или поставит на паузу воспроизведение."),
    (re.compile(r"^продолжить", re.I), "Продолжит воспроизведение с места остановки."),
    (re.compile(r"^включить\s+музык", re.I), "Запустит музыку — исполнителя, альбом или плейлист."),
    (re.compile(r"^пропустить", re.I), "Переключит на следующий трек."),
    (re.compile(r"^вернуть", re.I), "Вернётся к предыдущему треку."),
    (re.compile(r"^повтор", re.I), "Повторит текущий или указанный трек."),
    (re.compile(r"^активировать\s+режим", re.I), "Включит указанный режим на колонках."),
    (re.compile(r"^выключить\s+режим", re.I), "Выключит указанный режим."),
    (re.compile(r"^включить\s+нужн", re.I), "Включит выбранную языковую или субтитровую дорожку."),
    (re.compile(r"^управлять\s+громкост", re.I), "Изменит громкость колонки или музыки."),
    (re.compile(r"^включить\s+видео", re.I), "Найдёт и запустит видео из интернета или сервиса."),
    (re.compile(r"^включить\s+фильм", re.I), "Найдёт и запустит фильм на телевизоре."),
    (re.compile(r"^включить\s+сериал", re.I), "Найдёт и запустит сериал или нужную серию."),
    (re.compile(r"^включить\s+канал", re.I), "Переключит телевизор на указанный канал."),
    (re.compile(r"^включить\s+радио", re.I), "Включит радиостанцию."),
    (re.compile(r"^включить\s+плейлист", re.I), "Запустит указанный плейлист."),
    (re.compile(r"^включить\s+альбом", re.I), "Запустит указанный альбом."),
    (re.compile(r"^включить\s+аудиокниг", re.I), "Найдёт и включит аудиокнигу."),
    (re.compile(r"^найти\s+рецепт", re.I), "Найдёт и озвучит рецепт."),
    (re.compile(r"^запустить\s+игр", re.I), "Запустит игру или интерактив."),
    (re.compile(r"^вызов\s+", re.I), "Инициирует звонок."),
    (re.compile(r"^позвон", re.I), "Инициирует звонок."),
    (re.compile(r"аудиокниг", re.I), "Найдёт и включит аудиокнигу."),
    (re.compile(r"альбом", re.I), "Запустит альбом — треки идут по порядку."),
]

INFINITIVE_TO_FUTURE: dict[str, str] = {
    "установить": "Установит",
    "включить": "Включит",
    "выключить": "Выключит",
    "удалить": "Удалит",
    "остановить": "Остановит",
    "управлять": "Позволит управлять",
    "активировать": "Активирует",
    "продолжить": "Продолжит",
    "запустить": "Запустит",
    "найти": "Найдёт",
    "показать": "Покажет",
    "переключить": "Переключит",
    "отключить": "Отключит",
    "включите": "Включит",
    "выключите": "Выключит",
    "вызвать": "Позвонит",
    "позвонить": "Позвонит",
    "создать": "Создаст",
    "изменить": "Изменит",
    "отменить": "Отменит",
}


def _trim_result_cell(raw: str) -> str:
    text = re.sub(r"\s+", " ", raw.strip())
    if "/" in text and len(text.split("/")[0]) < 60:
        text = text.split("/")[0].strip()
    for stop in (
        " Треки ",
        " После ",
        " Вы можете ",
        ". ",
        " Если ",
        " При ",
        " Воспроизвод",
        " включается ",
    ):
        idx = text.find(stop)
        if idx > 10:
            text = text[:idx].strip()
    return text


def humanize_table_effect(result_raw: str, *, phrase: str | None = None) -> str:
    if phrase:
        from phrase_effects import infer_from_phrase, is_weak_effect

        inferred = infer_from_phrase(phrase)
        if inferred[1] and not is_weak_effect(inferred[1], _trim_result_cell(result_raw)):
            return inferred[1]

    text = _trim_result_cell(result_raw)
    if not text:
        return NEEDS_REVIEW

    for pattern, effect in TABLE_EFFECT_RULES:
        if pattern.search(text):
            return effect

    first = text.split()[0].lower().rstrip(",") if text.split() else ""
    future = INFINITIVE_TO_FUTURE.get(first)
    if future:
        rest = text[len(text.split()[0]) :].strip(" /,-")
        if len(rest) > 70:
            rest = rest[:70].rsplit(" ", 1)[0]
        if rest:
            return f"{future} {rest[0].lower()}{rest[1:]}."
        return f"{future}."

    if phrase:
        from phrase_effects import phrase_body

        body = phrase_body(phrase)
        if body:
            return f"Алиса выполнит команду «{body[:70]}»."

    short = text if len(text) <= 60 else text[:60].rsplit(" ", 1)[0]
    return f"Алиса выполнит: {short[0].lower()}{short[1:]}."
