# Шаг 06 — Граница данных: один писатель

> Перед чтением этапа: [README](README.md) §4, §6, §7.2, §7.3. **Ручной прогон перед вливанием.**
> Это самый рискованный шаг плана: он трогает код, который пишет пользовательские данные.

**Цель.** В Room пишет только `:data:repo-room`. Бэкап, восстановление, импорт и удаление сборников
выражены use case'ами домена над портами; Android-модули оставляют себе только платформенное:
сеть, файлы, расшифровку, Auto Backup API.

**Что не так сегодня.**
- `BackupManager` (app-compose) читает/пишет 7 DAO напрямую и знает про `ThemeMode` из `features`.
- `BookImporterImpl`, `BookUninstallerImpl`, `SeedBooksFromAssetsUseCase` (content-delivery) работают
  с `PwsDatabase` и `db.withTransaction` напрямую.
- В `:data:repo-room` `RoomTransactionRunner`, Koin-модуль и поиск лежат в `androidMain`; на 18
  реализаций — 2 тестовых файла. Репозитории нельзя прогнать на JVM.
- `InstallBookUseCase`/`UninstallBookUseCase`/`UpdateBookUseCase` — интерфейсы в домене с `Impl` в
  Android-модуле; остальные use case'ы — классы.

**Мина шага.** Перенос логики импорта/восстановления меняет результат на граничных случаях:
пользовательские правки текста песни затёрты обновлением сборника; «умная» привязка id песен
(`SmartSongBinder`) даёт другие id; восстановление пропускает записи сборников, которые ещё не
установлены. Защита: **сначала** характеризационные тесты на текущую реализацию (те же входы →
снимок содержимого БД), затем перенос, затем те же тесты на новую реализацию без правки ожиданий.
`BookImporterImplTest` (305 строк) и `BackupManagerTest` (199) — основа, их ожидания не меняются.

**Вне объёма.** Схема БД (G1), формат бэкапа и bundle (G2), legacy-миграции (`support/*`,
`migrateDataFromPrevDatabase.kt`), шифрование, `PwsDataProvider`.

---

## Этап 06.1 — repo-room на JVM: транзакции, DI, поиск, тесты  · модель: opus

### Прочитать
- `pws-core/data/repo-room/src/` целиком (22 файла), `data/repo-room/build.gradle.kts`
- `pws-core/data/db-room/build.gradle.kts` и `db-room/src/jvmTest` (как поднимается БД на JVM c `sqlite-bundled`)
- `pws-core/data/db-room/db-room-test-fixtures/`

### Решения этапа
- `RoomTransactionRunner` → `commonMain` на Room KMP API (`useWriterConnection { immediateTransaction { } }`
  / `useReaderConnection`). Семантика вложенности сохраняется: вложенный `inRwTransaction` внутри
  внешнего не открывает вторую транзакцию и не дедлочит — под это отдельный тест.
  **Если** на Android-драйвере с SQLCipher (`SupportOpenHelperFactory`) KMP-API транзакций недоступен
  или ведёт себя иначе — оставить `actual` для Android на `withTransaction`, а общую реализацию дать
  для JVM/iOS через `expect/actual`; решение и причину записать в заметки.
- `repoRoomModule` → `commonMain`.
- `SongSearchRepositoryImpl`, `searchGrouping.kt`: перенести в `commonMain`, если не зависят от
  Android API; иначе оставить и записать причину.
- F5 закрыта: iOS в ближайший год не цель. Критерий этапа — всё перечисленное собирается и
  тестируется на `jvm`. Работоспособность на iOS не проверяется и не является целью; если перенос
  какого-то файла в `commonMain` требует усилий только ради iOS — не переносить, записать в заметки.
  Существующие iOS-цели модулей при этом не удаляются (сборка должна остаться зелёной).
- Тесты репозиториев — `commonTest`/`jvmTest` на in-memory БД с `sqlite-bundled`, данные — из
  `db-room-test-fixtures`. Минимум на каждую из 18 реализаций: запись → чтение; `observe` эмитит
  после записи; удаление; для `TransactionRunner` — откат при исключении.

