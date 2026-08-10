# Мониторинг: crash-репортинг, non-fatal ошибки и продуктовая аналитика

Как в приложении устроена телеметрия, что нужно настроить в консолях и что проверить перед релизом.

Реализация плана [
`ai/plans/2026-08-07_crash-reporting-and-analytics_plan.md`](ai/plans/2026-08-07_crash-reporting-and-analytics_plan.md).

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

Ключ **не коммитится**. Источники, в порядке приоритета (тот же паттерн, что у ключа расшифровки БД
в `:data:db-android`):

1. переменная окружения `APPMETRICA_API_KEY` (CI-секрет);
2. `local.properties` → `appmetrica.apiKey` (локальная разработка);
3. gradle-property `appmetrica.apiKey` (`-P…` или `~/.gradle/gradle.properties`).

```properties
# local.properties
appmetrica.apiKey=<API key из AppMetrica → Настройки → Основное>
```

> ⚠️ **Грабли, на которые уже наступили.** Gradle **не** отдаёт содержимое `local.properties` как
> project properties — `project.findProperty("appmetrica.apiKey")` для него всегда возвращает `null`.
> Файл нужно читать явно через `Properties()`. Симптом: ключ в `local.properties` есть, а
> `BuildConfig.APPMETRICA_API_KEY` пустой и в консоли тишина. Чтобы это не повторилось молча, при
> пустом ключе сборка теперь печатает предупреждение на этапе конфигурации.

Если ключа нет, приложение собирается и работает как раньше: SDK не активируется, биндится
`NoOpTelemetry`.

Проверить, что ключ доехал до сборки:

