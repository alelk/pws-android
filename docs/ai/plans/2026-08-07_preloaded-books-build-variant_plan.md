# Предустановленные сборники в APK для отдельных build variant

> **Статус:** РЕАЛИЗОВАНО (Phases A–F) — собрано и проверено end-to-end на `rustore`/`uk`
> **Дата:** 2026-08-07
> **Ветка:** next

---

## 0. Статус реализации (2026-08-07)

**Готово и проверено:**

- **Phase B** — `BookImporterImpl.import(bundle, source = DOWNLOADED)`; ASSET не понижается при
  re-import (читается существующий `source`). Юнит-тесты в `BookImporterImplTest` (feature
  «install source»).
- **Phase C** — `SeedBooksFromAssetsUseCase` (ядро `seedBundles(names, readBytes)` развязано от
  `AssetManager`), DI в `contentDeliveryModule`, триггер в `MainActivity` (tri-state `preloadedReady`
  до гейта онбординга). Юнит-тесты `SeedBooksFromAssetsUseCaseTest` (ASSET-пометка, идемпотентность
  по имени файла, версионный до-импорт, clean-вариант).
- **Phase A** — `seedBooksByFlavor` в `app-compose/build.gradle.kts` (`rustore→PV3300`,
  `uk→Psalmovivi`), задача `DownloadSeedBundlesTask` (fetch каталога → resolve version/checksum →
  download → verify SHA-256), AGP-обвязка `addGeneratedSourceDirectory`. Задачи регистрируются
  только для `rustore`/`uk` (×3 build type); `ru`/`full` — чистые. **Проверено**:
  `assembleRustoreDebug` кладёт `assets/seed-books/PV3300-debug-3.4.2.book.yaml.gz.enc` в APK.
- **Phase D** — UI не менялся: `isBuiltIn`/скрытие Uninstall/статус built-in уже работают.
- **Phase E** — раздел «Предустановленные сборники (seed-books)» в `docs/release-workflow.md`.
- **Phase F** — юнит-тесты зелёные (`:data:content-delivery:testDebugUnitTest`).

**Отклонения от плана:** ядро сидера вынесено в `internal seedBundles(...)` для тестируемости без
Robolectric-ассетов (O2-splash отдельного стейта не понадобился — tri-state гейт `preloadedReady`
держит loading-поверхность до завершения сидинга). Реальные bookId взяты из живого каталога
(`PV3300`/`Psalmovivi`), а не `pws-ru`/`pws-uk` из golden-фикстуры.
> **Связано:** `2026-07-08_universal-apk-onboarding_plan.md` (универсальный чистый APK + онбординг),
> `2026-06-27_global-book-library_plan.md` (каталог, install/update, smart-remap),
> `2026-07-27_rustore-build-variant_plan.md` (flavor rustore)

---

## 1. Цель

Универсальный APK ставится «чистым», сборники пользователь добавляет на онбординге. Нужен **общий,
переиспользуемый механизм**: для **некоторых build variant** заранее вшить выбранные сборники прямо в
APK, чтобы после установки приложение сразу содержало контент (без сети, без онбординга).

Требования (из обсуждения):

1. **Список сборников задаётся per-variant** (в `build.gradle.kts` / отдельном конфиге). Наборы у
   разных вариантов **разные** (напр. `rustore` — один набор, `uk` — другой).
2. **Сборка сама скачивает** указанные бандлы из каталога и упаковывает их в APK. Скачивание — на
   этапе сборки (в т.ч. на CI, где сеть есть).
3. **Варианты без списка остаются чистыми** — текущее поведение не меняется.
4. **Предустановленные сборники — неснимаемая база** (пользователь не может удалить), **но без
   изменения схемы данных и без миграций.**

---

## 2. Принятые решения (развилки закрыты)

| # | Развилка | Решение |
|---|----------|---------|
| Q1 | Как variant объявляет предустановку | **Config-driven**: карта `flavor → [bookId]` в Gradle. Нет нового flavor/dimension. Пустой список ⇒ чистый APK. |
| Q2 | Откуда файлы бандлов | **Gradle-задача качает из каталога** на этапе сборки (те же URL, что рантайм: GitHub Pages → Cloudflare → Yandex). |
| Q3 | Можно ли удалить предустановленный сборник | **Нельзя — `source = ASSET`.** Текущая схема это **уже поддерживает без миграции** (см. §3). |

