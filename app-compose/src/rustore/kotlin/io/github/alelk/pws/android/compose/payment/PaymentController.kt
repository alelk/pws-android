package io.github.alelk.pws.android.compose.payment

import io.github.alelk.pws.domain.telemetry.NoOpTelemetry
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.github.alelk.pws.domain.telemetry.TelemetryAttr
import io.github.alelk.pws.domain.telemetry.TelemetryEvent
import io.github.alelk.pws.domain.telemetry.TelemetryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Paywall error surface, decoupled from any store SDK type. */
sealed interface PaymentError {
  data object AuthorizationRequired : PaymentError
  data object NoStoreApp : PaymentError
  data object PurchaseCancelled : PaymentError
  data class AuthCheckFailed(val cause: Throwable) : PaymentError
  data class ProductsLoadingFailed(val cause: Throwable) : PaymentError
  data class PurchasesLoadingFailed(val cause: Throwable) : PaymentError
  data class PurchaseFailed(val cause: Throwable) : PaymentError
}

data class PaymentUiState(
  val products: List<PaymentProduct> = emptyList(),
  val purchases: List<ActivePurchase> = emptyList(),
  val isAuthorized: Boolean = false,
  val isLoading: Boolean = false,
  val error: PaymentError? = null,
) {
  /** Products the user does not already own. */
  val availableProducts: List<PaymentProduct>
    get() = products.filterNot { product -> purchases.any { it.productId == product.id } }
}

/**
 * Drives the paywall UI. Wraps a [PaymentProvider] and reconciles online purchases into the offline
 * DataStore via [PurchaseSyncService].
 *
 * Soft degradation (monetisation currently disabled): every provider call is guarded — a failure
 * sets [PaymentUiState.error] and leaves the offline entitlement untouched, so the paywall shows a
 * message instead of crashing and premium features unlocked offline keep working.
 */
class PaymentController(
  private val provider: PaymentProvider,
  private val purchaseSync: PurchaseSyncService,
  private val telemetry: Telemetry = NoOpTelemetry,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val _uiState = MutableStateFlow(PaymentUiState())
  val uiState = _uiState.asStateFlow()

  private var loadingJob: Job? = null

  fun refreshData() {
    loadingJob?.cancel()
    loadingJob = scope.launch {
      _uiState.update { it.copy(isLoading = true, error = null) }
      try {
        checkAuthAndLoadPurchases()
        loadProducts()
      } finally {
        _uiState.update { it.copy(isLoading = false) }
      }
    }
  }

  private suspend fun checkAuthAndLoadPurchases() {
    val authorized = try {
      provider.authStatus() == AuthStatus.AUTHORIZED
    } catch (e: Throwable) {
      _uiState.update { it.copy(isAuthorized = false, error = PaymentError.AuthCheckFailed(e)) }
      // Store-SDK failures degrade silently for the user; without a non-fatal they would be
      // invisible to us too.
      telemetry.recordError(e, "payment_auth_check_failed", mapOf(TelemetryAttr.STAGE to "auth"))
      return
    }
    _uiState.update { it.copy(isAuthorized = authorized) }
    if (authorized) loadPurchases()
  }

  private suspend fun loadPurchases() {
    try {
      val purchases = provider.purchases()
      _uiState.update { it.copy(purchases = purchases) }
      purchaseSync.sync(purchases)
    } catch (e: Throwable) {
      _uiState.update { it.copy(error = PaymentError.PurchasesLoadingFailed(e)) }
      telemetry.recordError(e, "payment_purchases_load_failed", mapOf(TelemetryAttr.STAGE to "purchases"))
    }
  }

  private suspend fun loadProducts() {
    try {
      _uiState.update { it.copy(products = provider.products(ProductIds.ALL)) }
    } catch (e: Throwable) {
      _uiState.update { it.copy(error = PaymentError.ProductsLoadingFailed(e)) }
      telemetry.recordError(e, "payment_products_load_failed", mapOf(TelemetryAttr.STAGE to "products"))
    }
  }

  fun makePurchase(productId: String) {
    if (_uiState.value.isLoading) return
    scope.launch {
      _uiState.update { it.copy(isLoading = true, error = null) }
      when (val result = provider.purchase(productId)) {
        is PurchaseResult.Success -> {
          reportPurchase(TelemetryResult.OK)
          loadPurchases()
        }

        is PurchaseResult.Cancelled -> {
          reportPurchase(TelemetryResult.CANCELLED)
          _uiState.update { it.copy(error = PaymentError.PurchaseCancelled) }
        }

        is PurchaseResult.Failed -> {
          reportPurchase(TelemetryResult.ERROR)
          telemetry.recordError(result.cause, "purchase_failed", mapOf(TelemetryAttr.STAGE to "purchase"))
          _uiState.update { it.copy(error = PaymentError.PurchaseFailed(result.cause)) }
        }
      }
      _uiState.update { it.copy(isLoading = false) }
    }
  }

  /** Product id is a build constant, never user data — safe to omit; only the outcome is reported. */
  private fun reportPurchase(result: String) {
    telemetry.event(TelemetryEvent.PURCHASE, mapOf(TelemetryAttr.RESULT to result))
  }

  fun authorize() {
    runCatching { provider.openAuthorization() }
      .onFailure { e ->
        _uiState.update { s -> s.copy(error = PaymentError.NoStoreApp) }
        telemetry.recordError(e, "payment_authorization_unavailable")
      }
  }

  fun proceedIntent(intent: android.content.Intent) {
    provider.proceedIntent(intent)
  }
}
