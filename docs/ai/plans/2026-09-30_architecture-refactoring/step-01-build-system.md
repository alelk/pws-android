# Шаг 01 — Сборка: convention plugins, статанализ, один gate

> Перед чтением этапа: [README](README.md) §4, §6, §7.2, §7.3.
> Опорный скилл: `kotlin-project-layout` (актуальная версия из `engineering-ai-skills`).

**Цель.** `plugins {}` модуля — одна строка; JDK toolchain, опции компилятора, Kotest, Detekt и
ktlint задаются в одном месте; `./gradlew build` = компиляция всех целей + тесты + статанализ.

**Что не так сегодня.** Каждый модуль повторяет цели KMP, `minSdk = 23`, `useJUnitPlatform()`,
11 строк `--add-opens`; в `app-compose/build.gradle.kts` 250 строк task-классов; статанализа нет;
CI собирает вручную подобранный набор задач.

**Мина шага.** Convention plugin тихо меняет набор целей или опций компилятора модуля, и
опубликованный артефакт pws-core теряет таргет (iOS/JS) или меняет JVM target. Защита: до и после
этапа сравнить вывод `./gradlew :<module>:outgoingVariants` для `:domain` и `:features` и список
публикаций `./gradlew publishToMavenLocal --dry-run`; различия — только ожидаемые.

**Вне объёма.** Смена набора целей, обновление версий библиотек, изменение publish/release-скриптов.

---

## Этап 01.1 — Convention plugins в pws-core  · модель: sonnet

### Прочитать
- `kotlin-project-layout/SKILL.md` и его `templates/`
- все `build.gradle.kts` в pws-core (корень + модули)

### Решения этапа
- Included build `convention-plugins/` (через `pluginManagement { includeBuild(...) }`), каталог версий
  импортируется из `../gradle/libs.versions.toml`.
- Префикс плагинов: `pws`. Набор:
  - `pws.kmp` — Kotlin MPP, цели `jvm + iosArm64 + iosSimulatorArm64 + js(IR, browser)`, toolchain 21,
    Kotest (JUnit 5), `maven-publish`;
  - `pws.kmp.android` — `pws.kmp` + Android KMP library (`minSdk = 23`, `compileSdk` из константы);
  - `pws.kmp.serialization` — надстройка с kotlinx.serialization;
  - `pws.compose` — `pws.kmp.android` + Compose MP + compose compiler.
- Модули без js-цели сегодня (`:data:db-room`, `:data:repo-room`) **остаются без неё**: плагин
  принимает расширение `pws { js = false }` либо используется отдельный `pws.kmp.nojs`. Набор целей
  каждого модуля после этапа идентичен набору до.
- Корневой `build.gradle.kts` после этапа: версия из `app.version`, репозитории публикации — и всё.
- Отключение native-тестов в `db-room` (`fixme`) переносится как есть, с тем же комментарием.

### Работа
1. Снять «до»: для каждого модуля сохранить `outgoingVariants` во временный файл вне репозитория.
2. Создать `convention-plugins/`, перенести общую конфигурацию, заменить блоки в модулях.
3. `TYPESAFE_PROJECT_ACCESSORS` в `settings.gradle.kts`; заменить `project(":domain")` на `projects.domain`.
4. Снять «после», сравнить с «до».

### Проверки
```bash
./gradlew check assemble
./gradlew publishToMavenLocal --dry-run
cd ../pws-android && ./gradlew :app-compose:assembleRuDebug   # composite build жив
```

### Готово, когда
- В модулях нет повторяющейся конфигурации; `outgoingVariants` совпадают с «до»; gate зелёный.

### Заметки исполнителя
<!-- -->

---

## Этап 01.2 — Convention plugins и build-логика pws-android  · модель: sonnet · [R8]

### Прочитать
- `pws-android/app-compose/build.gradle.kts` целиком
- `data/db-android/build.gradle.kts`, `data/content-delivery/build.gradle.kts`
- `AGENTS.md` §3 (flavors), §8 (RuStore, R8)

