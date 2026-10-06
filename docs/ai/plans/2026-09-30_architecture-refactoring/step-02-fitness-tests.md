# Шаг 02 — Fitness-тесты: правила в сборке

> Перед чтением этапа: [README](README.md) §4, §6, §7.2, §7.3.
> Опорный скилл: `architecture-fitness-tests` (+ его `templates/`).

**Цель.** Каждое правило из README §3 и «hard rules» `AGENTS.md` превращается в тест внутри
`./gradlew build`. Сегодняшние нарушения записываются в списки `KNOWN_*`; дальнейшие шаги плана
(03–09) измеряются тем, как эти списки пустеют.

**Мина шага.** Тест, который не может упасть: неверный рабочий каталог или regex, не совпадающий ни
с чем, — и все проверки «зелёные». Защита: у каждого теста guard непустоты и проверка «красным»
(внести нарушение → увидеть красное → вернуть), рецепт записан в KDoc теста.

**Правила для всех этапов шага**
- Kotest `FunSpec`, source set `jvmTest` (или `test` в Android-модулях), сканирование исходников
  через `java.io.File` относительно каталога модуля.
- Сравнение фактического множества нарушений с `KNOWN_*` — **точным равенством** (тест падает и на
  новое нарушение, и на исправленное, но не вычеркнутое).
- `KNOWN_*` хранит относительные пути файлов (или `файл:правило`), отсортированные.
- KDoc каждого теста: какое правило, зачем оно, как увидеть тест красным.
- Прод-код в этом шаге **не исправляется** — только фиксируется.

---

## Этап 02.1 — Каркас и правила слоёв  · модель: sonnet

### Прочитать
- `architecture-fitness-tests/SKILL.md`, `references/source-scanning.md`, `templates/domain/*`
- `pws-core/AGENTS.md` §5, §8; `pws-core/settings.gradle.kts`

### Решения этапа
- Новый модуль не создаём. Общий хелпер сканирования (`SourceScan.kt`: `ktFiles()`, `violations {}`,
  guard непустоты) кладётся в `:domain:domain-test-fixtures` **только если** он не тянет `java.io` в
  `commonMain`; иначе — копия по ~30 строк в `jvmTest` каждого модуля, где есть fitness-тесты.
- Тесты этапа и их расположение:

| Тест | Модуль | Правило |
|------|--------|---------|
| `DomainPurityTest` | `:domain` jvmTest | в `domain/src/commonMain` нет импортов `java.`, `android.`, `androidx.`, `platform.`, `io.ktor.`, `androidx.room.` |
| `ModuleDependencyTest` | `:domain` jvmTest | по `build.gradle.kts` всех модулей: `:domain` не зависит ни от одного `project(...)`, кроме своих; `:features` не зависит от `:data:*` и `:api:*`; `:data:*` не зависит от `:features`; `:api:contract` не зависит от `:domain` |
| `FeaturesLayerTest` | `:features` jvmTest | в `features/src/commonMain` нет импортов `io.github.alelk.pws.database.`, `…pws.data.`, `…pws.api.`; импорт `…domain.*.repository.*` разрешён только в `di/` |
| `UseCaseShapeTest` | `:domain` jvmTest | файл в пакете `usecase` объявляет ровно один публичный класс/интерфейс с суффиксом `UseCase` |

- Ожидаемый `KNOWN_*` для `FeaturesLayerTest` сегодня: `song/detail/SongDetailScreenModel.kt`,
  `song/detail/SongDetailBySongIdScreenModel.kt`, `song/edit/songEditScreenModelModule.kt`
  (последний исчезнет после 00.1). Фактический список берётся из кода, а не из этого текста.

### Работа
1. Хелпер сканирования + guard непустоты.
2. Четыре теста со списками `KNOWN_*`.
3. Каждый тест проверить «красным».
4. В `AGENTS.md §8` у каждого закреплённого правила дописать имя теста.

### Проверки
```bash
./gradlew :domain:jvmTest :features:jvmTest && ./gradlew build
```

### Готово, когда
- Четыре теста в сборке; для каждого в заметках записан факт проверки «красным».

