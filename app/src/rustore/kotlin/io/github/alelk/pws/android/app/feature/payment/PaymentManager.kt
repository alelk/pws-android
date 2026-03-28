package io.github.alelk.pws.android.app.feature.payment

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.rustore.sdk.pay.ProductInteractor
import ru.rustore.sdk.pay.PurchaseInteractor
import ru.rustore.sdk.pay.UserInteractor
import ru.rustore.sdk.pay.model.PreferredPurchaseType
import ru.rustore.sdk.pay.model.Product
import ru.rustore.sdk.pay.model.ProductId
import ru.rustore.sdk.pay.model.ProductPurchase
import ru.rustore.sdk.pay.model.ProductPurchaseParams
import ru.rustore.sdk.pay.model.Purchase
import ru.rustore.sdk.pay.model.RuStorePaymentException
import ru.rustore.sdk.pay.model.SubscriptionPurchase
import ru.rustore.sdk.pay.model.UserAuthorizationStatus
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

object ProductIds {
  const val FULL_ACCESS_V1 = "full_access_v1"
  const val MONTHLY_SUBSCRIPTION_V1 = "monthly_subscription_v1"
  const val YEARLY_SUBSCRIPTION_V1 = "yearly_subscription_v1"
  val ALL = listOf(FULL_ACCESS_V1, MONTHLY_SUBSCRIPTION_V1, YEARLY_SUBSCRIPTION_V1)
}

sealed class PaymentError(val originalException: Throwable? = null) {
  data class AuthorizationCheckFailed(val cause: Throwable) : PaymentError(cause)
  data object NoRuStoreApp : PaymentError()
  data class ProductsLoadingFailed(val cause: Throwable) : PaymentError(cause)
  data class PurchasesLoadingFailed(val cause: Throwable) : PaymentError(cause)
  data object ProductPurchaseCancelled : PaymentError()
  data class PurchaseFailed(val cause: Throwable) : PaymentError(cause)
  data object AuthorizationRequired : PaymentError()
}

data class PaymentUiState(
  val allProducts: List<Product> = emptyList(),
  val purchases: List<Purchase> = emptyList(),
  val isAuthorized: Boolean = false,
  val isLoading: Boolean = false,
  val isInitialDataFetched: Boolean = false,
  val fullAccessPaid: Boolean? = null,
  val subscriptionUntil: Date? = null,
  val error: PaymentError? = null,
) {
  val isPremiumActive: Boolean? = when {
    fullAccessPaid == true -> true
    subscriptionUntil != null -> subscriptionUntil.after(Date())
    isInitialDataFetched -> false
    else -> null
  }
  val purchaseProductIds
    get() = purchases.filterIsInstance<ProductPurchase>().map { it.productId } + purchases.filterIsInstance<SubscriptionPurchase>().map { it.productId }

  val availableProducts: List<Product>
    get() = allProducts.filterNot { it.productId in purchaseProductIds }
}

object PaymentPreferenceKeys {
  val PURCHASE_FULL_ACCESS = booleanPreferencesKey("purchase_full_access")
  val PURCHASE_SUBSCRIPTION_UNTIL = stringPreferencesKey("purchase_subscription_until")
}

val preferenceSubscriptionUntilDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

