# Шаг 09 — Дизайн-система

> Перед чтением этапа: [README](README.md) §4, §6, §7.2, §7.3. F4 закрыта: делаем.
> Шаг 08 отложен, поэтому экраны остаются на Voyager: шаг выполняется после шага 03 (по умолчанию — после 05).
> Опорный скилл: `compose-design-system` (актуальный, из `engineering-ai-skills`).

**Цель.** Экраны говорят на собственном словаре токенов (`AppTheme.colors/spacing/shapes/typography`)
и используют обёртки компонентов; `MaterialTheme.*` и dp-литералы остаются только внутри
`designsystem/`. Смена палитры или плотности — правка токенов, а не 33 файлов.

**Что есть сегодня.** `theme/` уже содержит `Color`, `Typography`, `Shape`, `Spacing` (+`LocalSpacing`),
`Motion` — хорошая основа. Но экраны обращаются к `MaterialTheme.*` 575 раз в 33 файлах и содержат
181 dp-литерал вне `theme/`.

**Мина шага.** «Незаметная» визуальная регрессия: токен подставлен близкий, но не тот (отступ 12 → 16,
`onSurfaceVariant` → `outline`). Защита: JVM-галерея, рендерящая экраны в PNG до и после
(скилл, `references/wrappers-and-review.md`); сравнение пиксельной разницы по каждому переведённому
семейству; нулевая разница — критерий готовности, ненулевая — перечислена и принята владельцем.

**Вне объёма.** Редизайн. Новые цвета, размеры и шрифты не вводятся: токены получают **сегодняшние**
значения.

**Связь с редизайном (F4a).** Этот шаг — инженерная подготовка, внешний вид не меняется. Если
владелец позже сделает новый визуальный язык (сам в инструменте дизайна или поручит агенту), он
применяется заменой значений токенов в `designsystem/` и правкой обёрток — экраны не трогаются.
Поэтому решение о редизайне шаг не блокирует и может быть принято после него.

---

## Этап 09.1 — designsystem: токены, обёртки, ratchet  · модель: sonnet

### Прочитать
- `compose-design-system/SKILL.md`, `references/tokens.md`, `references/wrappers-and-review.md`, `templates/*`
- `features/.../theme/*`, `features/.../components/*` (список файлов и размеры)

### Решения этапа
- Пакет `features/.../designsystem/` — лист: не импортирует ничего из `features`, домена и DI.
  Существующий `theme/` переезжает в него (перемещение пакета; `AppTheme` остаётся точкой входа).
- `AppTheme.colors / typography / shapes / spacing / sizing` — data-классы через
  `staticCompositionLocalOf`, геттеры `@ReadOnlyComposable`. Значения — текущие.
- Обёртки первой очереди — только те M3-компоненты, которые экраны используют чаще всего (взять
  топ-8 по `grep`), плюс уже существующие общие (`AppTopBar`, `AppLargeTopBar`, `AppSheet`,
  `AppConfirmDialog`, `SearchField`) — переезжают в `designsystem/`.
- Предметные виджеты (`SongListItem`, `BookCard`, `TagChip`, `HomeSearch`, `NumberInputModal`,
  `DonationPrompt*`) остаются в `components/` — они знают домен.
- JVM-галерея: тест/задача, рендерящая ключевые `XxxContent` с фиктивным state в PNG
  (`features/build/gallery/`), светлая и тёмная тема.
- Fitness-тесты: `DesignSystemIsLeafTest`; `MaterialThemeRatchetTest` (из 02.2) и новый
  `DpLiteralRatchetTest` — списки файлов, только сокращаются.

### Работа
1. Перенос `theme/` → `designsystem/`, токены, обёртки.
2. Галерея + базовые снимки «до».
3. Тесты-ratchet с текущими списками.

### Проверки
```bash
./gradlew :features:jvmTest && ./gradlew build
```

### Готово, когда
- `designsystem/` — лист под тестом; галерея даёт снимки; поведение и вид не изменились.

