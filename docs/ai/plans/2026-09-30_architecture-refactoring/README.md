---
status: in progress     # шаг 00 done (2026-10-01); следующий — 01.1; шаг 08 отложен (F3)
owner: Alex
updated: 2026-10-01
scope: pws-core + pws-android (pws-server — вне объёма, см. §9)
---

# План архитектурного рефакторинга PWS (pws-core + pws-android)

> **Точка входа для агента-оркестратора.** Этот файл — единственное, что оркестратор держит в
> контексте постоянно. Каждый этап лежит в отдельном файле шага (`step-NN-*.md`) и написан так,
> чтобы исполнитель-сабагент стартовал «с холодного контекста»: читает §6–§7 этого README и
> **только свой этап**.
>
> Формат следует скиллу `doc-first-agentic-workflow` (шаг → этапы → протокол исполнителя) из
> `engineering-ai-skills`. Отступление: этапы одного шага лежат в одном файле, а не по файлу на этап.

---

## 1. Цель

Привести два прод-репозитория к состоянию, в котором:

1. правила архитектуры **проверяются сборкой**, а не только написаны в `AGENTS.md`;
2. слой представления тонкий и тестируемый (никакой логики в `MainActivity` и в 1600-строчных файлах);
3. данные пользователя (БД, бэкап, настройки, Pro-статус) трогает **один** слой через порты домена;
4. сборка единообразна (convention plugins, статанализ, один gate), версии не расходятся;
5. экраны говорят на собственном словаре токенов дизайн-системы, а не на `MaterialTheme` напрямую.

Переход на Navigation 3 + `ViewModel` (как в актуальных скиллах владельца) **отложен** решением F3:
навигация и state holder остаются на Voyager + `StateScreenModel`.

Приложение опубликовано (Google Play: `ru`/`uk`/`full`; RuStore: `rustore`). **Каждый этап обязан
оставлять приложение выпускаемым** и не менять поведение, если в этапе явно не сказано обратное.

## 2. Что не так сегодня (факты на 2026-09-30)

Размер: pws-core ≈ 39 тыс. строк Kotlin (features 14.8 тыс., domain 5.7 тыс.), pws-android ≈ 15 тыс.
(из них 5 тыс. — мёртвый legacy `:app`).

