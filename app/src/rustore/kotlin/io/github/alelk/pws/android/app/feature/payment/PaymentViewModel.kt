package io.github.alelk.pws.android.app.feature.payment

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.rustore.sdk.pay.model.RuStorePaymentException
import timber.log.Timber


@HiltViewModel
class PaymentViewModel @Inject constructor(
  private val paymentManager: PaymentManager
) : ViewModel() {

  val uiState: StateFlow<PaymentUiState> = paymentManager.uiState

  fun refreshData() {
    paymentManager.refreshData()
  }

  fun authorizeInRuStore() {
    paymentManager.authorizeInRuStore()
  }

  fun makePurchase(productId: String) {
    viewModelScope.launch {
      try {
        paymentManager.makePurchase(productId)
      } catch (exc: RuStorePaymentException.ProductPurchaseCancelled) {
        Timber.w(exc, "Purchase cancelled by user")
      } catch (exc: Throwable) {
        Timber.e(exc, "Error purchase")
      }
    }
  }
}