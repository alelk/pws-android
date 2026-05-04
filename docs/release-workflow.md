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

### Keystore — rustore (отдельный файл)

| Secret                            | Описание                                                |
|-----------------------------------|---------------------------------------------------------|
| `RELEASE_KEYSTORE_RUSTORE_BASE64` | Отдельный keystore для RuStore, закодированный в base64 |
| `RELEASE_STORE_PASSWORD_RUSTORE`  | Пароль keystore для RuStore                             |
| `RELEASE_KEY_ALIAS_RUSTORE`       | Alias ключа для `rustore` flavor                        |
| `RELEASE_KEY_PASSWORD_RUSTORE`    | Пароль ключа для RuStore                                |

### Как закодировать keystore в base64

```shell
# macOS
base64 -i release.jks | pbcopy   # копирует в буфер обмена

# Linux
base64 release.jks | xclip -selection clipboard
```

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