### Заметки исполнителя
- **Этап 09.1 — done (2026-10-06).** Сделано (pws-core `:features`):
  - `theme/` -> `designsystem/` перемещением пакета (Color, Typography, Shape, Spacing, Motion, Theme, DynamicColor + actual-ы android/skiko/ios). Туда же переехали `AppTopBar`, `AppLargeTopBar`, `AppSheet` (`AppModalBottomSheet`), `AppConfirmDialog`, `SearchField`, `TestTagModifier` (expect/actual, нужен `AppSheet`/`AppConfirmDialog`). Импорты обновлены в `:features` и в pws-android (`PaymentActivity`).
  - Токены: `AppTheme.colors` (новый `AppColors` — 1:1 роли M3 `ColorScheme`, значения те же), `typography`/`shapes` (= M3 `Typography`/`Shapes` под нашим именем), `spacing` (прежний `Spacing`; `MaterialTheme.spacing` оставлен, чтобы не трогать 27 импортов), `sizing` (новый `Sizing`: `iconSm/Md/Lg/Xl`, `touchTarget` с теми же значениями, что в `Spacing`; дубль в `Spacing` убрать в 09.2+ после миграции экранов). Данные-классы через `staticCompositionLocalOf`, геттеры `@ReadOnlyComposable`.
  - Обёртки первой очереди (топ-8 по `grep` среди ещё не обёрнутых M3-компонентов: Text 42, Icon 35, IconButton 22, HorizontalDivider 18, Scaffold 15, Surface 14, TextButton 13, Card 9): `AppText`, `AppIcon` (vector/painter), `AppIconButton`, `AppDivider`, `AppScaffold`, `AppSurface`, `AppTextButton`, `AppCard` в `designsystem/Wrappers.kt` — сквозные, с дефолтами M3 (вид не меняется); параметры добавляются по мере нужды экранов (09.2+). Пока ни один экран их не использует.
  - Fitness: `DesignSystemIsLeafTest` (красный проверен: импорт доменного `ThemeMode` в `Sizing.kt` -> тест упал, откат), `MaterialThemeRatchetTest` (исключение `designsystem/`; из списка ушли 4 переехавших файла: 47 -> 43), новый `DpLiteralRatchetTest` (38 файлов вне `designsystem/`, регэксп `(?<![\w.])\d+(\.\d+)?\.dp\b` по коду без комментариев; включает `components/` и `icons/FilledHeart.kt`). `ScreenFileSizeTest`: исключение `theme/` -> `designsystem/`. `AGENTS.md` §8 и `docs/DESIGN.md` — путь обновлён.
  - Галерея: `./gradlew :features:renderGallery` (JavaExec в `features/build.gradle.kts`, исходники `features/src/jvmTest/.../gallery/`), PNG в `features/build/gallery/{light,dark,black}/<entry>.png`, `-Pgallery.out=<dir>` — другой каталог. 4 записи: `designsystem-tokens`, `designsystem-wrappers`, `state-empty`, `state-error` x 3 темы = 12 PNG; вывод детерминирован (две прогонки — одинаковый md5). **В `./gradlew build` не входит** (осознанно: нужен нативный Skiko; зависимость `skiko-awt-runtime-<os>-<arch>` в `jvmTestImplementation`, версия `skiko` в `libs.versions.toml`, ОС/архитектура — по JVM Gradle). В песочнице linux-aarch64 headless **рендер работает** (JavaExec идёт на родной arm64 JVM; обычные Test-задачи в песочнице идут под qemu x86_64 — для них natives не было бы). На Mac/CI ожидается работа; на Mac не проверено.
