package io.github.alelk.pws.android.compose

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.domain.preferences.model.AppPreferences
import io.github.alelk.pws.domain.preferences.model.FavoritesSortMode
import io.github.alelk.pws.domain.preferences.model.ThemeMode
import io.github.alelk.pws.domain.preferences.repository.UserPreferencesRepository
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin

/**
 * Mine of step 04: after an update the user sees reset settings because the new repository reads another
 * file, another key or another default. Every one of the 10 settings is written the way the app used to
 * write it before the repository existed (a raw DataStore entry with the old key name and value type)
 * and read by [DataStoreUserPreferencesRepository], and the other way round, on the very same DataStore
 * instance the app uses.
 *
 * See it red: change one key name in [DataStoreUserPreferencesRepository] (e.g. `"keep-screen-on"` to
 * `"keep-screen-on2"`), run
 * `./gradlew :app-compose:testRuDebugUnitTest --tests '*DataStoreUserPreferencesRepositoryTest*'`, then revert.
 *
 * One SDK is enough: DataStore is a plain file, its behaviour does not depend on the Android version.
 */
@RobolectricTest(sdk = [34])
class DataStoreUserPreferencesRepositoryTest :
  FeatureSpec({

    // Robolectric instantiates the real PwsComposeApplication for each test, and its onCreate
    // starts Koin. Stop it between tests so the next Application.onCreate can start it cleanly.
    afterTest { runCatching { stopKoin() } }

    suspend fun emptyStore(): Context {
      val ctx = ApplicationProvider.getApplicationContext<Context>()
      ctx.appSettingsDataStore().edit { it.clear() }
      return ctx
    }

    fun repository(ctx: Context) = DataStoreUserPreferencesRepository(ctx.appSettingsDataStore())

    feature("old functions and the repository share keys, formats and defaults") {
      scenario("both directions cover all 10 settings") {
        OLD_WRITES.map { it.key } shouldContainExactlyInAnyOrder SETTING_KEYS
        NEW_WRITES.map { it.key } shouldContainExactlyInAnyOrder SETTING_KEYS
      }

      scenario("an empty store reads as the old defaults") {
        val ctx = emptyStore()
        val actual = repository(ctx).preferences.first()

        // The defaults of the removed `Context.xxxFlow()` functions.
        assertSoftly {
          actual.themeMode shouldBe ThemeMode.SYSTEM
          actual.songText.scale shouldBe 1.0f
          actual.songText.expanded shouldBe true
          actual.favorites.sortMode shouldBe FavoritesSortMode.ADDED_DATE
          actual.favorites.ascending shouldBe false
          actual.useDynamicColor shouldBe false
          actual.keepScreenOn shouldBe false
          actual.songText.lineHeightMultiplier shouldBe 1.0f
          actual.songText.serifFont shouldBe false
          actual.songText.showNavButtons shouldBe false
        }
      }

      scenario("a value written under the old key is read by the repository") {
        assertSoftly {
          OLD_WRITES.forEach { case ->
            val ctx = emptyStore()
            ctx.rawWrite(case.key, case.rawValue)
            withClue(case.key) { case.read(repository(ctx).preferences.first()) shouldBe case.expected }
          }
        }
      }

      scenario("a value written by the repository is readable under the old key and touches only its key") {
        assertSoftly {
          NEW_WRITES.forEach { case ->
            val ctx = emptyStore()
            repository(ctx).update(case.transform)
            withClue(case.key) {
              ctx.rawRead(case.key) shouldBe case.expectedRaw
              ctx.appSettingsDataStore().data.first().asMap().keys.map { it.name } shouldBe listOf(case.key)
            }
          }
        }
      }
    }

    feature("wiring") {
      scenario("Koin serves the DataStore adapter, not the in-memory default") {
        GlobalContext.get().get<UserPreferencesRepository>().shouldBeInstanceOf<DataStoreUserPreferencesRepository>()
      }
    }
  })

