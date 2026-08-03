package io.github.alelk.pws.android.compose.payment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.alelk.pws.android.compose.R
import java.text.SimpleDateFormat
import java.util.Locale

private val purchaseDateFormat get() = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

/**
 * Paywall screen. Decoupled from any store SDK type — it renders the generic [PaymentProduct] /
 * [ActivePurchase] models exposed by [PaymentController].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
  controller: PaymentController,
  onNavigateBack: () -> Unit,
) {
  val uiState by controller.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  BackHandler(enabled = true, onBack = onNavigateBack)

  val errorMessage = uiState.error?.let { errorStringRes(it) }?.let { stringResource(it) }
  LaunchedEffect(errorMessage) {
    errorMessage?.let { snackbarHostState.showSnackbar(it) }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.screen_payments_title)) },
        navigationIcon = {
          TextButton(onClick = onNavigateBack) {
            Text(stringResource(R.string.common_close))
          }
        },
      )
    },
  ) { paddingValues ->
    PaymentScreenContent(
      modifier = Modifier.padding(paddingValues),
      uiState = uiState,
      onPurchaseClick = { controller.makePurchase(it) },
      onAuthorizeClick = { controller.authorize() },
    )
  }
}

@Composable
private fun PaymentScreenContent(
  modifier: Modifier = Modifier,
  uiState: PaymentUiState,
  onPurchaseClick: (String) -> Unit,
  onAuthorizeClick: () -> Unit,
) {
  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item { Text(stringResource(R.string.payment_products_title), style = MaterialTheme.typography.titleLarge) }

      if (uiState.availableProducts.isEmpty() && !uiState.isLoading) {
        item { Text(stringResource(R.string.payment_products_empty)) }
      } else {
        items(uiState.availableProducts) { product ->
          ProductItem(product = product, onPurchaseClick = { onPurchaseClick(product.id) })
        }
      }

      if (!uiState.isAuthorized) {
        item { AuthorizationPrompt(onAuthorizeClick) }
      }

      if (uiState.purchases.size > 1) {
        item { MultipleSubscriptionsWarning() }
      }

      if (uiState.isAuthorized && uiState.purchases.isNotEmpty()) {
        item {
          Spacer(Modifier.height(16.dp))
          Text(stringResource(R.string.payment_active_purchases_title), style = MaterialTheme.typography.titleLarge)
        }
        items(uiState.purchases) { purchase -> ActivePurchaseItem(purchase) }
      }
    }

    if (uiState.isLoading) {
      CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
  }
}

@Composable
private fun ProductItem(product: PaymentProduct, onPurchaseClick: () -> Unit) {
  ListItem(
    modifier = Modifier.clickable(onClick = onPurchaseClick),
    headlineContent = { Text(product.title) },
    supportingContent = { product.description?.let { Text(it) } },
    trailingContent = {
      Text(product.priceLabel, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    },
  )
  HorizontalDivider()
}

@Composable
private fun ActivePurchaseItem(purchase: ActivePurchase) {
  val expiration = purchase.expiration
  if (expiration != null) {
    ListItem(
      headlineContent = { Text(purchase.title) },
      overlineContent = { Text(stringResource(R.string.payment_purchase_id, purchase.invoiceId)) },
      supportingContent = {
        Text(stringResource(R.string.payment_active_purchase_valid_until, purchaseDateFormat.format(expiration)))
      },
    )
  } else {
    ListItem(
      headlineContent = { Text(purchase.title) },
      supportingContent = { Text(stringResource(R.string.payment_purchase_id, purchase.invoiceId)) },
    )
  }
  HorizontalDivider()
}

@Composable
private fun AuthorizationPrompt(onAuthorizeClick: () -> Unit) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(stringResource(R.string.payment_auth_prompt_title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
      Text(stringResource(R.string.payment_auth_prompt_description), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
      Button(onClick = onAuthorizeClick) { Text(stringResource(R.string.payment_auth_button)) }
    }
  }
}

@Composable
private fun MultipleSubscriptionsWarning() {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(stringResource(R.string.payment_multiple_subscriptions_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
      Text(stringResource(R.string.payment_multiple_subscriptions_description), style = MaterialTheme.typography.bodyMedium)
    }
  }
}

private fun errorStringRes(error: PaymentError): Int = when (error) {
  is PaymentError.AuthCheckFailed -> R.string.error_auth_check_failed
  is PaymentError.ProductsLoadingFailed -> R.string.error_products_loading_failed
  is PaymentError.PurchasesLoadingFailed -> R.string.error_purchases_loading_failed
  is PaymentError.PurchaseFailed -> R.string.error_purchase_failed
  PaymentError.AuthorizationRequired -> R.string.error_authorization_required
  PaymentError.NoStoreApp -> R.string.error_no_rustore_app
  PaymentError.PurchaseCancelled -> R.string.info_purchase_cancelled
}