---

## 3. Факты разведки (проверено в коде)

### 3.1. Схема уже поддерживает неснимаемую базу — миграция НЕ нужна

- `BookInstallSource { ASSET, DOWNLOADED, MIGRATION }` — `ASSET` уже существует
  (`pws-core/domain/.../booklibrary/model/BookInstallSource.kt`).
- `InstalledBookEntity` (`pws-core/data/db-room/.../installed_book/InstalledBookEntity.kt`) уже имеет
  колонку `source: BookInstallSource`. TypeConverter round-trip'ит через `.name` / `valueOf`
  (`DbTypeConverters.kt:96,99`) → значение `ASSET` пишется/читается **в существующей колонке**, новых
  полей не требуется.
- `BookUninstallerImpl` (`data/content-delivery/.../install/BookUninstallerImpl.kt:16`) уже блокирует
  удаление: `check(installed.source != BookInstallSource.ASSET)`.
- **UI уже готов**: `BookLibraryItem.isBuiltIn = installed?.source == ASSET`
  (`BookLibraryUiState.kt:15`). В `BookLibraryScreen.kt` для built-in показывается статус
  `book_library_status_built_in` и **кнопка Uninstall не рендерится** (строка 307 vs 324/339).
- ⚠️ **Нюанс:** `hasUpdate` для built-in = `false` (`BookLibraryUiState.kt:18` — `&& !isBuiltIn`).
  → Предустановленные (ASSET) сборники **не предлагаются к обновлению через каталог из UI**. Значит
  обновление предустановленного контента приходит **только с обновлением APK** (новая версия бандла в
  ассетах) — это диктует идемпотентность seed'а по `bookId + version` (§5, Phase C).

**Вывод по Q3:** реализуем как неснимаемую (`source = ASSET`). Единственное изменение кода в этой
части — снять хардкод `DOWNLOADED` в импортёре (см. ниже), схему не трогаем.

### 3.2. Импортёр и готовый примитив импорта

- `BookImporterImpl.import(bundle)` (`data/content-delivery/.../install/BookImporterImpl.kt`)
  **хардкодит** `source = BookInstallSource.DOWNLOADED` (строка 146). Импорт транзакционный, уже
  умеет: upsert книги/песен, **не перезаписывать отредактированные пользователем песни**, пропускать
  не-новые версии, smart-bind номеров. То есть повторный импорт более новой версии безопасен.
- `ImportBundleFromFileUseCase` (там же) — готовый паттерн: `decodeBookAuto(bytes, key)` +
  `importer.import(bundle)`. Читает из `Uri`/`contentResolver`. Для ассетов нужен близнец, читающий
  из `context.assets`.
- `BundleSerializer.decodeBookAuto(bytes, key)` — **авто-детект** encrypted vs plain-gzip (работает и
  для release `.enc`, и для localSeed plain).
- Ключ дешифровки — `ContentKeyProvider.keyHex()`, привязан к buildType (release/debug/localSeed)
  через `db-android` BuildConfig. Совпадает с вариантом бандла (release-бандлы шифрованы release-
  ключом; их же дешифрует рантайм release-сборки).

### 3.3. Формат каталога и имён бандлов (для Gradle-задачи)

- `app-compose/build.gradle.kts` уже содержит helper `catalogUrl(variant)` → список из 3 зеркал
  (`catalogGhPages`, `catalogCloudflare`, `catalogYandex`), `books-catalog-{variant}.json`.
- `variant` здесь = **bundleVariant** (`release`/`debug`), задаётся в buildTypes через
  `BUNDLE_VARIANT` (не зависит от flavor).
- Имя файла бандла: `{bookId}-{bundleVariant}-{catalogVersion}.book.yaml.gz.enc`, где
  `catalogVersion` — **верхнеуровневое поле `version` каталога** (см.
  `BookCatalogRepositoryImpl.buildDownloadUrl` — использует `catalog.version`, не версию книги).
- Каталог JSON: `{ version, books: [ { book: { id, version, ... }, checksum, fileSizeBytes } ] }`
  (portable-модель). `checksum` = SHA-256, пригодится для верификации при скачивании.

### 3.4. Прецеденты и точки интеграции

