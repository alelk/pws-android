package io.github.myorg.myapp.features.platform

interface PreferencesStorage {
    fun getString(key: String): String?
    fun setString(key: String, value: String)
    fun remove(key: String)
}
