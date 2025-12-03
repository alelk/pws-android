package io.github.alelk.pws.android.app.feature.payment

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.alelk.pws.android.app.R
import kotlinx.coroutines.launch
import ru.rustore.sdk.pay.model.Product
import ru.rustore.sdk.pay.model.Purchase
import ru.rustore.sdk.pay.model.SubscriptionPurchase
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
  viewModel: PaymentViewModel = viewModel(),
  showTopAppBar: Boolean = true,
  onNavigateBack: () -> Unit
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  BackHandler(enabled = true, onBack = onNavigateBack)

  val errorMessage = uiState.error?.let { mapErrorToString(it) }
  LaunchedEffect(errorMessage) {
    errorMessage?.let {
      snackbarHostState.showSnackbar(it)
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      if (showTopAppBar) {
        TopAppBar(
          title = { Text(stringResource(R.string.screen_payments_title)) },
          navigationIcon = {
            IconButton(onClick = onNavigateBack) {
              Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Close dialog")
            }
          }
        )
      }
    },
    bottomBar = {
      if (!showTopAppBar) {
        Column(modifier = Modifier.fillMaxWidth()) {
          HorizontalDivider()
          Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
          ) {
            TextButton(onClick = onNavigateBack, modifier = Modifier.align(Alignment.CenterEnd)) {
              Text(stringResource(R.string.common_close))
            }
          }
        }
      }
    }
  ) { paddingValues ->
    PaymentScreenContent(
      modifier = Modifier.padding(paddingValues),
      uiState = uiState,
      onPurchaseClick = { productId ->
        viewModel.viewModelScope.launch {
          viewModel.makePurchase(productId)
        }
      },
      onAuthorizeClick = {
        viewModel.authorizeInRuStore()
      }
    )
  }
}

@Composable
private fun PaymentScreenContent(
  modifier: Modifier = Modifier,
  uiState: PaymentUiState,
  onPurchaseClick: (String) -> Unit,
  onAuthorizeClick: () -> Unit
) {
  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Available products
      if (!uiState.isAuthorized || uiState.fullAccessPaid != true) {
        item { Text(text = stringResource(R.string.payment_products_title), style = MaterialTheme.typography.titleLarge) }
        if (uiState.allProducts.isEmpty() && !uiState.isLoading) {
          item { Text(stringResource(R.string.payment_products_empty)) }
        } else {
          items(uiState.availableProducts) { product ->
            ProductItem(product = product, onPurchaseClick = { onPurchaseClick(product.productId.value) })
          }
        }
      }

      item {
        if (!uiState.isAuthorized) {
          AuthorizationPrompt(onAuthorizeClick)
        }
      }

      if (uiState.purchases.size > 1) {
        item { MultipleSubscriptionsWarning() }
      }

      // Active purchases
      if (uiState.isAuthorized && uiState.purchases.isNotEmpty()) {
        item {
          Spacer(Modifier.height(16.dp))
          Text(text = stringResource(R.string.payment_active_purchases_title), style = MaterialTheme.typography.titleLarge)
        }
        items(uiState.purchases) { purchase ->
          ActivePurchaseItem(purchase = purchase)
        }
      }
    }

    if (uiState.isLoading) {
      CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
  }
}

@Composable
private fun AuthorizationPrompt(onAuthorizeClick: () -> Unit) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = stringResource(R.string.payment_auth_prompt_title),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center
      )
      Text(
        text = stringResource(R.string.payment_auth_prompt_description),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
      )
      Button(onClick = onAuthorizeClick) {
        Text(stringResource(R.string.payment_auth_button))
      }
    }
  }
}

@Composable
private fun ProductItem(product: Product, onPurchaseClick: () -> Unit) {
  ListItem(
    modifier = Modifier.clickable(onClick = onPurchaseClick),
    headlineContent = { Text(product.title.value) },
    supportingContent = { product.description?.value?.let { Text(it) } },
    trailingContent = {
      Text(
        text = product.amountLabel.value,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary
      )
    }
  )
  HorizontalDivider()
}

val dateFormat get() = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

@Composable
private fun ActivePurchaseItem(purchase: Purchase) {
  when (purchase) {
    is SubscriptionPurchase -> {
      ListItem(
        headlineContent = { Text(purchase.description.value) },
        overlineContent = { Text(stringResource(R.string.payment_purchase_id, purchase.invoiceId.value)) },
        supportingContent = { Text(stringResource(R.string.payment_active_purchase_valid_until, dateFormat.format(purchase.expirationDate))) },
      )
    }

    else -> {
      ListItem(
        headlineContent = { Text(purchase.description.value) },
        supportingContent = { Text(stringResource(R.string.payment_purchase_id, purchase.invoiceId.value)) }
      )
    }
  }
  HorizontalDivider()
}

@Composable
private fun MultipleSubscriptionsWarning() {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = stringResource(R.string.payment_multiple_subscriptions_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.error
      )
      Text(
        text = stringResource(R.string.payment_multiple_subscriptions_description),
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}

@Composable
private fun mapErrorToString(error: PaymentError): String {
  return when (error) {
    is PaymentError.AuthorizationCheckFailed -> stringResource(R.string.error_auth_check_failed)
    is PaymentError.ProductsLoadingFailed -> stringResource(R.string.error_products_loading_failed)
    is PaymentError.PurchasesLoadingFailed -> stringResource(R.string.error_purchases_loading_failed)
    is PaymentError.PurchaseFailed -> stringResource(R.string.error_purchase_failed)
    PaymentError.AuthorizationRequired -> stringResource(R.string.error_authorization_required)
    PaymentError.NoRuStoreApp -> stringResource(R.string.error_no_rustore_app)
    PaymentError.ProductPurchaseCancelled -> stringResource(R.string.info_purchase_cancelled)
  }
}

@Preview(showBackground = true)
@Composable
private fun PaymentScreenContentPreview() {
  MaterialTheme {
    PaymentScreenContent(
      uiState = PaymentUiState(
        isAuthorized = false,
        allProducts = listOf(
          // Здесь нужны mock-объекты Product, если они есть
        ),
        isLoading = false
      ),
      onPurchaseClick = {},
      onAuthorizeClick = {}
    )
  }
}