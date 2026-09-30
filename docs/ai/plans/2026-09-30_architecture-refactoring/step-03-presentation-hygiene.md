# Шаг 03 — Слой представления: долги по ratchet

> Перед чтением этапа: [README](README.md) §4, §6, §7.2, §7.3.
> Навигация и state holder остаются на Voyager (`StateScreenModel`) — их замена в шаге 08.

**Цель.** Опустошить списки `KNOWN_*` тестов `FeaturesLayerTest`, `EffectsChannelTest`,
`NoSwallowedErrorsTest`, `ScreenFileSizeTest`, `NoServiceLocatorInComposablesTest` (шаг 02).
Поведение экранов не меняется.

**Мина шага.** При объединении двух моделей SongDetail незаметно теряется одна из тонкостей:
отложенная запись просмотра в историю (`viewDelay`, 5 с, отмена при смене песни), разный
`FavoriteSubject` (`BookedSong` vs `StandaloneSong`), поздний резолв `SongNumberId` для песни без
сборника. Защита: тесты из 00.4 и существующий `SongDetailBySongIdScreenModelTest` проходят
**без изменения ожиданий**; для записи просмотра — тест на виртуальном времени, проверенный красным
(убрать отмену → тест падает).

**Вне объёма.** Визуальные изменения, новые функции, переименование строковых ресурсов.

---

## Этап 03.1 — Одна модель SongDetail, без репозиториев в UI  · модель: opus

### Прочитать
- `features/.../song/detail/`: `SongDetailScreenModel.kt`, `SongDetailBySongIdScreenModel.kt`,
  `SongDetailUiState.kt`, `SongDetailBySongIdScreen.kt`, начало `SongDetailScreen.kt` (класс `Screen`)
- `features/.../di/FeaturesModule.kt` (две фабрики SongDetail)
- тесты `features/src/jvmTest/.../song/detail/`
- `domain/.../song/repository/SongObserveRepository.kt`, `domain/.../songnumber/repository/SongNumberReadRepository.kt`

### Решения этапа
- Вместо прямых репозиториев — use case'ы в `:domain` (создать, если нет):
  `ObserveBookSongsUseCase(bookId): Flow<Map<Int, SongSummary>>` (обёртка над
  `SongObserveRepository.observeAllInBook`) и use case(ы) для тех вызовов `SongNumberReadRepository`,
  которые реально делает модель (имена — по действию, по конвенции `{Action}{Entity}UseCase`).
  Зарегистрировать в `di/UseCasesModule.kt`.
- Общая логика выносится в **один** класс-делегат без наследования от Voyager:
  `SongDetailStateHolder(target, deps, scope)`, где
  `sealed interface SongTarget { data class InBook(val id: SongNumberId); data class Standalone(val id: SongId) }`.
  Делегат владеет: потоком `SongDetailUiState`, эффектами, записью просмотра, избранным, тегами,
  ссылками, карточкой доната.
- Зависимости группируются в 2–3 data-класса по смыслу (`SongDetailUseCases`, `DonationPromptUseCases`),
  чтобы конструктор не имел 18 параметров. Koin-фабрики собирают их из `get()`.
- Pager/переход по номеру (`bookSongNumberIds`, `bookNumberMap`, `currentSongNumberId`) остаётся
  только для `InBook`.
- Два Voyager-экрана и две `ScreenModel` **остаются** как тонкие обёртки над делегатом (их слияние —
  шаг 08), публичный API моделей для экранов не меняется.
- Параметр-заглушка `@Suppress("UNUSED_PARAMETER") songObserveRepository` удаляется.

### Работа
1. Use case'ы в домене + тесты на фейках.
2. `SongDetailStateHolder` + перенос логики; обе модели делегируют.
3. Обновить Koin-фабрики.
4. Вычеркнуть оба файла из `KNOWN_*` в `FeaturesLayerTest`.

### Проверки
```bash
./gradlew :domain:jvmTest :features:jvmTest && ./gradlew build
grep -rn "repository\." features/src/commonMain --include=*.kt | grep -v "/di/"     # пусто
```

### Готово, когда
- В `features` вне `di/` нет импортов репозиториев; суммарный размер двух моделей + делегата
  меньше исходных 635 строк; тесты из 00.4 зелёные без правки ожиданий.

### Заметки исполнителя
<!-- -->

---

## Этап 03.2 — Эффекты через Channel, ошибки не глотаются  · модель: sonnet

### Прочитать
- `pws-core/AGENTS.md §8` (ScreenModel / UI state), `features/.../app/UiMessage.kt`
- модели с `MutableSharedFlow`: Settings, Favorites, SongEdit, SongDetail (делегат из 03.1)
- все места из `KNOWN_*` теста `NoSwallowedErrorsTest`

### Решения этапа
- Эффекты: `private val _effects = Channel<Effect>(Channel.BUFFERED)`; `val effects = _effects.receiveAsFlow()`;
  отправка — `_effects.send(...)` из корутины модели (или `trySend`, где корутины нет).
  Место сбора в экране не меняется (`LaunchedEffect` + `collect`).
- Каждый пустой `catch`:
  - это действие пользователя (toggle, save, replace) → `Effect.ShowError(UiMessage.Failure(...))`
    + `telemetry.recordError(e, "<snake_case_контекст>")`;
  - это необязательная подгрузка (список для pager, ссылки) → состояние остаётся рабочим, но
    `telemetry.recordError(...)` обязателен; комментарий, почему деградация допустима;
  - `CancellationException` всегда пробрасывается (`catch (e: CancellationException) { throw e }`
    или `runCatching`-обёртка с `ensureActive()`).
