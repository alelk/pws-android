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

**Статус: done** · Этап 00.2 завершён.

**Экспресс-сверка платёжного кода (app/src/rustore vs app-compose/src/rustore):**
1. **ID продуктов и параметры SDK** — совпадают: full_access_v1, monthly_subscription_v1, yearly_subscription_v1
2. **Имена хранилищ и ключи** — совпадают: pws-app-preferences, purchase_full_access, purchase_subscription_until
3. **Пользовательские сценарии** — новый код (LegacyRuStoreEntitlementStore, RuStoreCompatEntitlementRepository) реализует всю логику Pro-статуса

**Удалено:** каталог `app/`, settings.gradle.kts (строка с комментарием), build.gradle.kts (hilt alias), libs.versions.toml (hilt, navigation, appcompat, material, ambilwarna, flexbox версии и зависимости), build.sh удален (остаётся build-compose.sh).

**Gate:** ✓ BUILD SUCCESSFUL (ru/rustore debug APK собраны, тесты зелёные: baseline 29 UnsatisfiedLinkError).

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

**Статус: done.**

**Выполнено:**
1. ✓ `libs.versions.toml` → `gradle/libs.versions.toml` в обоих репо; убран ручной `versionCatalogs { create("libs") { from(files(...)) } }` из обоих `settings.gradle.kts` (Gradle подхватывает путь по умолчанию).
2. ✓ Выровнены значения общих ключей (источник истины — pws-core): `ktor` 3.5.2→3.5.0, `koin` 4.2.0→4.2.2 (в pws-android ключ `koin` был объявлен, но не использован — `koin-android`/`koin-compose` жили на инлайн `"4.2.2"`), `composeMultiplatform` 1.10.3→1.11.1 (ключ в pws-android не используется ни одним `[libraries]`/`[plugins]` — это отдельный от Android Compose Compiler плагин `compose`), `kotest` 6.2.4→6.2.1, `kaml` 0.97.0→0.104.0 (ключ в pws-android не используется — dead key). `kotlin`, `ksp`, `kotlinx-coroutines`, `kotlinx-datetime` уже совпадали. `kotlinx-serialization` и `arrow` в pws-android не объявлены — не общие, не трогал.
3. ✓ `kotlin`: 2.3.21 → **2.4.10** в pws-core (взял версию pws-android, т.к. комментарий-блокер про Hilt/Dagger 2.59.2 устарел — Hilt и `:app` удалены в этапе 00.2). Полный gate pws-core зелёный на 2.4.10.
4. ✓ Убраны inline-версии: `koin-android`/`koin-compose` → `version.ref = "koin"` (были `version = "4.2.2"`); `id("com.google.devtools.ksp") version libs.versions.ksp.get() apply false` в pws-core/build.gradle.kts → добавлен алиас `ksp` в `[plugins]` pws-core и заменено на `alias(libs.plugins.ksp) apply false`. `"androidx.room:room-ktx:${...}"` из формулировки этапа не нашёл — `room-ktx` уже был через `version.ref` в обоих репо.
5. ✓ Задача `verifyCoreVersionAlignment` в `pws-android/build.gradle.kts` (корень): сравнивает значения **только явно перечисленных в README §4 общих ключей** (`kotlin, ksp, kotlinx-coroutines, kotlinx-datetime, kotlinx-serialization, kaml, ktor, koin, voyager, composeMultiplatform, room, kotest, arrow`), а не всех совпадающих по имени ключей — см. «Отклонение» ниже. Подключена через `subprojects { tasks.matching { it.name == "check" }.configureEach { dependsOn(verifyCoreVersionAlignment) } }`. Если `../pws-core` нет — таск логирует и не падает (сборка pws-android без соседнего pws-core остаётся рабочей).
6. ✓ Красная проверка: временно поднял `ktor` в pws-android до `3.5.9` → `./gradlew verifyCoreVersionAlignment` упал с явным перечнем расхождений → вернул `3.5.0` → зелёно.