| №  | Риск | Находка | Доказательство |
|----|------|---------|----------------|
| P1 | **высокий** | Стартовая логика приложения живёт в композиции `MainActivity` (406 строк): гейт миграции → сидирование → онбординг → согласие на телеметрию, на `remember { mutableStateOf }`. `onboardingSkipped` теряется при повороте/пересоздании; логика не покрыта тестами | `app-compose/.../MainActivity.kt:106–170` |
| P2 | **высокий** | Бэкап/восстановление и импорт сборников ходят в Room DAO напрямую из Android-модулей, минуя репозитории домена. Два независимых писателя в пользовательские данные | `BackupManager.kt`, `PwsBackupAgent.kt`, `content-delivery/install/BookImporterImpl.kt`, `BookUninstallerImpl.kt`, `SeedBooksFromAssetsUseCase.kt` |
| P3 | **высокий** | Собственные «hard rules» из `pws-core/AGENTS.md §8` нарушены и ничем не проверяются: 2 ScreenModel импортируют репозитории; 5 моделей используют `MutableSharedFlow` для эффектов; 28 `catch`-блоков, из них ≥12 пустых `catch (_: Exception) {}` | `grep` по `features/src/commonMain` (см. step-02) |
| P4 | **высокий** | Расхождение версий в composite build: Kotlin 2.3.21 (core) vs 2.4.10 (android); CMP 1.11.1 vs 1.10.3; Koin 4.2.2 vs 4.2.0(+4.2.2 inline); Ktor 3.5.0 vs 3.5.2; kaml 0.104 vs 0.97. Комментарий-блокер про Hilt устарел | оба `libs.versions.toml` |
| P5 | средний | `SongDetailScreenModel` (355) и `SongDetailBySongIdScreenModel` (280) — почти копии с одинаковыми 18 зависимостями; во второй `@Suppress("UNUSED_PARAMETER") songObserveRepository` | `features/song/detail/` |
| P6 | средний | Файлы-монолиты: `SongDetailScreen.kt` 1628 строк (25 composable), `SettingsScreen.kt` 794, `TagsScreen.kt` 555 | `wc -l` |
| P7 | средний | Настройки отображения читаются из DataStore в Activity и протаскиваются через `AppRoot` (15 параметров) и 9 nullable `CompositionLocal`. Composable-ы тянут зависимости через `koinInject()` (Settings, UpsellHost) | `AppRoot.kt`, `SettingsScreen.kt:132–134` |
| P8 | средний | Навигация на Voyager версии `2.2.21-1.10.3`, привязанной к CMP 1.10.3, при том что core собирается на CMP 1.11.1. Актуальные скиллы владельца уже на Navigation 3 + `ViewModel` | каталоги версий; `engineering-ai-skills/.../compose-navigation3` |
| P9 | средний | Нет статанализа (Detekt/ktlint), нет convention plugins; build-логика на 250 строк (3 кастомных task-класса) лежит прямо в `app-compose/build.gradle.kts`; `jvmArgs --add-opens` продублированы по модулям | `build.gradle.kts` модулей |
| P10 | средний | Мёртвый код: в pws-core — `backup/` (не в settings, дубль `portable-data`), `commonMain/`, корневой `src/`, пустой `:core:ui`, 16 неиспользуемых per-feature Koin-модулей (`*ScreenModule.kt`, `*ScreenModelModule.kt`), `SongNumberSetSerializer` из трёх `TODO()`. В pws-android — `:app` (238 файлов, закомментирован в settings; `build.sh` всё ещё зовёт `:app:check`), Hilt/navigation/appcompat в каталоге | `settings.gradle.kts`, `grep` |
| P11 | средний | `:data:repo-room`: `RoomTransactionRunner`, Koin-модуль и поиск есть только в `androidMain`; на 18 реализаций репозиториев 2 тестовых файла. Native-тесты `db-room` отключены (`fixme`) | `data/repo-room/src` |
| P12 | низкий | Тексты тостов захардкожены по-английски в шелле («Backup saved», «Import failed: …»); `startActivity(ACTION_VIEW/SENDTO)` без обработки `ActivityNotFoundException` | `MainActivity.kt` |
| P13 | низкий | 575 обращений к `MaterialTheme.*` в 33 файлах экранов и 181 dp-литерал вне `theme/` — нет слоя дизайн-системы | `grep` |
| P14 | низкий | Несогласованность домена: use case — где класс, где интерфейс с `Impl` в Android-модуле; `AGENTS.md` обещает «typed sealed results», фактически 85 файлов на `Either`; generic `UseCase<TagId>` (12 классов) | `domain/src/commonMain` |
| P15 | низкий | Копии скиллов в `.claude/skills` и `.junie/skills` обоих репозиториев отстали от `engineering-ai-skills` (нет `architecture-fitness-tests`, `kotlin-testing-strategy`, `compose-navigation3`, `compose-design-system`, `doc-first-agentic-workflow`) | `diff -rq` |

Что уже хорошо и **не трогаем**: доменная модель (value-class id, команды, `OptionalField`,
`TransactionRunner`), `:api:*`, `portable-data` с golden-тестами совместимости, шифрование БД,
`LegacyMigrationGate`, flavor-интеграция через `FlavorIntegration.kt`, semantic-release.

## 3. Целевая архитектура

