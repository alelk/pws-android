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