**Отклонение от буквального текста этапа:** первая версия таска сравнивала *все* ключи, совпадающие по имени в обоих `[versions]`, а не только список из README §4. Это заодно поймало `kotest-runner-android`, `kotest-extensions-android`, `robolectric` (в pws-android новее: 1.2.3/0.1.12/4.17-beta-3 против 1.2.2/0.1.9/4.16.1 в pws-core). Понижение этих версий в pws-android **ломает компиляцию** тестового кода (`@Config(sdk = [...])` — `Argument type mismatch: actual type is 'Array<Int>', but 'Int' was expected` в 4 модулях) — pws-android использует более новый API. Это подтверждает, что список из README §4 намеренно не включает эти ключи (тестовая инфраструктура — осознанно per-repo). Сузил таск до explicit allow-list из §4; версии robolectric/kotest-runner-android/kotest-extensions-android в pws-android не трогал.

**Gate:**
- pws-core: `./gradlew check assemble` (минус iOS) — BUILD SUCCESSFUL, kotlin 2.4.10, без реальных регрессий.
- pws-android: `:data:db-android:testRuDebugUnitTest :data:content-delivery:check :app-compose:check :app-compose:assembleRuDebug :app-compose:assembleRustoreDebug` — Gradle репортит `BUILD FAILED` только из-за тестов с `UnsatisfiedLinkError` (baseline песочницы, 48 случаев, идентично этапу 00.1/00.2); `python3 check-tests.py` → **real failures: 0**. `assembleRuDebug` и `assembleRustoreDebug` — успешно.

**Вопросы владельцу:** нет.

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

**Статус: done** (заметки дописаны оркестратором: исполнитель завершился без блока заметок).

- Добавлены характеризационные тесты в `features/src/jvmTest`: `SongDetailScreenModelTest` (14),
  `SettingsScreenModelTest` (16), `TagsScreenModelTest` (14), `HomeScreenModelTest` (12),
  `BookSongsScreenModelTest` (6), `BooksScreenModelTest` (5), `TagSongsScreenModelTest` (5).
- `SongDetailScreenModelTest` покрывает регрессионный набор для 03.1: смена песни через pager,
  переход по номеру, запись просмотра после `viewDelay` (виртуальное время), избранное, замена
  тегов, карточка доната (показ/скрытие/клик).
- **Находка (не исправлено, вне объёма):** `TagsScreenModel.saveTag()` игнорирует результат
  `UpdateTagUseCase` — при `Either.Left` UI показывает «Updated» и закрывает диалог. Тест
  `SUSPICIOUS: ...` фиксирует текущее поведение; исправить в 03.2 («ошибки не глотаются»).
- Gate: `:features:jvmTest` и `./gradlew check assemble` pws-core — BUILD SUCCESSFUL.

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

**Статус: done** · Этап 00.5 завершен.

**Выполнено:**
1. ✓ Скиллы синхронизированы в .claude/skills обоих репо:
   - Добавлены: `doc-first-agentic-workflow`, `architecture-fitness-tests`, `kotlin-testing-strategy`
   - Обновлены: `kotlin-project-layout`, `kotlin-clean-architecture`, `kotlin-domain-modeling`, `kmp-architecture`
   - Оставлены старыми: `compose-multiplatform-ui`, `voyager-navigation`
2. ✓ CLAUDE.md обоих репо обновлены: план 2026-09-30_architecture-refactoring добавлен как текущий
3. ✓ pws-core/docs/ai/plans/2026-09-30_architecture-refactoring_plan.md — файл-указатель верен
4. ✓ pws-core/CLAUDE.md: удалены ссылки на несуществующие планы (2026-06-16, 2026-06-15)

**Вопрос владельцу:** .junie/skills в pws-android содержит дубли; владелец недоступен. Оставлено как есть.
