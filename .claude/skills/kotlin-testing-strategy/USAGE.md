# Usage — Kotlin Testing Strategy

## 1. Setup (convention plugins already wire JUnit 5)

```kotlin
// KMP module
commonTest.dependencies {
    implementation(libs.kotest.framework.engine)
    implementation(libs.kotest.assertions)
    implementation(libs.kotlinx.coroutines.test)
}
jvmTest.dependencies { implementation(libs.kotest.runner.junit5) }

// server:infra / server:app
testImplementation(libs.kotest.runner.junit5)
testImplementation(libs.testcontainers.postgresql)
testImplementation(libs.ktor.server.test.host)
testRuntimeOnly(libs.logback.classic)     // Testcontainers diagnostics
```

## 2. For every new use-case

- happy path (value + emitted events + stamped time);
- one test per `ensure` (each refusal and its error type);
- idempotent replay for creates; `Conflict` for a stale version;
- tenant: a foreign tenant's id is `NotFound`.

## 3. For every new repository method

Round-trip all columns; tenant scoping; CAS; duplicate id; ordering/limits for lists. If the method
implements a rule that also exists in the domain → a parity test (template).

## 4. For every new route

Status + `ApiErrorDto.code` for success and each error class; auth (401 without token, 403 for the
wrong role); the tenant comes from the token (a foreign id in the body changes nothing).

## 5. Identify the step's mine

Ask: *which defect would pass every test above?* — a race, a counter nobody reads, a read before a
check, two expressions of one rule drifting, a silent fallback, a token of one kind accepted by
another, a journal that blocks the action, a refusal that leaves half a write, data that outlives
its tenant. Write the special-form test for it (`references/special-forms.md`,
`references/security-boundaries.md`), then **prove it red** on a broken implementation.

For every protection the step adds, write the **pair** as well: the stranger is refused *and*
one's own still passes.

## 6. Bugs

Reproduce with a failing test at the lowest layer that can express it, then fix, then keep the test.

```bash
./gradlew :domain:jvmTest            # fast loop
./gradlew build                      # the gate (needs Docker)
```

Then walk through [`checklists/testing-checklist.md`](checklists/testing-checklist.md).