### Работа
1. Перенос в `commonMain` (или `expect/actual` — по результату проверки на SQLCipher).
2. Тесты репозиториев.
3. Обновить `docs/MODULES.md`.

### Проверки
```bash
./gradlew :data:repo-room:jvmTest :data:repo-room:testAndroidHostTest && ./gradlew build
cd ../pws-android && ./gradlew build        # приложение на SQLCipher собирается и тесты БД зелёные
```

### Готово, когда
- У каждой реализации репозитория есть тест на JVM; откат транзакции проверен красным
  (убрать транзакцию → тест падает).

### Заметки исполнителя
- 2026-10-06, статус **done**. Изменения только в pws-core; схема Room (v15) и `schemas/` не тронуты; pws-android не менялся.
  `RoomTransactionRunner`, `repoRoomModule`, `SongSearchRepositoryImpl`, `searchGrouping.kt` → `repo-room/commonMain`
  (`git mv`); `SearchGroupingTest` → `commonTest`; `androidHostTest`-исходников в repo-room больше нет.
- Транзакции: общая реализация на Room KMP API — `useWriterConnection { immediateTransaction }` /
  `useReaderConnection { deferredTransaction }`, без `expect/actual`. SQLCipher проверен чтением исходников Room 2.8.4 /
  sqlite-framework 2.6.2 (на устройстве не проверялось — ручной прогон перед вливанием): в compatibility-режиме
  (`SupportOpenHelperFactory`) Room ведёт эти транзакции через `PassthroughConnectionPool` + `compatTransactionCoroutineExecute`,
  т. е. на том же транзакционном потоке, что `withTransaction`; `BEGIN IMMEDIATE/DEFERRED` переводятся в
  `beginTransactionNonExclusive()` / `beginTransactionReadOnly()` (у `net.zetetic` SQLiteDatabase 4.18 оба метода есть).
  Вложенность с существующими `db.withTransaction` в pws-android (импорт, бэкап) работает в обе стороны (вложенная
  Android-транзакция). Отличия от старой реализации: (1) `inRoTransaction` теперь deferred/read-only, а не эксклюзивная;
  (2) на JVM-пуле вложенный вызов — SAVEPOINT; `inRwTransaction` внутри `inRoTransaction` на JVM падает (Room не
  повышает reader до writer) — в домене таких вложений нет, на Android (passthrough) поведение прежнее.
- DI: `repoRoomModule` в `commonMain` делает `includes(repoRoomPlatformModule)` — `internal expect val` с биндингом
  `named("onDataChanged")`: Android — `BackupManager(get<Context>()).dataChanged()` (как было), JVM/iOS (`nativeMain`) — no-op.
  Публичный API модуля и `AppModules.kt` в pws-android не меняются.
- Отклонение: чтобы `SongSearchRepositoryImpl` ушёл в `commonMain`, запросы `findBySongNumber`/`findBySongText` и
  `SongSearchResultEntity` перенесены из Android-`SongDao` в `SongDaoBase`/`commonMain` db-room (SQL без изменений, Android API
  не использовали). Это запросы, не схема: `schemas/` не изменились. Курсорные `getSuggestionsBy*` (SearchManager) остались в Android.
  iOS-компиляция/KSP здесь не проверяются (песочница), сам перенос ради iOS ничего не добавлял.
- Тесты: `repo-room/src/jvmTest` — реальная Room in-memory на `BundledSQLiteDriver` (linux-aarch64 работает), сущности из
  `db-room-test-fixtures` с фиксированным seed. Все 12 классов-реализаций + `RoomTransactionRunner` + `RepoRoomModuleTest`
  (Koin резолвит все порты на JVM): 62 новых теста (1 выключен, см. баг ниже), запись→чтение, `observe*` эмитит после записи (`emissionAfter`), удаление;
  транзакции — коммит, откат при исключении, вложенный rw без дедлока, откат внешнего откатывает вложенный, ro внутри rw.
  Сделано красным (8 мутаций, все в своё время упали и откатаны): транзакция убрана → 2 теста runner'а; `priority > 0` → `>= 0`
  в поиске; `description`↔`preface` в маппинге книги; перевёрнутый `ToggleResult`; история без дедупликации;
  `displayShortName` вместо `displayName` в `observeSongsByTag`; `installedAt + 1` в маппинге installed book.
