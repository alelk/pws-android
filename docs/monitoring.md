# Мониторинг: crash-репортинг, non-fatal ошибки и продуктовая аналитика

Как в приложении устроена телеметрия, что нужно настроить в консолях и что проверить перед релизом.

Реализация плана [`ai/plans/2026-08-07_crash-reporting-and-analytics_plan.md`](ai/plans/2026-08-07_crash-reporting-and-analytics_plan.md).

---

## 1. Архитектура в двух словах

```text
pws-core :domain
  io.github.alelk.pws.domain.telemetry
    Telemetry           — порт: recordError / log / event / setUserProperty
    NoOpTelemetry       — дефолт: тесты, jvm/js, сборки без API-ключа
    TelemetryEvent      — весь словарь имён событий
    TelemetryAttr       — весь словарь ключей атрибутов (allow-list)
    TelemetryPrivacy    — санитайзер: allow-list ключей + обрезка значений

pws-core :features
    featuresModule { single<Telemetry> { NoOpTelemetry } }   — дефолтный биндинг
    TrackScreenViews(navigator)                              — авто screen_view
    TelemetrySettings / LocalTelemetrySettings               — тумблер согласия в UI

pws-android :app-compose
    AppMetricaTelemetry           — реализация поверх Yandex AppMetrica
    TelemetryConsentStore         — согласие (SharedPreferences, синхронное чтение на старте)
    PwsComposeApplication.onCreate — активация SDK **первым делом**, затем Koin
```

Ключевой принцип: **бизнес-код зависит только от `Telemetry`**. Замена провайдера (Firebase, Sentry)
не затрагивает ни одного вызова.

> **Почему `Telemetry` в `:domain`, а не в `:features`** (отступление от T-A01/O4 плана):
> non-fatal нужно репортить из `data/content-delivery` — модуля данных, который не должен зависеть
> от Compose-UI-модуля `:features`. `:domain` уже является зависимостью и `features`, и
> `content-delivery`, поэтому порт живёт там.

## 2. Что собирается

Полный словарь — в `TelemetryEvent` и `TelemetryAttr` (pws-core). Пользовательский контент не
собирается **никогда**: `TelemetryPrivacy` отбрасывает любой ключ вне allow-list и обрезает значения
до 64 символов, так что строка песни или поисковый запрос физически не проходит.

События: `screen_view`, `song_open`, `search` (только длина запроса + число результатов),
`book_install`, `book_update`, `book_uninstall`, `book_import`, `onboarding_complete`,
`paywall_shown`, `purchase`, `donation_prompt`.

User properties: `flavor`, `bundle_variant`, `device_language`, `installed_books`.

Формулировки для пользователя — [`privacy-policy.md`](privacy-policy.md).

## 3. API-ключ

Ключ **не коммитится**. Источники, в порядке приоритета:

1. Gradle-property `appmetrica.apiKey` (положить в `local.properties` или `~/.gradle/gradle.properties`);
2. переменная окружения `APPMETRICA_API_KEY` (CI-секрет).

Если ключа нет, приложение собирается и работает как раньше: SDK не активируется,
биндится `NoOpTelemetry`. Это дефолт для форков и локальных сборок.

```properties
# local.properties
appmetrica.apiKey=<32-символьный API key из AppMetrica → Настройки → Приложение>
```

## 4. Деобфускация release-сборок (mapping.txt)

Release минифицируется R8 — без загруженного маппинга стектрейсы нечитаемы.

Официальный AppMetrica Gradle Plugin **не используется**: его актуальная версия (1.0.1) построена на
удалённом `com.android.build.gradle.api.ApplicationVariant` и несовместима с AGP 9.x проекта. Вместо
него для каждой release-сборки регистрируется задача, которая кладёт маппинг в стабильное место:

```shell
./gradlew :app-compose:stageAppMetricaMappingRuRelease
# → output/appmetrica-mapping/mapping-ruRelease-<versionName>-<versionCode>.txt
```

Файл загружается в AppMetrica: **Настройки приложения → Файлы mapping → Загрузить**, с указанием
versionName/versionCode из имени файла. Шаг обязателен для каждого публичного релиза.

## 5. Настройка консолей (сделать один раз)

### 5.1. AppMetrica

1. Создать приложение в <https://appmetrica.yandex.ru> (**один** ключ на все flavors — срез по
   `flavor` идёт через user property; при неудобстве срезов разнести позже).
2. Скопировать API key → `local.properties` / CI-секрет `APPMETRICA_API_KEY`.
3. **Алерты (это и есть «баги приходят ко мне как можно раньше»):**
   Отчёты → «Ошибки» → настроить уведомления на:
   - новая (ранее не встречавшаяся) ошибка;
   - резкий рост числа падений;
   - падение доли crash-free сессий ниже порога (рекомендую 99%).
     Канал: email `alelkdev@gmail.com` + push в мобильном приложении AppMetrica.
4. Проверить, что версия приложения приходит как `versionName` (см. `withAppVersion`) — это
   измерение, по которому видно регрессии между релизами.

### 5.2. Store vitals (бесплатный второй источник, ноль кода)

- **Google Play Console → Качество → Android Vitals** — для `ru`/`uk`/`full`. Включить уведомления
  о превышении «плохих» порогов ANR/крэшей.
- **RuStore Console → раздел отчётов о сбоях** — для `rustore`.

Двойной учёт (AppMetrica + vitals) — норма: это независимые источники, их числа не складываются.

## 6. Проверка

| Что | Как |
|---|---|
| Компиляция | `./gradlew :app-compose:compileRuDebugKotlin` |
| Сборка rustore | `./gradlew :app-compose:assembleRustoreRelease` |
| Юнит-тесты телеметрии | `./gradlew :app-compose:testRuDebugUnitTest` и (в pws-core) `./gradlew :domain:jvmTest` |
| Событие долетает | debug-сборка → включить тумблер в Настройках → открыть песню → событие `song_open` в AppMetrica (задержка до нескольких минут) |
| Деобфускация | искусственный краш в release → стектрейс читаем после загрузки маппинга |
| Тумблер работает | выключить → события перестают приходить (`AppMetrica.setDataSendingEnabled(false)`) |

## 7. Чек-лист перед публичным релизом

См. раздел «Мониторинг и приватность» в [`release-workflow.md`](release-workflow.md).

---

Last reviewed: 2026-08-07
