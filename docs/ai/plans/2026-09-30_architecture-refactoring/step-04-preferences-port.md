# Шаг 04 — Настройки как порт домена

> Перед чтением этапа: [README](README.md) §4, §6, §7.2, §7.3. **Ручной прогон перед вливанием.**

**Цель.** Настройки отображения перестают течь через `MainActivity` → 15 параметров `AppRoot` →
nullable `CompositionLocal`. Экраны получают их как обычные данные: порт в домене, реализация в шелле.

**Что не так сегодня.** `MainActivity` собирает 10 `Flow` из DataStore, упаковывает в
`SongDetailDisplaySettings` / `FavoritesDisplaySettings` / `ThemeSettings` вместе с лямбдами записи и
отдаёт в `AppRoot`. Режим сортировки избранного ездит строкой (`"ADDED_DATE"`). Экран без шелла
(тест, desktop, web) получает `null` и молча теряет настройки.

**Мина шага.** Пользователь после обновления видит сброшенные настройки: другой файл DataStore,
другое имя ключа или другое значение по умолчанию. Защита: тест адаптера, который пишет значения
**старыми** функциями из `ThemePreferences.kt`, а читает новым репозиторием (и наоборот) — на одном
и том же DataStore; плюс `StorageNamesPinnedTest` из 02.3.

**Закреплённые имена (G3).** DataStore `app-settings`; ключи: `app-theme`, `song-text-scale`,
`song-text-expanded`, `favorites-sort-mode`, `favorites-ascending`, `use-dynamic-color`,
`keep-screen-on`, `song-line-height-multiplier`, `song-serif-font`, `show-song-nav-buttons`.
Значения по умолчанию — как сейчас в `MainActivity.kt` (`collectAsState(initial = …)`) и в
`ThemePreferences.kt`. Ключ `app-theme` читает и пишет `BackupManager` — формат значения не менять.

**Вне объёма.** Новые настройки; перенос согласия на телеметрию (`TelemetryConsentStore`) и
Pro-статуса — они остаются как есть.

---

## Этап 04.1 — Порт и Android-адаптер  · модель: opus

### Прочитать
- `pws-android/app-compose/.../ThemePreferences.kt`, `BackupManager.kt` (работа с `appThemeKey`)
- `pws-android/app-compose/.../MainActivity.kt` (блок чтения настроек, строки ~265–345)
- `pws-core/features/.../theme/ThemeMode.kt`, `song/detail/SongDetailPlatformSettings.kt`
- `pws-core/domain/.../donationprompt/repository/` (образец порта Read/Write без Room)

### Решения этапа
- В `:domain`, пакет `io.github.alelk.pws.domain.preferences`:
  ```kotlin
  data class AppPreferences(
    val themeMode: ThemeMode, val useDynamicColor: Boolean, val keepScreenOn: Boolean,
    val songText: SongTextPreferences,        // scale, expanded, lineHeightMultiplier, serifFont, showNavButtons
    val favorites: FavoritesPreferences,      // sortMode: FavoritesSortMode, ascending
  )
  interface UserPreferencesRepository {
    val preferences: Flow<AppPreferences>
    suspend fun update(transform: (AppPreferences) -> AppPreferences)
  }
  ```
  `ThemeMode` переезжает в домен (в `features` остаётся `typealias` до конца шага);
  `FavoritesSortMode` — enum в домене, строковые значения сериализации совпадают с сегодняшними.
- Use case'ы: `ObserveAppPreferencesUseCase`, `UpdateAppPreferencesUseCase`.
- Значение по умолчанию `showNavButtons` зависит от платформы (`PlatformDefaultShowSongNavButtons`) —
  дефолт задаёт реализация порта, а не домен.
- Android-реализация `DataStoreUserPreferencesRepository` в `app-compose` над **тем же** экземпляром
  `DataStore<Preferences>`, который уже зарегистрирован в Koin (`appSettingsDataStore()`); второй
  DataStore на тот же файл открывать нельзя.
- Дефолт в `featuresModule`: `InMemoryUserPreferencesRepository` (для тестов и целей без шелла);
  шелл переопределяет его своим модулем, загруженным позже (README §6 п. 4).
- Старые функции `Context.xxxFlow()/setXxx()` пока остаются — их удалит этап 04.2.