- **Найден баг (не чинил, вне объёма):** `SongReferenceRepositoryImpl.getReferencesToSong(refSongId)` вызывает
  `SongReferenceDao.getBySongIds`, которая фильтрует по `song_id`, — возвращает ссылки ИЗ песни, а не НА неё (контракт порта:
  «songs that reference this song»). Следствие: `GetSongReferencesWithDetailsUseCase` дублирует исходящие ссылки и не видит
  входящие. Тест с правильным ожиданием есть и выключен (`enabled = false`, комментарий BUG) — проверен красным: ожидалось
  `[1, 3]`, получено `[2]`. Владельцу: чинить в отдельной задаче (нужен запрос `WHERE ref_song_id IN (...)`), затем включить тест.
- Попутные наблюдения (не трогал): репозитории оборачивают DAO в `runCatching` → ошибка внутри `inRwTransaction` превращается
  в `Left` и транзакция коммитится (ловушка «Left commits»); `runCatching` глотает и `CancellationException`;
  `FavoriteRepositoryImpl.clearAll` не зовёт `onDataChanged`; `SongTagRepositoryImpl` пишет в `println`.
- Baseline'ы: ktlint repo-room 238 → 235, db-room 398 → 395 (регенерированы из-за переноса путей/сдвига строк); detekt не менялся;
  `ktlintFormat` не запускался. Gate pws-core (`build` + corex) — зелёный; `:data:repo-room:jvmTest :data:repo-room:testAndroidHostTest`
  — зелёные; pws-android `:app-compose:assembleRuDebug` — зелёный (SQLCipher-приложение собирается; Robolectric-тесты БД здесь не гоняются).

---

## Этап 06.2 — Бэкап как use case'ы домена  · модель: opus

### Прочитать
- `pws-android/app-compose/.../BackupManager.kt`, `PwsBackupAgent.kt`, `BackupManagerTest.kt`
- `pws-core/portable-data/.../model/Backup.kt`, `BackupService`
- `pws-core/portable-data/src/commonTest/.../BackupCompatibilityTest.kt`
- заметки 06.1; порт настроек из шага 04 (если шаг 04 ещё не влит — см. решение ниже)

### Решения этапа
- Use case'ы живут в **`:portable-data`** (он уже зависит от `:domain` и владеет моделью `Backup`;
  домен от `portable-data` зависеть не должен):
  `ExportBackupUseCase(source: String): Backup`, `RestoreBackupUseCase(backup: Backup): RestoreReport`.
  Работают только через репозитории/use case'ы домена внутри `TransactionRunner.inRwTransaction`.
- Если домену не хватает операций (например, «все отредактированные песни», «все пользовательские
  теги с песнями», «приоритеты сборников») — добавить методы в соответствующие `*ReadRepository` и
  реализации в `repo-room`; SQL берётся из тех же DAO-методов, которые вызывает `BackupManager` сегодня.
- Настройки в бэкапе: сегодня сохраняется только ключ `app-theme`. Use case получает/отдаёт настройки
  через `UserPreferencesRepository` (шаг 04). Если шаг 04 не влит — через узкий порт
  `BackupSettingsPort { suspend fun read(): Map<String,String>; suspend fun apply(Map<String,String>) }`,
  реализованный в шелле существующим кодом. Ключ и формат значения не меняются (G2, G3).
- `BackupManager` в app-compose становится тонким фасадом над use case'ами (или удаляется, если
  все вызовы можно заменить напрямую). `PwsBackupAgent` остаётся в шелле: `runBlocking` в колбэках
  Auto Backup допустим (фреймворк зовёт их на своём потоке) — не менять.
