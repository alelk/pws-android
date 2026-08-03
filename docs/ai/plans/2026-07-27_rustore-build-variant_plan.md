# RuStore build variant — перенос монетизации в основной проект

> **Статус:** РЕАЛИЗОВАНО (Phases A–E + F-код) — осталась ручная верификация I1/I5 и e2e (F01/F03/F04/F07)
> **Дата:** 2026-07-27 (реализация 2026-07-28)
> **Ветка:** next

---

## 0. Статус реализации (2026-07-28)

**Готово и собирается** (`assembleRustoreDebug` ✅, `compileRuDebugKotlin` ✅, офлайн-тест ✅):

- **Phase A** — version catalog `rustore-sdk-pay`, maven-репо RuStore ограничен `includeGroup("ru.rustore.sdk")`,
  flavor-scoped `rustoreImplementation`, `src/rustore/AndroidManifest.xml` (SDK meta-data + BROWSABLE deeplink
  на `.MainActivity` + `PaymentActivity`), строки `CONSOLE_APPLICATION_ID` + payment/error (en/ru/uk/pl).
- **Phase B** — `PremiumStatus`/`EntitlementRepository`/`PremiumGate` + `AlwaysActiveEntitlementRepository`/
  `DefaultPremiumGate` в **публичном** `pws-core:features` (generic, English). Дефолтные Koin-биндинги в
  `featuresModule`. Гейт-точки — на UI-уровне через `rememberPremiumGate()` (composable-раннер): избранное,
  редактирование песни, редактирование тегов, «поделиться» (`SongDetailScreen`), смена темы (`SettingsScreen`).
  Пейволл вызывается через `PremiumGate.paywallRequests`, наблюдается в `MainActivity`.
  > Отклонение от плана: гейт встроен в **точках вызова UI** внутри `pws-core:features`, а не в конструкторы
  > ScreenModel — тот же generic-порт, меньше инвазивности (без правки FeaturesModule/тестов). Для Google Play
  > гейт прозрачен (Active).
- **Phase C** — `RuStoreCompatEntitlementRepository` (читает DataStore `pws-app-preferences`, офлайн),
  rustore Koin-модуль (`FlavorIntegration.kt`) переопределяет `EntitlementRepository`; юнит-тест офлайн-
  разблокировки (4 кейса) — зелёный (`:app-compose:testRustoreDebugUnitTest`).
- **Phase D** — generic `PaymentProvider` + модели (`PaymentProduct`/`ActivePurchase`/`AuthStatus`/
  `PurchaseResult`), `RuStorePaymentProvider` (обёртка над `RuStorePayClient`, `Task.coAwait()`),
  `PurchaseSyncService` (запись в DataStore), `PaymentController` с мягкой деградацией.
- **Phase E** — `PaymentScreen` (Compose, развязан от RuStore-типов), `PaymentActivity` (deeplink +
  `AppTheme`), строки en/ru/uk/pl, точка входа «Покупки» в настройках (видна только при
  `openPaywall != null`, т.е. только rustore).
- **Phase F (код)** — `build-compose.sh` копирует rustore `.aab/.apk`; `versionCode` 46 (> 38);
  `docs/release-workflow.md` дополнен I1-проверкой сертификата и разделом о монетизации.

**Осталось (ручное, требует keystore/устройства):** T-F01 (сверка SHA-256 сертификата при
`assembleRustoreRelease`), T-F03 (апгрейд 2.3.1→новый: данные пользователя), T-F04 (сценарий офлайн-
разблокировки на устройстве), T-F06 (архив форка), T-F07 (Maestro e2e для rustore). T-A06: SDK 2025.08
авто-инициализируется через meta-data (как в форке) — проверить на устройстве.
> **Связано:** `2026-07-08_universal-apk-onboarding_plan.md` (миграция БД old→Compose),
> `docs/data-security.md`, `docs/release-workflow.md`

---

## 1. Цель

Ликвидировать отдельный форк `../pws-android-rustore` и превратить сборку для RuStore в **build
variant** внутри основного проекта `pws-android` (модуль `app-compose`, flavor `rustore`), при этом:

1. **Полная APK-совместимость** с версией, уже установленной у пользователей RuStore
   (обновление «на месте», без потери данных и оплаченного статуса).
2. **Офлайн-разблокировка платных функций** для тех, у кого подписка/полный доступ уже оплачены —
   статус хранится в настройках на устройстве и читается **без интернета**.
3. **Поддержка RuStore Pay SDK** (на будущее). Сейчас монетизация на аккаунте отключена → SDK-вызовы
   работать не будут; код должен деградировать мягко и не ломать офлайн-разблокировку.
