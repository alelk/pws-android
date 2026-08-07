# Release Build Workflow

Воркфлоу `.github/workflows/release-build.yml` выполняет ручную сборку подписанных
APK/AAB для выбранных flavors, прогоняет E2E-тесты и создаёт GitHub Release
с прикреплёнными артефактами.

---

## Запуск через GitHub UI

1. Перейдите в репозиторий → **Actions** → **Release Build**.
2. Нажмите **Run workflow**.
3. Заполните параметры:

| Параметр         | Тип                  | Описание                                                                          |
|------------------|----------------------|-----------------------------------------------------------------------------------|
| `tag`            | string, **required** | Тег, для которого делается релиз. Тег должен существовать в Git. Пример: `v2.4.0` |
| `flavor_ru`      | boolean              | Собрать `ru` flavor (Google Play RU)                                              |
| `flavor_uk`      | boolean              | Собрать `uk` flavor (Google Play UK)                                              |
| `flavor_rustore` | boolean              | Собрать `rustore` flavor (RuStore)                                                |
| `release_title`  | string, optional     | Заголовок релиза. Если не указан — равен значению `tag`                           |

4. Нажмите **Run workflow** → зелёную кнопку.

---

## Запуск через gh CLI

```shell
# Только ru flavor
gh workflow run release-build.yml \
  -f tag=v2.4.0 \
  -f flavor_ru=true

# Несколько flavors с кастомным заголовком
gh workflow run release-build.yml \
  -f tag=v2.4.0 \
  -f flavor_ru=true \
  -f flavor_uk=true \
  -f flavor_rustore=true \
  -f release_title="Release 2.4.0 — all stores"

# Проверить статус
gh run list --workflow=release-build.yml
gh run watch  # интерактивный мониторинг последнего запуска
```

---

## Что делает воркфлоу (шаги)

```
Validate inputs
  └── Checkout at <tag>
      └── Setup JDK 21 + Gradle cache
          ├── Decode keystore (ru/uk)          ← только если выбраны ru или uk
          ├── Decode keystore (rustore)         ← только если выбран rustore
          ├── Fetch DB assets                   ← fetch-db.sh per flavor
          ├── Build APK + AAB                   ← Gradle per flavor
          ├── Collect & rename artifacts        ← output/release/pws-app-<tag>-<flavor>.*
          ├── E2E tests — ru                    ← Android emulator API 34 + Maestro
          │   (заглушка для uk/rustore — тесты пока не реализованы)
          └── Create GitHub Release             ← gh release create
```

Артефакты релиза именуются как:
- `pws-app-v2.4.0-ru.apk` / `pws-app-v2.4.0-ru.aab`
- `pws-app-v2.4.0-uk.apk` / `pws-app-v2.4.0-uk.aab`
- `pws-app-v2.4.0-rustore.apk` / `pws-app-v2.4.0-rustore.aab`

---

## Предустановленные сборники (seed-books)

Некоторые flavors поставляются с уже вшитыми сборниками — приложение открывается сразу с контентом,
без онбординга и без сети. Механизм общий и **декларативный**: список задаётся в
`app-compose/build.gradle.kts` (`seedBooksByFlavor: Map<flavor → [bookId]>`). Flavors, которых нет в
карте, собираются как универсальный «чистый» APK — без изменений.

Текущая конфигурация:

| Flavor    | Предустановленные сборники                 |
|-----------|--------------------------------------------|
| `rustore` | `PV3300` (Песнь Возрождения 3300)          |
| `uk`      | `Psalmovivi` (Псалмоспіви)                 |
| `ru`      | — (чистый)                                 |
| `full`    | — (чистый)                                 |

**Как это работает:**

- На сборке per-variant задача `generateSeedBundles<Variant>` качает указанные бандлы из каталога
  (`books-catalog-{release|debug}.json`, те же зеркала, что рантайм), сверяет SHA-256 и кладёт их в
  `assets/seed-books/`. `release`-сборки берут `release`-бандлы (шифрованы release-ключом),
  `debug`/`localSeed` — `debug`-бандлы; это совпадает с рантайм-ключом дешифровки.
- На первом запуске `SeedBooksFromAssetsUseCase` импортирует бандлы, помечая книги
  `source = ASSET` → **неснимаемые** (built-in). Идемпотентность — по имени файла (содержит версию),
  так что новый APK с новой версией бандла до-импортируется автоматически, сохраняя правки
  пользователя.