- Поведение «записи сборников, которые ещё не установлены, откладываются до установки»
  (`applyPendingRestoreIfNeeded`) сохраняется.

### Работа
1. Характеризационный тест: на БД с данными всех типов (избранное, история, правки песен, теги,
   приоритеты, тема) `BackupManager.exportBackup` → снимок `Backup` (YAML через `BackupService`);
   `restoreBackup` на пустую БД → снимок таблиц. Сохранить снимки как golden.
2. Реализовать use case'ы и недостающие методы репозиториев.
3. Прогнать те же golden на новой реализации — совпадают байт-в-байт (YAML) и по содержимому таблиц.
4. Переключить шелл, очистить `KNOWN_*` в `DirectDaoAccessTest` для `BackupManager.kt`.

### Проверки
```bash
./gradlew :portable-data:jvmTest && ./gradlew build        # pws-core
./gradlew :app-compose:testRuDebugUnitTest && ./gradlew build   # pws-android
```

### Готово, когда
- Golden-тесты старой и новой реализации совпадают; `BackupCompatibilityTest` не тронут.
- **Ручной прогон (владелец):** `docs/testing-backup-real-device.md` — бэкап предыдущего релиза
  восстанавливается сборкой шага; бэкап сборки шага читается предыдущим релизом.

### Заметки исполнителя
- 2026-10-06, статус **done**. Схема Room (v15), `schemas/`, формат бэкапа, ключ `app-theme` не менялись;
  `BackupCompatibilityTest` и `BackupManagerTest` не тронуты (SDK 34 зелёный; 3 падения `[SDK 37]` — окружение).
- **Мина — сначала характеризация.** Сценарий с данными всех типов (`BackupScenario`, правки песни в двух сборниках,
  избранное, 2 пользовательских тега, предопределённый тег, приоритеты, история, тема; на стороне восстановления — тег с тем же
  именем, повтор избранного и истории, записи для неустановленных номеров/сборника, тег с именем предопределённого) и снимки
  `BackupGolden` (YAML экспорта + дамп таблиц после восстановления) лежат в `pws-core/data/db-room/db-room-test-fixtures`
  (`.../database/backup/`). Снимки записаны со **старого** `BackupManager` новым `BackupGoldenTest` (Robolectric SDK 34) и были
  зелёными на нём; после переноса тот же тест зелёный без правки ожиданий. В pws-core `portable-data/src/jvmTest/.../BackupUseCasesTest`
  гоняет use case'ы на реальных Room-репозиториях (bundled SQLite) против тех же снимков — тоже байт-в-байт.
- Use case'ы в `:portable-data` (`io.github.alelk.pws.portable.backup`): `ExportBackupUseCase(source): Backup` (чтение в
  `inRoTransaction`), `RestoreBackupUseCase(backup): Either<RestoreBackupError, RestoreReport>`. `BackupManager` — тонкий фасад
  (Left → исключение, как и раньше бросал); use case'ы и фасад в Koin (`di/DatabaseModule.kt`). `BackupManager.kt` ушёл из
  `KNOWN_DIRECT_DAO_USERS`. `PwsBackupAgent` остался в списке (`bookDao().count()`, `installedBookDao()` в
  `applyPendingRestoreIfNeeded` — вне объёма этапа); `applyPendingRestoreIfNeeded` теперь принимает `BackupManager`, логика
  отложенного восстановления не менялась.
- Новые методы портов (SQL — те же DAO-методы, что вызывал `BackupManager`): `FavoriteReadRepository.getAllSongNumbers`,
  `SongReadRepository.getAllEdited` / `SongWriteRepository.saveUserEdit` (сырой текст, без parse/toText — мина «правки
  затёрты»), `TagReadRepository.getAllNotPredefined/findByName/nextCustomTagId`, `BookStatisticRepository.getAllEnabled`
  (deprecated `getAllActive`, `@Suppress`), `HistoryReadRepository.getAllViews` / `HistoryWriteRepository.restoreView`. Новый
  запрос `HistoryDao.count(bookId, songId, accessTimestamp)` (не схема): `restoreView` проверяет точный дубль вместо ловли
  UNIQUE-нарушения — те же строки и те же `id` (проверено снимком). Фейки в тестах domain/features дополнены; `api:client`:
  `getAllEdited` → пусто, `saveUserEdit` → Left «не поддерживается сервером».