- Новые ключи `UiMessage` — только типизированные, без локализованного текста в модели.
- Имена контекстов для `recordError` — новые, существующие события телеметрии не менять (G12).

### Работа
1. Перевести 5 моделей на `Channel`.
2. Обработать каждый `catch` по правилу выше.
3. Тесты: для каждого действия пользователя — ветка отказа выдаёт `ShowError`.
4. Очистить `KNOWN_*` в `EffectsChannelTest` и `NoSwallowedErrorsTest`.

### Проверки
```bash
./gradlew :features:jvmTest && ./gradlew build
grep -rn "MutableSharedFlow" features/src/commonMain      # пусто
```

### Готово, когда
- Оба списка `KNOWN_*` пусты; у каждой ветки отказа есть тест.

### Заметки исполнителя
<!-- -->

---

## Этап 03.3 — Разрезать SongDetailScreen.kt  · модель: sonnet

### Прочитать
- `features/.../song/detail/SongDetailScreen.kt` (1628 строк; оглавление функций — `grep -n "^@Composable\|^private fun\|^fun " …`)
- `compose-multiplatform-ui/references/feature-module-structure.md` из `engineering-ai-skills` (правило Entry/Content)

### Решения этапа
- **Только перемещение кода.** Тела функций не меняются, кроме видимости (`private` → `internal`).
- Раскладка в пакете `song/detail/`:

| Файл | Содержимое |
|------|------------|
| `SongDetailScreen.kt` | класс `Screen`, `SongDetailPager`, `SongDetailContent`, enum `SongDetailSheet` |
| `SongLyricView.kt` | `LyricRenderItem`, `toRenderItems`, `SongContent`, `LyricPartView`, `IntrinsicChorusView` |
| `SongHeaderSection.kt` | `BookContextBanner`, `SongHeader`, `SongMetadata`, `MetadataItem` |
| `SongReferencesSection.kt` | `SongReferencesSection`, `SongReferenceItem` |
| `SongTagsSection.kt` | `SongTagsSection`, `SongTagsRow`, `TagEditorSheet` |
| `SongDetailSheets.kt` | `TextSettingsSheet`, `SongActionsSheet`, `ActionItem`, `JumpToNumberSheet` |
| `SongShareText.kt` | `buildShareText` (чистая функция) + тест |

- Если после раскладки `SongDetailScreen.kt` всё ещё > 600 строк — выделить `SongDetailContent` в
  отдельный файл.

### Работа
1. Разнести по файлам; поправить импорты.
2. Тест на `buildShareText`.
3. Вычеркнуть файл из `KNOWN_*` в `ScreenFileSizeTest`.

### Проверки
```bash
./gradlew :features:jvmTest && ./gradlew build
cd ../pws-android && ./gradlew :app-compose:assembleRuDebug
```
Визуальная проверка экрана песни — оркестратор просит владельца (скриншот до/после) либо прогон
Maestro `e2e/flows/compose`.

### Готово, когда
- Ни один файл пакета не превышает 600 строк; diff — перемещение без изменения логики.

### Заметки исполнителя
<!-- -->

---

## Этап 03.4 — Settings/Tags: разрезать, убрать koinInject  · модель: sonnet

### Прочитать
- `features/.../settings/SettingsScreen.kt`, `SettingsScreenModel.kt`, `SettingsUiState.kt`
- `features/.../tags/TagsScreen.kt`
- `features/.../premium/UpsellHost.kt`, `PremiumGateHandler.kt`

### Решения этапа
- `SettingsScreen.kt` (794) → `SettingsScreen.kt` (Entry + Content) и файлы секций по смыслу
  (внешний вид, сборники, данные/бэкап, приватность, «о приложении»). `TagsScreen.kt` (555) →
  экран + `TagEditDialog.kt`/`TagColorPicker.kt` (по фактическому содержимому).
- В `SettingsScreen` три `koinInject` (`MonetizationMode`, `EntitlementRepository`, `Telemetry`)
  переезжают в `SettingsScreenModel`: режим монетизации и `entitlementInfo` становятся частью
  `SettingsUiState.Content`, телеметрия вызывается из модели.
- `UpsellHost` и `PremiumGateHandler` остаются с `koinInject` — это корневые хосты, не экраны
  (они в allow-list теста).
- Разделить каждый экран на `XxxEntry` (получение модели, сбор state/effects) и `XxxContent(state, onAction…)`
  без доступа к модели — только для Settings и Tags в этом этапе.

### Работа
1. Перенос зависимостей в модель + тесты модели на новые поля state.
2. Раскладка по файлам, Entry/Content.
3. Очистить `KNOWN_*` в `ScreenFileSizeTest` и `NoServiceLocatorInComposablesTest`.

### Проверки
```bash
./gradlew :features:jvmTest && ./gradlew build
# gate pws-android (флейвор rustore обязателен: MonetizationMode переопределяется там)
```

### Готово, когда
- Оба списка `KNOWN_*` пусты; экран настроек во флейворах `ru` и `rustore` показывает те же секции,
  что и до этапа (rustore — блок Pro).

### Заметки исполнителя
<!-- -->
