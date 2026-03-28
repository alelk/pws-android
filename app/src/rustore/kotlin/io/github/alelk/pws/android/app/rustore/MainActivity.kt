package io.github.alelk.pws.android.app.rustore

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.feature.home.MainActivity as BaseMainActivity
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.android.app.feature.payment.PaymentActivity
import ru.rustore.sdk.pay.IntentInteractor
import javax.inject.Inject

/**
 * RuStore-specific MainActivity that extends the base MainActivity
 * and adds RuStore payment integration
 */
@AndroidEntryPoint
class MainActivity : BaseMainActivity() {

  @Inject
  lateinit var intentInteractor: IntentInteractor

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState == null) {
      intentInteractor.proceedIntent(intent)
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    intentInteractor.proceedIntent(intent)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val id = item.itemId
    if (id == R.id.paymentActiviy) {
      val intent = Intent(this, PaymentActivity::class.java)
      startActivity(intent)
      return true
    }
    return super.onOptionsItemSelected(item)
  }
}