```text
pws-android :app-compose (тонкий шелл)
  Application: Koin (di/*.kt), телеметрия
  MainActivity: setContent { AppRoot(platform = AndroidPlatformActions(...)) }   ≤ ~80 строк
  platform/: реализации портов (share, файлы бэкапа, URL, paywall), DataStore-адаптер настроек
      │
pws-core :features
  app/        AppRoot, маршруты (NavKey), AppStartupModel (гейт старта — чистая машина состояний)
  designsystem/ (лист)  ←  components/  ←  <экран>/  (Entry + Content + ViewModel + UiState)
  platform/   интерфейсы, которые реализует шелл
      │  только use case'ы
pws-core :domain          модели, команды, порты (repository/, preferences/, backup/), use case'ы
      │
pws-core :data:repo-room (commonMain)   ← единственный писатель в Room
pws-core :data:db-room                  схема v15 (в этом плане НЕ меняется)
pws-android :data:db-android            провайдер БД, SQLCipher, legacy-миграции
pws-android :data:content-delivery      сеть/файлы/расшифровка; запись в БД — через порты домена
```

Правила (каждое закрепляется fitness-тестом в шаге 02):
- UI → только use case'ы; `features` не импортирует `*.repository.*`, `database.*`, `data.*`.
- Одноразовые сигналы — `Channel<Effect>(BUFFERED)`; ошибки не глотаются.
- Экран = `XxxEntry` (получает ViewModel, собирает state/effects) + `XxxContent` (чистая функция от state).
- Файл экрана ≤ 600 строк.
- Строки UI — только из ресурсов; 4 локали (`en`, `pl`, `ru`, `uk`) с одинаковым набором ключей.
- В пользовательские данные пишет только `:data:repo-room`.

## 4. Решения (исполнители их не пересматривают)

| №   | Вопрос | Решение |
|-----|--------|---------|
| G1  | Менять ли схему Room? | **Нет.** Версия 15 остаётся. Любая потребность в миграции — стоп и вопрос владельцу |
| G2  | Формат бэкапа и bundle | Не меняется. `BackupCompatibilityTest`, `BundleGoldenFileTest`, `CatalogCompatibilityTest` должны оставаться зелёными без правки ожиданий |
| G3  | Имена хранилищ | Не переименовывать и не пересоздавать: БД `pws.db`; DataStore настроек (имя и ключи из `ThemePreferences.kt`); `pws-app-preferences` (Pro-статус RuStore); SharedPreferences `pws_donation`, `pws_catalog_source` |
| G4  | `applicationId`, `db_authority`, `versionCode`, подписи | Не трогать. `verifyRustoreReleaseInvariants` остаётся |
| G5  | Контракт API | `:api:contract` / `:api:mapping` не меняются (согласование с pws-server вне объёма) |
| G6  | DI | Остаётся Koin. Hilt из каталога удаляется вместе с `:app` |
| G7  | Модель ошибок домена | `Either<DomainError, T>` (Arrow) — фактический стандарт; `AGENTS.md` приводится в соответствие |
| G8  | Форма UI-state | Остаётся `sealed Loading | Content | Error` на экран (уже принято рефактором 2026-06) |
| G9  | Тесты | Kotest; фейки, не моки, в `commonTest`; «баг → сначала красный тест» |
| G10 | Ratchet | Списки `KNOWN_*` в fitness-тестах только сокращаются |
| G11 | Ветки и релизы | Работа в ветках от `next` в обоих репо; шаг вливается целиком; перед вливанием шагов 04, 05, 06, 08 — rc-сборка и ручной прогон (см. §8) |
| G12 | Телеметрия | Имена событий и атрибутов (`TelemetryEvent`, `TelemetryAttr`) не меняются |
| G13 | R8 | Никаких `-keep class <пакет>.** { *; }`. Новые keep-правила — точечные, с комментарием |

## 5. Развилки для владельца (все закрыты 2026-09-30, кроме необязательной F4a)