4. **Универсальный, независимый от конкретного Pay SDK** механизм платных функций — чтобы в любой
   момент подключить другую платёжную систему без переписывания фич.

---

## 2. Что установлено разведкой (факты)

### 2.1. Форк `pws-android-rustore` — старая архитектура

Форк отстал: версия **2.3.1** (main — 3.5.0), архитектура **до** выделения `pws-core`. UI на
**Fragment/Activity** (`MainActivity`, `SongActivity`, `GeneralPreferenceFragment`,
`AppCompatPreferenceActivity`), DI на **Hilt/Dagger + kapt**, модули (`domain`, `database`, `backup`)
инлайн. Текущий проект — **Compose** (`app-compose` — тонкая оболочка ~6 файлов) + `pws-core:features`
(Compose Multiplatform, Voyager, **Koin**). → Перенос — это **реархитектура биллинга под Compose/Koin**,
а не копирование файлов.

### 2.2. Хранение оплаченного статуса (ядро совместимости)

Форк хранит статус в Jetpack **DataStore Preferences**, файл **`pws-app-preferences`**:

| Ключ                          | Тип     | Формат                         |
|-------------------------------|---------|--------------------------------|
| `purchase_full_access`        | Boolean | полный доступ куплен            |
| `purchase_subscription_until` | String  | дата `yyyy-MM-dd` (`Locale.US`) |

Логика активности премиума (`PaymentManager.PaymentUiState.isPremiumActive`):
```
fullAccessPaid == true            -> true
subscriptionUntil != null && after(now) -> true
initialDataFetched                -> false
else                              -> null (неизвестно)
```
> Текущий `app-compose` использует **другой** DataStore-файл `app-settings` (`ThemePreferences.kt`).
> Файлы сосуществуют. Новая rustore-сборка должна читать **старый** файл `pws-app-preferences` с теми
> же ключами → офлайн-разблокировка на месте.

### 2.3. Платные функции форка (что гейтится)

Через `PaymentManager.handlePremiumFeature(fm) { ... }` (если премиум активен — действие; иначе
показывается `PaymentDialogFragment`):

- **Добавление в избранное** (FAB, `SongActivity`)
- **Редактирование песни** (`menu_edit` → `SongEditActivity`)
- **Редактирование тегов/категорий** (`menu_edit_categories`)
- **Поделиться песней** (`menu_share`)
- **Смена темы оформления** (`GeneralPreferenceFragment`)

Базовый просмотр/поиск/навигация — **бесплатны**. Google Play-версия (flavors ru/uk/full) сейчас
**полностью бесплатна** + модель донатов (`donation` пакет, Boosty).

### 2.4. RuStore Pay SDK (форк)

- Каталог: `rustore-sdk-bom = ru.rustore.sdk:bom:2025.08.01`, артефакт `ru.rustore.sdk:pay`.
- Maven-репозиторий: `https://artifactory-external.vkpartner.ru/artifactory/maven`.
- Manifest (авто-инициализация SDK, **без** явного `RuStorePayClient.init`):
  - `<meta-data name="sdk_pay_scheme_value" value="io.github.alelk.pws.app"/>`
  - `<meta-data name="console_app_id_value" value="@string/CONSOLE_APPLICATION_ID"/>`
    (`CONSOLE_APPLICATION_ID = 2063600773`, закоммичен, не секрет)
  - Launcher-activity: intent-filter `VIEW + BROWSABLE`, `<data scheme="io.github.alelk.pws.app"/>`
    — **возврат в приложение после оплаты** (deeplink).
- Interactors: `Purchase/User/Product/Intent`. Product IDs: `full_access_v1`,
  `monthly_subscription_v1`, `yearly_subscription_v1`.
- Пейволл `PaymentScreen.kt` **уже на Compose/Material3**, но завязан на типы `ru.rustore.sdk.pay.model.*`
  (`Product`, `Purchase`, `SubscriptionPurchase`) и `R.string.*` → переиспользуем после развязки от
  RuStore-типов.

### 2.5. Каркас flavor уже частично есть ✅

В `app-compose/build.gradle.kts` **уже** объявлен flavor `rustore` (dimension `contentLevel`):
`applicationId = "io.github.alelk.pws.app"`, `versionNameSuffix = "-rustore"`,
`db_authority = "io.github.alelk.pws.database"`, отдельный `signingConfig("release-rustore")`
(`keystorePathRustore` / `keyAliasRuRustore` / `keyPasswordRustore` / `storePasswordRustore`).
Есть source set `app-compose/src/rustore/res/`. `build-compose.sh` уже собирает
`bundleRustoreRelease`/`assembleRustoreRelease` (строки `cp` закомментированы). **Платёжного кода
в flavor нет.**

