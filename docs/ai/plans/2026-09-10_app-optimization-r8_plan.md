# Оптимизация приложения: R8 shrinking/obfuscation, размер DEX и APK

> **Статус:** ГОТОВ К РЕАЛИЗАЦИИ — 2026-09-10
> **Ветка:** next (работать в отдельной ветке `feat/r8-optimization`)
> **Повод:** Google Play Console → App Bundle Explorer → «App optimization: **Low**»,
> Optimization 18% · Obfuscation 18% · Shrinking 19% · **Total uncompressed DEX size 36.8 MB**.
> **Связано:** `2026-08-07_crash-reporting-and-analytics_plan.md` (mapping.txt → AppMetrica),
> `2026-07-27_rustore-build-variant_plan.md` (flavor `rustore` — SDK платежей, зона риска).

---

## 0. TL;DR для исполнителя

**Одна главная причина всех трёх низких процентов** — файл
[`app-compose/proguard-rules.pro`](../../../app-compose/proguard-rules.pro): в нём **11 правил**
вида
`-keep class <пакет>.** { *; }` — `kotlin`, `kotlinx`, `androidx.compose`, `androidx.datastore`,
`androidx.security.crypto`, `org.koin`, `cafe.adriel.voyager`, `net.zetetic.database` и три наших
пакета (`pws.domain`, `pws.features`, `pws.portable`). Такое правило запрещает R8 и удалять, и
переименовывать классы. R8 включён и работает в Full Mode — ему просто **запрещено** что-либо
делать.

Самый дорогой побочный эффект: `-keep class androidx.compose.** { *; }` тащит в APK
**11 400 классов иконок** `androidx.compose.material.icons` (~9.3 МБ кода + 0.7 МБ строк ≈ **27%
всего
DEX**), из которых реально используется **ровно одна** — `Icons.Filled.Favorite`.

Порядок работы: **Phase A** (замер) → **Phase B** (переписать proguard-rules.pro) → **Phase C**
(починить и проверить) — это ~90% выигрыша. Дальше D–G по убыванию ценности.

**Главное правило исполнителя:** если после Phase B что-то сломалось — **НЕЛЬЗЯ** чинить это
возвратом широкого `-keep class X.** { *; }`. Нужно найти конкретный класс/член и добавить точечное
правило с комментарием «зачем». См. §7 «Дерево решений при поломке».

---

## 1. Цель и целевые метрики

| Метрика (Play Console)      | Сейчас      | Цель             | Стретч-цель |
|-----------------------------|-------------|------------------|-------------|
| Total uncompressed DEX size | **36.8 MB** | **≤ 12 MB**      | ≤ 9 MB      |
| Obfuscation percentage      | **18%**     | **≥ 85%**        | ≥ 92%       |
| Shrinking percentage        | **19%**     | **≥ 60%**        | ≥ 70%       |
| Optimization percentage     | **18%**     | **≥ 80%** (High) | ≥ 90%       |
| Классов в DEX               | 32 087      | ≤ 13 000         | ≤ 10 000    |
| Методов в DEX               | 151 693     | ≤ 90 000         | ≤ 70 000    |
| APK `ru` (universal)        | 21.1 MB     | ≤ 12 MB          | ≤ 10 MB     |
| APK `uk` (с seed-книгами)   | 26.6 MB     | ≤ 17 MB          | ≤ 15 MB     |

Что **не** является целью: менять `minSdk`, `targetSdk`, набор ABI под Play (AAB и так режет
по ABI), удалять функциональность, ломать формат бэкапов/бандлов.

---

## 2. Факты разведки (измерено на `output/compose/pws-app-release-3.7.0-uk.apk`)

### 2.1. Из чего состоит APK

```
classes.dex  21.1 MB ┐
classes2.dex  7.7 MB ├─ 37.18 MB несжатого DEX  (Play показывает 36.8 MB — это оно)
classes3.dex  8.4 MB ┘
lib/          7.4 MB   (SQLCipher .so × 4 ABI; в AAB Play режет по ABI — не проблема)
assets/       5.3 MB   (5.1 MB — seed-книги флейвора uk, это контент, трогать нельзя)
res/          1.4 MB
resources.arsc 0.9 MB
```

### 2.2. Секции DEX — куда уходят 37 МБ

| Секция                   | Размер   | Комментарий                                                          |
|--------------------------|----------|----------------------------------------------------------------------|
| `code`                   | 18.73 MB | **9.25 MB из них — иконки** `androidx.compose.material.icons`        |
| `string_data`            | 8.56 MB  | 175 475 строк — имена классов/методов/полей, т.к. **нет обфускации** |
| `annotation` + directory | 3.80 MB  | kotlin.Metadata и пр. — не вычищаются, потому что всё «kept»         |
| `method_id`+`class_def`  | 2.36 MB  | прямое следствие 32 087 классов / 151 693 методов                    |

### 2.3. Топ пакетов по объёму кода

| Пакет                             | Классов    | Код, MB  | Что это                                             |
|-----------------------------------|------------|----------|-----------------------------------------------------|
| `androidx.compose.material.icons` | **11 400** | **9.25** | **Мусор.** Используется 1 иконка из ~11 400         |
| `androidx.compose.foundation`     | 2 441      | 1.44     | kept целиком, реально нужна часть                   |
| `androidx.compose.material3`      | 1 458      | 1.42     | kept целиком, компоненты M3 используются частично   |
| `androidx.compose.ui`             | 2 197      | 1.40     | kept целиком                                        |
| `io.github.alelk.*`               | 1 652      | 1.13     | наш код — kept целиком и **не обфусцирован**        |
| `io.appmetrica.analytics`         | 2 283      | 0.72     | свои consumer-правила, ок                           |
| `androidx.datastore.preferences`  | 564        | 0.55     | 523 из них — встроенный protobuf-lite, kept целиком |

