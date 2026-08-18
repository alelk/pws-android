package io.github.alelk.pws.android.compose.payment

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.alelk.pws.android.compose.themeModeFlow
import io.github.alelk.pws.features.theme.AppTheme
import io.github.alelk.pws.features.theme.ThemeMode
import org.koin.android.ext.android.inject

/**
 * Standalone paywall host. Also receives the RuStore payment deeplink (VIEW + BROWSABLE on the
 * `io.github.alelk.pws.app` scheme, declared in the rustore manifest overlay on the launcher
 * activity) — the intent is forwarded into the SDK via [PaymentController.proceedIntent].
 */
class PaymentActivity : ComponentActivity() {

  private val controller: PaymentController by inject()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
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
    controller.proceedIntent(intent)
  }

  override fun onResume() {
    super.onResume()
    controller.refreshData()
  }
}