### 2.6. Верифицированные инварианты совместимости (из опубликованных APK)

`pws-rustore-releases/pws-app-release-2.3.1-ru.apk` (последнее на устройствах пользователей):

| Параметр           | Опубликовано (2.3.1)                                   | Новый build (`rustore` flavor)             | OK? |
|--------------------|-------------------------------------------------------|--------------------------------------------|-----|
| applicationId      | `io.github.alelk.pws.app`                              | `io.github.alelk.pws.app`                  | ✅  |
| versionCode        | `38`                                                  | `45` (rootProject.extra, монотонно растёт) | ✅  |
| db_authority       | `io.github.alelk.pws.database`                        | `io.github.alelk.pws.database`             | ✅  |
| Сертификат подписи | `CN=Vera Elkina` SHA256 `A2:E3:5B:7E:BA:1C:34:97:29:90:0D:4E:4A:70:DC:6F:97:4B:90:C6:E7:79:D3:95:0E:E0:73:27:6A:46:0A:A5` | keystore `keystorePathRustore` — **ДОЛЖЕН совпасть** | ⚠️ verify |

---

## 3. Инварианты совместимости (MUST — нарушение = потеря пользователей/данных)

- **I1. Ключ подписи.** Релизный APK/AAB rustore-flavor подписан keystore, чей сертификат SHA-256 ==
  `A2:E3:5B:…:0A:A5` (CN=Vera Elkina). Иначе обновление у пользователей **не установится**.
- **I2. applicationId** = `io.github.alelk.pws.app` (не менять). Обеспечивает in-place обновление и
  сохранность приватной data-директории (БД + DataStore).
- **I3. versionCode** строго `>` опубликованного (38). Сейчас 45 — ок; держать монотонным.
- **I4. Оплаченный статус.** Читать DataStore `pws-app-preferences` → ключи `purchase_full_access`,
  `purchase_subscription_until` (формат `yyyy-MM-dd`). Разблокировка **без сети**.
- **I5. Данные пользователя.** Избранное/история/теги/пользовательские песни переживают апгрейд
  old-Fragment(2.3.1)→Compose через существующий `runLegacyMigration` (проверить для rustore-БД).
- **I6. db_authority** = `io.github.alelk.pws.database` (не менять — ContentProvider/бэкап).

---

## 4. Целевая архитектура: универсальный механизм платных функций

Три слоя. Всё, что выше провайдера, **не знает** про RuStore.

```
┌─ pws-core:features (commonMain, pay-agnostic, публичный) ──────────────┐
│  PremiumStatus { Active, Inactive, Unknown }                            │
│  interface EntitlementRepository { val status: StateFlow<PremiumStatus> }│
│  PremiumGate: suspend requirePremium(action) -> T?  (+ paywallRequests) │
│  Точки гейта в ScreenModels: favorite / edit / tags / share / theme     │
│  Koin default: EntitlementRepository = AlwaysActive (Google Play бесплатно)│
└─────────────────────────────▲───────────────────────────────────────────┘
                              │ (Koin override в flavor)
┌─ app-compose/src/rustore (Android, приватный flavor) ───────────────────┐
│  RuStoreCompatEntitlementRepository → читает DataStore pws-app-preferences│
│      (офлайн-разблокировка, source of truth для статуса)                 │
│  interface PaymentProvider  (универсальные модели PaymentProduct/Purchase)│
│  RuStorePaymentProvider  (обёртка над RuStorePayClient interactors)      │
│  PurchaseSyncService: online refresh → запись в DataStore (когда SDK жив) │
│  Paywall (Compose, развязан от RuStore-типов) + «требуется премиум» диалог│
└──────────────────────────────────────────────────────────────────────────┘
```

**Почему это универсально и офлайн-стойко:**
- Источник истины разблокировки — **DataStore** (`pws-app-preferences`), а не SDK. Нет сети / SDK
  мёртв → статус берётся из локального файла. Функции работают.
- `PaymentProvider` — единственная точка, знающая про конкретный магазин. Смена платёжки =
  новая реализация `PaymentProvider` (+ при необходимости новый flavor), порт `EntitlementRepository`/
  `PremiumGate` и точки гейта в фичах не трогаются.
- `pws-core:features` остаётся публичным и generic — никаких RuStore-типов/секретов в нём.

---

## 5. Фазы и задачи

### Phase A — Gradle / flavor / SDK-обвязка (сборка проходит, поведение не меняется)