### 2.4. Доказательство, что R8 работает — просто ему запретили

Библиотека иконок **Lucide** (`com.composables:icons-lucide`, ~1500 иконок) **не покрыта** ни одним
`-keep`-правилом. В APK от неё осталось **39 классов** — ровно те, что используются. Значит
shrinking
физически работает; ровно то же произойдёт с material-иконками и Compose, как только уберём keep.

### 2.5. Что в коде реально требует keep-правил (проверено grep'ом)

| Механизм                       | Где                                                                                | Нужно keep?                                           |
|--------------------------------|------------------------------------------------------------------------------------|-------------------------------------------------------|
| JNI                            | SQLCipher `System.loadLibrary("sqlcipher")`                                        | **Да** — `net.zetetic.database.**` (всего 65 классов) |
| Reflection на generated-класс  | Room: `Room.databaseBuilder(ctx, PwsDatabase::class.java, …)` → `PwsDatabase_Impl` | **Да** — точечно                                      |
| Reflection по именам полей     | protobuf-lite внутри DataStore **и** внутри Tink (EncryptedSharedPreferences)      | **Да** — `<fields>` у `GeneratedMessageLite`          |
| kotlinx.serialization          | 114 файлов с `@Serializable`, kaml/Json                                            | **Да** — стандартные `-if`-правила для R8 full mode   |
| Имя класса как данные          | `TelemetryCompose.kt:31` → `it::class.simpleName` как имя экрана в AppMetrica      | **Да** — `-keepnames` только для Voyager `Screen`     |
| ServiceLoader                  | нет (Ktor создаётся как `HttpClient(CIO)` — явный движок)                          | Нет                                                   |
| Полиморфная сериализация       | `Reference` — но у всех подтипов явные `@SerialName("bible-ref"/"song-ref")`       | Нет (имена не зависят от FQCN)                        |
| Кастомные сериализаторы        | 16 шт., все с явными `PrimitiveSerialDescriptor("Color")` и т.п.                   | Нет                                                   |
| Java Serialization / Parcelize | не используется (`@Parcelize` — 0 совпадений)                                      | Нет                                                   |
| `kotlin-reflect`               | не подключён                                                                       | Нет                                                   |

### 2.6. Мёртвые зависимости

- `:data:db-android` → `implementation(libs.android.material)` — **не используется ни в одном файле
  модуля**. `com.google.android.material` нужен только легаси-модулю `:app`, который **отключён** в
  `settings.gradle.kts`. Тащит в APK 102 класса + ресурсы Material Components на все локали.
- `:app-compose` → `implementation(libs.appcompat)` — используется **только** ради родителя темы
  `Theme.AppCompat.DayNight.NoActionBar` в `app-compose/src/main/res/values/themes.xml`. Тащит
  `androidx.appcompat` + `androidx.fragment` (~350 классов) и большой пакет локализованных ресурсов.
- `pws-core:features` → `libs.compose.materialIconsExtended` — ради **одной** иконки
  `Icons.Filled.Favorite` (5 мест использования, см. §Phase D).

---

## 3. Принципы, которым подчинён весь план

1. **Ни одного нового `-keep class <пакет>.** { *; }`.** Широкий keep = отключённая оптимизация.
   Исключение ровно одно и оно обосновано: JNI-классы SQLCipher (65 классов).
2. **Каждое keep-правило сопровождается комментарием «зачем» и ссылкой на код.** Правило без
   объяснения через полгода никто не рискнёт удалить — так и появился текущий файл.
3. **Предпочитать `-keepnames` вместо `-keep`.** `-keepnames` = «не переименовывай», но R8 всё ещё
   вправе удалить неиспользуемое. `-keep` = «не трогай вообще».
4. **Сначала правила библиотек, потом свои.** Большинство библиотек (coroutines, Room, AppMetrica,
   Compose, Koin) уже поставляют consumer-правила внутри артефактов — дублировать их не нужно.
5. **Изменения по одной фазе, с замером после каждой.** Смешивать Phase B и Phase D нельзя —
   иначе непонятно, что сломалось.
6. **Каждый шаг проверяется сборкой ВСЕХ четырёх флейворов** (`ru`, `uk`, `full`, `rustore`), а не
   только `ru`.

---

## 4. Фазы и задачи

### Phase A — Замер до изменений (обязательно, ~15 минут)

- **T-A01** — Инструмент замера **уже создан**: [
  `tools/dex-report.py`](../../../tools/dex-report.py)
  (чистый Python 3, без зависимостей). Проверить, что работает:
  ```bash
  python3 tools/dex-report.py output/compose/pws-app-release-3.7.0-uk.apk --top 15
  ```
  Он печатает: несжатый размер DEX, число классов/методов, разбивку по секциям, оценку
  «Obfuscation percentage» и топ пакетов. Есть режим сравнения: `--diff BEFORE.apk AFTER.apk`.

- **T-A02** — Собрать **baseline** релизных APK из текущего кода и сохранить их вне `build/`:
  ```bash
  ./gradlew :app-compose:assembleRuRelease :app-compose:assembleUkRelease \
            :app-compose:assembleRustoreRelease
  mkdir -p /tmp/pws-baseline
  cp app-compose/build/outputs/apk/ru/release/*.apk      /tmp/pws-baseline/ru-before.apk
  cp app-compose/build/outputs/apk/uk/release/*.apk      /tmp/pws-baseline/uk-before.apk
  cp app-compose/build/outputs/apk/rustore/release/*.apk /tmp/pws-baseline/rustore-before.apk
  ```
  > **Если нет релизного keystore** (`android.release.keystorePath` не задан) — AGP всё равно
  > соберёт APK, но с именем `app-compose-ru-release-unsigned.apk`. Для замера DEX подписи не нужны;
  > `cp .../*.apk` покроет оба варианта имени. Для установки на устройство подпись нужна — см. §8.