### Заметки исполнителя
**02.1 (sonnet, 2026-10-06) — done.**
- Сделано (pws-core): в `:domain` jvmTest — `DomainPurityTest`, `ModuleDependencyTest`, `UseCaseShapeTest`; в `:features` jvmTest — `FeaturesLayerTest` (два теста: запрещённые импорты и репозитории вне `di/`). Хелпер `ScannedFile.kt` — копия в каждом модуле (в `domain-test-fixtures` нельзя: `java.io` в commonMain). Guard непустоты в каждом тесте. `AGENTS.md §8` дополнен именами тестов.
- Фактические `KNOWN_*`: Purity, UseCaseShape, ModuleDependency, запрещённые импорты — пусто (нарушений нет, 74 usecase-файла все с одним `*UseCase`). Репозитории вне `di/`: `song/detail/SongDetailBySongIdScreenModel.kt`, `song/detail/SongDetailScreenModel.kt` (`songEditScreenModelModule.kt` уже исчез после 00.1).
- Красным доказано (нарушение внесено, тест красный, откат): `java.util.UUID` в domain; лишний `class ExtraUseCase`; `internal class UpdateSongUseCase` (ноль public); `:features -> :data:repo-room`; `:api:contract -> :domain`; `:data:repo-room -> :features`; `:domain -> :api:contract`; `import ...pws.api.contract.Foo` в features (внесён в блок-комментарий, т.к. иначе не компилируется — regex по импортам видит и закомментированные строки); `import ...domain.song.repository.SongReadRepository` вне `di/`; лишняя «исчезнувшая» запись в KNOWN (ratchet краснеет на исчезнувшем нарушении).
- Отклонение: в `domain/build.gradle.kts` и `features/build.gradle.kts` добавлены `inputs` для `Test`-задач (исходники commonMain; в domain ещё все `build.gradle.kts`). Без этого `jvmTest` оставался UP-TO-DATE/из кэша при правке build-скриптов или импорта, не меняющего байткод, — тест «не мог упасть» (мина шага). Обнаружено именно при проверке «красным».
- Наблюдения: Kotest + `--tests '*X*'` не находит тесты (нужно гонять весь `:module:jvmTest`). Один прогон gate упал из-за гибели Gradle-демона на `jsBrowserTest` (окружение); повтор с `--max-workers=1` зелёный.
- Gate pws-core: `./gradlew build $(cat /tmp/claude-1000/01.3/corex)` — BUILD SUCCESSFUL. pws-android не затрагивался. Вопросов владельцу нет. Не закоммичено.

---

## Этап 02.2 — Правила UI-слоя и i18n  · модель: sonnet

### Прочитать
- `architecture-fitness-tests/templates/features/*`, `references/principles.md`
- `pws-core/AGENTS.md §8` (ScreenModel / Compose), `features/src/commonMain/composeResources/`

### Решения этапа
Тесты в `:features` jvmTest:

| Тест | Правило | Ожидаемый долг сегодня |
|------|---------|------------------------|
| `EffectsChannelTest` | в `*ScreenModel.kt` нет `MutableSharedFlow` | 5 файлов |
| `NoSwallowedErrorsTest` | в `*ScreenModel.kt` нет `catch (…) {}` с пустым телом или телом только из комментария | ≈12 мест |
| `ScreenFileSizeTest` | файлы в `features/src/commonMain` ≤ 600 строк (исключение: `theme/`) | `SongDetailScreen.kt`, `SettingsScreen.kt` |
| `NoServiceLocatorInComposablesTest` | `koinInject(` встречается только в `telemetry/TelemetryCompose.kt` и `premium/` | `SettingsScreen.kt` |
| `NoHardcodedUiStringsTest` | в `*Screen.kt` и `components/` нет `Text("…")` / `contentDescription = "…"` со строковым литералом, содержащим буквы | по факту |
| `ResourceKeysParityTest` | `values/`, `values-pl/`, `values-ru/`, `values-uk/` содержат одинаковый набор ключей строк | по факту |
| `ScreenModelPurityTest` | `*ScreenModel.kt` не импортирует `androidx.compose.ui`, `…foundation`, `…material3`, `org.jetbrains.compose.resources` | по факту |
| `MaterialThemeRatchetTest` | счётчик: число файлов вне `theme/` с `MaterialTheme.` не растёт (список файлов) | 33 файла |

- Regex-проверки намеренно грубые; ложное срабатывание лечится уточнением правила, а не
  `@Suppress` в тесте.

### Работа
1. Восемь тестов, `KNOWN_*` по факту.
2. Проверка «красным» каждого.
3. Обновить `AGENTS.md §8`.

### Проверки
```bash
./gradlew :features:jvmTest && ./gradlew build
```

### Готово, когда
- Тесты в сборке; размеры `KNOWN_*` записаны в заметки (это базовая линия для шагов 03, 09).

