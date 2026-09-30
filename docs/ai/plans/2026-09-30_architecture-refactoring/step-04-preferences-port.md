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
<!-- -->

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
