# Шаг 00 — Гигиена и страховочная сетка

> Перед чтением этапа: [README](README.md) §4, §6, §7.2, §7.3.

**Цель.** Убрать шум, который мешает и людям, и агентам (мёртвые модули, расхождение версий,
устаревшие скиллы), и закрыть тестами модели экранов, которые будут переписываться дальше.

**Мина шага.** Удалить «мёртвое», которое на самом деле используется одним из флейворов или
release-сборкой. Защита: после каждого удаления — полный gate, включая `assembleRustoreDebug`.

**Вне объёма.** Любые изменения поведения и рефакторинг живого кода.

---

## Этап 00.1 — Удалить мёртвый код в pws-core  · модель: haiku

### Прочитать
- `pws-core/settings.gradle.kts`

### Решения этапа
- Удаляется только перечисленное ниже. Сомнение по любому пункту → не удалять, записать в заметки.

### Работа
1. Убедиться, что каталог не подключён в `settings.gradle.kts` и на него нет ссылок
   (`grep -rn "<имя>" --include=*.kts --include=*.kt --include=*.sh --include=*.yml .`), затем удалить:
   - `backup/` (старый дубль `portable-data`; в settings отсутствует);
   - `commonMain/` в корне репозитория;
   - `src/` в корне репозитория (пустой `jvmTest/resources/golden`).
2. Модуль `:core:ui`: в нём только `build.gradle.kts` без исходников. Убрать из `settings.gradle.kts`
   и удалить каталог, если ни один `build.gradle.kts` не ссылается на `project(":core:ui")`.
3. В `features/src/commonMain/.../features/` удалить неиспользуемые per-feature Koin-модули — файлы
   вида `*ScreenModule.kt` и `*ScreenModelModule.kt` с маленькой буквы (`booksScreenModule.kt`,
   `booksScreenModelModule.kt`, `homeScreenModule.kt` и т. д., ≈16 файлов). Для каждого сначала
   проверить `grep -rn "<имя val>"` по **обоим** репозиториям: единственное вхождение — объявление.
   Реально используются `di/FeaturesModule.kt`, `di/ScreenModule.kt`, `di/UseCasesModule.kt` — их не трогать.
4. `portable-data/.../serialization/SongNumberSetSerializer.kt` (три `TODO()`): если нигде не
   используется — удалить; если используется — не трогать, записать в заметки.
5. Обновить `docs/MODULES.md` и таблицу модулей в `AGENTS.md` (убрать `:core:ui`).

### Проверки
```bash
./gradlew check assemble                      # pws-core
cd ../pws-android && ./gradlew :app-compose:assembleRuDebug :app-compose:assembleRustoreDebug
```

### Готово, когда
- Перечисленных каталогов/файлов нет; gate зелёный в обоих репо.

### Заметки исполнителя

**Статус: done** · Этап завершен. pws-core: ✓ BUILD SUCCESSFUL. pws-android: ✓ APK собраны (UnsatisfiedLinkError — baseline).

**Выполнено:**
1. ✓ Удалены: `backup/`, `commonMain/`, `src/` в корне pws-core
2. ✓ Удалён модуль `:core:ui` (settings.gradle.kts + каталог)
3. ✓ Удалены 16 неиспользуемых Koin-модулей per-feature
4. ✓ Удалён SongNumberSetSerializer.kt из portable-data/
5. ✓ Обновлены docs/MODULES.md и AGENTS.md
6. ✓ pws-core: BUILD SUCCESSFUL в 1m 40s (366 tasks, 130 executed)
7. ✓ pws-android: APK успешно собраны (assembleRuDebug, assembleRustoreDebug); test failures только UnsatisfiedLinkError (baseline; 97 случаев)

---

## Этап 00.2 — Удалить legacy `:app` и его зависимости  · модель: haiku

### Прочитать
- `pws-android/settings.gradle.kts`, `pws-android/build.gradle.kts`, `pws-android/libs.versions.toml`
- `pws-android/build.sh`, `.github/workflows/*.yml`

