# Usage — KMP Architecture

## 1. Create a new KMP module

With convention plugins in place the module is one line plus dependencies
(`plugins { id("app.kmp") }`). Without them, drop the template into `build.gradle.kts`:

```bash
cp <skill>/templates/shared/build.gradle.kts.kmp-module <module>/build.gradle.kts
```

If the module uses Compose:

```bash
cp <skill>/templates/shared/build.gradle.kts.kmp-compose-module <module>/build.gradle.kts
```

Create the source set directories:

```bash
mkdir -p <module>/src/{commonMain,commonTest,jvmMain,jvmTest,wasmJsMain}/kotlin
```

## 2. Convert an existing JVM module to KMP

```kotlin
// Before
plugins { alias(libs.plugins.kotlin.jvm) }
dependencies { implementation(libs.arrow.core) }

// After
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies { implementation(libs.arrow.core) }
        jvmTest.dependencies { implementation(libs.kotest.runner.junit5) }
    }
}
```

Don't add the Kotest multiplatform plugin — Kotest runs on the JVM via JUnit 5 (see the test
gotchas in [`references/web-target.md`](references/web-target.md)).

Move existing sources into `src/commonMain/kotlin/` (JVM-safe code only) or
`src/jvmMain/kotlin/` (JVM-only code).

## 3. Scan for JVM-only API leaks

```bash
./gradlew :<module>:compileKotlinWasmJs     # JVM-only APIs fail to compile
grep -rn "import java\." <module>/src/commonMain/
```

Fix each hit using the substitution table in
[kotlin-domain-modeling / value-types.md](../kotlin-domain-modeling/references/value-types.md).

## 4. Replace common JVM-only APIs

```kotlin
// Before
import java.util.UUID
data class OrderId(val value: UUID)

// After
import kotlin.uuid.Uuid
@JvmInline value class OrderId(val value: String)
val id = OrderId(Uuid.generateV7().toString())   // time-ordered; see kotlin-domain-modeling
```

```kotlin
// Before
import java.time.Instant
val createdAt = Instant.now()

// After — and inject the clock instead of reading it
import kotlin.time.Clock
import kotlin.time.Instant
class CreateOrder(private val clock: Clock /* … */) {
    fun stamp(): Instant = clock.now()
}
```

For types without a stdlib KMP equivalent (`LocalDate`, `File`, `URL`), wrap in a
`@JvmInline value class` — see [value-types.md](../kotlin-domain-modeling/references/value-types.md).

## 5. Add `expect/actual` only where unavoidable

Pattern + HTTP client example → [`references/expect-actual.md`](references/expect-actual.md).

## 6. Run all targets

```bash
./gradlew :<module>:jvmTest                  # JVM tests
./gradlew :<module>:compileKotlinWasmJs      # web purity gate (no runner needed)
./gradlew :<module>:compileTestKotlinWasmJs  # commonTest is portable too
./gradlew check                              # all modules, all targets
```

For web-target config, browser interop and Docker gotchas →
[`references/web-target.md`](references/web-target.md).

## Notes

- Kotest annotation-based config doesn't work off-JVM — use `FunSpec`, `ShouldSpec`, `DescribeSpec`.
- MockK is JVM-only — use `Fake*` implementations in `commonTest`.
- The Kotest multiplatform *compiler* plugin can lag the Kotlin version. If it does, run
  Kotest on the JVM and keep the web target as a compile-only gate — don't downgrade Kotlin
  for it.