| №  | Вопрос | Рекомендация | Блокирует |
|----|--------|--------------|-----------|
| F1 | ~~Удалить legacy-модуль `:app` из pws-android?~~ | **Закрыта 2026-09-30: да, удалять.** Остаётся только Compose-интерфейс; перед удалением — экспресс-сверка платёжного кода (этап 00.2) | — |
| F2 | ~~Каталог версий~~ | **Закрыта: два каталога + тест на совпадение общих ключей** (этап 00.3) | — |
| F3 | ~~Мигрировать на Navigation 3 + `ViewModel` сейчас?~~ | **Закрыта: пока пропускаем.** Шаг 08 в статусе `deferred`; Voyager и `StateScreenModel` остаются. Вернуться после шага 09 или когда Navigation 3 выйдет из alpha | — |
| F4 | ~~Дизайн-система — делать?~~ | **Закрыта: делаем** (шаг 09), без смены внешнего вида: токены получают сегодняшние значения. Редизайн — отдельная задача владельца (F4a) | — |
| F5 | ~~iOS — цель в ближайший год?~~ | **Закрыта: нет.** Этап 06.1 — в урезанном виде: тестируемость репозиториев на JVM, без переноса ради iOS | — |
| F6 | ~~Generic id у тегов — убрать?~~ | **Закрыта: оставляем.** Этап 07.3 — `skipped` | — |
| F4a | Новый внешний вид (палитра, типографика, формы): владелец делает отдельно в инструменте дизайна или поручает агенту? | Не блокирует шаг 09. После него редизайн сводится к замене значений токенов в `designsystem/` — отдельный план | ничего |

## 6. Инварианты (читает каждый исполнитель)

Нарушение любого пункта — дефект, даже если сборка зелёная.

1. Решения G1–G13 из §4.
2. **Порядок старта:** всё, что устанавливает сборники или восстанавливает данные, ждёт
   `LegacyMigrationGate`. Гейт открывается в `finally`.
3. **Pro-статус RuStore:** единственный владелец `pws-app-preferences` — `LegacyRuStoreEntitlementStore`;
   запись монотонна.
4. **Порядок Koin-модулей** в `PwsComposeApplication` значим (поздний переопределяет ранний):
   `featuresModule` → `monetizationModule` → `telemetryModule` → flavor-модули.
5. `DATABASE_PREV_NAMES` и legacy-провайдеры `support/PwsDb1x/2xDataProvider` не трогать.
6. Не ослаблять, не пропускать и не удалять тесты ради зелёной сборки.
7. Не коммитить `local.properties`, ключи, keystore.
8. Правила из `AGENTS.md` обоих репозиториев действуют, если этап явно не отменяет конкретное.

## 7. Протоколы

### 7.1 Оркестратор

1. Держи в контексте только этот README. Файл шага открывай, только чтобы взять заголовок этапа.
2. На каждый этап — **один новый сабагент** с моделью из таблицы §8. Промпт — шаблон ниже, без
   пересказа плана.
3. Этапы шага — строго по порядку. Параллельно можно запускать только пары, помеченные в §8 знаком ∥.
4. После отчёта сабагента: сам запусти gate-команды (§7.3) и `git status --short` / `git diff --stat`
   в обоих репо. Диффы целиком не читай; читай только файлы, названные в «вопросах владельцу».
5. Зелёно → поставь статус `done` в таблице §8 и иди дальше. Красно → верни тому же сабагенту лог
   ошибки (один раз); повторно красно → эскалируй модель на ступень выше (haiku → sonnet → opus).
6. Этап вернул `waiting` → не решай за владельца. Запиши вопрос в §10, продолжай этапами, которые
   от него не зависят.
7. Коммит — только если владелец просил; Conventional Commits; по одному коммиту на этап.
8. Перед шагами, помеченными «ручной прогон», остановись и попроси владельца выполнить проверку.

Шаблон промпта сабагенту:

```text
Ты исполнитель одного этапа рефакторинга PWS. Репозитории лежат рядом: pws-core и pws-android.
1. Прочитай pws-android/docs/ai/plans/2026-09-30_architecture-refactoring/README.md — только §4, §6, §7.2, §7.3.
2. Прочитай в файле <step-NN-*.md> раздел «Этап NN.k» и «Заметки исполнителей» предыдущих этапов этого шага.
3. Выполни этап NN.k. Ничего сверх него.
4. В конце допиши свой блок в «Заметки исполнителей» этапа и верни отчёт ≤ 15 строк:
   статус (done | waiting), что сделано, отклонения, вопросы владельцу, результат gate-команд.
```

### 7.2 Исполнитель

1. Один этап — одна сессия. Читай только перечисленное в «Прочитать».
2. Не принимай решений сверх плана. Неясно / план противоречит коду / нужно решение, которого нет —
   остановись, запиши вопрос в «Заметки исполнителя», верни `waiting`.
3. Не чини попутно. Находки вне объёма — в заметки.
4. Поведение приложения не меняется, если в этапе не сказано иное.
5. Этап закончен, когда зелёные его «Проверки» и gate (§7.3).
6. Не коммить и не пушь без явного указания.
7. Не читай `build/`, `.gradle/`, `.kotlin/`, `output/`, `local-repo/`, `kotlin-js-store/`, `.idea/`.

### 7.3 Gate-команды

```bash
# pws-core (из каталога pws-core)
./gradlew build

# pws-android (из каталога pws-android; ../pws-core подключается composite build)
./gradlew build -x assembleRuRelease -x assembleRustoreRelease -x assembleUkRelease -x assembleFullRelease

# для этапов с пометкой [R8] дополнительно (если настроена подпись; иначе — отметить в заметках):
./gradlew :app-compose:assembleRuRelease :app-compose:assembleRustoreRelease
```

После шага 01 gate = `./gradlew build` (или с исключением release-сборок в pws-android для CI без подписи).

## 8. Шаги, этапы, модели

Модели — тиры Claude для сабагента: **haiku** — механика по точному списку; **sonnet** — основная
рабочая модель, хорошо специфицированный рефакторинг; **opus** — проектирование, перенос логики с
риском для данных пользователя, миграции API. Оркестратор — самая сильная доступная модель.