### Заметки исполнителя
**02.2 (sonnet, 2026-10-06) — done.**
- Сделано (pws-core, `:features` jvmTest, пакет `architecture/`): `EffectsChannelTest`, `NoSwallowedErrorsTest`, `ScreenFileSizeTest`, `NoServiceLocatorInComposablesTest`, `NoHardcodedUiStringsTest`, `ResourceKeysParityTest`, `ScreenModelPurityTest`, `MaterialThemeRatchetTest`. В `ScannedFile.kt` (хелпер 02.1) добавлены `name`, `lineCount`, `code` (источник без комментариев), `scanFeatures()` (guard >50 файлов), `screenModels()` (guard >5). `AGENTS.md §8` дополнен именами тестов.
- Базовая линия `KNOWN_*` (для шагов 03, 09): MutableSharedFlow — 8 файлов (SongDetail/BySongId/Edit, Search, Favorites, History, Tags, Settings ScreenModel); пустые catch — 3 файла, 8 мест (`SettingsScreenModel`:1, `SongDetailBySongIdScreenModel`:3, `SongDetailScreenModel`:4; формат `файл:число`); файлы >600 строк — 2 (`SettingsScreen.kt`, `SongDetailScreen.kt`); `koinInject` вне разрешённых — 1 (`settings/SettingsScreen.kt`); хардкод-строки в `*Screen.kt`/`components/` — 0; пары `locale:key` без перевода — 65 (values-pl: 64 ключа — book_library_*, donation_prompt_*, settings_about/donation/license/version, song_edit_*, theme_system, tonality_*; values-uk: `song_edit_validation_text_format_invalid`); ScreenModel с UI-импортами — 3 (`SongEdit`, `Tags`, `TagSongs`); файлы с `MaterialTheme.` вне `theme/` — 32.
- Отклонения от оценок плана: MutableSharedFlow 8 (не 5); пустых catch 8 (не ≈12; считаются только пустое/комментарий-только тело); `MaterialTheme.` 32 вне `theme/` (плановые 33 — это счёт вместе с `theme/Spacing.kt`); `NoHardcodedUiStrings` — 0 (остатки `"¶"`, `"123"`, `"$n. $title"` без букв). `koinInject` ловится и в форме `koinInject<T>()` (regex `koinInject\s*[(<]`), иначе SettingsScreen не находился.
- Красным доказано (нарушение внесено, весь `:features:jvmTest` красный именно этим тестом, откат): MutableSharedFlow в `HomeScreenModel`; `catch (e: Exception) { /* ignore */ }` там же; +600 пустых строк в `TagChip.kt`; `koinInject<String>()` в `HomeScreen.kt`; `Text("Hello")` и `val contentDescription = "Back"` в `HomeScreen.kt`; удалён ключ из `values-ru` и добавлен ключ только в `values`; `import androidx.compose.ui.graphics.Color` в `HomeScreenModel`; `MaterialTheme.typography` в `HomeScreenModel`; ratchet на исчезнувшее нарушение (убрана запись `settings/SettingsScreen.kt` из KNOWN koinInject). Первая попытка Purity с полным именем без import осталась зелёной — правило смотрит на импорты, так и задумано.
- Наблюдения: `ktlintFormat` в песочнице не работает (iOS-задачи, «Unknown host target»), ktlint/detekt-замечания правились вручную по выводу gate. Демон Gradle иногда падает при параллельных запусках — гонять строго по одному. Вопросов владельцу нет. Не закоммичено.


---

## Этап 02.3 — Правила шелла и данных в pws-android  · модель: sonnet

### Прочитать
- `pws-android/AGENTS.md §8`, `app-compose/proguard-rules*.pro`
- `app-compose/src/*/kotlin/.../flavor/FlavorIntegration.kt` (все 4)

### Решения этапа
Тесты в `:app-compose` (`src/test`), Kotest:

| Тест | Правило |
|------|---------|
| `ProguardRulesTest` | ни в одном `*.pro` нет `-keep class <…>.** { *; }` и `-keep class ** ` (G13) |
| `FlavorContractTest` | каждый каталог `src/{ru,uk,full,rustore}` содержит `flavor/FlavorIntegration.kt`, объявляющий один и тот же набор top-level имён (`MONETIZATION`, `flavorKoinModules`, `flavorStartupTasks`, `flavorShowPaywall`) |
| `PaymentSdkIsolationTest` | импорт `ru.rustore.` встречается только под `src/rustore/` и `src/testRustore/` |
| `DirectDaoAccessTest` | `Dao()` / импорт `io.github.alelk.pws.database.*.…Dao` вне `:data:db-android` — ratchet-список (сегодня: `BackupManager.kt`, `PwsBackupAgent.kt`, `BookImporterImpl.kt`, `BookUninstallerImpl.kt`, `SeedBooksFromAssetsUseCase.kt`); тест живёт в `:app-compose` и сканирует также `../data/content-delivery/src/main` |
| `ShellStringsTest` | в `app-compose/src/main` нет `Toast.makeText(…, "…"` со строковым литералом — ratchet (сегодня `MainActivity.kt`) |
| `StorageNamesPinnedTest` | закреплённый список строковых имён хранилищ (G3): имя DataStore из `ThemePreferences.kt`, `pws-app-preferences`, `pws_donation`, `pws_catalog_source`, `pws.db` — каждое найдено в исходниках ровно в ожидаемом файле |

