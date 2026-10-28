# RuStore-релиз нового приложения: сохранность Pro-статуса, миграция данных и режим «Pro — скоро»

> **Статус:** КОД РЕАЛИЗОВАН (2026-09-29) — фазы A–F и документация G/H; остались ручные шаги
> (апгрейд-матрица на устройстве T-G, публикация T-H). Итог и
> отклонения — §10. Задачи `T-xxx` атомарны, у каждой есть критерий приёмки.
> **Дата:** 2026-09-29 · **Ветка:** `next` · **Репозитории:** `pws-android` (+ `pws-core` через
> composite build)
> **Предшественник:** [
`2026-07-27_rustore-build-variant_plan.md`](2026-07-27_rustore-build-variant_plan.md) — его Phases
> A–E
> реализованы (flavor `rustore`, `EntitlementRepository`/`PremiumGate`, офлайн-чтение статуса,
`PaymentProvider`, пейволл).
> Этот план закрывает найденные аудитом дыры и добавляет режим «монетизация выключена».
> **Связано:** `2026-07-08_universal-apk-onboarding_plan.md`,
`2026-06-27_global-book-library_plan.md`,
> `docs/data-security.md`, `docs/release-workflow.md`.

---

## 0. TL;DR для исполнителя

Нужно выпустить в RuStore новое Compose-приложение (flavor `rustore`,
`applicationId = io.github.alelk.pws.app`)
поверх старого форка 2.3.1 так, чтобы:

1. **Ни один оплативший пользователь не потерял Pro** — ни при апгрейде, ни позже.
2. **Не потерялись пользовательские данные** (избранное, история, правки песен, теги).
3. Пока в RuStore выключена монетизация, **вместо пейволла показывается «Pro-версия — скоро»**,
   а Pay SDK вообще не вызывается. Включение продаж позже — это смена одного Gradle-флага.

**Блокеры релиза (P0), найденные аудитом — чинить первыми:**

| #  | Проблема                                                                                                                                                                     | Последствие                                                                                     | Задача       |
|----|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|--------------|
| C1 | `pws.2.3.0.db` (имя БД форка 2.3.1) **отсутствует** в `DATABASE_PREV_NAMES`                                                                                                  | Избранное/история/правки/теги пользователей 2.3.1 **не мигрируют**                              | T-A01        |
| C2 | Гонка: `runLegacyMigration` (Application) ↔ `SeedBooksFromAssetsUseCase` (PV3300, MainActivity)                                                                              | Phase 1 миграции пропускается → данные по другим книгам молча теряются, старая БД **удаляется** | T-A02, T-A03 |
| C3 | `PurchaseSyncService.sync()` пишет `purchase_full_access=false`, если в списке покупок нет `full_access_v1` (пустой список, другой аккаунт RuStore, выключенная монетизация) | **Пожизненный Pro стирается навсегда** при открытии «Покупок»                                   | T-D01, T-C03 |
| C4 | Нет флага «продажи выключены»: гейт открывает пейволл, который ходит в SDK и показывает ошибки                                                                               | Плохой UX, риск модерации, путь к C3                                                            | Phase C      |
| H1 | Чтение статуса без обработки ошибок (`dataStore.data` без `catch`)                                                                                                           | Краш/«вечный Unknown» — гейт молча не реагирует на нажатия                                      | T-B02        |

Порядок: **A → B → C → D → E → F → G (верификация) → H (релиз)**. После A+B+C+D1 сборка уже
безопасна для релиза.

---

## 1. Цель и границы

**Цель:** заменить в RuStore форк `pws-android-rustore` (2.3.1, versionCode 38) на `pws-android`
flavor `rustore`
(3.7.x, versionCode ≥ 48) с полной обратной совместимостью данных и оплаченного статуса.

**В scope:**

- совместимость Pro-статуса (офлайн, без SDK), надёжное чтение и «монотонная» запись;
- режим `PremiumComingSoon` (feature flag) + UI «Pro-версия — скоро» с указанием конкретной функции;
- миграция пользовательских данных из БД форка, детерминированный порядок старта;
- совместимость оболочки (ярлык лаунчера, подпись, applicationId, versionCode);
- матрица апгрейд-тестов, чек-лист релиза, план отката и вывода форка из эксплуатации.

**Вне scope (не делать сейчас):**

- включение реальных продаж (код остаётся готовым, но выключенным);
- обработка возвратов/отзыва покупок (revocation) — осознанно не делаем, см. §4.3;
- удаление legacy-модуля `:app` и `pws-android/app/src/rustore` — отдельной итерацией (Phase H);
- remote config / серверные флаги.

---

## 2. Что установлено аудитом (факты)

### 2.1. Опубликованные в RuStore версии форка

Источник: `pws-android-rustore/README.md`, теги git, `output/*.apk`.

| Версия | versionCode | Что было                               | БД (файл, Room ver)     | Ключи оплаты в DataStore `pws-app-preferences`        | Launcher-activity                                           |
|--------|-------------|----------------------------------------|-------------------------|-------------------------------------------------------|-------------------------------------------------------------|
| 1.1.0  | 31          | Pro-функции заблокированы, покупок нет | `pws.1.8.0.db` (8)      | —                                                     | `io.github.alelk.pws.android.app.activity.MainActivity`     |
| 2.1.1  | 36          | Покупка `full_access_v1`               | `pws.2.0.0.db` (11)     | `purchase_full_access`                                | `io.github.alelk.pws.android.app.activity.MainActivity`     |
| 2.2.0  | 37          | + подписки month/year                  | `pws.2.0.0.db` (11)     | `purchase_full_access`, `purchase_subscription_until` | `io.github.alelk.pws.android.app.feature.home.MainActivity` |
| 2.3.1  | 38          | + сборник, мигратор БД 2.0.0           | **`pws.2.3.0.db`** (11) | `purchase_full_access`, `purchase_subscription_until` | `io.github.alelk.pws.android.app.feature.home.MainActivity` |

Общее для всех: `applicationId = io.github.alelk.pws.app`,
`db_authority = io.github.alelk.pws.database`, `minSdk 23`,
БД **не зашифрована**. Опубликованные APK 2.1.0–2.3.1 лежат в `pws-android-rustore/output/` —
использовать в апгрейд-тестах.

### 2.2. Как форк хранил оплату (ядро совместимости)

Файл: `files/datastore/pws-app-preferences.preferences_pb` (Jetpack DataStore Preferences). Ключи *
*одинаковы** во всех
версиях 2.1.1–2.3.1 — миграция ключей не требуется.

| Ключ                          | Тип     | Семантика                                                           |
|-------------------------------|---------|---------------------------------------------------------------------|
| `purchase_full_access`        | Boolean | пожизненный доступ                                                  |
| `purchase_subscription_until` | String  | `yyyy-MM-dd`, `Locale.US`, часовой пояс устройства на момент записи |

Правило активности в форке (`PaymentUiState.isPremiumActive`): `full_access == true` → Pro; иначе
`subscription_until` после «сейчас» → Pro; иначе нет.

Особенности, которые надо учитывать:

- `makePurchase()` в 2.2.0+ ставит `purchase_full_access = true` **для любой** покупки (включая
  подписку), и только
  последующий `loadPurchases()` исправляет на `false`. Если синк тогда упал, подписчик остался с
  «пожизненным» флагом.
  **Решение: уважаем то, что записано** (форк вёл себя так же).
- `syncDataStoreWithPurchases()` форка *понижал* `purchase_full_access` до `false` при отсутствии
  покупки в списке.
  Новый `PurchaseSyncService` скопировал это поведение (см. C3).
- В том же файле лежат настройки форка: `app-theme` (`light|dark|black`), `song-text-expanded` (
  Boolean),
  `song-text-size` (Float, абсолютный размер). Идентификаторы тем совпадают с `ThemeMode`.

