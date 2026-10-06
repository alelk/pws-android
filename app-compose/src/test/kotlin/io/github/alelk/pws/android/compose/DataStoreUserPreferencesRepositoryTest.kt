package io.github.alelk.pws.android.compose

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.domain.preferences.model.AppPreferences
import io.github.alelk.pws.domain.preferences.model.FavoritesSortMode
import io.github.alelk.pws.domain.preferences.repository.UserPreferencesRepository
import io.github.alelk.pws.features.theme.ThemeMode
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
 * file, another key or another default. Every one of the 10 settings is written by the old
 * `ThemePreferences.kt` functions and read by [DataStoreUserPreferencesRepository], and the other way
 * round, on the very same DataStore instance the app uses.
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

        assertSoftly {
          actual.themeMode shouldBe ctx.themeModeFlow().first()
          actual.songText.scale shouldBe ctx.songTextScaleFlow().first()
          actual.songText.expanded shouldBe ctx.songTextExpandedFlow().first()
          actual.favorites.sortMode.identifier shouldBe ctx.favoritesSortModeFlow().first()
          actual.favorites.ascending shouldBe ctx.favoritesAscendingFlow().first()
          actual.useDynamicColor shouldBe ctx.useDynamicColorFlow().first()
          actual.keepScreenOn shouldBe ctx.keepScreenOnFlow().first()
          actual.songText.lineHeightMultiplier shouldBe ctx.songLineHeightMultiplierFlow().first()
          actual.songText.serifFont shouldBe ctx.songSerifFontFlow().first()
          actual.songText.showNavButtons shouldBe ctx.showSongNavButtonsFlow().first()
        }
      }

      scenario("a value written by the old setter is read by the repository") {
        assertSoftly {
          OLD_WRITES.forEach { case ->
            val ctx = emptyStore()
            case.write(ctx)
            withClue(case.key) { case.read(repository(ctx).preferences.first()) shouldBe case.expected }
          }
        }
      }

      scenario("a value written by the repository is read by the old flow and touches only its key") {
        assertSoftly {
          NEW_WRITES.forEach { case ->
            val ctx = emptyStore()
            repository(ctx).update(case.transform)
            withClue(case.key) {
              case.readOld(ctx) shouldBe case.expected
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

/** The keys of `ThemePreferences.kt` (G3). */
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

/** A non-default value written by the old setter and how the repository exposes it. */
private class OldWrite(
  val key: String,
  // the old `Context.setXxx()` call
  val write: suspend (Context) -> Unit,
  // the field of the repository's view
  val read: (AppPreferences) -> Any,
  val expected: Any,
)

/** A non-default value written through the repository and how the old flow reads it back. */
private class NewWrite(
  val key: String,
  // the change made through the repository
  val transform: (AppPreferences) -> AppPreferences,
  // the old `Context.xxxFlow()` read
  val readOld: suspend (Context) -> Any,
  val expected: Any,
)

private val OLD_WRITES: List<OldWrite> =
  listOf(
    OldWrite("app-theme", { it.setThemeMode(ThemeMode.DARK) }, { it.themeMode }, ThemeMode.DARK),
    OldWrite("song-text-scale", { it.setSongTextScale(1.4f) }, { it.songText.scale }, 1.4f),
    OldWrite("song-text-expanded", { it.setSongTextExpanded(false) }, { it.songText.expanded }, false),
    OldWrite(
      "favorites-sort-mode",
      { it.setFavoritesSortMode("SONG_NAME") },
      { it.favorites.sortMode },
      FavoritesSortMode.SONG_NAME,
    ),
    OldWrite("favorites-ascending", { it.setFavoritesAscending(true) }, { it.favorites.ascending }, true),
    OldWrite("use-dynamic-color", { it.setUseDynamicColor(true) }, { it.useDynamicColor }, true),
    OldWrite("keep-screen-on", { it.setKeepScreenOn(true) }, { it.keepScreenOn }, true),
    OldWrite(
      "song-line-height-multiplier",
      { it.setSongLineHeightMultiplier(1.6f) },
      { it.songText.lineHeightMultiplier },
      1.6f,
    ),
    OldWrite("song-serif-font", { it.setSongSerifFont(true) }, { it.songText.serifFont }, true),
    OldWrite("show-song-nav-buttons", { it.setShowSongNavButtons(true) }, { it.songText.showNavButtons }, true),
  )

private val NEW_WRITES: List<NewWrite> =
  listOf(
    NewWrite("app-theme", { it.copy(themeMode = ThemeMode.BLACK) }, { it.themeModeFlow().first() }, ThemeMode.BLACK),
    NewWrite(
      "song-text-scale",
      { it.copy(songText = it.songText.copy(scale = 0.8f)) },
      { it.songTextScaleFlow().first() },
      0.8f,
    ),
    NewWrite(
      "song-text-expanded",
      { it.copy(songText = it.songText.copy(expanded = false)) },
      { it.songTextExpandedFlow().first() },
      false,
    ),
    NewWrite(
      "favorites-sort-mode",
      { it.copy(favorites = it.favorites.copy(sortMode = FavoritesSortMode.SONG_NUMBER)) },
      { it.favoritesSortModeFlow().first() },
      "SONG_NUMBER",
    ),
    NewWrite(
      "favorites-ascending",
      { it.copy(favorites = it.favorites.copy(ascending = true)) },
      { it.favoritesAscendingFlow().first() },
      true,
    ),
    NewWrite("use-dynamic-color", { it.copy(useDynamicColor = true) }, { it.useDynamicColorFlow().first() }, true),
    NewWrite("keep-screen-on", { it.copy(keepScreenOn = true) }, { it.keepScreenOnFlow().first() }, true),
    NewWrite(
      "song-line-height-multiplier",
      { it.copy(songText = it.songText.copy(lineHeightMultiplier = 1.2f)) },
      { it.songLineHeightMultiplierFlow().first() },
      1.2f,
    ),
    NewWrite(
      "song-serif-font",
      { it.copy(songText = it.songText.copy(serifFont = true)) },
      { it.songSerifFontFlow().first() },
      true,
    ),
    NewWrite(
      "show-song-nav-buttons",
      { it.copy(songText = it.songText.copy(showNavButtons = true)) },
      { it.showSongNavButtonsFlow().first() },
      true,
    ),
  )
