package io.github.alelk.pws.android.compose.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.android.compose.R
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.koin.core.context.stopKoin

/**
 * Plan 2026-09-30, 05.3: no app for ACTION_VIEW / ACTION_SENDTO / ACTION_SEND must not crash; the user
 * gets a string-resource message and the failure is recorded.
 *
 * Red check: replace the `catch` in [startActivityOrReport] with a rethrow and run
 * `./gradlew :app-compose:testRuDebugUnitTest --tests '*ExternalActionsTest*'`, then revert.
 */
@RobolectricTest(sdk = [34])
class ExternalActionsTest :
  FeatureSpec({

    afterTest { runCatching { stopKoin() } }

    class Fixture(private val missingApp: Boolean) {
      val started = mutableListOf<Intent>()
      val toasts = mutableListOf<Int>()
      val errors = mutableListOf<String?>()

      val context: Context = object : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
        override fun startActivity(intent: Intent) {
          started += intent
          if (missingApp) throw ActivityNotFoundException("no app")
        }
      }

      val telemetry = object : Telemetry {
        override fun recordError(throwable: Throwable, message: String?, attributes: Map<String, String>) {
          errors += message
        }
        override fun log(message: String) = Unit
        override fun event(name: String, params: Map<String, Any?>) = Unit
        override fun setUserProperty(key: String, value: String?) = Unit
      }
      val toaster = ShellToaster { toasts += it }
    }

    feature("url, e-mail and share actions") {
      scenario("start the matching intents when an app exists") {
        val f = Fixture(missingApp = false)
        val urls = AndroidUrlActions(f.context, f.telemetry, f.toaster)
        urls.openUrl("https://example.org")
        urls.sendEmail("mailto:a@example.org")
        AndroidShareActions(f.context, f.telemetry, f.toaster).shareText("text")

        f.started.map { it.action } shouldContainExactly
          listOf(Intent.ACTION_VIEW, Intent.ACTION_SENDTO, Intent.ACTION_CHOOSER)
        f.toasts shouldContainExactly emptyList()
        f.errors shouldContainExactly emptyList()
      }

      scenario("report a missing app instead of crashing") {
        val f = Fixture(missingApp = true)
        val urls = AndroidUrlActions(f.context, f.telemetry, f.toaster)
        urls.openUrl("https://example.org")
        urls.sendEmail("mailto:a@example.org")
        AndroidShareActions(f.context, f.telemetry, f.toaster).shareText("text")

        f.toasts shouldBe List(3) { R.string.no_app_to_open_link }
        f.errors shouldBe List(3) { "no_app_to_open_link" }
      }
    }
  })