/** The key names (G3). */
private val SETTING_KEYS =
  listOf(
    "app-theme",
    "song-text-scale",
    "song-text-expanded",
    "favorites-sort-mode",
    "favorites-ascending",
    "use-dynamic-color",
    "keep-screen-on",
    "song-line-height-multiplier",
    "song-serif-font",
    "show-song-nav-buttons",
  )

/** The raw DataStore entry the app has always written for a setting: key name, value of the key's type. */
private suspend fun Context.rawWrite(key: String, value: Any) {
  appSettingsDataStore().edit { prefs ->
    when (value) {
      is String -> prefs[stringPreferencesKey(key)] = value
      is Boolean -> prefs[booleanPreferencesKey(key)] = value
      is Float -> prefs[floatPreferencesKey(key)] = value
      else -> error("unsupported value type: ${value::class}")
    }
  }
}

private suspend fun Context.rawRead(key: String): Any? =
  appSettingsDataStore()
    .data
    .first()
    .asMap()
    .entries
    .firstOrNull { it.key.name == key }
    ?.value

/** A non-default value stored under the old key and how the repository exposes it. */
private class OldWrite(
  val key: String,
  // the raw value of the key's type
  val rawValue: Any,
  // the field of the repository's view
  val read: (AppPreferences) -> Any,
  val expected: Any,
)

/** A non-default value written through the repository and the raw value expected under the old key. */
private class NewWrite(
  val key: String,
  // the change made through the repository
  val transform: (AppPreferences) -> AppPreferences,
  val expectedRaw: Any,
)

private val OLD_WRITES: List<OldWrite> =
  listOf(
    OldWrite("app-theme", "dark", { it.themeMode }, ThemeMode.DARK),
    OldWrite("song-text-scale", 1.4f, { it.songText.scale }, 1.4f),
    OldWrite("song-text-expanded", false, { it.songText.expanded }, false),
    OldWrite("favorites-sort-mode", "SONG_NAME", { it.favorites.sortMode }, FavoritesSortMode.SONG_NAME),
    OldWrite("favorites-ascending", true, { it.favorites.ascending }, true),
    OldWrite("use-dynamic-color", true, { it.useDynamicColor }, true),
    OldWrite("keep-screen-on", true, { it.keepScreenOn }, true),
    OldWrite("song-line-height-multiplier", 1.6f, { it.songText.lineHeightMultiplier }, 1.6f),
    OldWrite("song-serif-font", true, { it.songText.serifFont }, true),
    OldWrite("show-song-nav-buttons", true, { it.songText.showNavButtons }, true),
  )

private val NEW_WRITES: List<NewWrite> =
  listOf(
    NewWrite("app-theme", { it.copy(themeMode = ThemeMode.BLACK) }, "black"),
    NewWrite("song-text-scale", { it.copy(songText = it.songText.copy(scale = 0.8f)) }, 0.8f),
    NewWrite("song-text-expanded", { it.copy(songText = it.songText.copy(expanded = false)) }, false),
    NewWrite(
      "favorites-sort-mode",
      { it.copy(favorites = it.favorites.copy(sortMode = FavoritesSortMode.SONG_NUMBER)) },
      "SONG_NUMBER",
    ),
    NewWrite("favorites-ascending", { it.copy(favorites = it.favorites.copy(ascending = true)) }, true),
    NewWrite("use-dynamic-color", { it.copy(useDynamicColor = true) }, true),
    NewWrite("keep-screen-on", { it.copy(keepScreenOn = true) }, true),
    NewWrite(
      "song-line-height-multiplier",
      { it.copy(songText = it.songText.copy(lineHeightMultiplier = 1.2f)) },
      1.2f,
    ),
    NewWrite("song-serif-font", { it.copy(songText = it.songText.copy(serifFont = true)) }, true),
    NewWrite("show-song-nav-buttons", { it.copy(songText = it.songText.copy(showNavButtons = true)) }, true),
  )
