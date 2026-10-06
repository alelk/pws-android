package io.github.alelk.pws.android.compose.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.StringRes
import io.github.alelk.pws.android.compose.R
import io.github.alelk.pws.domain.telemetry.Telemetry

/** Shows a short user-visible message from a string resource (never a raw exception text). */
fun interface ShellToaster {
  fun show(@StringRes messageRes: Int)
}

/** [ShellToaster] backed by a platform [Toast]. */
class AndroidShellToaster(private val context: Context) : ShellToaster {
  override fun show(@StringRes messageRes: Int) {
    Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
  }
}

/**
 * Starts [intent]; when no app can handle it, tells the user and records the failure instead of crashing.
 */
internal fun Context.startActivityOrReport(intent: Intent, telemetry: Telemetry, toaster: ShellToaster) {
  try {
    startActivity(intent)
  } catch (e: ActivityNotFoundException) {
    telemetry.recordError(e, "no_app_to_open_link")
    toaster.show(R.string.no_app_to_open_link)
  }
}
