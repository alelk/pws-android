# Usage — Kotlin Project Layout

Templates live in [`templates/`](templates/). Replace the `app.` plugin prefix and the
`io.github.myorg` group with the project's own.

## 1. Root files

```bash
mkdir my-project && cd my-project && git init
echo "0.1.0" > app.version
mkdir -p gradle
cp <skill>/templates/shared/settings.gradle.kts   settings.gradle.kts
cp <skill>/templates/shared/build.gradle.kts.root build.gradle.kts
cp <skill>/templates/shared/libs.versions.toml    gradle/libs.versions.toml
cp <skill>/templates/shared/gradle.properties     gradle.properties
cp <skill>/templates/shared/detekt.yml            detekt.yml
```

Edit: `rootProject.name`, the module list, `group`. Strip unused catalogue entries.

## 2. Convention plugins

```bash
cp -R <skill>/templates/convention-plugins ./convention-plugins
# rename the prefix if you want: app.kmp.gradle.kts → acme.kmp.gradle.kts (id becomes acme.kmp)
```

The included build reads the root catalogue (`convention-plugins/settings.gradle.kts`) and puts
every wrapped Gradle plugin on its own classpath from catalogue markers
(`convention-plugins/build.gradle.kts`). Details → [`references/convention-plugins.md`](references/convention-plugins.md).

## 3. Wrapper

```bash
gradle wrapper --gradle-version <exact-version>
```

Commit `gradlew`, `gradlew.bat`, `gradle/wrapper/`.

## 4. First modules

KMP (domain, contract, client, features):

```kotlin
// domain/build.gradle.kts
plugins { id("app.kmp") }

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.arrow.core)
        }
        commonTest.dependencies {
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions)
        }
        jvmTest.dependencies { implementation(libs.kotest.runner.junit5) }
    }
}
```

JVM-only (server modules):

```kotlin
// server/infra/build.gradle.kts
plugins { id("app.jvm.serialization") }   // app.jvm when no *Pm / JSON is needed

dependencies {
    implementation(projects.domain)
    testImplementation(libs.kotest.runner.junit5)
}
```

## 5. Verify

```bash
./gradlew build           # THE gate: compile all targets, tests, detekt, ktlint
./gradlew projects
./gradlew ktlintFormat    # before committing formatting changes
```

Then walk through [`checklists/layout-checklist.md`](checklists/layout-checklist.md).

## Notes

- A module is not usable until it is `include(...)`d in `settings.gradle.kts`.
- In an agent sandbox that OOM-kills the full gate, agree a constrained form once and write it in
  the plan's README → [`references/constrained-gate.md`](references/constrained-gate.md).
- Adding a new kind of module (e.g. Android) → a new convention plugin, never a copy of an
  existing module's block.
- When a Gradle plugin moves (e.g. Shadow: `com.github.johnrengelman.shadow` →
  `com.gradleup.shadow`), change the catalogue marker once; modules don't know.