- **T-A01** Version catalog: добавить `rustore-sdk-bom = ru.rustore.sdk:bom:2025.08.01`,
  `rustore-sdk-pay = { module = "ru.rustore.sdk:pay" }`.
- **T-A02** Maven-репозиторий RuStore (`https://artifactory-external.vkpartner.ru/artifactory/maven`)
  в `dependencyResolutionManagement` (`settings.gradle.kts`), ограничить `content { includeGroup("ru.rustore.sdk") }`.
- **T-A03** `app-compose/build.gradle.kts`: flavor-scoped зависимости
  `rustoreImplementation(platform(libs.rustore.sdk.bom))` + `rustoreImplementation(libs.rustore.sdk.pay)`
  (только rustore-flavor тянет SDK; ru/uk/full — чистые).
- **T-A04** Source set `app-compose/src/rustore/kotlin/…` + `src/rustore/AndroidManifest.xml`
  (мержится с main): meta-data `sdk_pay_scheme_value`, `console_app_id_value`, BROWSABLE
  intent-filter со scheme `io.github.alelk.pws.app` на launcher-activity.
- **T-A05** `app-compose/src/rustore/res/values/strings.xml`: `CONSOLE_APPLICATION_ID = 2063600773`
  (+ payment/error строки; локализации ru/uk/pl — из форка).
- **T-A06** Проверить, что SDK 2025.08 авто-инициализируется через meta-data (иначе — init в
  Application-хуке rustore-flavor). Сверить с актуальной докой RuStore Pay.

### Phase B — Универсальный порт entitlement (pay-agnostic, pws-core)

- **T-B01** `PremiumStatus` (Active/Inactive/Unknown) + `EntitlementRepository` (StateFlow) в
  `pws-core:features` commonMain.
- **T-B02** `PremiumGate` — инъектируемый сервис: `requirePremium { action }` возвращает результат
  или `null` и эмитит запрос на показ пейволла (`SharedFlow<Unit>` / навигационный колбэк).
- **T-B03** Дефолтный Koin-биндинг `AlwaysActiveEntitlementRepository` + no-op `PremiumGate`
  (Google Play = как сейчас). Механизм override биндинга во flavor (per-flavor Koin-модуль,
  загружается после `featuresModule`).
- **T-B04** Встроить точки гейта в ScreenModels фич (см. §2.3): favorite toggle, song edit, tags edit,
  share, смена темы (набор — как в форке, O1). Для Google Play гейт — прозрачный (Active).
  > ⚠️ Затрагивает публичный `pws-core` (решено O2: порт живёт здесь). Строго generic, без RuStore,
  > комментарии на английском.
- **T-B05** Пейволл как Voyager-экран + «требуется премиум» bottom-sheet/диалог (замена
  `PaymentDialogFragment`); навигация по `paywallRequests`.

### Phase C — Слой совместимости (офлайн-разблокировка) — rustore flavor

- **T-C01** `RuStoreCompatEntitlementRepository` (в `src/rustore`): отдельный DataStore
  `preferencesDataStore("pws-app-preferences")`, чтение `purchase_full_access` /
  `purchase_subscription_until`, маппинг в `PremiumStatus` по формуле §2.2. Работает без сети.
- **T-C02** Koin-модуль rustore-flavor: override `EntitlementRepository` → Compat-реализация,
  `PremiumGate` → реальный (показывает пейволл).
- **T-C03** Тест: заранее записанный `purchase_full_access=true` (или будущая дата подписки) →
  `PremiumStatus.Active` при полном отсутствии сети/SDK.

### Phase D — Абстракция платёжного провайдера + RuStore-реализация (на будущее)

- **T-D01** `interface PaymentProvider` + универсальные модели `PaymentProduct(id,title,desc,priceLabel,type)`,
  `ActivePurchase(productId,invoiceId,expiration?)`, `AuthStatus`, `PurchaseResult` (без RuStore-типов).
- **T-D02** `RuStorePaymentProvider` (в `src/rustore`): обёртка над `RuStorePayClient`
  (`Product/Purchase/User` interactors), `Task.coAwait()` (портировать `rustoreSdkOpts.kt`).
- **T-D03** `PurchaseSyncService`: при старте/открытии пейволла — `provider.purchases()` → вычислить
  full-access / subscription-until → записать в DataStore `pws-app-preferences` (актуализация офлайн-
  статуса, когда монетизация заработает). Портировать `syncDataStoreWithPurchases`.
- **T-D04** Мягкая деградация: ошибки провайдера (сейчас монетизация выключена) ловятся, статус
  остаётся из DataStore. Пейволл показывает «войти в RuStore / восстановить покупки», а не падает.