- **Отклонения / решения, которых не было в плане:**
  1. Доменный `ThemeMode` в лист `designsystem/` импортировать нельзя -> в `designsystem/` свой `AppThemeMode {System, Light, Dark, Black}`, а `AppTheme(themeMode, useDynamicColor, content)` принимает его. Маппинг домен -> дизайн-система: `features/app/AppThemeFromPreferences.kt` (`AppRoot` и `PaymentActivity` вызывают её).
  2. Лист `designsystem/` разрешает импорт `features.resources.*` (сгенерированные строки, напр. «Назад» в `AppTopBar`) — иначе `AppTopBar`/`SearchField`/`AppConfirmDialog` не переехать. Всё остальное из `io.github.alelk.pws.*`, Koin и Voyager — запрещено тестом.
  3. В галерее пока только компоненты дизайн-системы и `EmptyContent`/`ErrorContent`; экранные `XxxContent` (Books, Favorites…) требуют Voyager `Navigator` и фейкового state — каждое семейство в 09.2+ добавляет свои записи в `GalleryEntries.kt` **до** миграции (иначе не будет «до»). Снимки «до» для семейств в 09.1 не сняты.
  4. ktlint intellij_idea-стиль требует однострочных сигнатур и ругается на PascalCase `@Composable` (в базлайне это тысячи записей): в новых файлах (`Theme.kt`, `Wrappers.kt`, `AppThemeFromPreferences.kt`, галерея) — `@file:Suppress("ktlint:standard:function-naming", "ktlint:standard:function-signature")`; в detekt — `@Suppress("LongParameterList")` на `AppText/AppScaffold/AppSurface` (зеркалят параметры M3).
  5. ktlint-baseline `features`: пути перенесённых файлов переписаны; после сортировки импортов (замена `features.theme.*` меняет лексикографический порядок) сдвиги строк -> `ktlintGenerateBaseline`; `<error` было 2346, стало 2332 (не выросло). detekt-baseline не менялся (ключи по имени файла). Импорты во всех затронутых файлах пересортированы вручную скриптом (без `ktlintFormat`).
- **Вопросы владельцу:** нет блокирующих. Принять: `AppTheme.colors` — это 1:1 копия ролей M3 (без смысловых цветов); смысловые цвета (если нужны) добавляются при редизайне/09.n.
- **Проверить вручную:** на Mac запустить `./gradlew :features:renderGallery` и открыть PNG (шрифты/рендер); визуально приложение не должно измениться (экраны не менялись, `AppTheme` строит ту же `ColorScheme`/`Typography`/`Shapes`).
- **Gate:** pws-core `./gradlew build $(cat .../corex) --continue` — BUILD SUCCESSFUL (fitness, detekt, ktlint, тесты); pws-android `:app-compose:assembleRuDebug :app-compose:assembleRustoreDebug --max-workers=1` — BUILD SUCCESSFUL. Вне песочницы дополнительно: iOS-компиляция (`iosMain` actual-ы переехали вместе с пакетом, здесь не собирается).


---

## Этапы 09.2…09.n — Перевод экранов  · модель: sonnet
> По одному семейству экранов на сессию, в порядке: (a) Books, BookSongs, BookLibrary →
> (b) Favorites, History → (c) Search, SearchResults → (d) Tags, TagSongs → (e) Settings, Onboarding →
> (f) Home, Library → (g) SongDetail, SongEdit.
> Оркестратор заводит под-статусы в README.

### Прочитать
- заметки 09.1; файлы своего семейства; `designsystem/` (список токенов и обёрток)

### Решения этапа
- В файлах семейства: `MaterialTheme.colorScheme.x` → `AppTheme.colors.y`; `NN.dp` → токен
  `spacing`/`sizing`; сырые M3-компоненты → обёртки. Недостающий токен/обёртка добавляется в
  `designsystem/` с сегодняшним значением — не подбирается «ближайший».
- Если значение встречается один раз и не ложится в шкалу — оставить литерал и добавить файл в
  pinned-исключения теста с причиной (лучше честный литерал, чем неверный токен).

### Работа
1. Перевести семейство.
2. Снять снимки галереи «после», сравнить с «до».
3. Вычеркнуть файлы из ratchet-списков.

### Проверки
```bash
./gradlew :features:jvmTest && ./gradlew build
```

### Готово, когда
- Файлы семейства вычеркнуты из `KNOWN_*`; пиксельная разница снимков нулевая либо перечислена в
  заметках для решения владельца.

### Заметки исполнителя
<!-- по блоку на семейство -->