**Требования к сборке:**

- Предустановленные flavors (`rustore`, `uk`) **требуют сеть на сборке** (в т.ч. на CI) — каталог
  должен быть доступен. При недоступности каталога или отсутствии книги в нём сборка **падает явно**.
- Up-to-date проверяется по списку книг / варианту / URL. Версия каталога на сервере не входит во
  входы задачи → чтобы подтянуть свежие бандлы, запусти `clean` (или измени `seedBooksByFlavor`).
- Изменить набор: правь `seedBooksByFlavor` в `app-compose/build.gradle.kts` (bookId должны
  существовать в `books-catalog-{release}.json`).

---

## E2E тесты

| Flavor    | E2E статус             | Эмулятор                                           |
|-----------|------------------------|----------------------------------------------------|
| `ru`      | ✅ Реализованы          | Android API 34, `google_apis`, `x86_64`, `pixel_6` |
| `uk`      | 🔲 Пока не реализованы | —                                                  |
| `rustore` | 🔲 Пока не реализованы | —                                                  |

E2E для `ru` — **quality gate**: падение тестов блокирует создание релиза.

Для `uk` и `rustore` воркфлоу выводит `notice` и продолжает.

---

## GitHub Secrets

Добавьте следующие секреты в **Settings → Secrets and variables → Actions**:

### Keystore — ru и uk (один файл, два alias)

| Secret                    | Описание                                                                               |
|---------------------------|----------------------------------------------------------------------------------------|
| `RELEASE_KEYSTORE_BASE64` | Keystore-файл (`.jks` / `.keystore`), закодированный в base64: `base64 -i release.jks` |
| `RELEASE_STORE_PASSWORD`  | Пароль keystore                                                                        |
| `RELEASE_KEY_ALIAS_RU`    | Alias ключа для `ru` flavor                                                            |
| `RELEASE_KEY_ALIAS_UK`    | Alias ключа для `uk` flavor                                                            |
| `RELEASE_KEY_PASSWORD`    | Пароль ключа (общий для ru и uk)                                                       |

### Мониторинг

| Secret                | Описание                                                                                    |
|-----------------------|---------------------------------------------------------------------------------------------|
| `APPMETRICA_API_KEY`  | API key приложения в AppMetrica. Если секрет не задан — сборка проходит, но телеметрия выключена (`NoOpTelemetry`). См. [`monitoring.md`](monitoring.md) |

### Keystore — rustore (отдельный файл)

| Secret                            | Описание                                                |
|-----------------------------------|---------------------------------------------------------|
| `RELEASE_KEYSTORE_RUSTORE_BASE64` | Отдельный keystore для RuStore, закодированный в base64 |
| `RELEASE_STORE_PASSWORD_RUSTORE`  | Пароль keystore для RuStore                             |
| `RELEASE_KEY_ALIAS_RUSTORE`       | Alias ключа для `rustore` flavor                        |
| `RELEASE_KEY_PASSWORD_RUSTORE`    | Пароль ключа для RuStore                                |

> ⚠️ **Инвариант совместимости I1 (блокер релиза).** RuStore-сборка обновляется «на месте» поверх
> опубликованной версии 2.3.1. Обновление установится у пользователей **только** если сертификат
> подписи совпадает с уже опубликованным:
> `CN=Vera Elkina`, SHA-256 `A2:E3:5B:7E:BA:1C:34:97:29:90:0D:4E:4A:70:DC:6F:97:4B:90:C6:E7:79:D3:95:0E:E0:73:27:6A:46:0A:A5`.
> Перед публикацией сверить:
> ```shell
> ./gradlew :app-compose:assembleRustoreRelease
> apksigner verify --print-certs app-compose/build/outputs/apk/rustore/release/app-compose-rustore-release.apk \
>   | grep -i 'SHA-256'
> ```
> Несовпадение = обновление не встанет ни у кого. `applicationId` (`io.github.alelk.pws.app`) и
> `versionCode` (> 38) менять нельзя по той же причине.

### Монетизация (rustore flavor)

RuStore-сборка — единственный flavor с платными функциями. Механизм универсальный и офлайн-стойкий:

- Статус премиума хранится в DataStore `pws-app-preferences` (ключи `purchase_full_access`,
  `purchase_subscription_until`) — **тот же файл**, что писал форк 2.3.1, поэтому оплаченный статус
  переживает обновление и читается **без сети и без Pay SDK**
  (`RuStoreCompatEntitlementRepository`).
