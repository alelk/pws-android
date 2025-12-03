package io.github.alelk.pws.android.app.feature.payment

import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull

suspend inline fun <T> PaymentManager.handlePremiumFeature(fragmentManager: FragmentManager, onPaid: () -> T): T? =
  if (this.uiState.mapNotNull { it.isPremiumActive }.first()) onPaid()
  else PaymentDialogFragment().show(fragmentManager, PaymentDialogFragment::class.simpleName).let { null }