### Решения этапа
- Included build `convention-plugins/`: `pws.android.library` (общие `compileSdk`, `minSdk`, toolchain,
  `testOptions` с `--add-opens`, build type `localSeed`), `pws.android.application`.
- Task-классы `VerifyRustoreInvariantsTask`, `DownloadSeedBundlesTask`, `StageMappingFileTask` и
  объект `RustoreInvariants` переезжают в `convention-plugins/src/main/kotlin/` **без изменения
  логики, имён задач и значений**. Регистрация задач в `androidComponents.onVariants` остаётся в
  `app-compose/build.gradle.kts` (это конфигурация приложения, а не конвенция).
- Flavors, signing, `applicationId`, `resValue`, `buildConfigField`, `seedBooksByFlavor`,
  каталожные URL — **не меняются** (G4).

### Работа
1. Снять «до»: `./gradlew :app-compose:tasks --all > /tmp/tasks-before.txt`; для `ruDebug` и
   `rustoreDebug` — `aapt2 dump badging` собранных APK (package, versionCode, versionName).
2. Перенести, заменить блоки в модулях.
3. Снять «после»: список задач и badging идентичны (кроме порядка).

### Проверки
```bash
# gate pws-android + [R8]
./gradlew :app-compose:verifyRustoreReleaseInvariants   # если доступна без подписи — иначе отметить
```

### Готово, когда
- `app-compose/build.gradle.kts` содержит только конфигурацию приложения; список задач и badging
  совпадают с «до».

### Заметки исполнителя
<!-- -->

---

## Этап 01.3 — Detekt + ktlint с baseline  · модель: sonnet

### Прочитать
- `kotlin-project-layout/SKILL.md` (раздел про статанализ), `.editorconfig` обоих репо

### Решения этапа
- Detekt и ktlint подключаются convention-плагинами, по main **и** test, для всех source set'ов.
- Один `detekt.yml` в корне каждого репо (можно одинаковый). Baseline — на модуль, фиксирует
  сегодняшний долг; gate блокирует только **новое**.
- **Никакого авто-форматирования существующего кода в этом этапе** (diff должен остаться обозримым).
  `.editorconfig` дополнить так, чтобы соответствовал фактическому стилю (отступ 2 пробела).
- Порог: `maxIssues: 0` сверх baseline.

### Работа
1. Подключить плагины, сгенерировать baseline'ы, закоммитить их рядом с модулями.
2. Включить в `check`.
3. В `AGENTS.md` обоих репо добавить правило: baseline только уменьшается; команда перегенерации.

### Проверки
```bash
./gradlew build          # оба репо
```
Проверка «красным»: добавить в любой файл неиспользуемый импорт → сборка красная → вернуть.

### Готово, когда
- `./gradlew build` включает detekt + ktlint и зелёный; новый нарушающий код ломает сборку.

### Заметки исполнителя
<!-- -->

---

## Этап 01.4 — CI запускает ровно gate  · модель: haiku

### Прочитать
- `.github/workflows/ci.yml` (pws-core), `.github/workflows/android.yml` (pws-android)

### Решения этапа
- Шаги semantic-release, публикации, release-build workflow — не трогать.
- Шаг тестов в обоих CI заменяется на `./gradlew build`. В pws-android после него остаётся сборка
  pre-bundle задач как есть.
- Проверка «Room schema is committed» в pws-core остаётся.

### Работа
1. Заменить подобранные вручную наборы задач на `./gradlew build`.
2. Обновить «Hot paths» в `AGENTS.md` и §7.3 в README этого плана: gate = `./gradlew build`.

### Проверки
- `./gradlew build` локально зелёный в обоих репо; YAML валиден (`python3 -c "import yaml,sys; yaml.safe_load(open(sys.argv[1]))" <file>`).

### Готово, когда
- CI и локальный gate — одна команда; README §7.3 обновлён.

### Заметки исполнителя
<!-- -->
