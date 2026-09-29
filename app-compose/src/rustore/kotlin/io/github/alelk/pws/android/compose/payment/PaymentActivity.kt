package io.github.alelk.pws.android.compose.payment

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.alelk.pws.android.compose.MainActivity
import io.github.alelk.pws.android.compose.flavor.MONETIZATION
import io.github.alelk.pws.android.compose.themeModeFlow
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.github.alelk.pws.domain.telemetry.TelemetryEvent
import io.github.alelk.pws.features.theme.AppTheme
import io.github.alelk.pws.features.theme.ThemeMode
import org.koin.android.ext.android.get

/**
 * Standalone paywall host. Also receives the RuStore payment deeplink (VIEW + BROWSABLE on the
 * `io.github.alelk.pws.app` scheme, declared in the rustore manifest overlay) — the intent is
 * forwarded into the SDK via [PaymentController.proceedIntent].
 *
 * While purchases are disabled (`PremiumComingSoon`) the deeplink filter stays (the SDK will need it
 * later), but the activity touches neither [PaymentController] nor the SDK: it forwards to
 * [MainActivity] and finishes (invariant I8).
 */
class PaymentActivity : ComponentActivity() {

  private val purchasesEnabled get() = MONETIZATION.purchasesEnabled

  // Resolved lazily and only when purchases are enabled — in ComingSoon it is not even in Koin.
  private val controller: PaymentController by lazy { get() }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (!purchasesEnabled) {
      startActivity(
        Intent(this, MainActivity::class.java)
          .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
      )
      finish()
      return
    }
    enableEdgeToEdge()
    if (savedInstanceState == null) get<Telemetry>().event(TelemetryEvent.PAYWALL_SHOWN)
    controller.proceedIntent(intent)

    setContent {
      val themeMode by applicationContext.themeModeFlow().collectAsState(initial = ThemeMode.DEFAULT)
      AppTheme(themeMode = themeMode) {
        PaymentScreen(controller = controller, onNavigateBack = { finish() })
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    if (purchasesEnabled) controller.proceedIntent(intent)
  }

  override fun onResume() {
    super.onResume()
    if (purchasesEnabled) controller.refreshData()
  }
}