### Работа
1. Порт, модели, use case'ы, in-memory реализация, тесты домена.
2. Android-адаптер + регистрация в Koin.
3. Тест совместимости (Robolectric, как `BackupManagerTest`): запись старой функцией → чтение
   репозиторием и обратно, по каждому из 10 ключей; дефолты при пустом DataStore совпадают со старыми.
4. Тест проверить красным: изменить имя одного ключа в адаптере → тест падает → вернуть.

### Проверки
```bash
./gradlew :domain:jvmTest && ./gradlew build      # pws-core
./gradlew :app-compose:testRuDebugUnitTest && ./gradlew build   # pws-android
```

### Готово, когда
- Порт и адаптер существуют и покрыты тестом совместимости; поведение приложения не изменилось
  (новый код ещё никем не используется).

### Заметки исполнителя
**04.1 (opus, 2026-10-06) — done.**
- Сделано (pws-core `:domain`, пакет `domain.preferences` с подпакетами по конвенции модуля: `model/` — `AppPreferences`, `SongTextPreferences`, `FavoritesPreferences`, `ThemeMode` (перенесён, `identifier` те же), `FavoritesSortMode` (`identifier` = `"ADDED_DATE"`/`"SONG_NUMBER"`/`"SONG_NAME"`, неизвестное → `ADDED_DATE`, как `valueOf`+`getOrDefault`); `repository/` — `UserPreferencesRepository`, `InMemoryUserPreferencesRepository(initial)` (по образцу `InMemoryTokenStorage`); `usecase/` — `ObserveAppPreferencesUseCase`, `UpdateAppPreferencesUseCase`. Дефолты — `AppPreferences.defaults(showNavButtons)`: всё, кроме `showNavButtons`, задаёт домен (значения = `ThemePreferences.kt`/`MainActivity`), `showNavButtons` передаёт реализация порта. Тесты `commonTest`: закреплённые identifier'ы, фолбэки, дефолты, use case'ы, конкурентные update. `features`: `theme/ThemeMode.kt` → `typealias` на доменный (+ `ThemeSettings`/`LocalThemeSettings` на месте); `featuresModule` — `single<UserPreferencesRepository> { InMemory…(defaults(PlatformDefaultShowSongNavButtons)) }`, use case'ы — в `useCasesModule`.
- Сделано (pws-android): `DataStoreUserPreferencesRepository(DataStore<Preferences>)` — собственные ключи (те же 10 имён), дефолт nav-кнопок из `PlatformDefaultShowSongNavButtons` (mobile = false); `update` — атомарно в одном `edit`, пишет **только изменившиеся** ключи (как старые сеттеры: нетронутые настройки остаются отсутствующими, бэкап не обрастает дефолтами). Koin: `preferencesModule` в `PwsComposeApplication` сразу после `featuresModule`, берёт `get<DataStore<Preferences>>()` (= `appSettingsDataStore()`), второго DataStore нет; относительный порядок §6 п.4 не менялся. Старые `Context.xxx()` не тронуты.
- Тест совместимости `DataStoreUserPreferencesRepositoryTest` (Robolectric, реальный `appSettingsDataStore()`): пустой store = старые дефолты (10 полей); старый сеттер → репозиторий; репозиторий → старый flow + в файле только этот ключ; покрытие всех 10 ключей в обе стороны; Koin отдаёт адаптер, а не in-memory. Красным проверен: `"keep-screen-on"` → `"keep-screen-on2"` в адаптере ⇒ 2 сценария падают (`keep-screen-on expected:<true> but was:<false>`); откат.
- Отклонения: (1) тест на `sdk = [34]`, а не `[34, 37]` как `BackupManagerTest`: DataStore — файл, от SDK не зависит; SDK 37 в песочнице не поднимается (UnsatisfiedLinkError `libandroid_runtime.so` — то же, что у `BackupManagerTest [SDK 37]`), иначе +1 падение окружения на флейвор. Если владельцу нужен и 37 — одна правка аннотации. (2) detekt-baseline app-compose: ID `SpreadOperator` (`PwsComposeApplication`, полный текст `modules(...)`) обновлён на месте под добавленный `preferencesModule` — число записей 34 → 34. (3) ktlint-baseline перегенерирован из-за сдвига строк: features 2348 → 2347 (ушла запись старого `ThemeMode.kt`), domain 987 → 987, app-compose 128 → 128 (только сдвиги). (4) Для обхода конфликта ktlint `class-signature` (≤160 — в одну строку) и detekt `MaxLineLength` (120) у `SongTextPreferences` параметры с KDoc.
- Для 04.2: в `features` остался свой `FavoriteSortMode` (UI, те же имена) рядом с доменным `FavoritesSortMode` — унификация в объёме 04.2. Тест совместимости зовёт старые `Context.xxx()` — при их удалении в 04.2 тест придётся перевести на прямые ключи/сырой DataStore (сценарии «старое → новое» сохранить).
- Gate: pws-core `./gradlew build $(cat corex)` — BUILD SUCCESSFUL (fitness `domain`/`features` зелёные, `KNOWN_*` не трогал). pws-android: `testRuDebugUnitTest` 27 тестов / `testRustoreDebugUnitTest` 57 — падают только 3 `BackupManagerTest [SDK 37]` в каждом (UnsatisfiedLinkError), новый тест 5/5 в обоих; `assembleRuDebug assembleRustoreDebug ktlintCheck detekt` — BUILD SUCCESSFUL. Тесты гонялись по одному (`--max-workers=1`): в общем прогоне с assemble Robolectric падал на «No space left on device» в /tmp (почищены `/tmp/karma-*`, `/tmp/sqlite-*libsqlitejdbc.so`, `/tmp/robolectric-*`). Не закоммичено. Вопросов владельцу нет.

---

## Этап 04.2 — Экраны читают настройки через use case  · модель: sonnet

### Прочитать
- заметки этапа 04.1
- `pws-core/features/.../app/AppRoot.kt`, `theme/Theme.kt`, `theme/ThemeMode.kt`
- потребители `LocalSongDetailDisplaySettings`, `LocalFavoritesDisplaySettings`, `LocalThemeSettings`
  (`grep -rn "LocalSongDetailDisplaySettings\|LocalFavoritesDisplaySettings\|LocalThemeSettings" features/src`)
- `pws-android/.../MainActivity.kt`

### Решения этапа
- Настройки песни → поле `SongDetailUiState.Content` (или отдельный `StateFlow` делегата из 03.1);
  изменения — методы модели, вызывающие `UpdateAppPreferencesUseCase`.
- Настройки избранного → `FavoritesScreenModel` / `FavoritesUiState`.
- Тема: `AppRoot` сам наблюдает `ObserveAppPreferencesUseCase` (через маленькую `AppRootModel` или
  `koinInject` в корневом хосте — корень в allow-list) и применяет `AppTheme`. Пока значение не
  пришло, рисуется только фон `Surface` (как сейчас для `hasInstalledBooks == null`), чтобы не было
  вспышки светлой темы.
- `keepScreenOn` — платформенный эффект: `AppRoot` принимает `onKeepScreenOnChanged: (Boolean) -> Unit`,
  шелл ставит/снимает `FLAG_KEEP_SCREEN_ON` как сейчас.
- Удаляются: `SongDetailDisplaySettings`, `FavoritesDisplaySettings`, `ThemeSettings` и их
  `CompositionLocal`; параметры `AppRoot`: `themeMode`, `onThemeModeChange`, `useDynamicColor`,
  `onUseDynamicColorChange`, `keepScreenOn`, `onKeepScreenOnChange`, `songDetailDisplaySettings`,
  `favoritesDisplaySettings`. `Effect.ApplyTheme` в `SettingsScreenModel` заменяется прямым вызовом use case.
- В pws-android удаляются функции `Context.xxxFlow()/setXxx()` из `ThemePreferences.kt`; остаются
  ключи (их использует адаптер и `BackupManager`) и `appSettingsDataStore()`.

### Работа
1. Перевести SongDetail, Favorites, Settings, корень темы.
2. Сузить `AppRoot`, упростить `MainActivity`.
3. Тесты моделей: изменение настройки доходит до репозитория; значение из репозитория попадает в state.
4. Обновить `pws-android/AGENTS.md` §7 («DataStore → AppRoot» больше не действует) и §8.

### Проверки
```bash
./gradlew build      # оба репо
grep -rn "DisplaySettings\|LocalThemeSettings" ../pws-core/features/src ../pws-android/app-compose/src   # пусто
```

### Готово, когда
- `AppRoot` не принимает настроек отображения; `MainActivity` не читает DataStore напрямую.
- **Ручной прогон (владелец):** установить предыдущую релизную сборку, выставить нестандартные
  значения всех 10 настроек, обновить на сборку шага — все значения сохранились; сменить тему,
  масштаб, сортировку избранного — переживают перезапуск; бэкап → восстановление сохраняет тему.

### Заметки исполнителя
<!-- -->
