/*
 * Copyright (C) 2018 The P&W Songs Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alelk.pws.database.util

import android.util.Log
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

/**
 * Localized strings provider
 *
 * Created by Alex Elkin on 18.07.17.
 */
object LocalizedStringsProvider {
  private const val RESOURCE_BUNDLE_NAME = "strings"
  private val LOG_TAG = LocalizedStringsProvider::class.java.getSimpleName()

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
