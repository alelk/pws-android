package io.github.alelk.pws.android.compose.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.alelk.pws.domain.telemetry.Telemetry

/** Opens web links and composes e-mails through other apps; a missing app is reported, not thrown. */
class AndroidUrlActions internal constructor(
  private val context: Context,
  private val telemetry: Telemetry,
  private val toaster: ShellToaster = AndroidShellToaster(context),
) {
  fun openUrl(url: String) = open(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

  fun sendEmail(mailto: String) = open(Intent(Intent.ACTION_SENDTO, Uri.parse(mailto)))

  private fun open(intent: Intent) = context.startActivityOrReport(intent, telemetry, toaster)
}
