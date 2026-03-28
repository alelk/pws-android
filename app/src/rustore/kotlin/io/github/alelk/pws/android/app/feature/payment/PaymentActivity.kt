package io.github.alelk.pws.android.app.feature.payment

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.rustore.sdk.pay.RuStorePayClient

@AndroidEntryPoint
class PaymentActivity : ComponentActivity() {

  private val intentInteractor by lazy { RuStorePayClient.Companion.instance.getIntentInteractor() }
  private val viewModel: PaymentViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    intentInteractor.proceedIntent(intent)

    setContent {
        MaterialTheme {
            PaymentScreen(onNavigateBack = { finish() })
        }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    intentInteractor.proceedIntent(intent)
  }

  override fun onResume() {
    super.onResume()
    lifecycleScope.launch { viewModel.refreshData() }
  }
}