```shell
./gradlew :app-compose:generateRuDebugBuildConfig
grep APPMETRICA_API_KEY app-compose/build/generated/source/buildConfig/ru/debug/io/github/alelk/pws/android/compose/BuildConfig.java
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

### 5.1. Регистрация приложения в AppMetrica — пошагово

Делается один раз, занимает ~15 минут. Названия пунктов интерфейса приведены дословно.

#### Шаг 1. Аккаунт

Нужен обычный аккаунт Яндекса. Зайти на <https://appmetrica.yandex.ru> → «Войти», принять условия
использования сервиса. Отдельной регистрации/оплаты не требуется — AppMetrica бесплатна без лимита
на число событий.

> Аккаунт станет владельцем данных приложения. Стоит использовать тот же ящик, что и для Play
> Console / RuStore Console, чтобы доступы не разъехались.

#### Шаг 2. Создать приложение

«Добавить приложение» и заполнить:

| Поле                            | Что указать для P&W Songs                                                                  |
|---------------------------------|--------------------------------------------------------------------------------------------|
| Название приложения             | `P&W Songs` — одно приложение на **все** flavors (см. врезку ниже)                         |
| Ссылка на приложение в магазине | ссылка на Google Play `com.alelk.pws.pwapp` (или оставить пустым, если релиза ещё не было) |
| Часовой пояс отчётов            | тот, в котором вы читаете отчёты — менять потом больно, все графики пересчитаются          |

> **Одно приложение на все четыре flavor'а — осознанное решение (O1 плана).** У `ru`/`full`/`uk`/
> `rustore` разные `applicationId`, но API key в AppMetrica ни к какому `applicationId` не привязан:
> это просто ключ записи. Приложение шлёт `flavor` и `bundle_variant` как user properties, поэтому
> срезы по сборкам строятся фильтром в отчётах. Плюс — общая картина crash-free по всей аудитории.
> Если срезы окажутся неудобными, всегда можно завести второе приложение под `rustore` и раздать
> ключи per-flavor через `flavorKoinModules()`; код для этого менять не придётся.

#### Шаг 3. Забрать API key

**Настройки → Основное** → поле **API key** (32 hex-символа). Скопировать.

Положить в два места:

1. Локально — `local.properties` (файл в `.gitignore`, в репозиторий не попадёт):

   ```properties
   appmetrica.apiKey=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
   ```

2. Для CI — GitHub → Settings → Secrets and variables → Actions → New repository secret,
   имя **`APPMETRICA_API_KEY`**. Воркфлоу `release-build.yml` уже пробрасывает его во все три
   сборки.

Проверка, что ключ подхватился:

```shell
./gradlew :app-compose:assembleRuDebug
# BuildConfig.APPMETRICA_API_KEY будет непустым; в logcat при старте НЕ должно быть
# "AppMetrica API key is not configured — telemetry disabled for this build"
```

#### Шаг 4. Включить email-уведомления об ошибках

Это и есть цель «баги приходят ко мне как можно раньше». Сервис проверяет приложение на проблемные
сессии **каждые 10 минут**.

**Настройки → Основное → вкладка «Еще» → «Крэши» → вкладка «Настройка почтовых уведомлений»**.

Включить оба типа:

- **«При появлении новой группы»** — новый (ранее не встречавшийся) крэш или ошибка. Отметить
  и крэши, и ошибки: non-fatal'ы, которые шлёт приложение (сбой установки сборника, сбой импорта,
  сбой платежа), приходят именно как «ошибки».
- **«При превышении доли проблемных сессий»** — пороги в процентах:

  | Порог | Рекомендация | Почему |
    |---|---|---|
  | «Сессий с крэшами, %» | `1` | ниже 99% crash-free — это уже регрессия, которую видно в Play |
  | «Сессий с крэшами одной группы, %» | `0.5` | ловит один конкретный расходящийся баг |
  | «Сессий с ошибками, %» | `5` | non-fatal'ы шумнее, порог выше |
  | «Сессий с ошибками одной группы, %» | `2` | например, массовый сбой скачивания сборника |

Указать email (`alelkdev@gmail.com`), нажать **«Сохранить настройки»** и **подтвердить адрес по
ссылке из письма** — без подтверждения уведомления не пойдут.

> Если писем окажется слишком много — повышать пороги, а не выключать уведомления.

#### Шаг 5. Загрузить mapping для release-сборок

Без этого стектрейсы release-сборок обфусцированы R8 и бесполезны. См. §4: собрать
`stageAppMetricaMapping<Flavor>Release`, затем в консоли приложения найти раздел загрузки
mapping-файлов и загрузить `output/appmetrica-mapping/mapping-*.txt`, указав versionName/versionCode
из имени файла.

> Точный путь к разделу загрузки в документации не зафиксирован и менялся между редизайнами
> консоли — искать по слову «mapping» в настройках приложения. Шаг обязателен для **каждого**
> публичного релиза, поэтому он вынесен в релизный чек-лист.

#### Шаг 6. Проверить, что данные доходят

1. Собрать debug: `./gradlew :app-compose:installRuDebug`.
2. В приложении: **Настройки → Приватность** → включить тумблер «Отправлять отчёты о сбоях и
   анонимную статистику» (в debug он выключен по умолчанию, чтобы не засорять статистику
   dev-запусками).
3. Открыть любую песню, подождать 5 секунд (`viewDelay`) — уйдёт `song_open`; переход между
   экранами даст `screen_view`.
4. Убедиться по logcat, что событие действительно ушло (см. §6.2), и только потом искать его в
   консоли.

> **События появляются не мгновенно.** SDK копит их в буфере и отправляет пачками; сервис принимает
> события задним числом до 7 дней и показывает их по времени возникновения. Первые события обычно
> видны в течение нескольких минут, полноценные отчёты — с задержкой. Не пугаться пустого экрана
> сразу после запуска.

#### Шаг 7. Перед первым публичным релизом

Сбор данных без раскрытия — нарушение правил обоих магазинов и 152-ФЗ. До публикации сборки с
непустым ключом обязательно пройти чек-лист «Мониторинг и приватность» в
[`release-workflow.md`](release-workflow.md): опубликованная политика конфиденциальности,
заполненные Play Data Safety и аналог в RuStore, проверенный тумблер отказа.

### 5.2. Store vitals (бесплатный второй источник, ноль кода)

- **Google Play Console → Качество → Android Vitals** — для `ru`/`uk`/`full`. Включить уведомления
  о превышении «плохих» порогов ANR/крэшей.
- **RuStore Console → раздел отчётов о сбоях** — для `rustore`.

Двойной учёт (AppMetrica + vitals) — норма: это независимые источники, их числа не складываются.

## 6. Проверка

### 6.1. «Работает ли мониторинг прямо сейчас?» — один взгляд в logcat

При старте приложение печатает ровно одну строку о состоянии телеметрии:

```shell
adb logcat -c && adb shell am force-stop com.alelk.pws.pwapp
adb shell monkey -p com.alelk.pws.pwapp -c android.intent.category.LAUNCHER 1
adb logcat -d | grep PwsTelemetry
```

| Строка в логе | Что это значит | Что делать |
|---|---|---|
| `AppMetrica API key is not configured` | ключ не доехал до сборки | см. §3 |
| `AppMetrica activated (… dataSending=false) — nothing will be sent…` | ключ есть, но **согласие выключено** | Настройки → Приватность → включить тумблер |
| `AppMetrica activated (… dataSending=true)` | всё включено | смотреть §6.2 |
| `AppMetrica activation failed` | SDK не поднялся (стектрейс рядом) | приложение работает, телеметрии нет |

> **Debug-сборки по умолчанию НЕ отправляют данные** (`defaultEnabled = !BuildConfig.DEBUG`) —
> это осознанное решение, чтобы dev-запуски не искажали продуктовую статистику. Для проверки
> пайплайна тумблер нужно включить вручную. Именно это, а не поломка, — самая частая причина
> «ничего не вижу в AppMetrica».

### 6.2. Убедиться, что события реально уходят на сервер

В debug-сборках включён подробный лог SDK (`withLogs()`), поэтому виден весь путь события:

```shell
adb logcat -d | grep -i AppMetrica | grep -E "Event saved to db|Event sent"
```

Рабочий вывод выглядит так:

```
Event saved to db: EVENT_TYPE_REGULAR with name screen_view with value {"screen":"HomeScreen"}
Event sent: screen_view with value {"screen":"HomeScreen"}
```

`Event saved to db` без последующего `Event sent` = событие записано в буфер, но не отправлено
(нет сети / отправка отключена).

### 6.3. Остальное

| Что | Как |
|---|---|
| Компиляция | `./gradlew :app-compose:compileRuDebugKotlin` |
| Сборка rustore | `./gradlew :app-compose:assembleRustoreRelease` |
| Юнит-тесты телеметрии | `./gradlew :app-compose:testRuDebugUnitTest`, в pws-core `./gradlew :domain:jvmTest :features:jvmTest` |
| Событие в консоли | после §6.2 — отчёт в AppMetrica; помнить про задержку (события идут пачками, принимаются задним числом до 7 дней) |
| Деобфускация | искусственный краш в release → стектрейс читаем после загрузки маппинга |
| Тумблер работает | выключить → в логе пропадают `Event sent` |

## 7. Чек-лист перед публичным релизом

См. раздел «Мониторинг и приватность» в [`release-workflow.md`](release-workflow.md).

---

Last reviewed: 2026-08-07
