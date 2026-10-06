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
**01.1 — статус: done** (не закоммичено; изменения в pws-core: `convention-plugins/`, `settings.gradle.kts`, `build.gradle.kts`, `build.gradle.kts` всех модулей).
- Плагины (included build `convention-plugins/`, каталог из `../gradle/libs.versions.toml`): `pws.kmp.base` (jvm, toolchain 21, JUnit 5 + kotest-runner в jvmTest, junitXml, maven-publish), `pws.kmp` (+ios+js browser), `pws.kmp.nojs`, `pws.kmp.noios`, `pws.kmp.android`, `pws.kmp.android.nojs`, `pws.compose`, `pws.kmp.serialization` (надстройка: только плагин serialization, применяется рядом с любым `pws.kmp*`).
- Отклонения: (1) вместо флага `pws { js = false }` — отдельные плагины по наборам целей (цели должны существовать до тела модуля, т.к. модули обращаются к `jsMain`); добавлен `pws.kmp.noios`, т.к. `api:*` = jvm+js без iOS; (2) `pws.kmp.serialization` не включает `pws.kmp`; (3) `domain-test-fixtures` (js = nodejs) и `api:client:di` (+nodejs) объявляют js-цель в модуле; (4) `ksp` и `io.kotest` остаются в модулях, порядок в `plugins {}` изменён: ksp раньше kotest (иначе kotest-плагин падает: «KSP neither found»); (5) `compose.resources` в `features` настраивается через `extensions.configure<ComposeExtension>`; (6) `androidSdkVersion` из корня удалён (в pws-android не используется), repositories google/mavenCentral перенесены в `dependencyResolutionManagement` settings; корень оставляет version, yarn-политику и publishing-репозитории.
- Различие publishToMavenLocal --dry-run: исчезли 3 задачи `:api/:core/:data:publishToMavenLocal SKIPPED` — родительские каталоги без модуля, раньше получали maven-publish из `subprojects{}`. Остальное идентично.
- Проверки до/после (снимки в /tmp/claude-1000/01.1/): `outgoingVariants` 13 модулей — идентичны; `:features:outgoingVariants` падает с ConcurrentModificationException и ДО правок (баг Gradle), поэтому для всех модулей дополнительно сравнены атрибуты/зависимости/capabilities потребляемых конфигураций, набор KotlinTarget, список compile-задач, jvmTarget, freeCompilerArgs, optIn — идентичны.
- Gate: `check assemble -x <4 iOS compile>` в pws-core — BUILD SUCCESSFUL; `pws-android :app-compose:assembleRuDebug` — BUILD SUCCESSFUL.
- Находки вне объёма: пустой `jsTest.dependencies {}` в `domain`; корневой `alias(androidLibrary)` не используется; iOS-компиляция не проверена в песочнице (только конфигурация).

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
**01.2 — статус: done** (не закоммичено; `convention-plugins/` (новый), `settings.gradle.kts`, `build.gradle.kts`, `app-compose/`, `data/db-android/`, `data/content-delivery/` build-скрипты).
- Included build `convention-plugins/` (каталог из `../gradle/libs.versions.toml`; AGP и KGP берутся через `toDep()` из каталога). Плагины: `pws.android.library`, `pws.android.application`; общее в `BuildConventions.kt` (compileSdk 37, minSdk 23, Java 21, `jvmToolchain(21)`, `isIncludeAndroidResources`, build type `localSeed`, `useJUnitPlatform()` + 11 `--add-opens`).
- `VerifyRustoreInvariantsTask`, `RustoreInvariants`, `DownloadSeedBundlesTask`, `StageMappingFileTask` перенесены без изменения логики в пакет `io.github.alelk.pws.build` (импортируются в `app-compose/build.gradle.kts`); регистрация в `onVariants` осталась в app-compose.
- Отклонения: (1) `--add-opens`/`useJUnitPlatform` заданы через `tasks.withType<Test>().configureEach`, а не `testOptions.unitTests.all` (тот же набор задач); (2) `localSeed` создаётся в конвенции, модули делают `getByName("localSeed")`; (3) удалены `sdkVersion` из корня и блок `KotlinCompile.jvmTarget` (заменён toolchain 21); `lint.targetSdk` в db-android = `compileSdk`; targetSdk приложения = compileSdk (оба 37); (4) `alias(libs.plugins.compose)` и kotest-плагин остались в модулях; test-зависимости не переносились (вне решений этапа); (5) db-android получил `jvmToolchain(21)` (jvmTarget уже был 21).
- Проверки (снимки в /tmp/claude-1000/01.2/): списки задач `:app-compose`, `:data:db-android`, `:data:content-delivery` идентичны до/после; `aapt2 dump badging` ruDebug и rustoreDebug идентичны (package/versionCode 48/versionName/SDK). Для badging пришлось использовать /home/agent/tools/aapt2/aapt2 (aapt2 из .sbx-android-sdk x86, не запускается на aarch64).
- Gate (без изменений относительно базы): те же 6 упавших test-задач (UnsatisfiedLinkError, Robolectric): 63 строки FAILED, множества совпадают до/после; assembleRuDebug/RustoreDebug зелёные. [R8]: `verifyRustoreReleaseInvariants` OK; `minify*ReleaseWithR8` проходит, `assemble*Release` падает на `SigningConfig ... missing storeFile` (нет секретов) — одинаково до и после.
- Окружение: при 5 ГБ gradle OOM-убивался; запускал с `-Xmx2500m`, `--max-workers=2`, `-Pkotlin.daemon.jvmargs=-Xmx1200m`.
- Находки вне объёма: уже до правок `:verifyCoreVersionAlignment` не совместим с configuration cache (ссылки на script object), кэш отбрасывается; предупреждения Kotlin о deprecated `extra(...)` делегатах в корневом build.gradle.kts.

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
