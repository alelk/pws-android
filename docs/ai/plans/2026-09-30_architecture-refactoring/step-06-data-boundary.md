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
<!-- -->

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
<!-- -->

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
