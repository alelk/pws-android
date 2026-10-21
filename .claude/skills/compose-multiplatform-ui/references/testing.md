# Testing ViewModels and screen logic

## Setup

```kotlin
commonTest.dependencies {
    implementation(libs.kotest.framework.engine)
    implementation(libs.kotest.assertions)
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.turbine)
}
jvmTest.dependencies { implementation(libs.kotest.runner.junit5) }
```

Kotest runs on the JVM (see [`kmp-architecture`](../../kmp-architecture/SKILL.md)); tests that
only need pure Kotlin live in `commonTest`, tests that need `Dispatchers.setMain` or source-file
scanning live in `jvmTest`.

## ViewModel: event → state → effect

```kotlin
class GoalsViewModelTest : FunSpec({
    val dispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(dispatcher) }
    afterTest { Dispatchers.resetMain() }

    test("a failed create lowers busy and shows the error") {
        runTest(dispatcher) {
            val api = FakeGoalsApi(createResult = ApiError.Validation(info("VALIDATION_ERROR")).left())
            val vm = GoalsViewModel(api)
            advanceUntilIdle()

            vm.onEvent(GoalsEvent.CreateClicked("Read more"))
            advanceUntilIdle()

            vm.state.value.busy shouldBe false          // the classic "eternal spinner" bug
            vm.state.value.error.shouldBeInstanceOf<ApiError.Validation>()
        }
    }

    test("a successful create emits Created once") {
        runTest(dispatcher) {
            val vm = GoalsViewModel(FakeGoalsApi())
            vm.effects.test {
                vm.onEvent(GoalsEvent.CreateClicked("Read more"))
                awaitItem() shouldBe GoalsEffect.Created
            }
        }
    }
})
```

## Fakes of client APIs

Client APIs are interfaces returning `Either<ApiError, T>` — a fake is a few lines, and it should
behave like the server where it matters (a repeated client id answers `409 ALREADY_EXISTS`, a stale
version `409 CONFLICT`). Shared fakes and DTO builders live in `features/src/commonTest/.../fakes/`.

```kotlin
class FakeGoalsApi(var createResult: Either<ApiError, GoalDto>? = null) : GoalsApi {
    val created = mutableListOf<CreateGoalRequest>()
    override suspend fun create(request: CreateGoalRequest) =
        createResult ?: aGoalDto(id = request.id, name = request.name).right().also { created += request }
}
```

## Pure screen logic first

Screen models, navigation plans (`NavigationPlan.for(role)`), address codecs
(`AppAddress.decode(encode(stack)) == stack`), `zoneRenderOf` — all pure functions, tested by
**iterating every route/role**, not by hand-picked examples.

## What else

| Layer                           | How                                                                                                                                                                               |
|---------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Conventions (layers, names)     | source-scanning tests — [`architecture-fitness-tests`](../../architecture-fitness-tests/SKILL.md)                                                                                 |
| Locale parity                   | `ResourceKeysTest` ([`localization.md`](localization.md))                                                                                                                         |
| Critical flows on a real screen | `runComposeUiTest` on the JVM target, sparingly                                                                                                                                   |
| Visual review                   | a JVM "gallery" that renders previews to PNG for human eyes — it asserts nothing                                                                                                  |
| Any `ImageComposeScene` test    | close every `render()` image — native Skia memory, outside `-Xmx` ([`compose-design-system`](../../compose-design-system/references/wrappers-and-review.md#previews-and-gallery)) |

Screenshot-diff infrastructure is not worth it before ViewModel and pure-logic coverage exist, and
there is no stable wasm UI test runner.

**Render chrome-dependent frames through the shell.** A gallery that draws screens inside a phone or
desktop frame — but not through `AppShell` — stays green while the shell drops a sidebar on one
width ([`shell-chrome.md`](shell-chrome.md)). Frames that are *about* chrome (tabs, sidebar,
banners, own-bottom screens, the undo capsule's inset) go through the real shell.

**Gallery at scale.** A product of ~50 routes ends up with ~80 gallery test classes. Keep sample
data in one shared fixture set, render every frame in each skin × theme, and — when a designer
supplies reference images — record a frame-by-frame acceptance table "gallery frame ↔ reference"
in the step plan. The designer-pass *process* (audit → scenarios → words → acceptance table) is
planned as a separate skill (`designer-pass-process`, later wave).

## Anti-patterns

| Anti-pattern                      | Fix                                           |
|-----------------------------------|-----------------------------------------------|
| Only UI tests, no ViewModel tests | ViewModel + pure-function tests first         |
| Mocking the Koin graph            | constructor-inject fakes                      |
| A fake that ignores ids/versions  | fakes mirror the server's idempotency and CAS |
| Shared mutable fakes across tests | fresh ViewModel + fresh fake per test         |