- **Атомарность.** Найдено: старый `restoreBackup` вообще не был в транзакции — каждая DAO-операция коммитилась сама, сбой
  посередине оставлял частичную запись. Теперь всё в одной `inRwTransaction`: любой Left репозитория → внутреннее исключение →
  откат → Left снаружи; тема (DataStore) применяется только после коммита. Тесты: Left / исключение / отмена на 2-й записи
  истории (после песен, избранного, тегов, приоритетов) → дамп БД не изменился, тема не применена. Красным: без транзакции —
  3 теста красные; `catch (Exception)` вместо своего исключения — красные «исключение» и «отмена»; без проверки дубля истории —
  красный golden восстановления. `CancellationException` не глотается (helper `eitherCatching` в repo-room для новых методов;
  `applyPendingRestoreIfNeeded` её пробрасывает).
- **Отклонения / вопросы владельцу:**
  1. В `:domain` добавлена зависимость `api(libs.kotlinx.datetime)`: локальное время просмотра хранится как `LocalDateTime`;
     через `HistoryEntry.viewedAt: Instant` (системная TZ) оно искажалось бы в «дырах» перехода на летнее время.
  2. ~~Тема через `UserPreferencesRepository` (экспорт только ≠ `system`)~~ — **отклонено оркестратором (G2)**, переделано:
     узкий порт `BackupSettingsPort { read(): Map<String,String>; apply(Map<String,String>) }` в `:portable-data`
     (`portable/backup/BackupSettingsPort.kt`), реализация в шелле `DataStoreBackupSettings` над тем же DataStore и
     `appThemeKey` (сырое значение: не задан → ключа нет; задан `system` → `"system"`; мусор экспортируется как есть).
     Use case'ы `UserPreferencesRepository` больше не используют. Применение — после коммита, с прежней валидацией
     (`ThemeMode.byIdentifier(v).identifier == v`, иначе игнор). Тесты (pws-core `BackupUseCasesTest` на фейке порта и
     pws-android `BackupGoldenTest` на реальном DataStore): не задан → ключа нет; `system` → экспортируется `"system"`;
     восстановление `system` поверх `dark` → `system`; golden/compat без правки ожиданий. Красным: экспорт без `system` →
     красный тест (2) в pws-core; порт, отбрасывающий `system`, → красные (2) и (3) в pws-android. Принятые оркестратором
     отклонения: п. 1, 3, 4 ниже.
  3. Отрицательный `bookPreference` (только в отредактированном вручную файле) теперь пропускается (`UpdateBookStatisticCommand`
     требует ≥ 0); раньше записывался. Номер песни ≥ 1 000 000 во входе считается «не найден» (ограничение доменного `SongNumber`).
  4. `PwsBackupAgent.onBackup` берёт `BackupManager` из Koin (`GlobalContext`); key/value-бэкап запускает Application обычным
     образом, но если Koin не поднят — агент ничего не пишет (транспорт сохраняет прежние данные ключа). Раньше агент сам
     открывал БД и DataStore.
  5. Репозитории при восстановлении зовут `onDataChanged` (Android `BackupManager.dataChanged()`), раньше прямые DAO — нет.
  6. `RestoreBackupUseCase`/`ExportBackupUseCase` — 11/8 зависимостей (`@Suppress("LongParameterList")`, по порту на вид данных).