- **T-A03** — Записать baseline-цифры в комментарий к задаче/PR: DEX MB, классы, методы,
  обфускация %, размер APK каждого флейвора. Без этого нечего будет предъявить в конце.

- **T-A04** — Сохранить диагностические артефакты R8 текущей сборки для сравнения:
  ```bash
  cp -r app-compose/build/outputs/mapping/ruRelease /tmp/pws-baseline/mapping-ru-before
  ```
  Полезные файлы внутри: `mapping.txt` (что во что переименовано), `usage.txt` (что удалено),
  `seeds.txt` (что удержано keep-правилами — **вот главный доказательный файл**),
  `configuration.txt` (итоговая конфигурация R8 со всеми consumer-правилами библиотек).

---

### Phase B — Переписать `app-compose/proguard-rules.pro` (ядро плана)

- **T-B01** — Создать рабочую ветку: `git checkout -b feat/r8-optimization`.

- **T-B02** — **Полностью заменить** содержимое `app-compose/proguard-rules.pro` текстом из
  **§5 настоящего плана**. Не «поправить пару строк», а заменить файл целиком — старые правила
  взаимно перекрываются, точечная правка приведёт к путанице.

- **T-B03** — Убедиться, что доступны диагностические артефакты R8 — без них Phase C делать вслепую.
  Правок в `build.gradle.kts` не требуется: AGP кладёт `mapping.txt`, `seeds.txt`, `usage.txt`,
  `configuration.txt` и (при ошибках) `missing_rules.txt` в
  `app-compose/build/outputs/mapping/<variant>/`. Задача — **знать про эти файлы и читать их**
  (см. Phase C и §7). Проверить после первой релизной сборки:
  ```bash
  ls -la app-compose/build/outputs/mapping/ruRelease/
  ```
  Если каких-то файлов нет — дописать в конец `proguard-rules.pro` (пути относительны корня
  модуля `app-compose`):
  ```proguard
  -printconfiguration build/outputs/mapping/r8-configuration.txt
  -printusage         build/outputs/mapping/r8-usage.txt
  -printseeds         build/outputs/mapping/r8-seeds.txt
  ```
  Эти три строки — диагностические; их можно оставить навсегда, на размер APK они не влияют.

- **T-B04** — Вынести keep-правила RuStore Pay SDK **в отдельный файл только для флейвора
  `rustore`**, чтобы Google-Play-сборки за них не платили. В `app-compose/build.gradle.kts`,
  в `productFlavors { create("rustore") { … } }` добавить:
  ```kotlin
  // Платёжный SDK RuStore живёт только в этом флейворе — и его keep-правила тоже.
  // Флейворы ru/uk/full не должны платить размером за чужие правила.
  proguardFiles("proguard-rules-rustore.pro")
  ```
  и создать `app-compose/proguard-rules-rustore.pro` с содержимым из **§5.2**.

- **T-B05** — Собрать `ru` release и посмотреть, что вышло:
  ```bash
  ./gradlew :app-compose:assembleRuRelease
  python3 tools/dex-report.py app-compose/build/outputs/apk/ru/release/*.apk --top 20
  ```
  Ожидание после только Phase B: DEX **~10–14 MB**, классов **~11 000–15 000**, обфускация
  **80–95%**. Если DEX всё ещё > 20 MB — значит какое-то правило осталось широким; открыть
  `build/outputs/mapping/ruRelease/seeds.txt` и найти, что именно удерживает 30 тысяч классов.

---

### Phase C — Починка и проверка (итеративно, самая долгая фаза)

- **T-C01 — Missing classes.** AGP падает с `Missing class …` если R8 не нашёл класс, на который
  есть ссылка. Готовые `-dontwarn` он же и предлагает в
  `app-compose/build/outputs/mapping/<variant>/missing_rules.txt`.
  **Порядок действий:** открыть этот файл → скопировать строки в конец `proguard-rules.pro`
  в секцию `# ── -dontwarn: опциональные зависимости, которых нет на Android ──` → **добавить
  комментарий, откуда взялось**. Копировать **только** `-dontwarn`, никогда не `-keep`.

- **T-C02 — Юнит-тесты.**
  ```bash
  ./gradlew :data:db-android:testRuDebugUnitTest :data:content-delivery:check :app-compose:check
  ```
  Тесты идут на debug-варианте (не минифицированном), поэтому они **не поймают** ошибок R8 — но
  обязаны остаться зелёными, чтобы отделить «сломал код» от «сломал правила».

- **T-C03 — Сборка всех флейворов.** Ни один из них не должен падать на этапе `minify…Release`:
  ```bash
  ./gradlew :app-compose:assembleRuRelease :app-compose:assembleUkRelease \
            :app-compose:assembleFullRelease :app-compose:assembleRustoreRelease
  ```

- **T-C04 — Ручной smoke на устройстве (обязателен, автотестами не заменяется).**
  Установить релизную сборку и пройти сценарии. Каждый пункт списка бьёт по конкретному риску
  из §6 — не сокращать:

  | # | Сценарий                                                              | Что проверяет                          |
          |---|-----------------------------------------------------------------------|----------------------------------------|
  | 1 | Первый запуск на чистом устройстве: онбординг → установка сборника     | Ktor, kaml, импорт в Room, Tink/Keystore|
  | 2 | **Обновление поверх старой версии** (установить 3.7.0 из Play → накатить новую) | ⚠️ Ключ БД в EncryptedSharedPreferences, миграции Room |
  | 3 | Открыть песню, пролистать, изменить размер шрифта, сменить тему        | DataStore (protobuf-lite)              |
  | 4 | Добавить/убрать избранное, создать тег, назначить тег                  | Room DAO, генерируемый код             |
  | 5 | Редактирование песни, потом бэкап → восстановление из файла            | kotlinx.serialization + kaml            |
  | 6 | Поиск, история, переходы по вкладкам, кнопка «назад»                   | Voyager, Koin                          |
  | 7 | Установка/удаление сборника из библиотеки                              | content-delivery, сеть, дешифровка     |
  | 8 | `rustore`-флейвор: экран платежа открывается и покупка проходит        | ⚠️ RuStore Pay SDK                     |