- **`localSeed`** — уже есть build type (в `app-compose` и `data:db-android`) для локального dev-
  сидинга + пустой каталог `app-compose/src/ruLocalSeed/assets`. Подтверждает, что variant-scoped
  ассеты и «сборка с контентом» — знакомый паттерн.
- **AGP merge assets**: файлы из `src/<variant>/assets/` и из generated-assets попадают в APK как
  `assets/...` автоматически. Рантайм читает `context.assets.list(...)`/`open(...)`.
- **Онбординг-гейт** (`MainActivity.kt:73–85`): онбординг показывается, пока `hasInstalledBooks ==
  false`. Как только seed заполнит БД → `hasInstalledBooks == true` → онбординг **сам** не покажется.
  Нужно лишь гарантировать, что seed завершается **до** оценки гейта на первом запуске (иначе мелькнёт
  онбординг).
- DI: `contentDeliveryModule(catalogUrls, bundleVariant, keyProvider)` — сюда добавляем новый
  use-case и его binding.

---

## 4. Архитектура решения (сквозной поток)

```text
СБОРКА (build-time, per variant)
  seedBooks[flavor] = [bookId...]              ← конфиг в build.gradle.kts
        │
        ▼
  task :app-compose:generateSeedBundles<Variant>
    1. bundleVariant = release|debug (из buildType)
    2. GET books-catalog-{bundleVariant}.json (перебор зеркал)
    3. resolve catalogVersion + checksum для каждого seed bookId
    4. GET {bookId}-{bundleVariant}-{catalogVersion}.book.yaml.gz.enc
    5. verify SHA-256, записать в build/generated/seedBundles/<variant>/seed-books/
        │  (dir подключён как assets-source этого variant через AGP variant API)
        ▼
  APK: assets/seed-books/*.book.yaml.gz.enc    (только у вариантов со списком)

ПЕРВЫЙ ЗАПУСК (runtime)
  SeedBooksFromAssetsUseCase
    1. assets.list("seed-books") → для каждого файла
    2. если (bookId+version) уже засижен (SharedPreferences) → skip
    3. decodeBookAuto(bytes, keyProvider.key) → importer.import(bundle, source = ASSET)
    4. запомнить (bookId+version) как засиженное
        │
        ▼
  hasInstalledBooks == true → онбординг пропускается, приложение сразу с контентом
  UI: книги помечены built-in (source=ASSET) → без кнопки Uninstall
```

Ключевой принцип: **на рантайме предустановленный сборник неотличим от скачанного** (тот же импортёр,
та же таблица) — отличается только `source = ASSET`. Значит все существующие механизмы (smart-remap,
сохранение правок, статистика) работают без изменений.

---

## 5. Фазы и задачи

### Phase A — Build-time: конфиг + скачивание бандлов в ассеты

- **T-A01** — Конфиг seed-списка. Карта `flavor → List<bookId>` в `app-compose/build.gradle.kts`
  (напр. `val seedBooks = mapOf("rustore" to listOf("gusli"), "uk" to listOf(...))`). Пустой/
  отсутствующий список ⇒ вариант остаётся чистым. *(Формат — см. открытый вопрос O1.)*
- **T-A02** — Gradle-задача `generateSeedBundles<Variant>` (per-variant): резолвит `bundleVariant` из
  buildType, качает `books-catalog-{bundleVariant}.json` (перебор зеркал из `catalogUrl`), находит
  `catalog.version` и по каждому seed `bookId` — `checksum`; скачивает
  `{bookId}-{bundleVariant}-{version}.book.yaml.gz.enc` в generated-dir; **верифицирует SHA-256**.
  Понятно падает, если книга из списка отсутствует в каталоге.
- **T-A03** — Подключить generated-dir как **assets-source** нужного variant через AGP variant API
  (`onVariants { it.sources.assets?.addGeneratedSourceDirectory(taskProvider) { outputDir } }`).
  Задача регистрируется/запускается **только** для вариантов с непустым списком.
- **T-A04** — Кэш/офлайн: объявить входы задачи (seed-список, bundleVariant, catalogVersion) и выходы
  для incremental build; корректно вести себя при `--offline` (переиспользовать уже скачанное, не
  ходить в сеть). Не коммитить скачанные бандлы в git (добавить generated-path в `.gitignore` если
  вне `build/`).
