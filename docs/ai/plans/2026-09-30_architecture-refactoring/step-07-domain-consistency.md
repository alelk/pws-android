# Шаг 07 — Согласованность домена

> Перед чтением этапа: [README](README.md) §4, §6, §7.2, §7.3.
> Опорные скиллы: `kotlin-domain-modeling`, `kotlin-clean-architecture` (актуальные версии).

**Цель.** Домен уже в хорошей форме; шаг убирает расхождения между кодом и документами и несколько
несогласованностей формы. Низкий приоритет, низкий риск.

**Мина шага.** «Улучшение» сигнатур use case'ов ломает бинарную совместимость для pws-server,
который потребляет опубликованный `:domain`. Защита: публичные сигнатуры существующих use case'ов и
портов не меняются; добавлять — можно, переименовывать и удалять — только то, что не используется
нигде, включая `pws-server` (проверять `grep` по `../pws-server`, если каталог есть рядом).

**Вне объёма.** `:api:*`, пакеты `auth`, `payment` (используются сервером), схема БД.

---

## Этап 07.1 — Единая форма use case, документы ↔ код  · модель: sonnet

### Прочитать
- `pws-core/AGENTS.md` §3, §5–§9; `docs/ARCHITECTURE.md`
- `domain/.../core/error/`, 3–4 use case'а записи разных сущностей (как возвращают ошибки)
- `domain/.../booklibrary/usecase/` (интерфейсы)

### Решения этапа
- Модель ошибок — `Either<…Error, T>` (G7). В `AGENTS.md §3` фразу «Write paths return typed sealed
  results» заменить описанием фактического соглашения с примером из кода.
- Правило формы: use case — `class` с одним `operator fun invoke`. Исключение — use case'ы с
  платформенной реализацией (`InstallBookUseCase`, `UninstallBookUseCase`, `UpdateBookUseCase`):
  `fun interface` в домене, реализация в адаптере. Исключение перечислено поимённо в `AGENTS.md`
  и в `UseCaseShapeTest` (pinned-список с причиной).
- Use case'ы чтения, которые сегодня объявлены не в `:domain`, а в `features`
  (`GetSongReferencesWithDetailsUseCase` регистрируется в `FeaturesModule`) — регистрацию перенести в
  `UseCasesModule`; сам класс уже в домене.

### Работа
1. Привести `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/DATA_FLOW.md` в соответствие с кодом после
   шагов 03–06 (порты настроек, старт, бэкап, импорт).
2. Дополнить `UseCaseShapeTest` правилом формы и pinned-исключениями.
3. Перенести регистрацию в `UseCasesModule`.

### Проверки
```bash
./gradlew build
```

### Готово, когда
- Документы описывают фактический код; форма use case закреплена тестом.

### Заметки исполнителя
**07.1 (sonnet, 2026-10-06) — done.**
- pws-core: `UseCaseShapeTest` — правило формы: use case = `class` ровно с одним `operator fun invoke`; pinned `PLATFORM_USE_CASES` (Install/Uninstall/UpdateBookUseCase, с причиной) обязаны быть `fun interface`; `KNOWN_SHAPE_VIOLATIONS` пуст. Красным проверено: второй `operator fun invoke` в `ClearHistoryUseCase` ⇒ тест падает; откат. Три интерфейса booklibrary стали `fun interface` (источник и бинарно совместимо; реализации/`object :` не менялись).
- `GetSongReferencesWithDetailsUseCase` — регистрация перенесена из `FeaturesModule` в `UseCasesModule` (явные типы `get<...>()`); неиспользуемый импорт убран.
- Документы: `AGENTS.md` (§3 — `Either<XxxError, T>` с примерами + порты настроек/старта/бэкапа/импорта; §8 — правило формы и исключения поимённо), `docs/ARCHITECTURE.md` (раздел «Platform ports», форма use case), `docs/DATA_FLOW.md` (транзакции `inRw/inRoTransaction`, схема settings/startup/backup/import).
- Отклонения: ktlint-baseline `features` регенерирован (сдвиг строк DI-модулей; `<error` 2347 → 2346, не вырос); мой длинный вызов в `UseCasesModule` отформатирован вручную, чтобы не плодить detekt MaxLineLength. `--tests '*UseCaseShapeTest*'` в песочнице пишет «No tests found» — гонять `:domain:jvmTest` целиком.
- Мимоходом (не трогал): use case'ы `:portable-data` (`ExportBackup/RestoreBackup/ImportBookBundleUseCase`) вне `:domain` и не под `UseCaseShapeTest`; pws-server рядом есть, переименований/удалений публичных сигнатур нет.
- Gate: pws-core `build $(corex)` — BUILD SUCCESSFUL; pws-android `:data:content-delivery:compileDebugUnitTestKotlin :app-compose:compileRustoreDebugUnitTestKotlin` — успешно. Не закоммичено.

---

## Этап 07.2 — Pass-through и доступ мимо use case  · модель: sonnet

### Прочитать
- `features/.../di/UseCasesModule.kt`, `FeaturesModule.kt`
- `domain/.../*/usecase/` — список файлов (`find domain/src/commonMain -path '*usecase*'`)

### Решения этапа
- Pass-through use case (тело — один вызов репозитория без транзакции и без логики) **оставляем**:
  это цена единого правила «UI → use case», и она дешевле исключений.
- Убираем обратное: неиспользуемые use case'ы (нет потребителей ни в pws-core, ни в pws-android,
  ни в pws-server) — удалить вместе с регистрацией.
- Use case чтения из локальной БД без `TransactionRunner.inRoTransaction`, выполняющий **больше
  одного** запроса, — обернуть в транзакцию (согласованное чтение).

### Работа
1. Составить список неиспользуемых use case'ов (в заметки), удалить подтверждённые.
2. Найти многозапросные чтения без транзакции, обернуть, добавить тест.

### Проверки
```bash
./gradlew build      # оба репо
```

### Готово, когда
- Неиспользуемых use case'ов нет; список удалённого — в заметках.

### Заметки исполнителя
<!-- -->

---

## Этап 07.3 — (опционально, F6) generic id у тегов  · модель: opus

**Статус: `skipped`** — F6 закрыта 2026-09-30: generic-параметр оставляем. Текст ниже сохранён на
случай пересмотра решения; оркестратор этап не запускает.

### Прочитать
- `domain/.../tag/`, `domain/.../songtag/` — все объявления с параметром типа id
- потребители в `features` (`Tag<TagId>`, `UseCase<TagId>`) и в `../pws-server`

### Решения этапа
- Если pws-server использует другой тип id (пользовательские теги) — **стоп**, generic нужен;
  записать в заметки, статус `waiting`.
- Иначе: заменить параметр типа конкретным `TagId`, сохранив имена классов.

### Проверки
```bash
./gradlew build      # оба репо
```

### Готово, когда
- В сигнатурах нет `<TagId>`-параметризации либо зафиксирована причина, почему она остаётся.

### Заметки исполнителя
<!-- -->