#### Семейство (a): Books, BookSongs, BookLibrary — 09.2a done (2026-10-06)
- **Снимки.** Записи галереи добавлены ДО правок: `features/src/jvmTest/.../gallery/BookFamilyGalleryEntries.kt` (подключены в `GalleryEntries.kt`): `books-content/-error`, `book-songs-content/-error`, `book-library-content` (built-in, update+recommended, installed, not installed, downloading, error), `book-library-empty`, `book-library-error`; 11 записей x 3 темы = 33 PNG. `XxxContent` обёрнуты в минимальный Voyager `Navigator` + `ScreenRegistry` с заглушками `SharedScreens.*` + Koin с `PremiumGate`, который не блокирует (его требует `SongListItem`). Код экранов для этого не менялся. **Пиксельная разница до/после: нулевая** (побайтовое совпадение всех 33 PNG, `cmp`; прогон повторен после финальных правок).
- **Не покрыто галереей:** диалоги (`GoToNumberDialog`, подтверждение удаления), состояния Loading (анимация), `BookCard` (остался в `components/` без правок - предметный виджет, ещё в обоих ratchet-списках).
- **Токены/обёртки (в `designsystem/`, значения сегодняшние):** `Sizing.bottomBarClearance = 80.dp` (хвост списка над нижней панелью; был в 3 файлах), `Sizing.listDividerInset = 72.dp`, `Sizing.gridCellMin = 160.dp` (одиночные литералы вне шкалы, сделаны токенами, а не pinned-исключениями: механизма pinned в `DpLiteralRatchetTest` нет, добавлять его не стали). Обёртки: `AppButton`, `AppOutlinedButton`, `AppTonalButton` (`FilledTonalButton`), `AppElevatedCard` - сквозные, с дефолтами M3. `16.dp/12.dp` баннера -> `spacing.cardHorizontal/cardVertical`; `16/8/4.dp` -> `lg/sm/xs`.
- **Осталось сырым M3 (не ratchet-ится, обёрток пока нет):** `LargeTopAppBar`/`TopAppBar` (`AppLargeTopBar`/`AppTopBar` не годятся: другие стиль заголовка, цвет контейнера, `maxLines`), `FloatingActionButton`, `AlertDialog`, `OutlinedTextField`, `LinearProgressIndicator`, `CardDefaults`/`ButtonDefaults`/`TopAppBarDefaults` для цветов. Решать в следующих семействах/09.n по частоте.
- **Ratchet:** `MaterialThemeRatchetTest` 42 -> 39, `DpLiteralRatchetTest` 38 -> 35 (вычеркнуты `books/BooksScreen.kt`, `book/songs/BookSongsScreen.kt`, `booklibrary/BookLibraryScreen.kt`).
- **Baseline:** ktlint-baseline `features` `<error` 2332 -> 2332 (пересоздан `ktlintGenerateBaseline` с iOS `-x`; первая попытка дала 2336 из-за переноса вызова на 4 строки в файле с 4-пробельным отступом - строка свёрнута обратно в одну); detekt-baseline 153 -> 153. Новые строки в detekt (MaxLineLength) поправлены в коде.
- **Gate:** pws-core `./gradlew build $(cat corex) --continue` - BUILD SUCCESSFUL. pws-android не пересобирался: публичные имена не менялись, только добавлены (`Sizing`-поля, обёртки).