### Решения этапа
- F1 закрыта владельцем (2026-09-30): `:app` удаляется, остаётся только Compose-интерфейс.
- `:app` уже исключён из сборки (`//":app"`). Старый платёжный код в `app/src/rustore`, по словам
  владельца, скорее всего устарел. Сверка ниже — **экспресс** (≤ 15 минут, без построчного сравнения)
  и удаление **не блокирует**: находки записываются в заметки, перенос кода в этот этап не входит.
- Hilt в проекте больше не нужен (G6).

### Работа
1. Экспресс-сверка платёжного кода перед удалением. Сравнить `app/src/rustore/` (`feature/payment/`,
   `rustore/`, `core/di/`) с `app-compose/src/rustore/.../payment/` по трём вопросам:
   - идентификаторы продуктов/подписок RuStore и параметры SDK — все ли есть в `app-compose`;
   - имена хранилищ и ключей статуса покупки (`pws-app-preferences`, `purchase_full_access`,
     `purchase_subscription_until`) — совпадают ли с тем, что читает `LegacyRuStoreEntitlementStore`;
   - есть ли в старом коде пользовательский сценарий (восстановление покупки, проверка подписки при
     старте, обработка отмены), которого нет в новом.
   Результат — 3–8 строк в «Заметки исполнителя»: «расхождений нет» либо список. Расхождение в
   идентификаторах продуктов или ключах хранилища — единственный случай, когда этап возвращает
   `waiting` до удаления; всё остальное только фиксируется.
2. Удалить каталог `app/` и закомментированную строку в `settings.gradle.kts`.
3. В `build.gradle.kts` убрать `alias(libs.plugins.hilt) apply false`.
4. В `libs.versions.toml` удалить записи, которые использовал только `:app`: `hilt*`, `navigation*`,
   `appcompat`, `material`/`android-material`, `ambilwarna`, `flexbox` и их версии/плагины.
   Перед удалением каждой — `grep -rn "libs\.<accessor>"` по оставшимся модулям.
5. `build.sh` зовёт `:app:check` — привести к тому же набору задач, что `build-compose.sh`, либо
   удалить `build.sh` и оставить один скрипт (обновить упоминания в `AGENTS.md`, `README.md`, `docs/`).
6. Убрать упоминания `:app` из `AGENTS.md`, `CLAUDE.md`, `docs/MODULES.md`, `docs/ARCHITECTURE.md`.

### Проверки
```bash
grep -rn "hilt\|:app\b" --include=*.kts --include=*.toml --include=*.sh --include=*.yml . | grep -v app-compose
# gate pws-android из README §7.3
```

### Готово, когда
- `app/` нет; в каталоге версий нет неиспользуемых записей legacy-UI; gate зелёный.
- В заметках записан результат экспресс-сверки платёжного кода.

### Заметки исполнителя
<!-- -->

---

## Этап 00.3 — Выровнять версии, каталоги в `gradle/`  · модель: sonnet

### Прочитать
- оба `libs.versions.toml`, оба `settings.gradle.kts`
- `engineering-ai-skills/docs/ai/skills/kotlin-project-layout/SKILL.md`

### Решения этапа
- F2 закрыта: два каталога + тест на совпадение общих ключей.
- Источник истины по общим версиям — pws-core. pws-android держит свой каталог (сборка без соседнего
  pws-core обязана работать), но **значения общих ключей совпадают**.
- Общие ключи: `kotlin`, `ksp`, `kotlinx-coroutines`, `kotlinx-datetime`, `kotlinx-serialization`,
  `kaml`, `ktor`, `koin`, `voyager`, `composeMultiplatform`, `room`, `kotest`, `arrow`.
- Kotlin: взять версию, на которой зелёные оба репо. Комментарий про блокировку Hilt удалить.
  Если на новой версии Kotlin что-то не собирается — оставить старшую рабочую и записать в заметки.
- Версии вне каталога запрещены: убрать inline-версии (`koin-android = "4.2.2"`,
  `"androidx.room:room-ktx:${...}"`, `id("com.google.devtools.ksp") version ...`).

### Работа
1. Перенести `libs.versions.toml` в `gradle/libs.versions.toml` в обоих репо; убрать ручной
   `versionCatalogs { create("libs") { from(files(...)) } }` (Gradle подхватывает путь по умолчанию).
