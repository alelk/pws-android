# Шаг 08 — Navigation 3 + ViewModel

> **ОТЛОЖЕН (F3, 2026-09-30).** Оркестратор этот шаг не запускает. Текст сохранён как готовая
> заготовка: вернуться после шага 09 или когда Navigation 3 выйдет из alpha. Перед возобновлением
> перечитать файл целиком — шаги 03–05 и 09 изменят код, на который он ссылается.

> Перед чтением этапа: [README](README.md) §4, §6, §7.2, §7.3. **Ручной прогон перед вливанием.**
> Требует F3 = «да». Опорные скиллы (актуальные, из `engineering-ai-skills`): `compose-navigation3`,
> `compose-multiplatform-ui` (`references/state-viewmodel.md`, `feature-module-structure.md`).

**Цель.** Навигация — Navigation 3 (`NavKey`-маршруты, back stack как список, `NavDisplay`);
state holder экрана — мультиплатформенный `androidx.lifecycle.ViewModel`, привязанный к записи стека.
Voyager и модуль `:core:navigation` удаляются.

**Почему.** Voyager в проекте закреплён на версии `2.2.21-1.10.3`, привязанной к CMP 1.10.3, тогда
как `:features` собирается на CMP 1.11.1; собственные актуальные скиллы владельца уже описывают
Navigation 3 + `ViewModel`; параметры экранов сегодня ездят строками (`songNumberIdString`,
`bookIdString`, `tagIdString`) и парсятся в экране.

**Мина шага.** (1) Пустой back stack: `NavDisplay` бросает исключение — каждый pop обязан быть
защищён `size > 1`. (2) Потеря состояния вкладок: у каждой из 4 вкладок свой стек; «повторный тап
по вкладке» сначала возвращает к корню, затем шлёт событие reselect — это поведение легко потерять.
(3) R8: сериализаторы маршрутов и ViewModel'и, создаваемые Koin'ом, вырезаются в release.
Защита: чистые функции операций над стеком с тестами (включая «pop на последнем элементе — no-op»);
тест reselect-поведения; обязательная release-сборка [R8] и прогон Maestro на ней.

**Вне объёма.** Изменение состава экранов и переходов; дизайн; web-история/URL (нет web-шелла).

---

## Этап 08.1 — Spike и ADR  · модель: opus

### Прочитать
- `compose-navigation3/SKILL.md` и `references/*`
- `pws-core/gradle/libs.versions.toml`, `features/build.gradle.kts`
- `features/.../app/AppRoot.kt`, `di/ScreenModule.kt`, `core/navigation/src/commonMain/`

### Решения этапа
- Spike — в отдельной ветке, в код `features` не вливается.
- Проверить на **всех целях `:features`** (android, jvm, iosArm64, iosSimulatorArm64, js): доступные
  версии `org.jetbrains.androidx.navigation3:navigation3-ui`,
  `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3`, `io.insert-koin:koin-compose-viewmodel`,
  совместимые с текущими Kotlin/CMP; собрать минимальный пример: 2 вкладки с отдельными стеками,
  экран с параметром, `koinViewModel { parametersOf(...) }`, сохранение стека при пересоздании.
- API nav3 — alpha и плавает: **имена из скилла сверить с фактической версией**, расхождения записать.
- Критерии «зелёного» spike: собирается на всех целях; ViewModel очищается при удалении записи;
  стек переживает пересоздание Activity; release-сборка с R8 работает.
- Если цель js или ios не поддержана — варианты в ADR: (а) отложить шаг; (б) сузить цели `:features`
  (решение владельца). Исполнитель не выбирает сам.

### Работа
1. Spike по критериям.
2. ADR `pws-core/docs/adr/0001-navigation3-viewmodel.md`: контекст, варианты, решение, точные версии
   артефактов, найденные расхождения API, keep-правила R8 (точечные), план отката.
3. Вердикт в заметках: `go` / `no-go` + причина.