### Работа
1. Шесть тестов; проверка «красным» каждого.
2. Обновить `AGENTS.md §8` pws-android ссылками на тесты.

### Проверки
```bash
./gradlew :app-compose:testRuDebugUnitTest :app-compose:testRustoreDebugUnitTest && ./gradlew build
```

### Готово, когда
- Тесты в сборке обоих флейвор-наборов; базовые `KNOWN_*` записаны в заметки.

### Заметки исполнителя
**02.3 (sonnet, 2026-10-06) — done.**
- Сделано (pws-android, `:app-compose` `src/test`, пакет `architecture/`, чистая JVM: без Robolectric и Android-классов): `ProguardRulesTest`, `FlavorContractTest`, `PaymentSdkIsolationTest`, `DirectDaoAccessTest`, `ShellStringsTest`, `StorageNamesPinnedTest` (+ хелпер `ScannedFile.kt`: корень репо = `..`, guard непустоты, сканер учитывает строковые литералы при вырезании комментариев — иначе `"*/*"` в `MainActivity` «съедал» один Toast). `AGENTS.md §8` дополнен ссылками на тесты. Тесты выполняются в обоих наборах (`testRuDebugUnitTest`, `testRustoreDebugUnitTest`).
- Базовые `KNOWN_*`: `-keep … **` — 1 (`proguard-rules.pro: -keep class net.zetetic.database.** { *; }`); прямой DAO вне `:data:db-android` — 6 файлов (`BackupManager`, `PwsBackupAgent`, `BookImporterImpl`, `BookUninstallerImpl`, `SeedBooksFromAssetsUseCase`, `SmartSongBinder`); хардкод-Toast — `MainActivity.kt:7` (формат `файл:число`); остальные три теста — без долга.
- Отклонения от плана: (1) в `proguard-rules.pro` уже есть широкий `-keep class net.zetetic.database.** { *; }` (нативный sqlcipher, осознанно, с комментарием) — записан в `KNOWN`, тест не «ни одного», а ratchet; (2) к DAO-списку добавлен `SmartSongBinder.kt` (план называл 5 файлов); (3) имя DataStore настроек — `app-settings` (в плане «из `ThemePreferences.kt`»), закреплено вместе с `pws-app-preferences`, `pws_donation`, `pws_catalog_source`, `pws.db`; (4) `FlavorContractTest` сравнивает только публичные top-level имена (в rustore есть `internal rustoreKoinModules`).
- Красным доказано (нарушение внесено, тест красный, откат): `-keep class com.example.** { *; }` и `-keep class ** { *; }`; удалён `net.zetetic` keep (исчезнувшее нарушение); `flavorShowPaywall`→`flavorShowPaywall2` в uk; лишний public top-level в uk; `FlavorIntegration.kt` full убран; литерал `"ru.rustore.sdk.Foo"` в `main`; `"db.songDao()"` в `main` и `import …database.song.SongDao` в новом файле; литерал-Toast в `main` и исчезнувший Toast (счёт 7→6); `"pws_donation"`→`"pws_donation2"` и лишний литерал `"pws.db"` в `main`.
- Gate: полный `./gradlew build …` в песочнице НЕ дошёл до конца ни разу (5 попыток; ядро убивает Gradle по OOM на фазе lint/package; часть памяти занимали чужие процессы — простаивавший демон Gradle 9.6.0 от pws-core остановил `--stop`). Вместо него: `:app-compose:testRuDebugUnitTest :app-compose:testRustoreDebugUnitTest :app-compose:ktlintCheck :app-compose:detekt`. База ДО изменений: падают 3 теста `BackupManagerTest` (UnsatisfiedLinkError SQLite) в обоих наборах; ktlint/detekt зелёные. ПОСЛЕ: те же 3 падения, новые тесты зелёные, ktlint/detekt зелёные (ktlint-baseline не менялся). `:data:*` тесты не гонялись. Вопросов владельцу: оставлять ли `net.zetetic` keep в ratchet или закрепить как «разрешённое исключение» (сейчас — ratchet). Не закоммичено.