### 2.3. Что уже есть в `pws-android` (реализовано по плану 2026-07-27)

- flavor `rustore`: `applicationId io.github.alelk.pws.app`,
  `db_authority io.github.alelk.pws.database`,
  `signingConfig release-rustore`, `proguard-rules-rustore.pro`, seed-книга `PV3300`;
  `versionCode = 48` (корневой `build.gradle.kts`), `minSdk 23` — ✅.
- `app-compose/src/rustore/.../payment/`: `PaymentDataStore.kt` (тот же файл и ключи ✅),
  `RuStoreCompatEntitlementRepository`,
  `PurchaseSyncService`, `PaymentController`, `RuStorePaymentProvider`, `PaymentActivity` (
  deeplink), `PaymentScreen`.
- `pws-core:features`: `premium/` (`PremiumStatus`, `EntitlementRepository`,
  `AlwaysActiveEntitlementRepository`,
  `PremiumGate`/`DefaultPremiumGate`, `rememberPremiumGate()`),
  `monetization/MonetizationMode {Donations, PremiumSales, None}`.
- Гейты в UI: `SongDetailScreen.kt` (избранное; в `SongDetailContent` — редактирование, теги,
  «поделиться»),
  `SettingsScreen.kt` (смена темы). Пейволл открывает `MainActivity` по
  `PremiumGate.paywallRequests`.
- Юнит-тест `RuStoreCompatEntitlementRepositoryTest` (4 кейса, файл пишется через DataStore API, не
  golden).

### 2.4. Найденные проблемы (ранжированы по риску)

| ID | Риск        | Проблема                                                                                                                                                                                                                                                                                                                                                                                           | Где                                                                                     |
|----|-------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| C1 | 🔴 Critical | `pws.2.3.0.db` нет в `DATABASE_PREV_NAMES` → данные пользователей 2.3.1 не мигрируют (файл просто лежит сиротой)                                                                                                                                                                                                                                                                                   | `data/db-android/.../migrateDataFromPrevDatabase.kt`                                    |
| C2 | 🔴 Critical | `runLegacyMigration` стартует в `PwsComposeApplication.onCreate` параллельно с `SeedBooksFromAssetsUseCase` из `MainActivity`. Если seed PV3300 закоммитится первым, `bookDao().count() != 0` → Phase 1 (установка книг из старой БД) пропускается → избранное/история/правки для песен других книг не сопоставляются и **теряются**, а старый файл **удаляется** (`dbFile.delete()` при «успехе») | `PwsComposeApplication.kt:152`, `MainActivity.kt:129`, `migrateDataFromPrevDatabase.kt` |
| C3 | 🔴 Critical | `PurchaseSyncService.sync()` безусловно пишет `setFullAccessPaid(hasFullAccess)` → пустой/чужой список покупок стирает пожизненный Pro. Путь: Настройки → «Покупки» → `PaymentActivity.onResume` → `refreshData()` → `sync()`. Покупки не фильтруются по статусу                                                                                                                                   | `PurchaseSyncService.kt`, `RuStorePaymentProvider.purchases()`                          |
| C4 | 🟠 High     | Нет режима «продажи выключены»: `MONETIZATION = PremiumSales`, кнопка «Покупки» видна, гейт открывает пейволл, SDK вызывается                                                                                                                                                                                                                                                                      | `src/rustore/.../FlavorIntegration.kt`, `MainActivity.kt:73-80, 268-273`                |
| H1 | 🟠 High     | `RuStoreCompatEntitlementRepository.status` — `dataStore.data.map{}.stateIn(...)` без `catch`: `IOException`/`CorruptionException` → необработанное исключение в scope (краш) или статус навсегда `Unknown` (гейт «висит»)                                                                                                                                                                         | `RuStoreCompatEntitlementRepository.kt`                                                 |
| H2 | 🟠 High     | Статус считается только при эмиссии DataStore: подписка, истёкшая пока процесс жив, остаётся активной; хранится только дата → граница «начало дня» отрезает пользователю до суток оплаченного времени                                                                                                                                                                                              | то же                                                                                   |
| M1 | 🟡 Medium   | Launcher-компонент сменился (`…app.feature.home.MainActivity` → `…compose.MainActivity`) → на многих лаунчерах закреплённая иконка исчезает/становится «недоступна» после апдейта                                                                                                                                                                                                                  | `app-compose/src/main/AndroidManifest.xml`                                              |
| M2 | 🟡 Medium   | Дыры в гейтах: избранное в `SongDetailBySongIdScreen` (стр. ~40) не гейтится; «Поделиться» в контекстном меню `SongListItem` (BookSongsScreen, TagSongsScreen) не гейтится                                                                                                                                                                                                                         | `pws-core/features/...`                                                                 |
| M3 | 🟡 Medium   | Настройки форка (тема, развёрнутый текст) не переносятся в `app-settings` → у пользователя «слетает» тема                                                                                                                                                                                                                                                                                          | `ThemePreferences.kt`                                                                   |
| M4 | 🟡 Medium   | Старая БД удаляется сразу после миграции — необратимо; нет отчёта о миграции (сколько записей перенесено)                                                                                                                                                                                                                                                                                          | `migrateDataFromPrevDatabase.kt`                                                        |
| L1 | 🟢 Low      | `rustoreDebug` подписывается debug-ключом → нельзя поставить поверх релизной 2.3.1 и сделать `run-as` для проверки файлов                                                                                                                                                                                                                                                                          | `app-compose/build.gradle.kts`                                                          |
| L2 | 🟢 Low      | CI не проверяет сертификат подписи rustore-артефакта                                                                                                                                                                                                                                                                                                                                               | `.github/workflows/*`                                                                   |
| L3 | 🟢 Low      | `PwsBackupAgent` (key/value) не включает Pro-статус → при переносе на новое устройство статус не переедет (форк с full Auto Backup переносил)                                                                                                                                                                                                                                                      | `PwsBackupAgent.kt`                                                                     |

---

## 3. Инварианты (MUST — нарушение = потеря пользователей/денег/данных)

- **I1. Подпись.** Релизный rustore-артефакт подписан ключом с SHA-256
  `A2:E3:5B:7E:BA:1C:34:97:29:90:0D:4E:4A:70:DC:6F:97:4B:90:C6:E7:79:D3:95:0E:E0:73:27:6A:46:0A:A5` (
  CN=Vera Elkina).
- **I2. applicationId** = `io.github.alelk.pws.app`. **I3. versionCode** > 38 и монотонно растёт. *
  *I4. minSdk** ≤ 23.
- **I5. db_authority** = `io.github.alelk.pws.database`.
- **I6. Файл `pws-app-preferences` и ключи `purchase_full_access`/`purchase_subscription_until`
  неприкосновенны:**
  не переименовывать, не удалять, не очищать, не ставить `ReplaceFileCorruptionHandler`, не
  создавать второй
  `DataStore` на этот файл (один процессный делегат).
- **I7. Разблокировка работает офлайн и без Pay SDK** (источник истины — локальный файл).
- **I8. В режиме `PremiumComingSoon` приложение не пишет в `pws-app-preferences` и не
  вызывает `RuStorePayClient`.**
- **I9. Запись Pro-статуса только монотонная:** может выдать/продлить, но никогда не понижает
  автоматически.
- **I10. Пользовательские данные форка** (избранное, история, правки песен, пользовательские теги)
  переживают апгрейд
  с любой опубликованной версии (1.1.0, 2.1.1, 2.2.0, 2.3.1); исходная БД не удаляется, пока всё не
  перенесено.

---

## 4. Целевая архитектура

### 4.1. Схема