| Этап | Название | Репо | Модель | Статус |
|------|----------|------|--------|--------|
| **00** | **[Гигиена и страховочная сетка](step-00-hygiene-and-safety-net.md)** | | | |
| 00.1 | Удалить мёртвый код в pws-core | core | haiku | done |
| 00.2 | Удалить legacy `:app` и его зависимости ∥ 00.1 | android | haiku | done |
| 00.3 | Выровнять версии, перенести каталоги в `gradle/` | оба | sonnet | done |
| 00.4 | Характеризационные тесты ScreenModel'ей без покрытия | core | sonnet | done |
| 00.5 | Обновить копии скиллов и `AGENTS.md`/`CLAUDE.md` ∥ 00.4 | оба | haiku | done |
| **01** | **[Сборка: convention plugins, статанализ, один gate](step-01-build-system.md)** | | | |
| 01.1 | Convention plugins в pws-core | core | sonnet | done |
| 01.2 | Convention plugins + вынос build-логики в pws-android [R8] | android | sonnet | done |
| 01.3 | Detekt + ktlint с baseline в обоих репо | оба | sonnet | done |
| 01.4 | CI запускает ровно `./gradlew build` | оба | haiku | done |
| **02** | **[Fitness-тесты: правила в сборке](step-02-fitness-tests.md)** | | | |
| 02.1 | Каркас + правила слоёв и зависимостей модулей | core | sonnet | done |
| 02.2 | Правила UI-слоя и i18n-паритет | core | sonnet | done |
| 02.3 | Правила шелла и данных в pws-android ∥ 02.2 | android | sonnet | done |
| **03** | **[Слой представления: долги по ratchet](step-03-presentation-hygiene.md)** | | | |
| 03.1 | Объединить две модели SongDetail, убрать репозитории из UI | core | opus | done |
| 03.2 | Эффекты через `Channel`, ошибки не глотаются | core | sonnet | done |
| 03.3 | Разрезать `SongDetailScreen.kt` | core | sonnet | done |
| 03.4 | Разрезать `SettingsScreen.kt`, `TagsScreen.kt`; убрать `koinInject` из composable | core | sonnet | done |
| **04** | **[Настройки как порт домена](step-04-preferences-port.md)** — ручной прогон | | | |
| 04.1 | Порт `UserPreferencesRepository` + Android-адаптер над существующим DataStore | оба | opus | done |
| 04.2 | Экраны читают настройки через use case; сузить `AppRoot` | оба | sonnet | done |
| **05** | **[Старт приложения и тонкий шелл](step-05-startup-and-shell.md)** — ручной прогон | | | |
| 05.1 | Машина состояний старта: чистая функция + тесты | оба | opus | done |
| 05.2 | `AppStartupModel`, переключение `MainActivity` [R8] | оба | opus | done |
| 05.3 | Платформенные действия, локализация сообщений, DI по файлам | android | sonnet | done |
| **06** | **[Граница данных: один писатель](step-06-data-boundary.md)** — ручной прогон | | | |
| 06.1 | `repo-room`: тестируемость на JVM, тесты репозиториев | core | opus | done |
| 06.2 | Бэкап: use case'ы экспорта/восстановления в домене | оба | opus | done |
| 06.3 | Импорт/удаление сборников через порты | оба | opus | done |
| **07** | **[Согласованность домена](step-07-domain-consistency.md)** | | | |
| 07.1 | Единая форма use case; `AGENTS.md` ↔ `Either` | core | sonnet | done |
| 07.2 | Убрать pass-through и чтение без use case | core | sonnet | done |
| 07.3 | (F6) generic id у тегов | core | — | skipped |
| **08** | **[Navigation 3 + ViewModel](step-08-navigation3-viewmodel.md)** — отложен (F3) | | | |
| 08.1 | Spike + ADR: nav3 на android/jvm/js/ios в этом проекте | core | opus | deferred |
| 08.2 | Маршруты, `NavDisplay`, вкладки с отдельными стеками | core | opus | deferred |
| 08.3 | Перевод экранов: ScreenModel → ViewModel (по семействам) | core | sonnet | deferred |
| 08.4 | Удалить Voyager и `:core:navigation` [R8] | оба | sonnet | deferred |
| **09** | **[Дизайн-система](step-09-design-system.md)** | | | |
| 09.1 | `designsystem/`: токены `AppTheme`, обёртки, ratchet-тест | core | sonnet | done |
| 09.2…n | Перевод экранов по одному семейству на этап | core | sonnet | done (a–g) |
| 09.2a | Перевод экранов: Books, BookSongs, BookLibrary | core | sonnet | done |
| 09.2b | Перевод экранов: Favorites, History | core | sonnet | done |
| 09.2c | Перевод экранов: Search, SearchResults | core | sonnet | done |
| 09.2d | Перевод экранов: Tags, TagSongs | core | sonnet | done |
| 09.2e | Перевод экранов: Settings, Onboarding | core | sonnet | done |
| 09.2f | Перевод экранов: Home, Library | core | sonnet | done |
| 09.2g | Перевод экранов: SongDetail, SongEdit | core | sonnet | done |

Зависимости между шагами: `00 → 01 → 02 → 03`; `04` и `05` после `03`; `06` после `02`
(независим от 03–05, можно вести параллельной веткой); `07` после `06`; `09` после `03`
(порядок по умолчанию: после `05`, чтобы не пересекаться с правками экранов). Шаг `08` отложен и в
очередь не входит; оркестратор его не запускает.

Ручной прогон перед вливанием шагов 04, 05, 06:
`./e2e/scripts/run-local.sh --flavor ru`, апгрейд-матрица `tools/rustore-upgrade-test.md`,
бэкап → удаление → восстановление по `docs/testing-backup-real-device.md`.