- **T-C05 — E2E Maestro** (если есть устройство/эмулятор). Прогнать полный набор против
  **релизной** (минифицированной) сборки — обычно он гоняется по debug, здесь важен именно release:
  ```bash
  ./e2e/scripts/run-compose.sh --flavor ru --full --clean --retries 1 \
    --apk app-compose/build/outputs/apk/ru/release/app-compose-ru-release.apk
  ```

- **T-C06 — Телеметрия не деградировала.** Собрать release, вызвать искусственный краш, проверить
  в консоли AppMetrica (после загрузки `mapping.txt` из `output/appmetrica-mapping/`):
    - стек-трейс читаемый (работает `-keepattributes SourceFile,LineNumberTable`);
    - событие `screen_view` приходит с **человеческим** именем экрана (`SongDetailScreen`, а не
      `a`) —
      это проверка правила `-keepnames … Screen` из §5.
      Задача `./gradlew :app-compose:stageAppMetricaMappingRuRelease` кладёт маппинг в
      `output/appmetrica-mapping/`.

- **T-C07 — Финальный замер и сравнение:**
  ```bash
  python3 tools/dex-report.py --diff /tmp/pws-baseline/ru-before.apk \
      app-compose/build/outputs/apk/ru/release/app-compose-ru-release.apk
  ```

---

### Phase D — Выкинуть `material-icons-extended` из `pws-core` (−11 400 классов детерминированно)

> Формально после Phase B R8 уже удалит неиспользуемые иконки. Но зависимость всё равно стоит
> убрать: она замедляет компиляцию и merge-ресурсов, и любое случайное широкое keep-правило в
> будущем моментально вернёт 10 МБ. Работа идёт в репозитории **`../pws-core`**.

- **T-D01** — Создать
  `pws-core/features/src/commonMain/kotlin/io/github/alelk/pws/features/icons/FilledHeart.kt`:
  ```kotlin
  package io.github.alelk.pws.features.icons

  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.graphics.SolidColor
  import androidx.compose.ui.graphics.vector.ImageVector
  import androidx.compose.ui.graphics.vector.path
  import androidx.compose.ui.unit.dp

  /**
   * Залитое сердце (Material «Favorite») — локальная копия одного вектора.
   *
   * Единственная иконка, которую мы брали из `material-icons-extended`: в Lucide (основной набор,
   * обводочный) залитого сердца нет. Ради одного пути тянуть библиотеку из ~11 400 классов
   * не имеет смысла — см. docs/ai/plans/2026-09-10_app-optimization-r8_plan.md.
   */
  val FilledHeart: ImageVector by lazy {
    ImageVector.Builder(
      name = "FilledHeart",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply {
      path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 21.35f)
        lineToRelative(-1.45f, -1.32f)
        curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
        curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
        curveToRelative(1.74f, 0f, 3.41f, 0.81f, 4.5f, 2.09f)
        curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
        curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
        curveToRelative(0f, 3.78f, -3.4f, 6.86f, -8.55f, 11.54f)
        lineTo(12f, 21.35f)
        close()
      }
    }.build()
  }
  ```
  > Путь скопирован 1:1 из `Icons.Filled.Favorite`, размер и viewport совпадают — визуально иконка
  > не изменится. `fill = SolidColor(Color.Black)` — так же, как в Material: реальный цвет задаёт
  > `Icon(tint = …)` на месте использования.

- **T-D02** — Заменить использования в 5 файлах `pws-core/features/src/commonMain/.../`:

  | Файл                                       | Строки           |
    |--------------------------------------------|------------------|
  | `components/DonationPromptDialog.kt`       | 3–4, 35          |
  | `components/SongListItem.kt`               | 18–19, 171, 235, 372 |
  | `components/DonationPromptCard.kt`         | 10–11, 66        |
  | `song/detail/SongDetailScreen.kt`          | 17–18, 364       |
  | `settings/SettingsScreen.kt`               | 17–18, 568       |

  В каждом: удалить `import androidx.compose.material.icons.Icons` и
  `import androidx.compose.material.icons.filled.Favorite`, добавить
  `import io.github.alelk.pws.features.icons.FilledHeart`, заменить `Icons.Filled.Favorite` →
  `FilledHeart`.

- **T-D03** — Удалить строку `implementation(libs.compose.materialIconsExtended)` из
  `pws-core/features/build.gradle.kts` (блок `commonMain.dependencies`) и обновить комментарий над
  `libs.compose.iconsLucide` — про material-иконки там больше нет правды.

- **T-D04** — Удалить алиас `compose-materialIconsExtended` из `pws-core/libs.versions.toml`
  (строка 88) и версию `composeMaterialIcons` из секции `[versions]`, **если** она больше нигде не
  используется (проверить `grep -rn "composeMaterialIcons" pws-core/`).

- **T-D05** — Проверить, что material-иконок больше нет нигде:
  ```bash
  grep -rn "androidx.compose.material.icons" ../pws-core --include="*.kt" | grep -v "/build/"
  ```
  Должно быть пусто. Собрать: `./gradlew :app-compose:assembleRuDebug` и `assembleRuRelease`,
  визуально проверить сердечко в списке песен / на экране песни / в настройках.

---

### Phase E — Убрать мёртвые UI-зависимости