@Singleton
class PaymentManager @Inject constructor(
  @ApplicationContext private val context: Context,
  private val purchaseInteractor: PurchaseInteractor,
  private val userInteractor: UserInteractor,
  private val productsInteractor: ProductInteractor,
  private val datastore: DataStore<Preferences>
) {
  private val _uiState = MutableStateFlow(PaymentUiState())
  val uiState = _uiState.asStateFlow()

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  init {
    scope.launch {
      datastore.data
        .map { preferences -> preferences[PaymentPreferenceKeys.PURCHASE_FULL_ACCESS] }
        .distinctUntilChanged()
        .filterNotNull()
        .collectLatest { isPaid ->
          _uiState.update { it.copy(fullAccessPaid = isPaid) }
        }
    }
    scope.launch {
      datastore.data
        .map { preferences -> preferences[PaymentPreferenceKeys.PURCHASE_SUBSCRIPTION_UNTIL] }
        .distinctUntilChanged()
        .filterNotNull()
        .collectLatest { until ->
          runCatching {
            val date = preferenceSubscriptionUntilDateFormat.parse(until)
            _uiState.update { it.copy(subscriptionUntil = date) }
          }.onFailure { exc ->
            Timber.e(exc, "Error parsing subscription until date from preferences: $until")
          }
        }
    }
    refreshData()
  }

  private var dataLoadingJob: Job? = null
  fun refreshData() {
    dataLoadingJob?.cancel()
    dataLoadingJob = scope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        checkAuthAndLoadPurchases()
        loadProducts()
      } finally {
        _uiState.update { it.copy(isLoading = false, isInitialDataFetched = true) }
      }
    }
  }

  /** Load configured Rustore products. It's possible to load products without authorization. */
  private suspend fun loadProducts() {
    try {
      val products = productsInteractor.getProducts(ProductIds.ALL.map { ProductId(it) }).coAwait()
      _uiState.update { it.copy(allProducts = products) }
    } catch (exc: Throwable) {
      Timber.e(exc, "Error products loading")
      _uiState.update { it.copy(error = PaymentError.ProductsLoadingFailed(exc)) }
    }
  }

  private suspend fun checkAuthAndLoadPurchases() {
    _uiState.update { it.copy(isAuthorized = false) }
    try {
      when (userInteractor.getUserAuthorizationStatus().coAwait()) {
        UserAuthorizationStatus.AUTHORIZED -> {
          _uiState.update { it.copy(isAuthorized = true) }
          loadPurchases()
        }

        UserAuthorizationStatus.UNAUTHORIZED -> {
          _uiState.update { it.copy(isAuthorized = false) }
        }
      }
    } catch (exc: Throwable) {
      Timber.e(exc, "Error when checking user authorization")
      _uiState.update { it.copy(error = PaymentError.AuthorizationCheckFailed(exc)) }
    }
  }

  private suspend fun loadPurchases() {
    if (!_uiState.value.isAuthorized) {
      _uiState.update { it.copy(error = PaymentError.AuthorizationRequired) }
    } else {
      try {
        val purchases = purchaseInteractor.getPurchases().coAwait()
        _uiState.update { s -> s.copy(purchases = purchases) }
        syncDataStoreWithPurchases(purchases)
      } catch (exc: Throwable) {
        Timber.e(exc, "Error purchases loading")
        _uiState.update { it.copy(error = PaymentError.PurchasesLoadingFailed(exc)) }
      }
    }
  }

  private suspend fun syncDataStoreWithPurchases(purchases: List<Purchase>) {
    val hasFullAccess = purchases.any { it is ProductPurchase && it.productId.value == ProductIds.FULL_ACCESS_V1 }
    setFullAccessPaid(hasFullAccess)
    val subscriptionUntil: Date? =
      purchases.filterIsInstance<SubscriptionPurchase>()
        .filter { it.productId.value in listOf(ProductIds.MONTHLY_SUBSCRIPTION_V1, ProductIds.YEARLY_SUBSCRIPTION_V1) }
        .maxByOrNull { it.expirationDate }
        ?.expirationDate
    subscriptionUntil?.let { setSubscriptionUntil(it) }
  }

  fun authorizeInRuStore() {
    runCatching {
      val intent = context.packageManager.getLaunchIntentForPackage("ru.vk.store")
      if (intent != null) context.startActivity(intent)
      else _uiState.update { it.copy(error = PaymentError.NoRuStoreApp) }
    }.onFailure { exc ->
      Timber.e(exc, "Unable to open RuStore app for authorization")
    }
  }

  suspend fun makePurchase(productId: String) {
    if (!_uiState.value.isLoading) {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val params = ProductPurchaseParams(productId = ProductId(productId))
        purchaseInteractor.purchase(params = params, preferredPurchaseType = PreferredPurchaseType.ONE_STEP).coAwait()
        setFullAccessPaid(true)
        loadPurchases()
        _uiState.update { it.copy(isLoading = false) }
      } catch (exc: RuStorePaymentException.ProductPurchaseCancelled) {
        Timber.w(exc, "Purchase cancelled by user")
        _uiState.update { it.copy(error = PaymentError.ProductPurchaseCancelled, isLoading = false) }
      } catch (exc: Throwable) {
        Timber.e(exc, "Error purchase")
        _uiState.update { it.copy(error = PaymentError.PurchaseFailed(exc), isLoading = false) }
      }
    }
  }

  suspend fun setFullAccessPaid(value: Boolean) {
    Timber.d("Set preference full access paid: $value")
    datastore.edit { it[PaymentPreferenceKeys.PURCHASE_FULL_ACCESS] = value }
  }

  suspend fun setSubscriptionUntil(date: Date) {
    val dateValue = preferenceSubscriptionUntilDateFormat.format(date)
    Timber.d("Set subscription until: $dateValue")
    datastore.edit { it[PaymentPreferenceKeys.PURCHASE_SUBSCRIPTION_UNTIL] = dateValue }
  }
}