```text
┌─ pws-core:features (commonMain, generic, публичный, комментарии EN) ───────────────────────────┐
│ monetization/MonetizationMode { Donations, PremiumSales, PremiumComingSoon, None }             │
│    .donationsEnabled · .premiumGatingEnabled · .purchasesEnabled                               │
│ premium/                                                                                       │
│    PremiumFeature { FAVORITES, SONG_EDIT, SONG_TAGS, SHARE, THEME }        ← ЧТО гейтится       │
│    EntitlementInfo (Lifetime | Subscription(validUntil) | Expired(on) | None | Unknown)        │
│    EntitlementRepository { info: StateFlow<EntitlementInfo>; status (derived) }  ← порт        │
│    PremiumGate.requirePremium(feature) { … }  → при блоке эмитит UpsellRequest(feature)        │
│    UpsellHost (в AppRoot) — ЕДИНСТВЕННЫЙ потребитель UpsellRequest:                            │
│         mode.purchasesEnabled ? externalActions.openPaywall(feature) : ProComingSoonSheet(...) │
│    ProComingSoonSheet · ProStatusSection (Settings)                                            │
│ Koin defaults: EntitlementRepository=AlwaysActive, MonetizationMode=None                       │
└───────────────────────────────▲────────────────────────────────────────────────────────────────┘
                                │ Koin override (модули shell/flavor грузятся после featuresModule)
┌─ app-compose/src/rustore ─────┴────────────────────────────────────────────────────────────────┐
│ BuildConfig.PURCHASES_ENABLED  ← gradle prop `pws.rustore.purchasesEnabled` (default false)    │
│ MONETIZATION = PURCHASES_ENABLED ? PremiumSales : PremiumComingSoon                            │
│ LegacyRuStoreEntitlementStore  (файл pws-app-preferences, ключи как в форке; один экземпляр)   │
│ RuStoreCompatEntitlementRepository (read-only, error-safe, Clock-aware)                        │
│ ── только при PremiumSales ──────────────────────────────────────────────────────────────────  │
│ PaymentProvider/RuStorePaymentProvider · PaymentController · PurchaseSyncService (монотонный)  │
│ PaymentActivity (deeplink) — при ComingSoon SDK не трогает                                     │
│ LegacySettingsImporter (тема/текст → app-settings, один раз)                                   │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
┌─ app-compose/src/main + data/db-android ───────────────────────────────────────────────────────┐
│ LegacyMigrationGate: сначала миграция старой БД → потом seed/onboarding/pending-restore        │
│ DATABASE_PREV_NAMES += "pws.2.3.0.db"; карантин вместо удаления; partial → повтор              │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 4.2. Поведение по режимам и статусам

| Режим \ Статус                       | Lifetime                                                          | Subscription (до D)              | Expired (истекла D)                                                                                                 | None (не платил)                                                             |
|--------------------------------------|-------------------------------------------------------------------|----------------------------------|---------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------|
| **PremiumComingSoon** (релиз сейчас) | всё работает; Настройки: «Pro активна навсегда»                   | всё работает; «Pro активна до D» | гейт → `ProComingSoonSheet(feature, expiredOn=D)`; Настройки: «Подписка истекла D. Продление станет доступно позже» | гейт → `ProComingSoonSheet(feature)`; Настройки: «Pro-версия появится позже» |
| **PremiumSales** (будущее)           | то же                                                             | то же                            | гейт → пейволл (`PaymentActivity`)                                                                                  | гейт → пейволл                                                               |
| **Donations** (Google Play)          | `AlwaysActive` — гейты прозрачны, секции Pro нет, донаты включены |                                  |                                                                                                                     |                                                                              |

Донаты в rustore выключены в обоих режимах (не рекламируем внешние платежи в RuStore).

### 4.3. Правила записи Pro-статуса

| Действие                                 | PremiumComingSoon | PremiumSales                                                |
|------------------------------------------|-------------------|-------------------------------------------------------------|
| Чтение `pws-app-preferences`             | да                | да                                                          |
| Запись `purchase_full_access = true`     | **нет**           | да — после успешной покупки/подтверждённой покупки в списке |
| Запись `purchase_full_access = false`    | **никогда**       | **никогда** (автоматически)                                 |
| Запись `purchase_subscription_until`     | **нет**           | только `max(текущая, новая)`                                |
| Вызовы `RuStorePayClient`                | **никогда**       | да, с мягкой деградацией                                    |
| Удаление/переименование файла или ключей | **никогда**       | **никогда**                                                 |

Возвраты (refund) не отзывают доступ автоматически — осознанный выбор в пользу пользователя: ложный
отзыв Pro
дороже редкого незаслуженного доступа. Если понадобится — отдельный план с явным статусом
«возвращено» из SDK.

### 4.4. Почему так (рассмотренные альтернативы)

- **Флаг на этапе сборки (BuildConfig), а не remote config.** Статус RuStore-аккаунта меняется
  редко, включение продаж
  всё равно требует релиза (проверка SDK, консоль). Remote config добавил бы сетевую зависимость и
  новый класс отказов.
  Интерфейс `MonetizationMode` через Koin оставляет место для remote-override позже.
- **Оставить старый файл как источник истины, а не мигрировать в новое хранилище.** Нулевая
  миграция = нулевой шанс
  потерять флаг при миграции; файл уже лежит на устройстве; новая сборка только читает.
- **Pay SDK остаётся в сборке, но не вызывается.** Включение продаж = один флаг. Риск
  авто-инициализации SDK проверяется
  на устройстве (R4).
- **`PremiumComingSoon` как значение enum, а не отдельный boolean.** Сохраняет принцип «одна ось
  монетизации»
  (см. KDoc `MonetizationMode`), не порождает бессмысленных комбинаций вроде «донаты + пейволл».
- **Один потребитель `UpsellRequest` в `AppRoot`, а не в `MainActivity`.** «Скоро»-лист — это
  Compose-UI из pws-core;
  два коллектора SharedFlow привели бы к двойному показу.

---

## 5. Фазы и задачи

Легенда приоритета: **P0** — блокер релиза, **P1** — сделать в этом релизе, **P2** — желательно.
Легенда модуля: `[core]` = `pws-core`, `[android]` = `pws-android`.

### Phase A — Сохранность пользовательских данных при апгрейде (P0)

- **T-A01 [android] P0 — добавить `pws.2.3.0.db` в миграцию.**
  `data/db-android/.../migrateDataFromPrevDatabase.kt`: в `DATABASE_PREV_NAMES` (plain SQLite)
  добавить `"pws.2.3.0.db"`.
  Схема — Room v11, уже поддерживается `PwsDb2xDataProvider` (`dbVersions = 11..13`).
  *Приёмка:* новый кейс в `MigrateDataFromPrevDatabaseTest` на фикстуре
  `test-db/v11-with-user-data`, скопированной
  под именем `pws.2.3.0.db`, проходит полный путь `migrateDataFromPrevDatabase(...)` (не только
  `migrateDataTo`):
  избранное/история/правки/теги перенесены. `./gradlew :data:db-android:testRuDebugUnitTest`
  зелёный.

- **T-A02 [android] P0 — детерминированный порядок старта (`LegacyMigrationGate`).**
    - В `PwsComposeApplication`: заменить fire-and-forget `launch { runLegacyMigration }` на
      `CompletableDeferred<Unit>`, который завершается в `finally` (даже при ошибке).
      Зарегистрировать в Koin
      `single { LegacyMigrationGate(deferred) }` с `suspend fun await()` и `val isCompleted`.
    - `MainActivity`:
      `LaunchedEffect(Unit) { get<LegacyMigrationGate>().await(); preloadedReady = seed() }` —
      seed строго после миграции; `booksGate` остаётся `null` (экран загрузки), пока миграция не
      завершилась;
      `PwsBackupAgent.applyPendingRestoreIfNeeded` — тоже после `await()`.
    - Миграция идёт на IO; UI не блокируется (уже есть loading-surface для
      `preloadedReady == null`).
      *Приёмка:* тест (Robolectric), где legacy-БД содержит книги A и B, а seed-ассет — книгу A:
      после старта избранное
      по книге B на месте. Нет пути, при котором `SeedBooksFromAssetsUseCase` вызывается до
      завершения миграции (code review + тест).

- **T-A03 [android] P0 — карантин вместо удаления + частичный успех.**
    - Вместо `dbFile.delete()` — переместить файл (и `-wal`/`-shm`, если есть) в
      `databases/legacy-migrated/<name>`.
      Файлы из карантина не матчатся `DATABASE_PREV_NAMES` → повторной миграции не будет.
    - `migrateDataTo` возвращает отчёт
      `MigrationReport(favorites: found/migrated, history, editedSongs, tags)`.
      Если что-то не сопоставлено (`migrated < found`) — **не** карантинить, оставить на месте:
      повтор при следующем
      запуске/установке книги (по образцу pending-restore в `PwsBackupAgent`). Защита от вечных
      повторов: счётчик попыток
      в `SharedPreferences("pws_legacy_migration")`, после N=5 — карантин + телеметрия.
    - Телеметрия: событие `legacy_migration` с числами (без содержимого) и именем исходного файла.
    - Удаление карантина — не раньше чем через 2 релиза (Phase H).
      *Приёмка:* юнит-тесты на: полный успех → карантин; частичный → файл на месте, повтор после
      установки книги; N неудач → карантин.

- **T-A04 [android] P0 — фикстуры апгрейда.** Добавить в
  `data/db-android/src/test/resources/test-db/` фикстуру
  реальной БД форка 2.3.1 (`pws.2.3.0.db`, снятую с устройства через debug-сборку форка, см. T-G02)
  с пользовательскими
  данными в ≥ 2 книгах, включая песни не из PV3300. *Приёмка:* тест T-A01/T-A02 использует её.

- **T-A05 [android] P2 — перенос настроек форка (`LegacySettingsImporter`, только rustore).**
  Один раз при старте: если в `app-settings` нет маркера `legacy_settings_imported`, прочитать через
  **тот же**
  процессный делегат `pws-app-preferences` ключи `app-theme` → `app-theme` (идентификаторы
  совпадают),
  `song-text-expanded` → `song-text-expanded`; `song-text-size` — пропустить (другая семантика,
  абсолютный размер).
  Записать маркер. Ничего не удалять из старого файла. *Приёмка:* юнит-тест; повторный запуск не
  перезаписывает
  изменённую пользователем тему.

### Phase B — Надёжное чтение Pro-статуса (P0)

- **T-B01 [android] P0 — `LegacyRuStoreEntitlementStore`.** Рефактор `PaymentDataStore.kt` в
  класс-обёртку над
  единственным делегатом `preferencesDataStore("pws-app-preferences")` (имя файла и ключи — без
  изменений, I6).
  API: `val snapshot: Flow<LegacyEntitlementSnapshot>` (`fullAccess: Boolean?`,
  `subscriptionUntil: LocalDate?`,
  `rawSubscriptionUntil: String?`) и `internal` методы монотонной записи (используются только в
  `PremiumSales`, T-D01).
  Парсинг даты — `kotlinx.datetime.LocalDate.parse` (формат ISO `yyyy-MM-dd` совпадает), без
  `SimpleDateFormat`.
  Нераспарсиваемая дата → `subscriptionUntil = null` + non-fatal в телеметрию (сырой строки в
  телеметрию не слать).

- **T-B02 [android] P0 — обработка ошибок чтения.** `snapshot` оборачивается в
  `.catch { e -> if (e is IOException) { telemetry.recordError(e, "entitlement_read_failed"); emit(ReadFailed) } else throw e }`
  (`CorruptionException` наследует `IOException`). `ReadFailed` → `EntitlementInfo.None` (UI:
  «Pro-версия появится позже»),
  **ничего не пишем**. Файл не пересоздаём. *Приёмка:* тест с битым `.preferences_pb` → статус
  `Inactive`, без краша,
  файл байт-в-байт не изменён.

- **T-B03 [android] P0 — правило истечения + часы.** Подписка активна, пока
  `today(TimeZone.currentSystemDefault()) <= subscriptionUntil`
  (включительно до конца дня — в файле только дата, реальный момент истечения был внутри этого дня;
  форк отрезал с начала дня).
  `Clock` и `TimeZone` — инъектируемые. Статус пересчитывается на локальной полуночи:
  `combine(snapshot, midnightTicker)`,
  где тикер эмитит в ближайшую полночь; дополнительно `DefaultPremiumGate` читает актуальное
  значение в момент вызова.
  *Приёмка:* тесты «сегодня == D → Active», «D+1 → Expired», «смена дня при живом процессе →
  Expired».

- **T-B04 [core] P0 — богатая модель статуса.** В `features/premium` добавить
  `sealed interface EntitlementInfo { Unknown; None; Lifetime; Subscription(validUntil: LocalDate); Expired(expiredOn: LocalDate) }`
  и `val info: StateFlow<EntitlementInfo>` в `EntitlementRepository`; `status` становится
  производным
  (`Lifetime|Subscription → Active`, `None|Expired → Inactive`, `Unknown → Unknown`) — обратная
  совместимость для
  существующих вызовов. `AlwaysActiveEntitlementRepository.info = Lifetime`. Никаких store-типов,
  KDoc на английском.
  Маппинг snapshot → info (в rustore): `fullAccess == true → Lifetime` (приоритет), иначе дата ≥
  сегодня → `Subscription`,
  дата < сегодня → `Expired`, иначе `None`.

- **T-B05 [android] P0 — golden-тесты на реальные файлы форка.** Сгенерировать `.preferences_pb` *
  *кодом форка**
  (или той же версией `datastore-preferences`, что в форке) и положить в
  `app-compose/src/testRustore/resources/legacy-entitlement/`:

  | Фикстура                   | Содержимое                                             | Ожидание            |
      |----------------------------|--------------------------------------------------------|---------------------|
  | `none.preferences_pb`      | только настройки (`app-theme=dark`)                    | `None`              |
  | `lifetime.preferences_pb`  | `purchase_full_access=true`                            | `Lifetime`          |
  | `lifetime_false_sub.pb`    | `full_access=false`, `until=<будущее>`                 | `Subscription`      |
  | `sub_future.preferences_pb`| `until=<будущее>`                                      | `Subscription`      |
  | `sub_past.preferences_pb`  | `until=<прошлое>`                                      | `Expired`           |
  | `sub_and_full.pb`          | `full_access=true`, `until=<прошлое>`                  | `Lifetime`          |
  | `bad_date.pb`              | `until="garbage"`                                      | `None`, без краша   |
  | `corrupted.pb`             | случайные байты                                        | `None`, без краша, файл не изменён |

  Для «будущее/прошлое» — фиксированные даты + инъектируемый `Clock`. Переписать существующий
  `RuStoreCompatEntitlementRepositoryTest` на эти фикстуры.
  `./gradlew :app-compose:testRustoreDebugUnitTest` зелёный.

- **T-B06 [android] P1 — телеметрия подтверждения.** Один раз за процесс после первого определённого
  статуса:
  событие `entitlement_resolved` с атрибутами `kind` (
  `lifetime|subscription|expired|none|read_failed`) и
  `source=legacy_rustore`. Только агрегаты, без дат и идентификаторов. Нужно, чтобы после релиза
  увидеть,
  что доля `lifetime/subscription` не упала до нуля. Константы — в `TelemetryEvent`/
  `TelemetryAttr` (`pws-core:domain`).

### Phase C — Режим «Pro — скоро» (feature flag) (P0)

- **T-C01 [core] P0 — расширить `MonetizationMode`.** Добавить `PremiumComingSoon` (KDoc: «premium
  features are gated
  and existing entitlements are honoured, but purchasing is not available yet; a blocked gate shows
  a coming-soon
  message instead of a paywall»). Свойства: `donationsEnabled` (= `Donations`),
  `premiumGatingEnabled`
  (`PremiumSales|PremiumComingSoon`), `purchasesEnabled` (= `PremiumSales`). `premiumSalesEnabled`
  оставить
  `@Deprecated(replaceWith = purchasesEnabled)`. Обновить `MonetizationModeTest`.
  В `featuresModule`: `single<MonetizationMode> { MonetizationMode.None }` (дефолт; shell
  переопределяет).

- **T-C02 [android] P0 — Gradle-флаг.** В `app-compose/build.gradle.kts`:
  `val rustorePurchasesEnabled = (findProperty("pws.rustore.purchasesEnabled") as String?)?.toBoolean() ?: false`;
  `defaultConfig { buildConfigField("boolean", "PURCHASES_ENABLED", "false") }`,
  `create("rustore") { buildConfigField("boolean", "PURCHASES_ENABLED", "$rustorePurchasesEnabled") }`.
  `logger.lifecycle(...)` с текущим значением (по образцу AppMetrica-ключа). В CI workflow — input
  `rustore_purchases_enabled` (default `false`) → `-Ppws.rustore.purchasesEnabled=...`.
  В `src/rustore/.../FlavorIntegration.kt`:
  `val MONETIZATION = if (BuildConfig.PURCHASES_ENABLED) MonetizationMode.PremiumSales else MonetizationMode.PremiumComingSoon`.
  В `PwsComposeApplication` зарегистрировать `single<MonetizationMode> { MONETIZATION }` в модуле,
  который грузится
  **после** `featuresModule`. *Приёмка:* `assembleRustoreRelease` без свойства → в `BuildConfig`
  `false`.

- **T-C03 [android] P0 — Koin-граф зависит от режима.** `flavorKoinModules()` в rustore:
  всегда — `LegacyRuStoreEntitlementStore`,
  `EntitlementRepository = RuStoreCompatEntitlementRepository`;
  только при `PremiumSales` — `PaymentProvider`, `PurchaseSyncService`, `PaymentController`.
  `flavorShowPaywall()` при `PremiumComingSoon` — no-op (гейт обрабатывает `UpsellHost`).
  *Приёмка (guardrail-тест):* `koinApplication { modules(flavorKoinModules()) }` в режиме
  ComingSoon —
  `getOrNull<PaymentProvider>() == null`, `getOrNull<PurchaseSyncService>() == null`. Grep-проверка
  в ревью:
  `ru.rustore.sdk` упоминается только в `RuStorePaymentProvider.kt`/`RustoreSdkOpts.kt`.

- **T-C04 [android] P0 — `PaymentActivity` под защитой.** Deeplink-фильтр остаётся (он нужен SDK в
  будущем), но при
  `!MONETIZATION.purchasesEnabled` активность не резолвит `PaymentController`, не вызывает
  `proceedIntent`/`refreshData`,
  а открывает `MainActivity` (`FLAG_ACTIVITY_CLEAR_TOP`) и `finish()`. *Приёмка:*
  `adb shell am start -a android.intent.action.VIEW -d "io.github.alelk.pws.app://x"` → без краша,
  без обращений к SDK (logcat).

- **T-C05 [core] P0 — `PremiumFeature` и `UpsellRequest`.**
  `enum class PremiumFeature { FAVORITES, SONG_EDIT, SONG_TAGS, SHARE, THEME }`;
  `data class UpsellRequest(val feature: PremiumFeature?)` (`null` — общий вход из Настроек).
  `PremiumGate`:
  `suspend fun <T> requirePremium(feature: PremiumFeature, action: suspend () -> T): T?`,
  `val upsellRequests: SharedFlow<UpsellRequest>`; `paywallRequests` удалить (единственный
  потребитель —
  `MainActivity`, переносится в T-C07). `PremiumActionRunner.gated(feature) { }` /
  `run(feature) { }`.
  Обновить все вызовы гейта (см. Phase E). Ожидание `Unknown` — с таймаутом 3 с; по таймауту
  действие не выполняется,
  апселл **не** показывается (нельзя ложно сказать оплатившему, что у него нет Pro), пишется
  non-fatal
  `entitlement_unknown_timeout`. После T-B02 `Unknown` дольше первого чтения файла не держится.

- **T-C06 [core] P0 — `ProComingSoonSheet`.** `ModalBottomSheet` (Material 3,
  `AppModalBottomSheet`), строки в
  `composeResources` (`values`, `values-ru`, `values-uk`, `values-pl`). Содержимое:
    - заголовок: «Pro-версия — скоро»;
    - текст для функции: ««{Избранное}» войдёт в Pro-версию приложения. Мы готовим её запуск — она
      появится в одном из
      следующих обновлений.»; без функции (из Настроек): «Расширенные функции станут доступны в
      Pro-версии приложения позже.»;
    - для `Expired`: доп. строка «Срок вашей подписки истёк {dd.MM.yyyy}. Продление станет доступно
      позже.»;
    - список Pro-функций (5 пунктов, по `PremiumFeature`);
    - одна кнопка «Понятно». **Без цен, без ссылок на внешнюю оплату.**
    - `testTag`: `sheet:pro-coming-soon`, `action:pro-coming-soon-ok`.
      Названия функций для подстановки — отдельные строковые ресурсы на `PremiumFeature`.

- **T-C07 [core+android] P0 — `UpsellHost` — единственный обработчик.** Composable в `AppRoot`
  собирает
  `PremiumGate.upsellRequests`, берёт `MonetizationMode` и `EntitlementInfo` из Koin:
  `purchasesEnabled` → `LocalSettingsExternalActions.current?.openPaywall?.invoke(feature)`; иначе →
  показать
  `ProComingSoonSheet`. `SettingsExternalActions.openPaywall` → `((PremiumFeature?) -> Unit)?`.
  Удалить коллектор `paywallRequests` из `MainActivity.onCreate` (стр. 73–80). Телеметрия: событие
  `premium_upsell_shown` с `feature` и `mode` (`paywall|coming_soon`) — заменяет `PAYWALL_SHOWN` по
  смыслу
  (константу `PAYWALL_SHOWN` оставить для режима Sales или переименовать осознанно, обновив
  `docs/monitoring.md`).

- **T-C08 [core+android] P0 — секция «Pro-версия» в Настройках.** Заменить кнопку «Покупки» на
  секцию, видимую при
  `mode.premiumGatingEnabled`: строка статуса по `EntitlementInfo` (табл. §4.2) + кнопка:
  Sales → «Покупки» (`openPaywall(null)`), ComingSoon → «Подробнее» (`ProComingSoonSheet(null)`).
  В `MainActivity` `openPaywall` передавать только при `MONETIZATION.purchasesEnabled`.
  *Приёмка:* пользователь с пожизненным Pro видит «Pro активна навсегда» — это и проверка
  сохранности, и успокаивает пользователя.

- **T-C09 [android] P1 — донаты выключены в rustore в обоих режимах.**
  `DonationConfig(enabled = MONETIZATION.donationsEnabled)`
  уже даёт `false` для обоих — закрепить юнит-тестом на `MonetizationMode`.

### Phase D — Безопасная синхронизация покупок (код для будущего режима Sales)

- **T-D01 [android] P0 — монотонный `PurchaseSyncService`.** Даже если сейчас он не вызывается (
  T-C03), исправить,
  чтобы включение продаж не стёрло статус:
    - никогда не писать `purchase_full_access = false`;
    - `true` — только если в списке есть `full_access_v1` в оплаченном статусе;
    - `purchase_subscription_until = max(текущая, max(expiration активных подписок))`, запись только
      при увеличении;
    - пустой список / не авторизован / ошибка SDK → **ничего не писать**;
    - запись только через `LegacyRuStoreEntitlementStore` (единый экземпляр DataStore).
      *Приёмка:* юнит-тесты: пустой список при `full_access=true` → остаётся `true`; более ранняя
      дата подписки → не перезаписывает;
      подписка без даты → игнор.

- **T-D02 [android] P1 — фильтр по статусу покупки.** В `RuStorePaymentProvider.purchases()` маппить
  только
  оплаченные/подтверждённые покупки и активные подписки; статус пробросить в `ActivePurchase` (
  `isPaid`), чтобы
  фильтровать выше. Названия статусов сверить с актуальной документацией RuStore Pay SDK для BOM
  `2026.08.01`
  (не угадывать; если сомнения — оставить TODO и не включать продажи до сверки).

- **T-D03 [android] P1 — успех покупки.** После `PurchaseResult.Success` для `full_access_v1` —
  сразу записать
  `full_access=true` (как форк), для подписок — дождаться `purchases()` и записать дату. Не
  выставлять
  `full_access=true` для подписок (ошибка форка, §2.2).

### Phase E — Покрытие гейтов (P1)

Инвентарь (сверить при реализации поиском `rememberPremiumGate`, `onToggleFavorite`, `shareText`,
`SongEditScreen(`, `SetTheme`):

| Функция (`PremiumFeature`) | Точка входа                                                         | Сейчас          | Действие           |
|----------------------------|---------------------------------------------------------------------|-----------------|--------------------|
| FAVORITES                  | `SongDetailScreen` (2 места, `onFavoriteClick`)                     | ✅ гейт          | `gated(FAVORITES)` |
| FAVORITES                  | `SongDetailBySongIdScreen` (`onFavoriteClick`, ~стр. 40)            | ❌ **нет гейта** | добавить           |
| SONG_EDIT                  | `SongDetailContent` → `onEditSong`                                  | ✅               | `gated(SONG_EDIT)` |
| SONG_TAGS                  | `SongDetailContent` → `onEditTags`                                  | ✅               | `gated(SONG_TAGS)` |
| SHARE                      | `SongDetailContent` → `onShare`                                     | ✅               | `gated(SHARE)`     |
| SHARE                      | `SongListItem` → контекстное меню (BookSongsScreen, TagSongsScreen) | ❌ **нет гейта** | добавить           |
| THEME                      | `SettingsScreen` → `onThemeSelected`                                | ✅               | `run(THEME)`       |

- **T-E01 [core] P1** — закрыть две дыры, перевести все вызовы на `PremiumFeature`.
- **T-E02 [core] P1 — удаление из избранного бесплатно** (решение по умолчанию, см. O2): гейтить
  только добавление;
  `FavoritesScreen` (удаление) не трогать.
- **T-E03 [android] P1 — Maestro e2e для rustore** (`e2e/flows/compose/rustore/`): (1) без Pro —
  каждое действие из
  таблицы показывает `sheet:pro-coming-soon` и не выполняется; (2) с Pro (подложенный файл) —
  действия выполняются,
  лист не появляется; (3) Настройки показывают правильный статус.

### Phase F — Совместимость оболочки и защита релиза (P1)

- **T-F01 [android] P1 — alias старого launcher-компонента.** В
  `app-compose/src/rustore/AndroidManifest.xml`:
  `<activity-alias android:name="io.github.alelk.pws.android.app.feature.home.MainActivity" android:targetActivity=".MainActivity" android:exported="true">`
  с intent-filter `MAIN`+`LAUNCHER` (+ `VIEW`, `SENDTO` как в main), а у `.MainActivity` в
  rustore-overlay убрать
  LAUNCHER-фильтр (`tools:node`), чтобы иконка была одна. *Приёмка:* в merged manifest
  (`build/intermediates/merged_manifests/rustoreRelease/...`) ровно одна LAUNCHER-точка — alias;
  на устройстве закреплённая иконка 2.3.1 после апдейта запускает приложение (проверить Pixel
  Launcher и One UI/MIUI, что есть).

- **T-F02 [android] P1 — guard-проверка инвариантов в Gradle.** Для варианта `rustoreRelease`
  задача, которая падает,
  если `applicationId != "io.github.alelk.pws.app"`, `versionCode <= 38`, `minSdk > 23`,
  `db_authority` иной.
  Подключить к `preBuild` варианта.

- **T-F03 [android] P1 — проверка сертификата в CI.** После сборки rustore:
  `apksigner verify --print-certs app-compose-rustore-release.apk` → сравнить SHA-256 с I1; при
  несовпадении — fail.
  Для `.aab` — `jarsigner -verify -verbose -certs` / `keytool -printcert -jarfile`.

- **T-F04 [android] P1 — `rustoreDebug` подписан rustore-ключом** (если заданы свойства
  `android.release.*Rustore`, как в
  форке) — чтобы ставить debuggable-сборку поверх релиза форка и делать
  `run-as io.github.alelk.pws.app` для проверки файлов.

- **T-F05 [android] P2 — перенос Pro-статуса при смене устройства.** Решить (O4): включать ли
  snapshot entitlement в
  payload `PwsBackupAgent` (восстановление только монотонно). По умолчанию — не делаем в этом
  релизе.

### Phase G — Верификация апгрейда (P0, ручная + скрипт)

- **T-G01 Матрица.**

  | Из версии (APK из `pws-android-rustore/output`) | Pro-состояние                                     | Сеть     | RuStore-приложение |
      |-------------------------------------------------|---------------------------------------------------|----------|--------------------|
  | 2.3.1 (осн.), 2.2.0, 2.1.1; 1.1.0 — если найдётся APK | none · lifetime · sub будущая · sub прошлая · full=true+sub прошлая | вкл/выкл | есть/нет           |

- **T-G02 Процедура** (оформить как `tools/rustore-upgrade-test.md` или скрипт
  `tools/rustore-upgrade-test.sh`):
    1. Собрать debug-форк 2.3.1 (`pws-android-rustore`: `./gradlew :app:assembleRuDebug` — он
       подписан rustore-ключом)
       **или** поставить релизный APK 2.3.1 и использовать debug-форк только для подготовки файлов.
    2. Создать данные: избранное в ≥ 2 книгах (одна — не PV3300), историю, правку песни,
       пользовательский тег, тёмную тему.
    3. Подложить Pro-состояние (процесс приложения остановлен,
       `adb shell am force-stop io.github.alelk.pws.app`):
       `adb exec-in run-as io.github.alelk.pws.app sh -c 'mkdir -p files/datastore && cat > files/datastore/pws-app-preferences.preferences_pb' < <fixture>.preferences_pb`
       (фикстуры из T-B05). Снять `sha256sum` файла (`adb exec-out run-as … cat files/datastore/…`).
    4. `adb install -r` новый `rustoreRelease` (или `rustoreDebug` по T-F04).
    5. Проверки: данные на месте; статус в Настройках соответствует фикстуре; без Pro —
       «скоро»-лист; в режиме офлайн
       холодный старт без крашей; в logcat нет сетевых вызовов RuStore Pay; `pws.2.3.0.db` в
       `databases/legacy-migrated/`;
       тема перенесена (если T-A05); закреплённая иконка работает (T-F01);
       `files/datastore/pws-app-preferences.preferences_pb` **байт-в-байт не изменён** (`sha256sum`
       до/после).
- **T-G03** Проверить, что книги, пришедшие из старой БД, корректно видны в библиотеке и обновляются
  из каталога без
  дублей (связь с `2026-06-27_global-book-library_plan.md`).
- **T-G04** Холодный старт без RuStore-приложения и без сети: SDK (авто-инициализация по meta-data)
  не роняет процесс.

### Phase H — Релиз, откат, вывод форка

- **T-H01 Чек-лист релиза** (дополнить `docs/release-workflow.md`, раздел RuStore):
  CI-сборка с `rustore_purchases_enabled=false` · проверка сертификата (T-F03) · загрузить **тот же
  тип артефакта**,
  что и для 2.3.1 (при AAB — убедиться, что в консоли RuStore зарегистрирован тот же ключ) ·
  обновить описание/скриншоты
  (новый интерфейс) · обновить декларацию данных в консоли (AppMetrica, `docs/privacy-policy.md`) ·
  текст «Что нового»
  (упомянуть, что оплаченный Pro сохраняется) · поэтапная публикация, если консоль её предлагает ·
  выгрузить mapping в AppMetrica.
- **T-H02 Мониторинг 72 ч:** crash-free, `legacy_migration` (доля частичных),
  `entitlement_resolved` (доли `lifetime/subscription`
  ≠ 0 и сопоставимы с числом покупок в консоли RuStore), `entitlement_read_failed`,
  `premium_upsell_shown`.
- **T-H03 План отката.** Откатить versionCode в сторе нельзя. Хотфикс — только новым versionCode из
  `pws-android`.
  Аварийный вариант — пересобрать форк с versionCode > текущего; ограничение: он не увидит данные,
  созданные в новом
  приложении, и БД из карантина (форку нужно добавить `legacy-migrated/pws.2.3.0.db` в поиск).
  Поэтому форк держать
  собираемым до конца периода стабилизации.
- **T-H04 Вывод форка (не раньше чем через 4 недели стабильной работы):** архивировать
  `pws-android-rustore` (read-only,
  README со ссылкой); удалить дубли `pws-android/app/src/rustore/**` (legacy `:app`); через 2
  релиза — удалить
  `databases/legacy-migrated/`; обновить `AGENTS.md`/`CLAUDE.md`, пометить этот план РЕАЛИЗОВАН.

---

## 6. Риски

| #  | Риск                                                                       | Вероятн.       | Ущерб      | Митигация                                                                                       |
|----|----------------------------------------------------------------------------|----------------|------------|-------------------------------------------------------------------------------------------------|
| R1 | Потеря Pro у оплативших (C3, H1, неверная дата)                            | средн. → низк. | 🔴 критич. | I6–I9, T-B01…B05, T-C03, T-D01, T-G02 (sha256 файла)                                            |
| R2 | Потеря пользовательских данных (C1, C2)                                    | высок. → низк. | 🔴 критич. | T-A01…A04, карантин, T-G02                                                                      |
| R3 | Неверный ключ подписи → апдейт не встаёт ни у кого                         | низк.          | 🔴 критич. | T-F03 в CI, ручная сверка перед загрузкой                                                       |
| R4 | Авто-инициализация Pay SDK падает без RuStore-приложения/сети              | низк.          | 🟠 высок.  | T-G04; при проблеме — убрать meta-data авто-init в режиме ComingSoon через manifest placeholder |
| R5 | Гонка/долгая миграция на слабых устройствах → долгий экран загрузки        | средн.         | 🟡 средн.  | миграция на IO, loading-surface, телеметрия длительности                                        |
| R6 | Исчезновение иконки с рабочего стола                                       | средн.         | 🟡 средн.  | T-F01                                                                                           |
| R7 | Модерация RuStore: «экран оплаты без оплаты»                               | низк.          | 🟡 средн.  | ComingSoon без цен и внешних ссылок, SDK не вызывается                                          |
| R8 | Пользователь переустановил приложение → локального флага нет, SDK выключен | низк.          | 🟡 средн.  | Вне scope; см. O5 (ручная выдача доступа)                                                       |
| R9 | Двойной показ апселла (два коллектора)                                     | средн.         | 🟢 низк.   | T-C07: единственный коллектор в `AppRoot`                                                       |

---

## 7. Решения по умолчанию и открытые вопросы

Приняты по умолчанию (исполнитель делает так, если владелец не решит иначе):

- **O1. Истёкшая подписка** → функции блокируются, показывается «скоро» с датой истечения (по
  требованию владельца).
  *Альтернатива:* продлить доступ подписчикам до включения продаж + N дней (они не могут продлить не
  по своей вине).
- **O2. Удаление из избранного** — бесплатно; гейтится только добавление.
- **O3. Смена темы** — гейт как в форке. *Альтернатива:* разрешить бесплатно «Системная».
- **O4. Перенос Pro через системный бэкап** — не делаем в этом релизе (T-F05).
- **O5. Пользователи, потерявшие локальный флаг** (переустановка/новое устройство) — вне scope.
  *Опция на будущее:*
  офлайн-код разблокировки, подписанный ключом разработчика (Ed25519, публичный ключ в приложении),
  выдаётся вручную
  после сверки покупки в консоли RuStore.
- **O6. Набор Pro-функций** — как в форке (5 функций). Новые функции Compose-приложения (библиотека
  книг, динамические
  цвета, бэкап и т. д.) остаются бесплатными.

---

## 8. Порядок выполнения и Definition of Done

**Порядок:** T-A01 → T-A02 → T-A03 → T-A04 → T-B01…B05 → T-C01…C08 → T-D01 → T-E01…E03 → T-F01…F04 →
T-G01…G04 →
T-B06, T-C09, T-D02, T-D03, T-A05 → T-H01…H03. T-H04 — отдельной итерацией.

**DoD релиза:**

- [ ] Все P0 задачи выполнены, критерии приёмки проверены.
- [ ] `pws-core`: `./gradlew :features:jvmTest` (или актуальная задача тестов features) — зелёный.
- [ ] `pws-android`:
  `./gradlew :data:db-android:testRuDebugUnitTest :app-compose:testRustoreDebugUnitTest :app-compose:assembleRustoreRelease :app-compose:assembleRuDebug` —
  зелёный (Google Play-флейворы не сломаны: гейты прозрачны, донаты работают).
- [ ] Матрица T-G01 пройдена минимум для 2.3.1 × все Pro-состояния × офлайн, и для 2.2.0/2.1.1 ×
  lifetime.
- [ ] Хэш `pws-app-preferences.preferences_pb` не изменился после апгрейда и недели использования в
  режиме ComingSoon.
- [ ] Сертификат совпадает с I1 (CI + ручная сверка).
- [ ] `docs/release-workflow.md` и `docs/monitoring.md` обновлены; этот план помечен РЕАЛИЗОВАН с
  датой.

---

## 9. Приложение: быстрые ссылки

| Что                                  | Где                                                                                                                   |
|--------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| Логика оплаты форка (эталон)         | `pws-android-rustore/app/src/main/kotlin/io/github/alelk/pws/android/app/feature/payment/PaymentManager.kt`           |
| DataStore форка                      | `pws-android-rustore/app/.../core/di/DataStoreModule.kt` (`pws-app-preferences`)                                      |
| Имя БД форка                         | `pws-android-rustore/database/src/androidMain/.../PwsDatabaseProvider.kt` (`pws.2.3.0.db`)                            |
| Опубликованные APK                   | `pws-android-rustore/output/pws-app-release-{2.1.0,2.1.1,2.2.0,2.3.1}-ru.apk`                                         |
| Rustore-код нового приложения        | `pws-android/app-compose/src/rustore/kotlin/io/github/alelk/pws/android/compose/{flavor,payment}/`                    |
| Порт entitlement/gate                | `pws-core/features/src/commonMain/kotlin/io/github/alelk/pws/features/{premium,monetization}/`                        |
| Миграция старых БД                   | `pws-android/data/db-android/src/main/kotlin/io/github/alelk/pws/database/migrateDataFromPrevDatabase.kt`             |
| Старт приложения / seed / onboarding | `pws-android/app-compose/src/main/kotlin/io/github/alelk/pws/android/compose/{PwsComposeApplication,MainActivity}.kt` |
| Product IDs                          | `full_access_v1`, `monthly_subscription_v1`, `yearly_subscription_v1` (совпадают с консолью)                          |
| Сертификат                           | SHA-256 `A2:E3:5B:7E:BA:1C:34:97:29:90:0D:4E:4A:70:DC:6F:97:4B:90:C6:E7:79:D3:95:0E:E0:73:27:6A:46:0A:A5`             |

---

## 10. Статус реализации (2026-09-29)

Легенда: ✅ сделано и покрыто тестами · 📝 сделано, проверка ручная / на устройстве · ⚠️ частично.

| Задача       | Статус | Где / комментарий                                                                                                                                                                                                                                                                                                                                                                                               |
|--------------|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| T-A01        | ✅      | `DATABASE_PREV_NAMES += "pws.2.3.0.db"`; `MigrateLegacyDatabaseFileTest` (полный путь `migrateDataFromPrevDatabase`)                                                                                                                                                                                                                                                                                            |
| T-A02        | ✅      | `LegacyMigrationGate` (`app-compose/.../LegacyMigrationGate.kt`), `CompletableDeferred` в `PwsComposeApplication` (`finally`); seed, pending-restore и повтор миграции — после `await()`. Тесты: `LegacyMigrationGateTest` (порядок) + `MigrateLegacyDatabaseFileTest` «C2 regression» / T-A04 (данные книг ≠ PV3300). Полноценный Robolectric-старт всего приложения не делался — заменён этими двумя уровнями |
| T-A03        | ✅      | карантин `databases/legacy-migrated/` (+ `-wal/-shm/-journal`), `MigrationReport`/`LegacyMigrationOutcome`, повтор при старте и после установки книги (`countAttempt=false`), `N=5` → карантин + non-fatal, событие `legacy_migration`                                                                                                                                                                          |
| T-A04        | ✅      | `test-db/v11-rustore-2.3.1/pws.2.3.0.dbz` — БД из **опубликованного APK 2.3.1** (3 книги) + пользовательские данные в схеме форка; генератор `tools/make-rustore-fork-db-fixture.py`. Снимок с реального устройства — опционально (T-G02)                                                                                                                                                                       |
| T-A05        | ✅      | `LegacySettingsImporter` (rustore), `LegacySettingsImporterTest`                                                                                                                                                                                                                                                                                                                                                |
| T-B01…B03    | ✅      | `LegacyRuStoreEntitlementStore`, `RuStoreCompatEntitlementRepository` (Clock/TimeZone, полуночный тикер, `currentInfo()`)                                                                                                                                                                                                                                                                                       |
| T-B04        | ✅      | `EntitlementInfo`, `EntitlementRepository.info` + `currentInfo()`, `status` — производное (extension)                                                                                                                                                                                                                                                                                                           |
| T-B05        | ✅      | golden-файлы `app-compose/src/testRustore/resources/legacy-entitlement/`; сгенерированы **независимым** кодировщиком протобуфа (`tools/make-legacy-entitlement-fixtures.py`), а не кодом приложения                                                                                                                                                                                                             |
| T-B06        | ✅      | `entitlement_resolved` (`kind`, `source=legacy_rustore`)                                                                                                                                                                                                                                                                                                                                                        |
| T-C01…C09    | ✅      | `MonetizationMode.PremiumComingSoon`, флаг `pws.rustore.purchasesEnabled` → `BuildConfig.PURCHASES_ENABLED`, `rustoreKoinModules(mode)`, `PaymentActivity`-guard, `PremiumFeature`/`UpsellRequest`, `ProComingSoonSheet`, `UpsellHost` в `AppRoot`, секция «Pro-версия» в Настройках; тесты `MonetizationModeTest`, `DefaultPremiumGateTest`, `RustoreKoinModulesTest`                                          |
| T-D01, T-D03 | ✅      | монотонный `PurchaseSyncService` (+ `onPurchaseSucceeded`), `PurchaseSyncServiceTest`                                                                                                                                                                                                                                                                                                                           |
| T-D02        | ✅      | `ActivePurchase.isPaid`; сверено с Pay SDK 11.1.0 (BOM 2026.08.01): Pro выдают только `ProductPurchaseStatus.CONFIRMED` (покупки `ONE_STEP`; `PAID` — удержание двухстадийной оплаты, может стать `REVERSED`) и `SubscriptionPurchaseStatus.ACTIVE`                                                                                                                                                             |
| T-E01, T-E02 | ✅      | гейт избранного в `SongDetailBySongIdScreen`, «поделиться» в `SongListItem`; гейтится только добавление в избранное                                                                                                                                                                                                                                                                                             |
| T-E03        | 📝     | Maestro `e2e/flows/compose/rustore/` + `e2e/scripts/rustore-entitlement.sh`; на устройстве не запускались                                                                                                                                                                                                                                                                                                       |
| T-F01        | ✅/📝   | `activity-alias` `…app.feature.home.MainActivity`; merged manifest `rustoreRelease` проверен — ровно одна LAUNCHER-точка (alias). Закреплённую иконку проверить на устройстве                                                                                                                                                                                                                                   |
| T-F02        | ✅      | `verifyRustoreReleaseInvariants` → `preRustoreReleaseBuild`                                                                                                                                                                                                                                                                                                                                                     |
| T-F03        | ✅      | шаг CI `Verify rustore signing certificate` (APK + AAB); скрипт проверен на артефактах 2.3.1 — совпадает с I1                                                                                                                                                                                                                                                                                                   |
| T-F04        | ✅      | `rustoreDebug` подписывается rustore-ключом, если заданы `android.release.*Rustore`                                                                                                                                                                                                                                                                                                                             |
| T-F05        | —      | не делаем в этом релизе (O4)                                                                                                                                                                                                                                                                                                                                                                                    |
| T-G01…G04    | 📝     | процедура и матрица — [`tools/rustore-upgrade-test.md`](../../../tools/rustore-upgrade-test.md)                                                                                                                                                                                                                                                                                                                 |
| T-H01…H03    | 📝     | [`docs/release-workflow.md`](../../release-workflow.md) → «RuStore: чек-лист релиза, мониторинг, откат»; [`docs/monitoring.md`](../../monitoring.md)                                                                                                                                                                                                                                                            |
| T-H04        | —      | отдельной итерацией                                                                                                                                                                                                                                                                                                                                                                                             |

**Отклонения и находки при реализации:**

- **Предустановленные теги.** Связи песен с предустановленными тегами терялись по FK, если такого
  тега ещё нет в новой БД (теги приходят только с бандлами). Теперь миграция создаёт недостающий
  предустановленный тег из старой строки. Без этого отчёт о миграции форка навсегда оставался бы
  «частичным».
- **История** больше не переносится «только в пустую таблицу»: записи сверяются по
  (книга, песня, время), поэтому повтор после частичной миграции идемпотентен и добирает остаток.
- **Правки песен** считаются по записям (книга, номер) — одна отредактированная песня, входящая в
  несколько книг, учитывается в каждой.
- Сбой чтения одного вида данных (например, тегов) больше не прерывает перенос остальных; прогон
  считается неуспешным, файл остаётся для повтора.
- **Иконка.** Rustore-иконка отличается от Google Play: белая лира на фиолетовом
  (`src/rustore/res/values/colors.xml` + `src/rustore/res/drawable/ic_launcher_legacy.xml`), чтобы
  два приложения на одном устройстве не путались. Попутно исправлено для всех флейворов: иконка была
  только адаптивной (`mipmap-anydpi-v26`), и на Android 6–7.1 (minSdk 23) её не было вовсе —
  добавлена `mipmap-anydpi/ic_launcher.xml` → `drawable/ic_launcher_legacy.xml` (цвета литералами:
  растеризатор AGP не понимает `@color`, а без растеризации Android 6 не умеет `fillType=evenOdd`).
- Тестовые testTag для Maestro: `theme-row-<id>` (Настройки), `action:share-song` (действия песни),
  `text:pro-status`, `action:pro-details`, `sheet:pro-coming-soon`, `action:pro-coming-soon-ok`.