## 9. pws-server

Вне объёма. Наблюдения для будущего: сервер закреплён на pws `2.22.0` при текущем core `3.3.1`;
use case'ы лежат в `infra/` (а не в домене); контракт общий через `:api:contract`. Этот план не
меняет `:api:*`, поэтому серверу ничего не ломает. Когда сервер вернётся в работу — отдельный план
(обновление зависимости на core, сверка с `kotlin-clean-architecture`).

## 10. Вопросы владельцу и журнал

| Дата | Этап | Вопрос / решение |
|------|------|------------------|
| 2026-09-30 | — | План создан, статус `draft`; ждут ответа развилки F2–F6 |
| 2026-09-30 | 00.2 | F1 закрыта владельцем: `:app` удаляем, остаётся только Compose. Старый платёжный код в `app/src/rustore`, скорее всего, устарел — экспресс-сверка и удаление |
| 2026-09-30 | — | Владелец закрыл развилки: F2 — два каталога + тест; F3 — пропускаем, шаг 08 `deferred`; F4 — делаем; F5 — iOS не в ближайший год; F6 — оставляем, 07.3 `skipped`. Статус плана → `stable` |
| 2026-10-01 | 00 | Шаг 00 выполнен целиком (00.1–00.5), по коммиту на этап. Ручные прогоны 04/05/06 владелец разрешил отложить до мержа (список в заметках владельца). Гейты в песочнице linux-aarch64: без iOS-задач; Robolectric-тесты с нативным SQLite падают по окружению (UnsatisfiedLinkError) — перед мержем прогнать гейты на Mac. Находка 00.4: `TagsScreenModel.saveTag()` глотает ошибку `UpdateTagUseCase` → исправить в 03.2. **Следующий этап: 01.1** |
| 2026-10-06 | 01 | Шаг 01 выполнен (01.1–01.4). Convention plugins в обоих репо, detekt+ktlint с baseline, CI = `./gradlew build` (в pws-android без `assemble*Release` — подпись есть только в release-build.yml). Попутно: `verifyCoreVersionAlignment` сделан совместимым с configuration cache (иначе `build` падал). iOS-задачи и Robolectric+SQLite в песочнице не проверяемы — прогнать `./gradlew build` на Mac. **Следующий этап: 02.1** |
| 2026-10-06 | 02 | Шаг 02 выполнен (02.1–02.3): fitness-тесты в обоих репо, ratchet-списки зафиксированы. Попутно по прогону владельца исправлена гонка тестовых БД в `:data:db-android` (общий каталог `test-db/` при параллельных flavor-задачах). `-keep class net.zetetic.database.** { *; }` (sqlcipher, существовал до плана) оставлен в ratchet `ProguardRulesTest`. **Следующий этап: 03.1** |
| 2026-10-06 | 03 | Шаг 03 выполнен (03.1–03.4). Исправлены баги «ошибка проглочена»: Tags save/create/delete, SongDetail (вне сборника) onSaveTags, перехват `CancellationException` в экранных моделях — решение расширить 03.2 на create/delete и отмену принял оркестратор. Ratchet'ы `KNOWN_*` UI-слоя пусты, кроме `MaterialTheme` (шаг 09). Перед мержем — визуальная проверка экранов песни, настроек (ru/rustore) и тегов. **Следующие: 04 (ручной прогон перед мержем) и 06 (независим)** |
| 2026-10-06 | 04 | Шаг 04 выполнен в коде (04.1–04.2): порт `UserPreferencesRepository` + DataStore-адаптер (тест совместимости по 10 ключам), экраны и `MainActivity` читают настройки через use case. **Ручной прогон перед мержем шага 04 — за владельцем** (отложен им до мержа). `SongDetailPlatformSettings.kt` → `SongDetailExternalActions.kt` (учесть в 05). **Следующий этап: 05.1** |
| 2026-10-06 | 05 | Шаг 05 выполнен в коде (05.1–05.3): чистая машина состояний старта (таблица 36 комбинаций), `AppStartupModel`, `MainActivity` 240 → 92 строки, платформенные действия в `platform/`, DI по файлам в прежнем порядке (`AppModulesTest`). R8-минификация зелёная. **Ручной прогон перед мержем шага 05 — за владельцем.** **Следующий этап: 06.1** |
| 2026-10-06 | 06.1 | **Вопросы владельцу (не блокируют):** (1) баг `SongReferenceRepositoryImpl.getReferencesToSong` фильтрует по `song_id` вместо `ref_song_id` — «ссылки на песню» показывают исходящие; тест с правильным ожиданием написан и выключен — чинить отдельной задачей? (2) `inRwTransaction` коммитит при `Left` («Left commits») и `runCatching` в репозиториях глотает `CancellationException` — кандидат в шаг 07. `RoomTransactionRunner` переведён на KMP API Room (чтение — read-only транзакция); поведение на SQLCipher проверено только по исходникам Room — **обязателен прогон тестов БД и ручной прогон на Mac/устройстве** |
| 2026-10-06 | 06 | Шаг 06 выполнен в коде (06.1–06.3): repo-room тестируется на JVM; бэкап и импорт/удаление сборников — через порты/use case'ы pws-core, golden-снимки БД до/после совпали; восстановление бэкапа стало атомарным (раньше не было транзакции); `KNOWN_DIRECT_DAO_USERS` пуст. Вопросы владельцу (не блокируют): метод `BookReadRepository.count()`; удаление сборника удаляет отредактированную пользователем песню, если она только в нём (так было и раньше). **Ручной прогон перед мержем шагов 04–06 — за владельцем.** **Следующий этап: 07.1** |
| 2026-10-06 | 07 | Шаг 07 выполнен (07.1–07.2; 07.3 skipped по F6): форма use case закреплена в `UseCaseShapeTest`, документы ↔ код, удалены 5 неиспользуемых use case'ов (pws-server их не использует). **Следующий: шаг 09 (дизайн-система), этап 09.1** |
| 2026-10-06 | 09 | Шаг 09 выполнен (09.1, 09.2a–g): `designsystem/` (токены с сегодняшними значениями, обёртки, JVM-галерея 258 PNG), все экраны переведены; пиксельная разница до/после — ноль по всем семействам. Ratchet: `MaterialTheme` 47 → 14 файлов, dp-литералы 38 → 11 — остались доменные виджеты `components/`, `AppRoot`, `ProComingSoonSheet` (кандидат на этап 09.n). Кандидаты на обёртки — в заметках 09.2g. **План выполнен (кроме отложенного шага 08). Перед мержем — проверки владельца на Mac/устройстве (см. журнал 04–06).** |

## 11. Отступления от скиллов (сознательные)

| Скилл говорит | В проекте | Почему |
|---------------|-----------|--------|
| `kmp-architecture`: цели `jvm + wasmJs` | `android + jvm + ios + js(IR)` | Мобильное приложение; web на js — задел. Не меняем |
| `compose-multiplatform-ui`: нет общей обёртки состояния, поля `Async` | `sealed Loading|Content|Error` | Локальная БД, а не сеть: экраны реактивны через `Flow`, а не «запрос-ответ» (G8) |
| `compose-navigation3`, `state-viewmodel` (актуальные) | Voyager + `StateScreenModel` | Миграция отложена (F3); копии `voyager-navigation` и старого `compose-multiplatform-ui` в репозиториях остаются рабочими |
| `kotlin-testing-strategy`: Testcontainers/Postgres | Room + bundled SQLite на JVM | Нет сервера в объёме |
| `doc-first-agentic-workflow`: дерево `docs/NN-*` | существующие `docs/` + `docs/ai/plans/` | Перестройка документации не окупается; берём только формат шагов/этапов |
