package io.github.alelk.pws.android.compose.donation

import android.content.SharedPreferences
import io.github.alelk.pws.domain.donationprompt.model.DonationPromptState
import io.github.alelk.pws.domain.donationprompt.repository.DonationPromptStateReadRepository
import io.github.alelk.pws.domain.donationprompt.repository.DonationPromptStateWriteRepository

private const val KEY_SUPPRESS_UNTIL = "donation_suppress_until"
private const val KEY_HAS_CLICKED_DONATE = "donation_has_clicked"
private const val KEY_DISMISSED_COUNT = "donation_dismissed_count"

/**
 * SharedPreferences-backed implementation of donation prompt state persistence.
 *
 * Stores three scalar values — no schema, no migration needed.
 * Instantiate with a dedicated preferences file: `"pws_donation"`.
 */
class SharedPrefsDonationPromptStateRepository(
  private val prefs: SharedPreferences,
) : DonationPromptStateReadRepository, DonationPromptStateWriteRepository {

  override suspend fun get(): DonationPromptState = DonationPromptState(
    suppressUntilViewCount = prefs.getLong(KEY_SUPPRESS_UNTIL, 0L),
    hasClickedDonate = prefs.getBoolean(KEY_HAS_CLICKED_DONATE, false),
    dismissedCount = prefs.getInt(KEY_DISMISSED_COUNT, 0),
  )

  override suspend fun save(state: DonationPromptState) {
    prefs.edit()
      .putLong(KEY_SUPPRESS_UNTIL, state.suppressUntilViewCount)
      .putBoolean(KEY_HAS_CLICKED_DONATE, state.hasClickedDonate)
      .putInt(KEY_DISMISSED_COUNT, state.dismissedCount)
      .apply()
  }
}

