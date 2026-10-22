package com.example.app.android.payment

import android.content.Intent
import com.example.app.domain.telemetry.BillingTelemetry
import com.example.app.domain.telemetry.NoOpBillingTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * TEMPLATE — store flavor source set. Drives the paywall; registered in DI ONLY when
 * MonetizationMode.purchasesEnabled (see templates/flavor/store/FlavorIntegration.kt).
 *
 * Soft degradation: every provider call is guarded. A failure sets PaymentUiState.error, records a
 * non-fatal, and leaves the local entitlement untouched — the paywall shows a message instead of
 * crashing, and premium unlocked offline keeps working.
 *
 * Evidence: pws-android app-compose/src/rustore/.../payment/PaymentController.kt.
 */

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
    /** Only paid purchases / active subscriptions — the same filter the sync applies. */
    val purchases: List<ActivePurchase> = emptyList(),
    val isAuthorized: Boolean = false,
    val isLoading: Boolean = false,
    val error: PaymentError? = null,
) {
    /** Products the user does not already own. */
    val availableProducts: List<PaymentProduct>
        get() = products.filterNot { product -> purchases.any { it.productId == product.id } }
}

class PaymentController(
    private val provider: PaymentProvider,
    private val purchaseSync: PurchaseSyncService,
    private val telemetry: BillingTelemetry = NoOpBillingTelemetry,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private var loadingJob: Job? = null

    /** Call from the paywall's onResume: the user may have signed in to the store app meanwhile. */
    fun refreshData() {
        loadingJob?.cancel()
        loadingJob =
            scope.launch {
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
        val authorized =
            try {
                provider.authStatus() == AuthStatus.AUTHORIZED
            } catch (e: Throwable) {
                _uiState.update { it.copy(isAuthorized = false, error = PaymentError.AuthCheckFailed(e)) }
                telemetry.recordError(e, "payment_auth_check_failed", mapOf("stage" to "auth"))
                return
            }
        _uiState.update { it.copy(isAuthorized = authorized) }
        // Signed out: we do NOT sync an empty list "to be safe" — we don't sync at all.
        if (authorized) loadPurchases()
    }

    private suspend fun loadPurchases() {
        try {
            val purchases = provider.purchases()
            _uiState.update { it.copy(purchases = purchases.filter { p -> p.isPaid }) }
            purchaseSync.sync(purchases) // monotonic: grants/extends only
        } catch (e: Throwable) {
            // The list could not be fetched: nothing is written.
            _uiState.update { it.copy(error = PaymentError.PurchasesLoadingFailed(e)) }
            telemetry.recordError(e, "payment_purchases_load_failed", mapOf("stage" to "purchases"))
        }
    }

    private suspend fun loadProducts() {
        try {
            _uiState.update { it.copy(products = provider.products(ProductIds.ALL)) }
        } catch (e: Throwable) {
            _uiState.update { it.copy(error = PaymentError.ProductsLoadingFailed(e)) }
            telemetry.recordError(e, "payment_products_load_failed", mapOf("stage" to "products"))
        }
    }

    fun makePurchase(productId: String) {
        if (_uiState.value.isLoading) return
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = provider.purchase(productId)) {
                is PurchaseResult.Success -> {
                    reportPurchase("ok")
                    // Lifetime first, so a failing reload below cannot lose it; a subscription's end
                    // arrives with the purchase list.
                    runCatching { purchaseSync.onPurchaseSucceeded(productId) }
                        .onFailure { e -> telemetry.recordError(e, "purchase_grant_failed", mapOf("stage" to "grant")) }
                    loadPurchases()
                }

                is PurchaseResult.Cancelled -> {
                    reportPurchase("cancelled")
                    _uiState.update { it.copy(error = PaymentError.PurchaseCancelled) }
                }

                is PurchaseResult.Failed -> {
                    reportPurchase("error")
                    telemetry.recordError(result.cause, "purchase_failed", mapOf("stage" to "purchase"))
                    _uiState.update { it.copy(error = PaymentError.PurchaseFailed(result.cause)) }
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /** Only the outcome is reported — no product id, no store purchase id. */
    private fun reportPurchase(result: String) {
        telemetry.event("purchase", mapOf("result" to result))
    }

    fun authorize() {
        runCatching { provider.openAuthorization() }
            .onFailure { e ->
                _uiState.update { s -> s.copy(error = PaymentError.NoStoreApp) }
                telemetry.recordError(e, "payment_authorization_unavailable")
            }
    }

    fun proceedIntent(intent: Intent) {
        provider.proceedIntent(intent)
    }
}
