# `PreferencesStorage` — KMP interface, shell-specific implementations

Common interface lives in `features`; each shell binds its own implementation in Koin.

## Interface (`features/commonMain`)

```kotlin
interface PreferencesStorage {
    fun getString(key: String): String?
    fun setString(key: String, value: String)
    fun remove(key: String)
}
```

Template → [`../templates/features/PreferencesStorage.kt`](../templates/features/PreferencesStorage.kt).

## JS shell — `localStorage`

```kotlin
class LocalStoragePreferences : PreferencesStorage {
    override fun getString(key: String): String?           = window.localStorage.getItem(key)
    override fun setString(key: String, value: String)     = window.localStorage.setItem(key, value)
    override fun remove(key: String)                       = window.localStorage.removeItem(key)
}
```

## Android shell — `SharedPreferences`

```kotlin
class AndroidPreferences(private val context: Context) : PreferencesStorage {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    override fun getString(key: String): String?           = prefs.getString(key, null)
    override fun setString(key: String, value: String)     = prefs.edit().putString(key, value).apply()
    override fun remove(key: String)                       = prefs.edit().remove(key).apply()
}
```

## Desktop (JVM) shell — `java.util.prefs`

```kotlin
class JavaPreferences : PreferencesStorage {
    private val prefs = java.util.prefs.Preferences.userRoot().node("myapp")
    override fun getString(key: String): String?           = prefs.get(key, null)
    override fun setString(key: String, value: String)     = prefs.put(key, value)
    override fun remove(key: String)                       = prefs.remove(key)
}
```

## Rules

- Only the interface lives in `features` — never `window.*`, `android.*`, or `java.*`.
- Each shell binds: `single<PreferencesStorage> { PlatformImpl(...) }` in its DI init.
- For structured data, serialise to JSON and store the string. Don't add typed methods.