### Проверки
- ADR существует; версии в нём проверены сборкой spike.

### Готово, когда
- Вердикт записан. При `no-go` оркестратор помечает шаг 08 `waiting` и идёт к шагу 09 / завершает.

### Заметки исполнителя
<!-- -->

---

## Этап 08.2 — Маршруты, NavDisplay, вкладки  · модель: opus

### Прочитать
- ADR из 08.1 и заметки
- `features/.../app/AppRoot.kt`, `components/NavigationBar.kt` (`NavDestination`, `TabReselectEvents`)
- `core/navigation/.../SharedScreens.kt`, `features/.../di/ScreenModule.kt`
- `features/.../telemetry/TelemetryCompose.kt` (`TrackScreenViews`)

### Решения этапа
- `features/.../app/AppRoute.kt`: `@Serializable sealed interface AppRoute : NavKey`; параметры —
  типизированные id домена (сериализуемые value-классы), а не строки. По одному маршруту на каждый
  сегодняшний экран из `SharedScreens` + онбординг.
- `SongDetailScreen` и `SongDetailBySongIdScreen` сливаются в один маршрут
  `SongDetail(target: SongTarget)` (делегат из этапа 03.1 уже общий).
- `features/.../app/NavStacks.kt` — чистые функции над стеками: `push`, `pop` (no-op при size == 1),
  `switchTab` (никогда не через пустой стек), `reselectTab` (есть вложенные → к корню; иначе → событие
  reselect). Покрыты тестами.
- Четыре вкладки (`Home`, `Books`, `Search`, `Library`), у каждой свой сохраняемый стек.
- Экраны получают **лямбды навигации** (`onOpenSong`, `onBack`), а не навигатор.
- Телеметрия `screen_view`: тот же набор имён экранов, что даёт сегодня `TrackScreenViews` (G12) —
  таблица «маршрут → имя» с тестом.
- Переход постепенный: в этом этапе новый каркас живёт за флагом сборки/константой
  (`USE_NAV3 = false`) рядом со старым; по умолчанию работает Voyager. Экраны на этом этапе не
  переписываются — записи `entryProvider` могут временно оборачивать существующий контент.

### Работа
1. Маршруты + сериализация (`SavedStateConfiguration`, полиморфизм без рефлексии).
2. Чистые операции над стеками + тесты (включая мину №1 и №2).
3. `AppShell` с `NavDisplay`, нижней панелью и декораторами (saveable state, ViewModel store).
4. Таблица имён экранов для телеметрии + тест паритета со старым набором.

### Проверки
```bash
./gradlew :features:jvmTest && ./gradlew build
```

### Готово, когда
- Каркас собирается на всех целях и покрыт тестами; приложение по умолчанию работает как раньше.

### Заметки исполнителя
<!-- -->

---

## Этап 08.3 — Экраны: ScreenModel → ViewModel  · модель: sonnet
> Выполняется несколькими сессиями — **по одному семейству экранов на сессию**, в порядке:
> (a) Books, BookSongs, BookLibrary → (b) Favorites, History → (c) Search, SearchResults →
> (d) Tags, TagSongs → (e) Settings, Onboarding → (f) Home, Library → (g) SongDetail, SongEdit.
> Оркестратор запускает отдельного сабагента на каждую букву и ведёт под-статусы в README.

### Прочитать
- заметки 08.2; `compose-multiplatform-ui/references/state-viewmodel.md`
- файлы своего семейства: `XxxScreen.kt`, `XxxScreenModel.kt`, `XxxUiState.kt`, тест модели

### Решения этапа
- `XxxScreenModel : StateScreenModel<S>` → `XxxViewModel : ViewModel()` со
  `StateFlow<S>` и `viewModelScope`. Логика и `UiState` не меняются. Имя файла и класса меняется на
  `XxxViewModel`.