- Гейт-точки (избранное, редактирование песни/тегов, «поделиться», смена темы) реализованы в
  публичном `pws-core:features` через generic `PremiumGate`/`EntitlementRepository` — без RuStore-типов.
- RuStore Pay SDK (`RuStorePaymentProvider`, пейволл) подключается только во flavor
  `app-compose/src/rustore`. Монетизация на аккаунте сейчас выключена → SDK деградирует мягко,
  офлайн-разблокировка продолжает работать.
- Google Play flavors (ru/uk/full) остаются **полностью бесплатными** (`AlwaysActiveEntitlementRepository`),
  гейты прозрачны.

### Как закодировать keystore в base64

```shell
# macOS
base64 -i release.jks | pbcopy   # копирует в буфер обмена

# Linux
base64 release.jks | xclip -selection clipboard
```

---

## Мониторинг и приватность — чек-лист перед публичным релизом

Обязателен для каждого релиза, где сбор телеметрии включён (есть `APPMETRICA_API_KEY`).
Подробности — [`monitoring.md`](monitoring.md) и [`privacy-policy.md`](privacy-policy.md).

- [ ] `APPMETRICA_API_KEY` задан в GitHub Secrets (иначе сборка тихо уйдёт без телеметрии —
      `NoOpTelemetry`, это не ошибка сборки).
- [ ] `mapping.txt` загружен в AppMetrica для каждого выпускаемого flavor:
      `./gradlew :app-compose:stageAppMetricaMapping<Flavor>Release` → `output/appmetrica-mapping/`
      → AppMetrica → Настройки приложения → Файлы mapping. Без этого release-стектрейсы нечитаемы.
- [ ] Алерты в AppMetrica включены (новая ошибка / рост падений / crash-free < 99%),
      канал доставки проверен.
- [ ] Android Vitals (Play) и отчёты о сбоях RuStore включены.
- [ ] Политика конфиденциальности опубликована и доступна по ссылке из приложения
      (`MainActivity.PRIVACY_POLICY_URL`) и из карточки магазина.
- [ ] **Play Data Safety** заполнено: Crash logs = Yes, App interactions/Diagnostics = Yes,
      «данные не продаются», шифрование в транзите = Yes, удаление по запросу = Yes.
      Формулировки совпадают с `privacy-policy.md` — расхождение приводит к отклонению при ревью.
- [ ] Аналог декларации заполнен в RuStore Console.
- [ ] PII-аудит пройден: новые вызовы `telemetry.event/recordError/log` используют только константы
      из `TelemetryEvent`/`TelemetryAttr`; ни один вызов не передаёт текст песни, правку
      пользователя или поисковый запрос.
- [ ] Тумблер «Отправлять отчёты о сбоях и анонимную статистику» проверен вручную на устройстве
      (выключение реально останавливает отправку).

---

## Добавление E2E для uk / rustore (в будущем)

1. Убедиться, что БД для нужного flavor присутствует в [pws-docs releases](https://github.com/alelk/pws-docs/releases).
2. Добавить шаг в `run-compose.sh` с поддержкой `--flavor` параметра (или создать отдельный конфиг `e2e/config/compose-uk.env`).
3. В воркфлоу заменить заглушку `E2E — uk (not yet implemented)` на полноценный шаг по образцу шага `E2E — ru`.

---

## Troubleshooting

| Симптом                                 | Причина                                     | Решение                                                               |
|-----------------------------------------|---------------------------------------------|-----------------------------------------------------------------------|
| `At least one flavor must be selected`  | Запущен без выбора flavor                   | Включите хотя бы один checkbox                                        |
| `Tag not found` / `--verify-tag` failed | Тег не существует в репозитории             | Создайте тег: `git tag v2.4.0 && git push origin v2.4.0`              |
| `Signing failed: keystore not found`    | Secret не задан или неправильно закодирован | Проверьте base64-кодирование и название секрета                       |
| `APK not found: output/release/...`     | Сборка упала раньше шага Collect            | Смотрите логи шага Build выбранного flavor                            |
| `No adb device ready`                   | Эмулятор не стартовал                       | Проверьте логи шага `E2E — ru`; редко — transient сбой, перезапустите |
| E2E flow failed                         | Регрессия в приложении                      | Разберите артефакты: `gh run download <run-id>`                       |