#### Семейство (b): Favorites, History — 09.2b done (2026-10-06)
- **Снимки.** Записи добавлены ДО правок: `features/src/jvmTest/.../gallery/FavoritesHistoryGalleryEntries.kt` (подключены в `GalleryEntries.kt`): `favorites-content` (по возрастанию), `favorites-content-desc` (SONG_NAME, по убыванию), `favorites-empty`, `favorites-error`, `history-content` (4 группы дат, Booked/Standalone), `history-empty`, `history-error`, `history-clear-dialog`; 8 записей x 3 темы = 24 PNG (всего в галерее 57). Приём семейства (a) переиспользован: `InNavigator` и `GalleryScreen` из `BookFamilyGalleryEntries.kt` стали `internal`, в `ScreenRegistry` добавлена заглушка `SharedScreens.SongById` (для Standalone-песен). Код экранов для галереи не менялся. **Пиксельная разница до/после: нулевая** (побайтовое `cmp` всех 57 PNG; «до» — `/tmp/claude-1000/09.2b/before`, «после» — `.../after`). История берёт `Clock.System.now()` внутри экрана, поэтому возраст записей в фикстуре выбран в середине корзин (50 мин, 3 ч 20 мин, 30 ч, 4 дн 5 ч, 20 дн 5 ч, 40 дн 5 ч) - снимки детерминированы.
- **Не покрыто галереей:** `FavoritesSortSheet` (ModalBottomSheet с анимацией появления, открывается по клику), Loading. Лист сортировки переведён теми же заменами (токены цвета/типографики/отступов, `AppText`), но «до/после» для него не снято.
- **Токены/обёртки:** новых не потребовалось, переиспользованы из 09.1/09.2a: `Sizing.listDividerInset` (72), `Sizing.bottomBarClearance` (80), `AppScaffold/AppText/AppIcon/AppIconButton/AppDivider/AppSurface`; `12.dp` строки сортировки -> `spacing.md`, `16/8.dp` заголовка группы -> `spacing.lg/sm`.
- **Осталось сырым M3 (не ratchet-ится):** `LargeTopAppBar`/`TopAppBarDefaults` (см. 09.2a), `RadioButton`, `SnackbarHost`/`SnackbarHostState`, `rememberModalBottomSheetState`. Кандидаты на обёртки в 09.n: `LargeTopAppBar` (теперь 3 семейства), `RadioButton`.
- **Находка вне объёма:** в обоих файлах остались неиспользуемые импорты `AlertDialog`, `TextButton` (старый долг в ktlint-baseline), не трогал.
- **Ratchet:** `MaterialThemeRatchetTest` 39 -> 37, `DpLiteralRatchetTest` 35 -> 33 (вычеркнуты `favorites/FavoritesScreen.kt`, `history/HistoryScreen.kt`).
- **Baseline:** ktlint-baseline `features` `<error` 2332 -> 2331 (`ktlintGenerateBaseline` с iOS `-x`; сдвиг строк от импортов); detekt-baseline `<ID>` 153 -> 153. Новые detekt-замечания (MaxLineLength в галерее) исправлены в коде.
- **Gate:** pws-core `./gradlew build $(cat corex) --continue` (+ `-Pkotlin.daemon.jvmargs=-Xmx1000m`) - BUILD SUCCESSFUL. pws-android не пересобирался: публичные имена не менялись.

#### Семейство (c): Search, SearchResults — 09.2c done (2026-10-06)
- **Снимки.** Записи добавлены ДО правок: `features/src/jvmTest/.../gallery/SearchGalleryEntries.kt` (подключены в `GalleryEntries.kt`): `search-idle`, `search-suggestions`, `search-results` (8 строк: один номер в двух книгах, разные номера, автономная песня с иконкой, без сниппета, длинное название, подсветка `<mark>`), `search-empty`, `search-error`; 5 записей x 3 темы = 15 PNG (всего в галерее 72). `SearchScreen` и `SearchResultsScreen` делят один `SearchContent`, поэтому записей на оба. Приёмы (a)/(b) переиспользованы без изменений: `InNavigator`, `GalleryScreen`, заглушки `ScreenRegistry`/Koin. Код экрана для галереи не менялся. **Пиксельная разница до/после: нулевая** (побайтовое `cmp` всех 15 PNG; «до» — `/tmp/claude-1000/09.2c/before`, «после» — `.../after`).
- **Не покрыто галереей:** Loading и `isLoading` в Results (анимация), переключение чипов области и режима «123» (локальный state, не задаётся снаружи), клавиатура/фокус.
- **Токены/обёртки:** одна новая запись `Sizing.numberBadge = 40.dp` (ширина колонки номера песни; тот же литерал остаётся в `SongListItem`/`TagsScreen`/`TagEditDialog` — они в своих семействах). Остальное переиспользовано: `Sizing.bottomBarClearance`, `listDividerInset`, `iconMd`; `AppScaffold/AppText/AppIcon/AppIconButton/AppTextButton/AppDivider/AppSurface`; `spacing.*`, `AppTheme.colors/typography`.
- **Осталось сырым M3 (не ratchet-ится):** `FilterChip`/`FilterChipDefaults` (кандидат на обёртку `AppFilterChip` в 09.n; в проекте пока только здесь — решить по частоте), `HighlightedText`/`SearchEmptyContent` — предметные виджеты в `components/` (в ratchet-списках остаются).
- **Ratchet:** `MaterialThemeRatchetTest` 37 -> 36, `DpLiteralRatchetTest` 33 -> 32 (вычеркнут `search/SearchScreen.kt`).
- **Baseline:** ktlint-baseline `features` `<error` 2331 -> 2331 (`ktlintGenerateBaseline` с iOS `-x`, сдвиг строк); detekt-baseline `<ID>` 153 -> 153.
- **Gate:** pws-core `./gradlew build $(cat corex) --continue` (+ `-Pkotlin.daemon.jvmargs=-Xmx1000m`) - BUILD SUCCESSFUL. pws-android не пересобирался: публичные имена не менялись, добавлено только поле `Sizing.numberBadge`.

