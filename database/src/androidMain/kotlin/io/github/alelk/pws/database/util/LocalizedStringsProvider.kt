package io.github.alelk.pws.database.util

import android.util.Log
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

/**
 * Localized strings provider
 *
 * Created by Alex Elkin on 18.07.17.
 */
// todo: refactor; move to app module?
object LocalizedStringsProvider {
  private const val RESOURCE_BUNDLE_NAME = "strings"
  private val LOG_TAG = LocalizedStringsProvider::class.java.simpleName

  @JvmStatic
  fun getResource(stringKey: String, locale: Locale): String? {
    val rb: ResourceBundle = try {
      getBundle(locale)
    } catch (exc: MissingResourceException) {
      Log.w(
        LOG_TAG,
        "Cannot get resource '$stringKey' for the locale $locale: No resource bundle found. Trying to get resource from default resource bundle.."
      )
      bundle
    }
    return rb.getString(stringKey)
  }

  private val bundle: ResourceBundle
    get() = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME)

  private fun getBundle(locale: Locale): ResourceBundle {
    return ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME, locale)
  }
}