- **T-E01** *(низкий риск, делать)* — В `data/db-android/build.gradle.kts` удалить
  `implementation(libs.android.material)`. Модуль не использует ни одного класса Material Components
  (проверено grep'ом); зависимость осталась от легаси-модуля `:app`.
  Проверка: `./gradlew :data:db-android:assembleRuRelease :app-compose:assembleRuRelease`.

- **T-E02** *(средний риск, опционально)* — Избавиться от `androidx.appcompat`. Он нужен только как
  родитель темы. Шаги:
    1. `app-compose/src/main/res/values/themes.xml`:
       ```xml
       <style name="Theme.PwsCompose" parent="android:Theme.Material.Light.NoActionBar">
         <item name="android:windowBackground">@color/splash_background</item>
         <item name="android:statusBarColor">@android:color/transparent</item>
         <item name="android:windowTranslucentStatus">false</item>
         <item name="android:windowTranslucentNavigation">false</item>
       </style>
       ```
    2. `values/colors.xml` → `<color name="splash_background">#FFFFFF</color>`;
       создать `values-night/colors.xml` → `<color name="splash_background">#121212</color>`.
    3. Удалить `implementation(libs.appcompat)` из `app-compose/build.gradle.kts`.
    4. Проверить, что сборка компилируется и **нет белой вспышки** при холодном старте в тёмной теме
       (`MainActivity` наследуется от `ComponentActivity`, AppCompat ему не нужен — проверено).
  > Если что-то пойдёт не так — откатить только этот пункт, он независим от остальных.

---

### Phase F — Прочее (опционально, по убыванию ценности)

- **T-F01 — Ограничить локали ресурсов.** В `app-compose/build.gradle.kts`, блок `android { }`:
  ```kotlin
  androidResources {
    // Библиотечные строки (AndroidX и пр.) переводятся на ~70 языков — в APK нужны только наши.
    // Список синхронизирован с pws-core/features/src/commonMain/composeResources/
    // (values = en, values-pl, values-ru, values-uk).
    localeFilters += listOf("en", "pl", "ru", "uk")
  }
  ```
  Перед применением **перепроверить фактический список локалей**:
  `ls ../pws-core/features/src/commonMain/composeResources/` — на 2026-09-10 это
  `values` (en), `values-pl`, `values-ru`, `values-uk`. Локали Compose-ресурсов лежат в
  `assets/composeResources` и под `localeFilters` не попадают, но списки обязаны совпадать —
  иначе получится системный UI на английском при русских строках песен.
  > В AGP 9 API называется `localeFilters`; в AGP 8 это `defaultConfig.resourceConfigurations`.
  > Проект на AGP 9.2.1 — использовать `localeFilters`.

- **T-F02 — `android:largeHeap="true"`** в `app-compose/src/main/AndroidManifest.xml`. Play в том же
  разделе жалуется на «memory usage». Флаг ставился во времена, когда БД целиком лежала в assets;
  сейчас книги ставятся в рантайме. **Не удалять вслепую:** сначала измерить пик памяти при импорте
  самого большого сборника (`PV3300`) через Android Studio Profiler на устройстве с 2 ГБ ОЗУ. Если
  пик < 128 МБ — убрать флаг и проверить импорт ещё раз. Если ≥ — оставить и записать в комментарий
  в манифесте, почему он нужен.

- **T-F03 — Baseline Profile.** В APK уже есть `assets/dexopt/` (профили, приехавшие из библиотек
  Compose). Свой профиль под наши экраны даёт обычно 15–30% к времени холодного старта. Это
  отдельная задача: модуль `:baselineprofile` с `androidx.baselineprofile` плагином +
  Macrobenchmark.
  На метрики «App optimization» **не влияет** — делать только после Phase B–E.

- **T-F04 — ABI `x86`.** В universal-APK 2.2 МБ занимают нативные библиотеки под 32-битный x86,
  которого нет ни на одном реальном устройстве. Для AAB Play режет по ABI сам, так что **на
  пользователей это не влияет** — трогать только если мешает размер локальных APK. Решение —
  `ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }`. **Осторожно:** это сломает
  запуск на 32-битных x86-эмуляторах, которые могут использоваться в CI/e2e.

---

### Phase G — Защита от регрессии

- **T-G01** — Шапка нового `proguard-rules.pro` (уже включена в §5) содержит явный запрет на широкие
  keep-правила. Не удалять её.

- **T-G02** — В `.github/workflows/release-build.yml` после шага сборки добавить шаг-«гейт»:
  Флаг `--max-dex-mb N` в `tools/dex-report.py` **уже реализован**: печатает `::error::…` для
  GitHub Actions и выходит с кодом 1, если порог превышен. Шаг CI сводится к одной команде:
  ```yaml
  - name: DEX size gate
    run: |
      python3 tools/dex-report.py \
        app-compose/build/outputs/apk/ru/release/app-compose-ru-release.apk \
        --top 15 --max-dex-mb 15
  ```
  Порог 15 МБ выставить **после** того, как станет известен реальный результат Phase B (взять
  фактический размер + 25% запаса).

- **T-G03** — Дописать в `AGENTS.md` в раздел «Hard rules»:
  ```markdown
  - ❌ **Никогда не добавляй `-keep class <пакет>.** { *; }`** в
    `app-compose/proguard-rules.pro`. Такое правило отключает и shrinking, и обфускацию для целого
    дерева пакетов — именно так DEX однажды дорос до 36.8 МБ, а Google Play поставил
    «App optimization: Low». Любое keep-правило — точечное и с комментарием «зачем».
    См. `docs/ai/plans/2026-09-10_app-optimization-r8_plan.md`.
  ```

- **T-G04** — По завершении: обновить статус этого плана на «РЕАЛИЗОВАН», вписать фактические
  цифры «до/после» и добавить строку в `CLAUDE.md` в список активных планов.

---

## 5. Готовые файлы правил

### 5.1. `app-compose/proguard-rules.pro` — заменить целиком

```proguard
# =============================================================================
#  R8 / ProGuard — :app-compose
#
#  ⛔ ПРАВИЛО №1: НЕ ДОБАВЛЯТЬ `-keep class <пакет>.** { *; }`.
#
#  Такое правило запрещает R8 и удалять, и переименовывать целое дерево пакетов.
#  Именно из-за одиннадцати таких строк несжатый DEX дорос до 36.8 МБ, а Google Play
#  поставил «App optimization: Low» (обфускация 18%, shrinking 19%).
#  В частности `-keep class androidx.compose.** { *; }` тащил в APK 11 400 классов
#  иконок material-icons, из которых использовалась ровно одна.
#
#  Если после изменения кода что-то падает в release — НЕ чинить широким keep.
#  Найти конкретный класс/член:
#    build/outputs/mapping/<variant>/usage.txt   — что R8 удалил
#    build/outputs/mapping/<variant>/mapping.txt — во что переименовал
#    build/outputs/mapping/<variant>/seeds.txt   — что уже удерживается
#  и добавить точечное правило с комментарием «зачем».
#  Подробности: docs/ai/plans/2026-09-10_app-optimization-r8_plan.md
# =============================================================================

# ── Читаемые стек-трейсы ─────────────────────────────────────────────────────
# Нужны для деобфускации крэшей в AppMetrica (mapping.txt заливается вручную,
# задача stageAppMetricaMapping<Variant>). Стоят ~0.15 МБ — это дёшево.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Атрибуты, нужные kotlinx.serialization в R8 full mode ────────────────────
# Аннотации — чтобы -if-правила ниже вообще смогли увидеть @Serializable.
# Signature — для generic-типов (List<Song>, Map<String, Tag>).
# InnerClasses/EnclosingMethod СОЗНАТЕЛЬНО не держим: они стоят байтов в секции
# annotation на каждом вложенном классе и лямбде (а их тут десятки тысяч),
# а весь код десериализации вызывает сериализаторы явно (X.serializer()),
# без рефлексивного поиска по KType — проверено в
# portable-data/serialization/CatalogSerializer.kt и BundleSerializer.kt.
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# ── kotlinx.serialization ────────────────────────────────────────────────────
# Плагин генерирует сериализаторы статически, но R8 full mode вырезает
# Companion и synthetic `serializer()`, если на них нет прямой ссылки в коде.
# Правила официальные (kotlinx.serialization README, раздел «Android»).
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Страховка на совместимость форматов между версиями приложения.
# У @Serializable-класса без явного @SerialName serialName == FQCN. Сейчас все
# полиморфные подтипы (Reference → BibleRef/SongRef) имеют явные @SerialName, то
# есть переименование безопасно — но бэкапы и .book.yaml-бандлы должны читаться
# сборками разных версий, и цена этого правила ~10 КБ. R8 всё ещё вправе удалить
# неиспользуемые классы (-keepnames = -keep,allowshrinking).
-keepnames @kotlinx.serialization.Serializable class io.github.alelk.pws.**

# ── Room ─────────────────────────────────────────────────────────────────────
# PwsDatabaseProvider вызывает Room.databaseBuilder(ctx, PwsDatabase::class.java, …),
# а Room ищет сгенерированный PwsDatabase_Impl рефлексией по имени «<FQCN>_Impl».
# Плюс KMP-конструктор из @ConstructedBy(PwsDatabaseConstructor::class).
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class * implements androidx.room.RoomDatabaseConstructor { *; }

# ── protobuf-lite: DataStore и Tink ──────────────────────────────────────────
# ⚠️ Критично. protobuf-lite ищет поля сгенерированных сообщений РЕФЛЕКСИЕЙ ПО ИМЕНИ.
# Переименование полей ломает:
#   • DataStore  → пропадают настройки темы/шрифта;
#   • Tink       → не читается EncryptedSharedPreferences, а в ней лежит
#                  passphrase базы (KeyManager) → пользователь теряет доступ к БД.
# Держим только поля, сами классы R8 волен удалять и переименовывать.
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {
    <fields>;
}

# ── SQLCipher (JNI) ──────────────────────────────────────────────────────────
# Единственный осознанный широкий keep: нативный код sqlcipher.so зовёт эти
# Java-классы и их методы по именам. Цена — 65 классов (~0.06 МБ).
-keep class net.zetetic.database.** { *; }

# ── Voyager + телеметрия экранов ─────────────────────────────────────────────
# pws-core TrackScreenViews (features/telemetry/TelemetryCompose.kt:31) отправляет
# `it::class.simpleName` как имя экрана в AppMetrica. Без этого в аналитике будут
# «a», «b», «c». -keepnames не мешает удалять неиспользуемые экраны.
-keepnames class * implements cafe.adriel.voyager.core.screen.Screen

# ── Java Serialization ───────────────────────────────────────────────────────
# Стандартный шаблон: если класс сериализуется через java.io.Serializable, его
# служебные методы нельзя вырезать. У нас это касается Voyager Screen на JVM.
-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ── -dontwarn: опциональные зависимости, которых нет на Android ──────────────
# ⚠️ Сюда попадают ТОЛЬКО строки из build/outputs/mapping/<variant>/missing_rules.txt.
# Никогда не -keep. Каждую строку сопровождать комментарием, откуда она.

# Google Tink / errorprone (транзитив androidx.security:security-crypto)
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**

# kotlinx-coroutines-debug — только для JVM-отладки, на Android не бандлится
-dontwarn java.lang.instrument.**
-dontwarn sun.misc.**

# okhttp3 (транзитив AppMetrica) — опциональные TLS-провайдеры
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── Чего здесь СОЗНАТЕЛЬНО нет ───────────────────────────────────────────────
#  kotlin.** / kotlinx.**            — stdlib не требует keep; coroutines везёт
#                                      свои consumer-правила внутри артефакта.
#  androidx.compose.**               — Compose полностью совместим с R8;
#                                      именно этот keep стоил ~10 МБ иконок.
#  androidx.datastore.** (целиком)   — достаточно правила про protobuf-поля выше.
#  org.koin.**                       — Koin 4 резолвит по KClass, не по строкам.
#  cafe.adriel.voyager.** (целиком)  — достаточно -keepnames для Screen.
#  io.github.alelk.pws.**            — наш код обязан обфусцироваться, это 1652
#                                      класса и заметная доля секции строк.
#  io.appmetrica.**                  — SDK везёт собственные consumer-правила.
#  androidx.room.** (целиком)        — room-runtime везёт свои consumer-правила.
```

### 5.2. `app-compose/proguard-rules-rustore.pro` — новый файл (только флейвор `rustore`)

```proguard
# =============================================================================
#  Правила только для флейвора `rustore` (RuStore Pay SDK).
#  Подключается через productFlavors { rustore { proguardFiles(...) } }, чтобы
#  Google-Play-флейворы (ru/uk/full) не платили размером за чужой SDK.
# =============================================================================

# SDK везёт свои consumer-правила; здесь только подавление предупреждений о
# необязательных транзитивных зависимостях. Заполнять СТРОГО по
# build/outputs/mapping/rustoreRelease/missing_rules.txt.
-dontwarn ru.rustore.sdk.**

# ⚠️ План «Б», если после минификации ломается оплата (см. риск R5 в плане):
# раскомментировать ЦЕЛИКОМ и завести issue на сужение правила. Стоимость —
# только для флейвора rustore, Google Play это не увидит.
#-keep class ru.rustore.sdk.** { *; }
```

---

## 6. Риски и как их ловить

| #   | Риск                                                                                | Вероятность | Последствие                                                                       | Митигация                                                                                                                                   |
|-----|-------------------------------------------------------------------------------------|-------------|-----------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| R1  | **Ломается EncryptedSharedPreferences** (Tink/protobuf) → не читается passphrase БД | Средняя     | **Критично**: пользователь теряет доступ к своим песням/избранному при обновлении | Правило `GeneratedMessageLite { <fields>; }` в §5.1 + **обязательный** тест-сценарий T-C04 #2 (обновление поверх старой версии)             |
| R2  | Ломается DataStore → сбрасываются настройки темы/шрифта                             | Средняя     | Заметно, но не фатально                                                           | То же правило; сценарий T-C04 #3                                                                                                            |
| R3  | Room не находит `PwsDatabase_Impl`                                                  | Низкая      | Падение на старте                                                                 | Правила Room в §5.1; ловится первым же запуском                                                                                             |
| R4  | kotlinx.serialization не находит сериализатор (`SerializationException`)            | Средняя     | Не ставятся сборники, не читается бэкап                                           | Официальные `-if`-правила в §5.1; сценарии T-C04 #1, #5, #7                                                                                 |
| R5  | **RuStore Pay SDK** ломается после минификации                                      | Средняя     | Не работают платежи в RuStore-сборке                                              | Отдельный файл правил §5.2 + план «Б» внутри него; сценарий T-C04 #8. В истории уже был фикс «fix rustore payment sdk issue» — зона хрупкая |
| R6  | В AppMetrica приезжают обфусцированные имена экранов                                | Низкая      | Аналитика становится нечитаемой                                                   | `-keepnames … Screen`; проверка T-C06                                                                                                       |
| R7  | Замена иконки (Phase D) визуально отличается от оригинала                           | Низкая      | Косметика                                                                         | Путь скопирован 1:1, viewport 24×24; визуальная проверка в T-D05                                                                            |
| R8  | Удаление `appcompat` (T-E02) даёт белую вспышку в тёмной теме при старте            | Средняя     | Косметика                                                                         | `values-night/colors.xml`; пункт независим и легко откатывается                                                                             |
| R9  | Исполнитель «чинит» падение возвратом широкого keep и обнуляет весь выигрыш         | **Высокая** | План бессмыслен                                                                   | §7 «дерево решений», шапка файла правил, гейт в CI (T-G02), правило в AGENTS.md (T-G03)                                                     |
| R10 | Меньший DEX ≠ меньший AAB, если ресурсы/assets не тронуты                           | —           | Ожидания не совпадут                                                              | В AAB `uk` 5.1 МБ — это seed-книги (контент). Оценивать выигрыш по DEX, а не только по размеру AAB                                          |

---

## 7. Дерево решений при поломке в release-сборке

```
Что-то падает / не работает ТОЛЬКО в release?
│
├─ Сборка упала с «Missing class …»?
│     → build/outputs/mapping/<variant>/missing_rules.txt
│     → скопировать ОТТУДА только строки -dontwarn, добавить комментарий
│
├─ Runtime: ClassNotFoundException / NoSuchMethodException / NoSuchFieldException?
│     → значит класс/член удалён или переименован, а его ищут по имени
│     → grep '<имя>' build/outputs/mapping/<variant>/usage.txt   (удалён?)
│     → grep '<имя>' build/outputs/mapping/<variant>/mapping.txt (переименован?)
│     → удалён   → -keep class <FQCN> { <нужный член>; }
│     → переименован → -keepnames class <FQCN>   ← ПРЕДПОЧТИТЕЛЬНО
│
├─ Runtime: SerializationException «Serializer for class X not found»?
│     → правила kotlinx.serialization из §5.1 не сработали для этого класса
│     → -keepclassmembers class <FQCN> { *** Companion; kotlinx.serialization.KSerializer serializer(...); }
│
├─ Строка-имя класса ушла в данные (аналитика, ключ, лог)?
│     → -keepnames class <точный класс или узкий паттерн>
│
└─ В ЛЮБОМ случае:
      ❌ НЕ добавлять `-keep class <пакет>.** { *; }`
      ✅ Правило должно называть конкретный класс или узкий паттерн
      ✅ Над правилом — комментарий: что сломалось, как проявлялось, почему так
      ✅ После добавления — пересобрать и заново снять tools/dex-report.py
```

Полезно: **деобфускация стек-трейса** релизного крэша —
`retrace` из Android SDK (`cmdline-tools/latest/bin/retrace`):

```bash
retrace app-compose/build/outputs/mapping/ruRelease/mapping.txt crash.txt
```

---

## 8. Как проверять (сводная таблица команд)

| Шаг                                | Команда                                                                                                                                        |
|------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| Быстрая компиляция                 | `./gradlew :app-compose:assembleRuDebug`                                                                                                       |
| Релиз `ru` (главный цикл)          | `./gradlew :app-compose:assembleRuRelease`                                                                                                     |
| Все флейворы                       | `./gradlew :app-compose:assembleRuRelease :app-compose:assembleUkRelease :app-compose:assembleFullRelease :app-compose:assembleRustoreRelease` |
| Отчёт по DEX                       | `python3 tools/dex-report.py app-compose/build/outputs/apk/ru/release/*.apk --top 20`                                                          |
| Сравнение до/после                 | `python3 tools/dex-report.py --diff /tmp/pws-baseline/ru-before.apk <новый>.apk`                                                               |
| Что удержано keep-правилами        | `wc -l app-compose/build/outputs/mapping/ruRelease/seeds.txt`                                                                                  |
| Что удалено                        | `wc -l app-compose/build/outputs/mapping/ruRelease/usage.txt`                                                                                  |
| Итоговая конфигурация R8           | `less app-compose/build/outputs/mapping/ruRelease/configuration.txt`                                                                           |
| Юнит-тесты                         | `./gradlew :data:db-android:testRuDebugUnitTest :data:content-delivery:check :app-compose:check`                                               |
| E2E (нужно устройство)             | `./e2e/scripts/run-compose.sh --flavor ru --full --clean --retries 1`                                                                          |
| Полная релизная сборка + артефакты | `./build-compose.sh`                                                                                                                           |
| Маппинг для AppMetrica             | `./gradlew :app-compose:stageAppMetricaMappingRuRelease` → `output/appmetrica-mapping/`                                                        |

> **Про подпись.** Ключи релиза берутся из Gradle-свойств `android.release.*`
> (`~/.gradle/gradle.properties` или `-P`), а **не** из `local.properties`. Если их нет, AGP
> соберёт `app-compose-ru-release-unsigned.apk` — этого достаточно для замеров DEX, но не для
> установки на устройство. Для ручного smoke-теста подписать debug-ключом:
> ```bash
> $ANDROID_HOME/build-tools/<ver>/apksigner sign --ks ~/.android/debug.keystore \
>     --ks-pass pass:android --out signed.apk app-compose-ru-release-unsigned.apk
> ```

---

## 9. Порядок реализации и чекпойнты

| # | Фаза                            | Ожидаемый эффект                               | Чекпойнт (что должно быть верно, чтобы идти дальше)         |
|---|---------------------------------|------------------------------------------------|-------------------------------------------------------------|
| 1 | **A** — замер                   | —                                              | Baseline-цифры записаны, `dex-report.py` работает           |
| 2 | **B** — новые правила           | DEX 36.8 → ~10–14 MB, обфускация ≥ 80%         | Все 4 флейвора собираются, отчёт показывает целевые цифры   |
| 3 | **C** — починка + проверка      | стабильность                                   | Юнит-тесты зелёные, ручной smoke (все 8 сценариев) пройден  |
| 4 | **D** — выкинуть material-icons | −11 400 классов гарантированно, быстрее сборка | Сердечко на месте, `grep` по material.icons пуст            |
| 5 | **E** — мёртвые зависимости     | −0.3…0.5 MB DEX + ресурсы                      | Сборка зелёная, старт без белой вспышки (если делали T-E02) |
| 6 | **F** — прочее (опционально)    | ресурсы / память / старт                       | По каждому пункту отдельно                                  |
| 7 | **G** — защита от регрессии     | не откатится назад                             | CI-гейт работает, AGENTS.md обновлён                        |

**Коммитить по фазам** (`git commit` после каждой), чтобы `git revert` откатывал ровно одну фазу.
Формат сообщений — Conventional Commits (в репозитории semantic-release):
`perf(build): remove blanket R8 keep rules`, `perf(core): drop material-icons-extended`, и т.п.

**Публиковать в Play только после полного T-C04.** Ошибка R1 (потеря доступа к БД) проявится не
у разработчика, а у пользователей при обновлении, и откатить её выкладкой уже не получится —
данные к тому моменту будут недоступны. Рекомендуется выкатывать через **staged rollout 10%**
и следить за crash-free в AppMetrica и Android Vitals минимум 48 часов.

---

## 10. Definition of Done

- [ ] `app-compose/proguard-rules.pro` не содержит ни одного `-keep class X.** { *; }`, кроме
  обоснованного `net.zetetic.database.**` (JNI), и у каждого правила есть комментарий «зачем».
- [ ] Все четыре флейвора собираются в release без ошибок.
- [ ] `tools/dex-report.py` показывает: DEX ≤ 12 MB, классов ≤ 13 000, обфускация ≥ 85%.
- [ ] Юнит-тесты `:data:db-android`, `:data:content-delivery`, `:app-compose` зелёные.
- [ ] Все 8 ручных сценариев T-C04 пройдены на релизной сборке, **включая обновление поверх
  установленной из Play версии 3.7.0**.
- [ ] В AppMetrica: стек-трейс релизного крэша читается после загрузки `mapping.txt`, событие
  `screen_view` содержит человеческие имена экранов.
- [ ] `material-icons-extended` удалён из `pws-core`, `grep androidx.compose.material.icons` пуст.
- [ ] CI-гейт на размер DEX добавлен, запрет на широкие keep записан в `AGENTS.md`.
- [ ] В Play Console после публикации: **App optimization ≥ 80% (High)**, obfuscation ≥ 85%.
- [ ] Статус этого плана обновлён на «РЕАЛИЗОВАН» с фактическими цифрами «до/после».
