package io.github.alelk.pws.android.compose.platform

import android.content.Context
import android.content.Intent
import io.github.alelk.pws.domain.telemetry.Telemetry

/** Shares plain text through the system chooser; a missing app is reported, not thrown. */
class AndroidShareActions internal constructor(
  private val context: Context,
  private val telemetry: Telemetry,
  private val toaster: ShellToaster = AndroidShellToaster(context),
) {
  fun shareText(text: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivityOrReport(Intent.createChooser(sendIntent, null), telemetry, toaster)
  }
}