### Phase E — Пейволл UI (Compose, универсальный)

- **T-E01** Портировать `PaymentScreen` из форка, развязав от `ru.rustore.sdk.pay.model.*` →
  универсальные `PaymentProduct/ActivePurchase`. Список продуктов, активные покупки, «войти в RuStore»,
  предупреждение о нескольких подписках.
- **T-E02** Строки `payment_* / error_*` (en/ru/uk/pl) → `composeResources` фич или в shell.
- **T-E03** Точка входа в настройках («Покупки»/«Подписка») — видима только для rustore-flavor.
- **T-E04** Тема/навигация пейволла в рамках `AppTheme` Compose-приложения.

### Phase F — Верификация совместимости и релиз

- **T-F01** **I1:** собрать `assembleRustoreRelease`, `keytool -printcert` / `apksigner` → сверить
  SHA-256 с `A2:E3:5B:…:0A:A5`. Блокер релиза при несовпадении.
- **T-F02** **I3:** подтвердить versionCode нового билда `> 38`.
- **T-F03** **I5:** прогнать апгрейд `2.3.1-ru`(установлен)→новый rustore: избранное/история/теги/
  пользовательские песни на месте (проверить `runLegacyMigration` на rustore-БД).
- **T-F04** **I4:** сценарий офлайн-разблокировки: на устройстве с `purchase_full_access=true`
  обновиться, выключить сеть → все платные функции доступны.
- **T-F05** `build-compose.sh`: раскомментировать копирование rustore `.aab/.apk`; обновить
  `docs/release-workflow.md`.
- **T-F06** Архивировать репозиторий `pws-android-rustore` (read-only), README со ссылкой на монорепо.
- **T-F07** e2e-flow (Maestro) для rustore-flavor: пейволл появляется у бесплатного, скрыт у премиума.

---

## 6. Риски

| # | Риск | Последствие | Митигация |
|---|------|-------------|-----------|
| R1 | Keystore не совпал с CN=Vera Elkina | Обновление не встанет ни у кого | T-F01 до публикации; хранить правильный keystore в CI-секретах |
| R2 | `runLegacyMigration` не подхватывает rustore-БД | Потеря данных пользователей | T-F03, при необходимости — доработать поиск legacy-БД по имени файла |
| R3 | Pay SDK требует явной init / изменил API в 2025.08 | Падение старта rustore-сборки | T-A06, сверка с докой RuStore Pay (context7/офиц. дока) |
| R4 | Гейт-точки в публичном pws-core «протекают» деталями RuStore | Загрязнение публичного API | Строго generic порт; RuStore только во flavor |
| R5 | Модерация RuStore при выключенной монетизации | Отклонение сборки | Пейволл деградирует мягко (T-D04); согласовать с политикой RuStore |
| R6 | Авто-бэкап Android исключает/сбрасывает `pws-app-preferences` | Потеря статуса при restore на новом устройстве | Проверить `data_extraction_rules.xml`/`backup_rules.xml` (для in-place апдейта не критично) |

---

## 7. Решения и открытые вопросы

**Зафиксировано (2026-07-27):**
- **O1. Набор платных функций — РЕШЕНО: точно как в форке.** Гейтим: добавление в избранное,
  редактирование песни, редактирование тегов/категорий, «поделиться», смена темы оформления.
  Бесплатно: просмотр/поиск/навигация.
- **O2. Расположение generic-порта — РЕШЕНО: `pws-core:features` commonMain.** `EntitlementRepository`
  + `PremiumGate` + гейт-точки в ScreenModels живут в публичном pws-core (строго generic, комментарии
  на английском, никаких RuStore-типов). RuStore-реализация/`PaymentProvider`/пейволл — только во
  flavor `app-compose/src/rustore`.

**Открыто:**
- **O3. UX пейволла** — полноэкранный Voyager + «требуется премиум» диалог (как форк) — оставляем оба?
- **O4. Product IDs** — сохраняем `full_access_v1 / monthly_subscription_v1 / yearly_subscription_v1`
  (обязаны совпадать с RuStore Console)?
- **O5. Google Play flavors** — подтверждаем, что ru/uk/full остаются **полностью бесплатными**
  (`AlwaysActive`), модель донатов без изменений?

---

## 8. Порядок выполнения

`A → B → C → (F-частично: T-F01/F03/F04 как только rustore-flavor собирается и читает статус) → D → E → F`.
Ценность раньше всего: после **A+B+C** rustore-сборка уже совместима и офлайн-разблокировка работает —
даже без Pay SDK. **D+E** добавляют онлайн-покупки к моменту, когда монетизацию починят.
