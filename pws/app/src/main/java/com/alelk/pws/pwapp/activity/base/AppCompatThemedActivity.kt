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
package com.alelk.pws.pwapp.activity.base

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alelk.pws.pwapp.theme.OnThemeChangeListener
import com.alelk.pws.pwapp.theme.ThemePreferences
import com.alelk.pws.pwapp.theme.ThemeType

/**
 * AppCompat Themed Activity
 *
 * Created by Alex Elkin on 24.08.17.
 */
open class AppCompatThemedActivity : AppCompatActivity() {
  private var mThemePreferences: ThemePreferences? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mThemePreferences = ThemePreferences(this)
    setTheme(mThemePreferences!!.appTheme.getThemeResId(themeType))
    mThemePreferences!!.registerThemeChangeListener(onThemeChange)
  }

  override fun onDestroy() {
    super.onDestroy()
    mThemePreferences!!.unregisterThemeChangeListener(onThemeChange)
  }

  private fun restartActivity() {
    val intent = intent
    intent.removeCategory(Intent.CATEGORY_LAUNCHER)
    startActivity(intent)
    finish()
  }

  private val onThemeChange = OnThemeChangeListener { restartActivity() }
  protected open val themeType: ThemeType?
    get() = ThemeType.NORMAL
}