- Регистрация: `viewModel { (id: BookId) -> BookSongsViewModel(id, get()) }` / `viewModelOf(::…)`.
  Получение: `koinViewModel<T> { parametersOf(...) }` в `XxxEntry`.
- Экран: `XxxEntry(route-параметры, лямбды навигации)` + `XxxContent(state, onAction)`;
  класс `Screen` Voyager для семейства удаляется, запись в `entryProvider` зовёт `XxxEntry`.
- Параметр `coroutineScope` для тестов заменяется стандартным `Dispatchers.setMain` +
  `runTest` (или инъекцией `CoroutineDispatcher`), тесты переносятся без изменения ожиданий.
- Пока не переведены все семейства, `USE_NAV3` остаётся `false` в `main`; сабагент проверяет своё
  семейство с `USE_NAV3 = true` локально. Последняя сессия (g) переключает флаг в `true`.

### Работа (на семейство)
1. ViewModel + регистрация + Entry/Content.
2. Перенести тесты модели.
3. Прогнать приложение с `USE_NAV3 = true`: вход на экран, назад, смена вкладки и возврат (состояние
   сохранено), поворот.

### Проверки
```bash
./gradlew :features:jvmTest && ./gradlew build
cd ../pws-android && ./gradlew :app-compose:assembleRuDebug
```

### Готово, когда
- Семейство работает на nav3; тесты моделей зелёные без правки ожиданий.

### Заметки исполнителя
<!-- по блоку на каждое семейство (a)…(g) -->

---

## Этап 08.4 — Удалить Voyager  · модель: sonnet · [R8]

### Прочитать
- заметки 08.1–08.3; `features/build.gradle.kts`, `pws-android/app-compose/build.gradle.kts`,
  `PwsComposeApplication.kt` (`ScreenRegistry`), `pws-android/settings.gradle.kts` (подстановки composite build)
- `app-compose/proguard-rules.pro`

### Решения этапа
- Удаляются: зависимости `voyager-*` из обоих каталогов и build-файлов; `di/ScreenModule.kt`;
  `ScreenRegistry { … }` в Application; модуль `:core:navigation` (и его подстановка в
  `pws-android/settings.gradle.kts`); флаг `USE_NAV3`; keep-правила Voyager.
- Keep-правила для nav3/сериализации маршрутов — только точечные, из ADR (G13).
- `AppStartupModel` (шаг 05) становится `ViewModel` уровня Activity.
- Скиллы в `.claude/skills` обоих репо: удалить `voyager-navigation`, обновить
  `compose-multiplatform-ui` до актуального, добавить `compose-navigation3`. Обновить `AGENTS.md`
  (§2 toolchain, §3 архитектура, §5 раскладка фичи, §7 образцы, §8 правила), `CLAUDE.md`.
- Fitness-тесты: `ScreenModelPurityTest` и др. переименовать/перенастроить на `*ViewModel.kt`;
  добавить `ScreenSurfaceTest` (набор маршрутов закреплён; у каждого `Entry` есть `Content`).

### Работа
1. Удаления и обновления по списку.
2. Release-сборки `ru` и `rustore` с R8; сравнить размер DEX с предыдущим релизом
   (`python3 tools/dex-report.py <apk>`) — записать в заметки.

### Проверки
```bash
grep -rni "voyager" --include=*.kt --include=*.kts --include=*.toml --include=*.pro ../pws-core ../pws-android | grep -v "/build/\|/docs/"   # пусто
./gradlew build      # оба репо, + [R8]
```

### Готово, когда
- Voyager отсутствует в обоих репо; release-сборки работают.
- **Ручной прогон (владелец):** Maestro `./e2e/scripts/run-local.sh --flavor ru` на release-сборке;
  вручную: все 4 вкладки, глубокий переход песня → ссылка → песня → назад ×3, повторный тап по
  вкладке, поворот на каждом экране, сворачивание с «Don't keep activities» — стек и состояние
  восстановлены.

### Заметки исполнителя
<!-- -->