- **T-A05** — Разнести buildType↔ключ: release-сборка качает `release`-бандлы (шифрованы release-
  ключом), debug — `debug`. Убедиться, что `BUNDLE_VARIANT`/`ContentKeyProvider` рантайма совпадают с
  тем, что скачано (иначе decode упадёт).

### Phase B — Импортёр: параметр source (снять хардкод)

- **T-B01** — `BookImporterImpl.import(bundle, source: BookInstallSource = DOWNLOADED)`. Дефолт
  сохраняет поведение существующих вызовов (`InstallBookUseCaseImpl`, `ImportBundleFromFileUseCase`).
- **T-B02** — При re-import уже установленной книги **не понижать `ASSET` до `DOWNLOADED`**: если
  запись `installed_books` уже `ASSET`, сохранить `ASSET` (читать существующий `source` перед
  upsert'ом `InstalledBookEntity`). Иначе апдейт/повторный сид сделает книгу удаляемой.
- **T-B03** — UI не трогаем: `isBuiltIn`/скрытие Uninstall/статус built-in уже работают (§3.1). Только
  подтвердить визуально на предустановленном варианте.

### Phase C — Runtime: seed из ассетов на первом запуске

- **T-C01** — `SeedBooksFromAssetsUseCase` в `:data:content-delivery` (близнец
  `ImportBundleFromFileUseCase`, но из `context.assets`): `assets.list("seed-books")` → для каждого
  файла `decodeBookAuto(bytes, keyProvider.key)` → `importer.import(bundle, source = ASSET)`.
- **T-C02** — Идемпотентность по `bookId + bundleVersion` (SharedPreferences/набор «засиженных»
  ключей). Позволяет: (а) не сидить повторно на каждом запуске; (б) при обновлении APK с **новой**
  версией бандла — доимпортировать её (importer сам сохранит правки и пропустит не-новое). Не сидить
  заново то, что пользователь уже имеет актуальным.
- **T-C03** — DI: зарегистрировать `SeedBooksFromAssetsUseCase` в `contentDeliveryModule` (нужны
  `androidContext()`, `BookImporterImpl`, `keyProvider`).
- **T-C04** — Триггер на старте **до онбординг-гейта**: запускать seed в `PwsComposeApplication`/
  `MainActivity` до/параллельно первой оценке `hasInstalledBooks`; на первом запуске держать короткий
  «preparing» стейт (splash/loading), пока seed не завершится, чтобы онбординг не мелькнул. Для чистых
  вариантов (нет `assets/seed-books`) — мгновенный no-op. *(Блокирующий splash vs async-гейт — O2.)*

### Phase D — Онбординг/UX

- **T-D01** — Подтвердить: после seed `hasInstalledBooks == true` ⇒ онбординг пропущен, приложение
  открывается сразу с контентом. Пользователь по-прежнему может добавить **другие** сборники из экрана
  Book Library (сеть) — предустановка не запрещает докачку.
- **T-D02** — (опц.) Строка статуса/иконка «встроен в приложение» уже есть
  (`book_library_status_built_in`) — проверить формулировку/локали (ru/uk/en/pl).

### Phase E — CI / release-обвязка

- **T-E01** — Убедиться, что CI-сборка предустановленных вариантов (напр. `rustore`, `uk`) имеет сеть
  и запускает `generateSeedBundles<Variant>` перед `mergeAssets`/`package`. Документировать
  зависимость сборки от доступности каталога.
- **T-E02** — Отразить в `docs/release-workflow.md`: какие варианты предустановлены, какие сборники,
  требование сети на сборке, поведение при недоступном каталоге (fail vs offline-cache).
- **T-E03** — Проверить `build.sh` / `build-compose.sh` — не мешают ли generated-assets, не нужно ли
  чистить кэш при смене seed-списка.

### Phase F — Тесты

- **T-F01** — Unit: `SeedBooksFromAssetsUseCase` — импорт тестового бандла из ассетов
  (Robolectric/`context.assets`), проверка `source = ASSET`, идемпотентность (повторный запуск не
  дублирует), доимпорт новой версии.
- **T-F02** — Unit: `BookImporterImpl` — параметр `source`; ASSET не понижается до DOWNLOADED при
  re-import; `BookUninstallerImpl` кидает на ASSET (регресс-тест уже логики).
- **T-F03** — Gradle: smoke-проверка `generateSeedBundles` (резолв версии, имя файла, verify SHA-256,
  fail при отсутствующей книге) — можно на замоканном каталоге/локальном фикстур-сервере.
- **T-F04** — E2E (Maestro) на предустановленном варианте: запуск → **без онбординга**, книга
  присутствует, для неё **нет** кнопки удаления; на чистом варианте — онбординг как прежде.

---

## 6. Открытые под-решения (уточнить при реализации)

- **O1 — Формат конфига seed-списка.** Варианты: (а) `Map` прямо в `build.gradle.kts` (просто, но
  логика в build-скрипте); (б) отдельный `seed-books.json`/`.properties` per flavor
  (`src/<flavor>/seed-books.txt`) — декларативно, ближе к контенту. *Рекомендация:* (а) на старте,
  вынести в файл если разрастётся.
- **O2 — Блокирующий splash vs async-гейт при seed.** Seed локальный и быстрый (без сети), но декод+
  импорт нескольких книг — это IO. *Рекомендация:* короткий «preparing» стейт до готовности на первом
  запуске; на последующих — мгновенно (idempotency short-circuit).
- **O3 — Верификация checksum.** На сборке (T-A02) — да (SHA-256 из каталога). На рантайме —
  опционально; бандл уже в подписанном APK, доверяем. *Рекомендация:* verify только на сборке.
- **O4 — Обновление предустановленного контента.** Из-за `hasUpdate == false` для built-in каталожный
  апдейт из UI недоступен. Обновление — через новую версию бандла в новом APK (T-C02 доимпортирует).
  Если в будущем нужен каталожный апдейт built-in — это отдельная доработка правила `hasUpdate` (вне
  этого плана).

---

## 7. Риски и крайние случаи

- **R1 — Рассинхрон ключ/вариант бандла** (release-рантайм × debug-бандл) → decode падает. Митигация:
  T-A05 жёстко связывает bundleVariant с buildType.
- **R2 — Книга из seed-списка исчезла из каталога** → сборка падает (это правильно; лучше явный fail,
  чем тихо чистый APK). T-A02.
- **R3 — Смена seed-списка не инвалидирует кэш** → в APK старый набор. Митигация: seed-список во
  входах задачи (T-A04).
- **R4 — Пользователь уже был на этом устройстве (обновление APK, не первый запуск).** БД непустая →
  idempotency по `bookId+version` не даст дублей; новые/новее-версии бандлы доимпортируются, правки
  сохраняются (importer).
- **R5 — Мелькание онбординга** до завершения seed. Митигация: T-C04 (гейт до оценки
  `hasInstalledBooks`).
- **R6 — Размер APK** растёт на суммарный размер бандлов (`fileSizeBytes` из каталога). Осознанный
  trade-off предустановленных вариантов.

---

## 8. Как проверять (по ходу реализации)

| Шаг | Команда |
|-----|---------|
| Компиляция content-delivery | `./gradlew :data:content-delivery:assembleDebug` |
| Юнит-тесты content-delivery | `./gradlew :data:content-delivery:test` (или flavor-scoped) |
| Сборка предустановленного варианта (пример) | `./gradlew :app-compose:assembleRustoreRelease` (проверить `assets/seed-books/` в APK) |
| Сборка чистого варианта (регресс) | `./gradlew :app-compose:assembleRuDebug` (пусто, онбординг как прежде) |
| Проверка ассетов в APK | `unzip -l <apk> | grep seed-books` |
| E2E | `./e2e/scripts/run-local.sh --flavor <variant>` |
| Финальная полная проверка pws-core | `./gradlew check` (медленно — в самом конце) |

---

## 9. Порядок реализации (рекомендуемый)

1. **Phase B** (параметр `source`) — маленькое изолированное изменение, ничего не ломает.
2. **Phase C** (runtime seed из ассетов) — можно тестировать, вручную положив бандл в
   `src/<flavor>/assets/seed-books/`.
3. **Phase A** (Gradle-скачивание) — автоматизирует наполнение ассетов.
4. **Phase D/E/F** — UX-подтверждение, CI, тесты.

Такой порядок даёт рабочую вертикаль (положил файл руками → сид работает → неснимаемо) ещё до
автоматизации скачивания.
