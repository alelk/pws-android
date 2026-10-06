# Шаг 05 — Старт приложения и тонкий шелл

> Перед чтением этапа: [README](README.md) §4, §6, §7.2, §7.3. **Ручной прогон перед вливанием.**

**Цель.** Логика первого запуска становится чистой тестируемой машиной состояний в `pws-core`;
`MainActivity` — ≤ ~80 строк: `enableEdgeToEdge`, `setContent { AppRoot(platform) }`.

**Что не так сегодня** (`app-compose/.../MainActivity.kt`, 406 строк, всё внутри `setContent`):
- гейт `booksGate` вычисляется из пяти `remember`-переменных (`preloadedReady`, `onboardingActive`,
  `onboardingSkipped`, `hasInstalledBooks`, `telemetryPending`) и трёх `LaunchedEffect`;
- `onboardingSkipped` и `onboardingActive` — `remember`, не `rememberSaveable`: поворот экрана во
  время онбординга сбрасывает их;
- повтор legacy-миграции и отложенное восстановление бэкапа запускаются из `LaunchedEffect(installedBookCount)`;
- зависимости берутся сервис-локатором `get<…>()` прямо в композиции;
- `pendingBackupText` (весь текст бэкапа) лежит в `remember` и теряется при пересоздании Activity
  между «экспорт» и выбором файла;
- тосты — английские литералы; `startActivity(ACTION_VIEW/ACTION_SENDTO)` без `ActivityNotFoundException`.

**Мина шага.** Нарушить порядок «legacy-миграция → сидирование/онбординг/восстановление» (README §6 п. 2):
пользователь RuStore-форка или старой версии теряет избранное и историю, и это не видно ни в одном
unit-тесте экрана. Защита: тест машины состояний на **каждую** комбинацию входов из таблицы ниже +
существующий `LegacyMigrationGateTest` без изменений + апгрейд-матрица `tools/rustore-upgrade-test.md`
вручную.

**Таблица поведения, которую нельзя изменить** (сегодняшний `booksGate`):

| preloadedReady | onboardingSkipped | onboardingActive | hasInstalledBooks | Результат |
|---|---|---|---|---|
| `null` | любое | любое | любое | загрузка (пустой фон) |
| `true` | любое | любое | любое | приложение |
| `false` | `true` | любое | любое | приложение |
| `false` | `false` | `true` | любое | онбординг (до явного Skip/Continue) |
| `false` | `false` | `false` | `null` / `false` / `true` | загрузка / онбординг-триггер / приложение |

`onboardingActive` защёлкивается в `true`, когда `preloadedReady == false && hasInstalledBooks == false`,
и больше не сбрасывается сам. Согласие на телеметрию: если оно `pending`, а результат — «приложение»,
применяется значение по умолчанию.

**Вне объёма.** Изменение самого онбординга, `LegacyMigrationGate`, `PwsDatabaseProvider`,
`PwsBackupAgent`, платёжного кода.

---

## Этап 05.1 — Машина состояний старта  · модель: opus

### Прочитать
- `pws-android/app-compose/.../MainActivity.kt` строки ~78–175
- `pws-android/app-compose/.../LegacyMigrationGate.kt` и его тест
- `pws-core/features/.../app/AppRoot.kt`, `onboarding/OnboardingScreen.kt` (как используется `LocalOnSkipOnboarding`)

### Решения этапа
- В `pws-core/features/.../app/startup/`:
  ```kotlin
  data class StartupInputs(val preloaded: Boolean?, val hasInstalledBooks: Boolean?,
                           val onboardingSkipped: Boolean, val onboardingLatched: Boolean)
  enum class StartupGate { Loading, Onboarding, App }
  fun reduceStartup(inputs: StartupInputs): StartupGate        // чистая функция
  fun latchOnboarding(prev: Boolean, inputs: StartupInputs): Boolean
  ```
- Порт для шелла (в `features/.../platform/`):
  ```kotlin
  interface AppStartupTasks {
    /** Ждёт legacy-миграцию, затем сидирует встроенные сборники. true = встроенный контент есть. */
    suspend fun seedPreloadedBooks(): Boolean
    /** Вызывается после каждой установки сборника: повтор миграции + отложенное восстановление. */
    suspend fun onBooksInstalled(count: Int)
  }
  ```
  Android-реализация переносит существующий код один-в-один (включая `migrationGate.afterMigration`,
  `countAttempt = false`, `Dispatchers.IO`, вызовы телеметрии с теми же именами).
- В этом этапе `MainActivity` **не переключается** — новый код живёт рядом и покрыт тестами.

