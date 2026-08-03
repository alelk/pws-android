package io.github.alelk.pws.android.compose.payment

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
    }
  }

  private suspend fun loadProducts() {
    try {
      _uiState.update { it.copy(products = provider.products(ProductIds.ALL)) }
    } catch (e: Throwable) {
      _uiState.update { it.copy(error = PaymentError.ProductsLoadingFailed(e)) }
    }
  }

  fun makePurchase(productId: String) {
    if (_uiState.value.isLoading) return
    scope.launch {
      _uiState.update { it.copy(isLoading = true, error = null) }
      when (val result = provider.purchase(productId)) {
        is PurchaseResult.Success -> loadPurchases()
        is PurchaseResult.Cancelled ->
          _uiState.update { it.copy(error = PaymentError.PurchaseCancelled) }
        is PurchaseResult.Failed ->
          _uiState.update { it.copy(error = PaymentError.PurchaseFailed(result.cause)) }
      }
      _uiState.update { it.copy(isLoading = false) }
    }
  }

  fun authorize() {
    runCatching { provider.openAuthorization() }
      .onFailure { _uiState.update { s -> s.copy(error = PaymentError.NoStoreApp) } }
  }

  fun proceedIntent(intent: android.content.Intent) {
    provider.proceedIntent(intent)
  }
}