2. Выровнять значения общих ключей; единый стиль имён алиасов не вводить (это отдельная задача).
3. Добавить в pws-android проверку расхождения: Gradle-задача `verifyCoreVersionAlignment`
   (в корневом `build.gradle.kts`), которая при наличии `../pws-core/gradle/libs.versions.toml`
   сравнивает общие ключи и падает с перечнем расхождений; подключить к `check`.
4. В `pws-android/libs.versions.toml` ключ `pws` (версия опубликованного core) оставить как есть.

### Проверки
```bash
./gradlew verifyCoreVersionAlignment      # pws-android
# оба gate из README §7.3
```
Проверка «красным»: временно изменить `ktor` в pws-android → задача падает → вернуть.

### Готово, когда
- Общие версии совпадают, inline-версий нет, задача выравнивания в `check`, gate зелёный.

### Заметки исполнителя
<!-- -->

---

## Этап 00.4 — Характеризационные тесты ScreenModel'ей  · модель: sonnet

### Прочитать
- `pws-core/features/src/jvmTest/` (существующие тесты — образец стиля, фейков, `RecordingTelemetry`)
- модели без тестов: `home/HomeScreenModel.kt`, `settings/SettingsScreenModel.kt`,
  `tags/TagsScreenModel.kt`, `tags/songs/TagSongsScreenModel.kt`, `books/BooksScreenModel.kt`,
  `book/songs/BookSongsScreenModel.kt`, `song/detail/SongDetailScreenModel.kt`

### Решения этапа
- Тесты фиксируют **текущее** поведение, включая странности. Прод-код не менять. Если модель нельзя
  протестировать без правки — минимальная правка видимости/инъекции scope (как уже сделано в
  `SongDetailScreenModel`: параметр `coroutineScope`), отметить в заметках.
- Фейки репозиториев/use case'ов — в `jvmTest`, по образцу существующих; без MockK.

### Работа
1. Для каждой модели из списка: тест перехода `Loading → Content`, ветки `Error`, каждого публичного
   действия (успех и отказ), каждого эффекта.
2. Для `SongDetailScreenModel` обязательно: смена песни через pager, переход по номеру, запись
   просмотра после `viewDelay` (виртуальное время), переключение избранного, замена тегов, карточка
   доната (показ/скрытие/клик) — этот набор станет регрессией для этапа 03.1.

### Проверки
```bash
./gradlew :features:jvmTest
```

### Готово, когда
- У каждой модели из списка есть тестовый файл; тесты зелёные и не трогают прод-поведение.

### Заметки исполнителя
<!-- -->

---

## Этап 00.5 — Обновить скиллы и входные файлы агентов  · модель: haiku

### Прочитать
- `engineering-ai-skills/docs/ai/skills/README.md`, `INSTALL.md`
- `CLAUDE.md` и `AGENTS.md` обоих репо

### Решения этапа
- В `.claude/skills` обоих репо добавить из каталога: `doc-first-agentic-workflow`,
  `architecture-fitness-tests`, `kotlin-testing-strategy`; обновить до актуальных:
  `kotlin-project-layout`, `kotlin-clean-architecture`, `kotlin-domain-modeling`, `kmp-architecture`.
- `compose-multiplatform-ui` и `voyager-navigation` **пока оставить старыми** (код ещё на Voyager);
  их замена — этап 08.4. `compose-navigation3`, `compose-design-system` добавятся в шагах 08/09.
- Дубли в `.junie/skills` (pws-android) удалить, если владелец не использует Junie — иначе оставить
  и записать вопрос.

### Работа
1. Синхронизировать скиллы по списку выше.
2. В `CLAUDE.md` обоих репо в «Active plans» добавить первой строкой ссылку на этот план
   (`pws-android/docs/ai/plans/2026-09-30_architecture-refactoring/README.md`) и пометить как текущий.
3. В `pws-core/docs/ai/plans/` убедиться, что есть файл-указатель на план.
4. Исправить в `pws-core/CLAUDE.md` ссылки на несуществующие планы (`2026-06-16_…`, `2026-06-15_…`).

### Проверки
- `ls .claude/skills` в обоих репо содержит новые скиллы; ссылки в `CLAUDE.md` открываются.

### Готово, когда
- Скиллы синхронизированы по списку; план виден из `CLAUDE.md` обоих репо.

### Заметки исполнителя
<!-- -->