### Работа
1. Чистые функции + табличный тест по всем комбинациям (3 × 3 × 2 × 2) против таблицы выше.
2. Порт `AppStartupTasks`, фейк для тестов.
3. Android-реализация `AndroidAppStartupTasks` + тест порядка: сидирование не начинается, пока гейт
   миграции закрыт (образец — `LegacyMigrationGateTest`).

### Проверки
```bash
./gradlew :features:jvmTest && ./gradlew build          # pws-core
./gradlew :app-compose:testRuDebugUnitTest :app-compose:testRustoreDebugUnitTest   # pws-android
```

### Готово, когда
- Табличный тест зелёный и проверен красным (поменять одну ветку `reduceStartup` → падает).

### Заметки исполнителя
- 2026-10-06, статус **done**. pws-core: `features/.../app/startup/StartupGate.kt` (`StartupInputs`, `StartupGate`,
  `reduceStartup`, `latchOnboarding` — сигнатуры по плану), порт `features/.../platform/AppStartupTasks.kt`,
  `jvmTest/.../app/startup/{StartupGateTest, FakeAppStartupTasks}.kt`. pws-android:
  `app-compose/.../AndroidAppStartupTasks.kt` (+ `AndroidAppStartupTasksTest`); код перенесён из `MainActivity`
  один-в-один (`afterMigration`, `await`, `countAttempt = false`, `Dispatchers.IO`, `reportLegacyMigration`,
  `legacy_migration_retry_failed`, `count > 0`). `MainActivity` и Koin не трогал.
- Мина: `preloaded == null` ⇒ всегда `Loading` и латч не срабатывает — онбординг (установка книг) невозможен до
  конца миграции+сидирования. Тесты: таблица 36 комбинаций (литералом, + проверка покрытия), латч, сценарии с фейком;
  Android — сидирование / повтор миграции / restore не стартуют при закрытом гейте. `LegacyMigrationGateTest` не менялся.
- Проверено красным: core — 6 мутаций (`null`-ветка `hasInstalledBooks`, онбординг при `preloaded == null`,
  перестановка skip/latch, латч без `preloaded == false` (дважды — до и после переформатирования), латч без `prev`) —
  каждая роняет 2–7 тестов; android — убрать `afterMigration` / `await()` → падает ровно соответствующий тест.
- Отклонения: `AndroidAppStartupTasks` имеет `internal`-конструктор с шовом `StartupOperations` (4 лямбды) и
  `ioDispatcher` (detekt `LongParameterList` не дал передать лямбды по одной); публичный конструктор
  `(context, migrationGate, telemetry, SeedBooksFromAssetsUseCase, database: () -> PwsDatabase, dataStore: () -> DataStore)`
  — БД/DataStore лениво, как `get<…>()` в оригинале. Тесты используют `@file:OptIn`, сигнатура класса
  `class X :\n  FunSpec({` — иначе ktlint `class-signature` (baseline не трогал).
- Для 05.2: `telemetry.setUserProperty(INSTALLED_BOOKS, count)` (срабатывает и при 0) и применение дефолтного согласия
  телеметрии при `App` в порт не входят — остаются заботой VM; `AndroidAppStartupTasks` в Koin не зарегистрирован.
- Gate: pws-core `build $(corex) --continue --max-workers=1` — SUCCESSFUL (первый прогон упал на `:domain:jsBrowserTest`
  — ChromeHeadless/OOM при двух живых демонах, после `--stop` зелёный). pws-android: `testRu/RustoreDebugUnitTest` —
  падают только 3 `BackupManagerTest [SDK 37]` в каждом; `assembleRuDebug assembleRustoreDebug ktlintCheck detekt` —
  зелёные. Baseline'ы detekt/ktlint и `KNOWN_*` не менялись. Не закоммичено.

---

## Этап 05.2 — AppStartupViewModel и переключение MainActivity  · модель: opus · [R8]

### Прочитать
- заметки 05.1; `MainActivity.kt` целиком; `AppRoot.kt`
- `features/.../telemetry/TelemetryCompose.kt` (`TelemetrySettings`, `pendingConsentDefault`)

### Решения этапа
- `AppStartupModel` в `features/.../app/startup/` — state holder уровня приложения (шаг 08 отложен,
  поэтому это обычный класс с собственным `CoroutineScope`, зарегистрированный в Koin как `single`; на Android
  это переживает пересоздание Activity, чего и не хватало). Состояние:
  `StateFlow<StartupUiState>(gate, installedBookCount)`. Действие: `skipOnboarding()`.