#### Семейство (d): Tags, TagSongs — 09.2d done (2026-10-06)
- **Снимки.** Записи добавлены ДО правок: `features/src/jvmTest/.../gallery/TagsGalleryEntries.kt` (подключены в `GalleryEntries.kt`): `tags-content`, `tags-empty`, `tags-error`, `tags-add-dialog`, `tags-edit-dialog` (диалог `TagEditDialog.kt` с выбором цвета, выбран третий цвет), `tags-delete-dialog`, `tags-hide-dialog` (предопределённый тег), `tag-songs-content`, `tag-songs-empty`, `tag-songs-error`, `tag-songs-not-found`; 11 записей x 3 темы = 33 PNG (всего в галерее 105). Диалоги открываются через state (`showAddDialog`/`editingTag`/`showDeleteConfirmation`) в `TagsContent`, поэтому отдельная обёртка не нужна. Приёмы (a)-(c) переиспользованы без изменений: `InNavigator`, заглушки `ScreenRegistry`/Koin. Код экранов для галереи не менялся. **Пиксельная разница до/после: нулевая** (побайтовое `cmp` всех 105 PNG, включая семейства (a)-(c); «до» — `/tmp/claude-1000/09.2d/before`, «после» — `.../after`; прогон повторен после финальных правок).
- **Не покрыто галереей:** Loading, snackbar-ы (`HandleTagsEffects`), состояние нажатия/ввод текста в поле имени, клик по цвету.
- **Токены/обёртки (в `designsystem/Sizing`, значения сегодняшние):** `listDividerInsetCompact = 56.dp` (разделитель строки тега), `tagSwatch = 40.dp` (индикатор цвета в строке и опция в выборе цвета), `tagSwatchDot = 16.dp`, `tagTitleDot = 12.dp` (точка цвета в заголовке TagSongs), `swatchSelectedBorder = 3.dp`. Новых обёрток нет. Переиспользованы: `bottomBarClearance`, `listDividerInset`, `iconSm` (галочка 20.dp), `AppScaffold/AppText/AppIcon/AppIconButton/AppDivider/AppSurface/AppTextButton`, `AppTheme.spacing/colors/typography`.
- **Осталось сырым M3 (не ratchet-ится):** `LargeTopAppBar` (теперь 4 семейства — кандидат на обёртку в 09.n), `FloatingActionButton`, `AlertDialog`, `OutlinedTextField` (диалог тега), `SnackbarHost`, `TopAppBarDefaults`. `TagChip` (предметный виджет в `components/`) не трогал.
- **Ratchet:** `MaterialThemeRatchetTest` 36 -> 33, `DpLiteralRatchetTest` 32 -> 29 (вычеркнуты `tags/TagEditDialog.kt`, `tags/TagsScreen.kt`, `tags/songs/TagSongsScreen.kt`).
- **Baseline:** ktlint-baseline `features` `<error` 2331 -> 2331 (`ktlintGenerateBaseline` с iOS `-x`, сдвиг строк); detekt-baseline `<ID>` 153 -> 153 (новые MaxLineLength поправлены в коде).
- **Gate:** pws-core `./gradlew build $(cat corex) --continue` (+ `-Pkotlin.daemon.jvmargs=-Xmx1000m`) - BUILD SUCCESSFUL. pws-android не пересобирался: публичные имена не менялись, добавлены только поля `Sizing`.

