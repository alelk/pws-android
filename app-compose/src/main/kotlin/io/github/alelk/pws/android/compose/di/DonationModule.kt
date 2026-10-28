package io.github.alelk.pws.android.compose.di

import android.content.Context
import io.github.alelk.pws.android.compose.donation.SharedPrefsDonationPromptStateRepository
import io.github.alelk.pws.android.compose.flavor.MONETIZATION
import io.github.alelk.pws.domain.donationprompt.config.DonationConfig
import io.github.alelk.pws.domain.donationprompt.repository.DonationPromptStateReadRepository
import io.github.alelk.pws.domain.donationprompt.repository.DonationPromptStateWriteRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.binds
import org.koin.dsl.module

internal val donationModule = module {
  // Donation prompt is on only for donation-mode builds; premium-selling builds suppress it.
  single { DonationConfig(enabled = MONETIZATION.donationsEnabled, boostyUrl = "https://boosty.to/hymna") }
  single {
    SharedPrefsDonationPromptStateRepository(
      androidContext().getSharedPreferences("pws_donation", Context.MODE_PRIVATE),
    )
  } binds arrayOf(DonationPromptStateReadRepository::class, DonationPromptStateWriteRepository::class)
}