- Модель сама: наблюдает `ObserveInstalledBooksUseCase`, вызывает `AppStartupTasks`, выставляет
  `TelemetryAttr.INSTALLED_BOOKS`, применяет согласие по умолчанию, когда gate = `App` и согласие pending
  (через существующий порт/колбэк `TelemetrySettings`; логика значения по умолчанию остаётся в шелле).
- `AppRoot` убирает параметры `hasInstalledBooks`, `onSkipOnboarding` и `LocalOnSkipOnboarding`;
  онбординг вызывает `skipOnboarding()` модели.
- `MainActivity` больше не содержит `LaunchedEffect` со стартовой логикой.
- Порядок Koin-модулей не меняется (README §6 п. 4); `AppStartupTasks` регистрируется в шелле.

### Работа
1. Модель + тесты на фейках (включая: «skip» переживает пересоздание потребителя; сидирование
   вызывается один раз; `onBooksInstalled` зовётся при каждом росте счётчика).
2. Переключить `AppRoot` и `MainActivity`.
3. Проверить R8-сборку: новая модель создаётся Koin'ом в release.

### Проверки
```bash
./gradlew build      # оба репо, + [R8]
```

### Готово, когда
- В `MainActivity` нет стартовой логики; табличный тест и тесты модели зелёные.
- **Ручной прогон (владелец):** чистая установка `ru` → онбординг → поворот экрана на онбординге →
  Skip → приложение; чистая установка `uk`/`rustore` (встроенные сборники) → сразу приложение, без
  вспышки онбординга; обновление с предыдущего релиза → сразу приложение, данные на месте;
  апгрейд-матрица `tools/rustore-upgrade-test.md`.

### Заметки исполнителя
- 2026-10-06, статус **done**. pws-core: `features/.../app/startup/AppStartupModel.kt` (`StartupUiState(gate, installedBookCount)`,
  `skipOnboarding()`, `applyPendingConsentDefault(gate, settings)`; свой `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`,
  в тестах — `backgroundScope`); `single { AppStartupModel(...) }` в `featuresModule`; `AppRoot` без `hasInstalledBooks` /
  `onSkipOnboarding`, `Crossfade` по `StartupGate`; `LocalOnSkipOnboarding` удалён. pws-android: `single<AppStartupTasks> { AndroidAppStartupTasks(...) }`
  в существующем `startupModule` (порядок модулей не менялся); из `MainActivity` убраны вся стартовая логика и пять `LaunchedEffect`/`remember`.
- Перенесено один-в-один: сидирование через `afterMigration` (теперь один раз на процесс, а не на каждое пересоздание Activity);
  `setUserProperty(INSTALLED_BOOKS, count.toString())` на каждое изменение счётчика, начиная с `0`; `onBooksInstalled` — через
  `distinctUntilChanged` + `collectLatest` (как отменяющий `LaunchedEffect(installedBookCount)`); латч и таблица — `reduceStartup`/`latchOnboarding`.
- Отклонения: (1) онбординг не может взять модель через `koinInject` (fitness `NoServiceLocatorInComposablesTest`, `KNOWN_*` пуст),
  а лямбда в конструкторе Voyager-`Screen` сломала бы сохранение состояния при повороте — добавлен тонкий `OnboardingScreenModel`
  (`koinScreenModel`, `factory`), который зовёт `AppStartupModel.skipOnboarding()`. (2) Согласие по умолчанию: правило «gate = App и
  `pendingConsentDefault != null` → `onDataSendingEnabledChange(default)`» в модели, но вызывается из `AppRoot`
  `LaunchedEffect(gate, telemetrySettings)` и получает gate и settings из одной композиции. Если бы модель сама сравнивала свой свежий
  gate с последними сохранёнными settings, то после Skip она увидела бы старый `pending` и перезаписала бы ответ пользователя
  (онбординг сначала сохраняет согласие, потом зовёт skip). Логика значения по умолчанию осталась в шелле (`TelemetrySettings`).
- Тесты: `AppStartupModelTest` (7: мина «Loading до сидирования», Skip переживает нового потребителя, сидирование 1 раз,
  `onBooksInstalled`/`installed_books` на каждое изменение счётчика, preloaded / существующий пользователь, согласие только при App);
  `AppStartupWiringTest` в app-compose (3: модель + настоящий `AndroidAppStartupTasks` — без открытого гейта миграции только Loading, без
  seed/retry/restore). Красным проверено: в модели 5 мутаций (skip no-op, `preloaded = false` на старте, seed дважды, без `distinctUntilChanged`,
  без проверки gate для согласия) — каждая роняет свои тесты; Android: убрать `afterMigration` → падают `AndroidAppStartupTasksTest` + 2 теста wiring.
  `LegacyMigrationGateTest`, `AndroidAppStartupTasksTest`, `StartupGateTest` не менялись.
