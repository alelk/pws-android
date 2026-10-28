# `expect` / `actual` vs interface + DI

**Default: an interface in `commonMain`, the implementation in the platform source set or the
shell, bound in Koin.** It can be faked in `commonTest`, can have several implementations
(real / unconfigured / test), and can carry state and lifecycle.

```kotlin
// features/commonMain
interface PreferencesStorage { fun getString(key: String): String?; fun setString(key: String, value: String) }
// webapp/wasmJsMain
class LocalStoragePreferences : PreferencesStorage { /* window.localStorage */ }
// webapp Koin module
single<PreferencesStorage> { LocalStoragePreferences() }
```

Use `expect/actual` only for small stateless glue with exactly one implementation per target:
an HTTP engine factory, a platform constant.

## Example: platform HTTP client

```kotlin
// commonMain
expect fun createPlatformHttpClient(config: ClientConfig): HttpClient
```

```kotlin
// jvmMain
actual fun createPlatformHttpClient(config: ClientConfig): HttpClient =
    HttpClient(CIO) {
        install(ContentNegotiation) { json() }
        config.proxy?.let { engine { proxy = it } }
    }
```

```kotlin
// jsMain
actual fun createPlatformHttpClient(config: ClientConfig): HttpClient =
    HttpClient(Js) {
        install(ContentNegotiation) { json() }
    }
```

## When NOT to use `expect/actual`

- Business logic — keep it in `commonMain`, parameterised via an interface.
- DTOs — they live entirely in `commonMain`.
- Use-cases — they live entirely in `commonMain` via `TransactionRunner`.

## Rules

- `expect` declarations live in `commonMain`.
- Every `expect` must have an `actual` in every target's `main` source set.
- `actual` contains glue only — no business `if/when`.