- Baseline'ы: ktlint repo-room 235 → 233, app-compose 106 → 94; domain/db-room/features/portable-data/api-client
  регенерированы из-за сдвига строк, число не выросло; detekt app-compose 34 → 32 (две записи `BackupManager.restoreBackup`
  ушли, ID `ReturnCount` `applyPendingRestoreIfNeeded` обновлён под новую сигнатуру). `ktlintFormat` не запускался.
- Gate: pws-core `build` + corex — зелёный. pws-android `:app-compose:testRuDebugUnitTest :app-compose:testRustoreDebugUnitTest
  :app-compose:assembleRuDebug :app-compose:assembleRustoreDebug :app-compose:ktlintCheck :app-compose:detekt` — всё зелёное,
  кроме 3 `BackupManagerTest [SDK 37]` в каждом флейворе (UnsatisfiedLinkError, окружение); `BackupGoldenTest` 5/5 (после
  доработки темы; gate повторён, результат тот же),
  `BackupManagerTest` SDK 34 3/3, `DirectDaoAccessTest` зелёные.
- Ручной прогон (владелец): `docs/testing-backup-real-device.md` в обе стороны; Auto Backup (`bmgr backupnow` → переустановка
  → восстановление после установки сборника); экспорт/импорт через файл; SQLCipher-устройство (экспорт идёт в
  `inRoTransaction` с `Flow.first()` внутри — на Robolectric и JVM работает, на устройстве не проверялось).

---

## Этап 06.3 — Импорт и удаление сборников через порты  · модель: opus

### Прочитать
- `pws-android/data/content-delivery/src/main/.../install/` (8 файлов) и тесты `src/test/.../install/`
- `pws-core/domain/.../booklibrary/` (модели, порты, use case-интерфейсы)
- `pws-core/portable-data/.../model/` (`BookBundle`)
- заметки 06.1, 06.2

### Решения этапа
- Логика записи сборника (`BookImporterImpl.import`, включая `SmartSongBinder`, сохранение
  пользовательских правок, очистку сирот, порядок удаления по FK в `BookUninstallerImpl`) переезжает
  в pws-core: порт `BookContentWriter` в домене (операции уровня «применить bundle», «удалить сборник»)
  с реализацией в `:data:repo-room`, **или** use case в `:portable-data` над мелкими методами
  репозиториев — исполнитель выбирает вариант с меньшим числом новых методов портов и фиксирует
  выбор в заметках. В обоих случаях одна транзакция на сборник, как сейчас.
- `:data:content-delivery` после этапа: HTTP, зеркала каталога, скачивание с прогрессом, проверка
  SHA-256, расшифровка, чтение ассетов/URI — и вызов use case'а записи. Прямых обращений к
  `PwsDatabase` нет.
- `InstallBookUseCase` / `UninstallBookUseCase` / `UpdateBookUseCase` остаются интерфейсами домена
  (у них платформенные реализации) — это осознанное исключение, фиксируется в `AGENTS.md` (шаг 07.1).
- Блокировка удаления для `BookInstallSource.ASSET` сохраняется.
- Имена событий телеметрии импорта/установки не меняются (G12).

### Работа
1. Расширить `BookImporterImplTest` до характеризационного набора: повторный импорт той же версии;
   обновление версии при отредактированной пользователем песне; песня, общая для двух сборников;
   удаление сборника с общей песней; сидирование из ассетов. Снимок таблиц — golden.
2. Перенести логику, переключить `content-delivery`.
3. Golden совпадают на новой реализации.
4. Очистить `KNOWN_*` в `DirectDaoAccessTest` полностью.

### Проверки
```bash
./gradlew build      # оба репо
grep -rn "PwsDatabase\|Dao()" ../pws-android/data/content-delivery/src/main   # только DI-проводка или пусто
```

### Готово, когда
- `DirectDaoAccessTest`: `KNOWN_*` пуст; golden совпадают.
- **Ручной прогон (владелец):** установка сборника из каталога, обновление, удаление, импорт из
  файла, чистая установка `uk` (11 встроенных сборников) и `rustore`; апгрейд с предыдущего релиза.

### Заметки исполнителя
<!-- -->