- Baseline'ы: ktlint `features` — 2347 → 2347 (перегенерирован, только сдвиг строк в AppRoot/FeaturesModule/OnboardingScreen);
  ktlint `app-compose` — 117 → 112 (ушли `no-multi-spaces` из `booksGate`). detekt-baseline'ы не трогал (запись `LongParameterList:AppRoot.kt`
  теперь устаревшая — у функции 6 параметров, detekt молчит). `KNOWN_*` не менялись.
- R8: `minifyRu/RustoreReleaseWithR8` зелёные; `AppStartupModel`, `OnboardingScreenModel`, `AndroidAppStartupTasks` в mapping есть
  (обфусцированы — Koin DSL берёт классы по ссылке, не по имени), keep-правила не нужны. Подписи нет — `assemble*Release` не запускал.
- Gate: pws-core `build $(corex) --continue --max-workers=1` — SUCCESSFUL. pws-android: `testRu/RustoreDebugUnitTest` — падают только
  3 `BackupManagerTest [SDK 37]` в каждом (окружение); `assembleRuDebug assembleRustoreDebug ktlintCheck detekt verifyRustoreReleaseInvariants
  minifyRu/RustoreReleaseWithR8` — зелёные. Не закоммичено.
- Вне объёма (для владельца / 05.3): `MainActivity` всё ещё 240 строк (внешние действия — это 05.3).

---

## Этап 05.3 — Платформенные действия, локализация, DI по файлам  · модель: sonnet

### Прочитать
- `MainActivity.kt` (после 05.2), `PwsComposeApplication.kt`
- `features/.../settings/SettingsExternalActions.kt`, `booklibrary/BookLibraryExternalActions.kt`,
  `song/detail/SongDetailPlatformSettings.kt` (`SongDetailExternalActions`)

### Решения этапа
- Реализации внешних действий выносятся из `setContent` в классы пакета
  `app-compose/.../platform/`: `AndroidUrlActions` (URL, e-mail), `AndroidShareActions`,
  `BackupFileActions` (экспорт/импорт через Activity Result API), `BundleImportActions`.
  Интерфейсы `*ExternalActions` в `pws-core` не меняются.
- `startActivity` для `ACTION_VIEW`/`ACTION_SENDTO`/`ACTION_SEND` оборачивается в обработку
  `ActivityNotFoundException` → сообщение пользователю + `telemetry.recordError`.
- Сообщения пользователю: строки в `app-compose/src/main/res/values{,-ru,-uk,-pl}/strings.xml`
  (`backup_saved`, `backup_export_failed`, `backup_import_done`, `backup_import_failed`,
  `bundle_imported`, `bundle_import_failed`, `no_app_to_open_link`). `it.message` пользователю не
  показывается — только в телеметрию.
- Текст бэкапа между «экспорт» и выбором файла хранится не в `remember`, а во временном файле в
  `cacheDir` (имя сохраняется через `rememberSaveable`/`SavedStateHandle`); файл удаляется после записи.
- `PwsComposeApplication`: локальные `module { }` переезжают в `app-compose/.../di/` по файлу на
  модуль (`DatabaseModule.kt`, `TelemetryModule.kt`, `DonationModule.kt`, `StartupModule.kt`, …);
  функция `appModules(...)` возвращает список **в прежнем порядке**. Существующий
  `RustoreKoinModulesTest` остаётся зелёным; добавить аналогичный тест для `ru`: граф Koin
  собирается (`checkModules`/ручной `get` ключевых типов).
- `Timber.plant(...)` из `PwsDatabaseProvider.buildDatabase` переносится в `Application.onCreate`
  (в провайдере БД это побочный эффект не по месту). Больше в `:data:db-android` ничего не менять.

### Работа
1. Классы платформенных действий + строки + обработка отсутствующего приложения.
2. Временный файл для экспорта бэкапа + тест.
3. Разнести Koin-модули, тест графа.
4. Очистить `KNOWN_*` в `ShellStringsTest`.

### Проверки
```bash
./gradlew build      # pws-android
wc -l app-compose/src/main/kotlin/io/github/alelk/pws/android/compose/MainActivity.kt   # ориентир ≤ 100
```

### Готово, когда
- `MainActivity` — только связывание; нет строковых литералов для пользователя; граф Koin под тестом.

### Заметки исполнителя
<!-- -->