#### Семейство (e): Settings, Onboarding — 09.2e done (2026-10-06)
- **Снимки.** Записи добавлены ДО правок: `features/src/jvmTest/.../gallery/SettingsOnboardingGalleryEntries.kt` (подключены в `GalleryEntries.kt`), 18 записей x 3 темы = 54 PNG (всего 159). Settings (вся страница, высота 2300): `settings-content` (Monetization None), `-content-donations` (Donations, блок доната, телеметрия, тёмная тема выбрана, переключатели включены), Pro-блок: `settings-pro-purchases-{none,lifetime,subscription,expired}` (PremiumSales, кнопка «Покупки»), `settings-pro-coming-soon-{none,lifetime,expired}` и `settings-pro-unknown` (PremiumComingSoon, кнопка «Подробнее»), `settings-loading`, `settings-license-dialog`. Onboarding: `onboarding-loading/-error/-content` (установлена/рекомендована/загрузка/ошибка + согласие на телеметрию)/`-content-fresh`/`-content-idle`/`-downloading`. Для галереи только виджетность: `SettingsContent`, `OnboardingContent`, `OnboardingTelemetryConsent` стали `internal` (было `private`), код не менялся; `fakeLibraryItems` из (a) стал `internal`. **Пиксельная разница до/после: нулевая** (побайтовое `cmp` всех 159 PNG; «до» — `/tmp/claude-1000/09.2e/before`, «после» — `.../after`; диалог лицензии детерминирован).
- **Не покрыто галереей:** Loading у Onboarding с анимацией выбора, нажатия, переходы `AnimatedVisibility` кнопки установки (в снимках `onboarding-content-fresh` кнопка видна), Snackbar/ProComingSoonSheet (из `premium/`, не семейство).
- **Токены/обёртки:** одна новая запись `Sizing.licenseViewportHeight = 400.dp` (высота окна текста лицензии). Переиспользованы: `spacing.xxs/xs/sm/md/lg/xl/listItemVertical` (14.dp строки доната), `Sizing.listDividerInsetCompact` (56, разделитель темы), `bottomBarClearance`, `touchTarget` (48 бокс индикатора книги), `AppText/AppIcon/AppDivider/AppCard/AppScaffold/AppSurface/AppButton/AppOutlinedButton/AppTextButton`. Новых обёрток нет.
- **Осталось сырым M3 (не ratchet-ится):** `Switch`, `RadioButton`, `Checkbox`/`CheckboxDefaults`, `SuggestionChip`/`SuggestionChipDefaults`, `LinearProgressIndicator`, `AlertDialog` (лицензия), `CardDefaults.cardColors`. Кандидаты 09.n: `AppSwitch` (4 места), `AppCheckbox`, `AppRadioButton`.
- **Ratchet:** `MaterialThemeRatchetTest` 33 -> 23, `DpLiteralRatchetTest` 29 -> 19 (вычеркнуты `onboarding/OnboardingScreen.kt` и 9 файлов `settings/Settings*`; `SettingsScreen.kt` был только в Material-списке).
- **Baseline:** ktlint `<error` 2331 -> 2331 (`ktlintGenerateBaseline` с iOS `-x`); detekt `<ID>` 153 -> 153 (ключ `LongMethod:OnboardingContent` переписан: `private fun` -> `internal fun`, иначе та же запись всплывала как новая). Импорты пересортированы скриптом (java/javax/kotlin — в конце), `ktlintFormat` не запускался. `OnboardingScreen.kt` по-прежнему с 4-пробельным отступом (старый долг, не трогал).
- **Gate:** pws-core `./gradlew build $(cat corex) --continue` (+ `-Pkotlin.daemon.jvmargs=-Xmx1000m`) + `--rerun-tasks` для `:features:detekt :features:jvmTest :features:ktlintCheck` — BUILD SUCCESSFUL. pws-android не пересобирался: публичные имена не менялись (видимость `private`->`internal` и поле `Sizing`).
