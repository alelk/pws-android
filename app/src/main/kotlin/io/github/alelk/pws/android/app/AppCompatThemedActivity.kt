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
package io.github.alelk.pws.android.app

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import io.github.alelk.pws.android.app.feature.preference.AppPreferencesViewModel
import io.github.alelk.pws.android.app.core.theme.ThemeType

/**
 * AppCompat Themed Activity
 *
 * Created by Alex Elkin on 24.08.17.
 */
open class AppCompatThemedActivity : AppCompatBackButtonActivity() {

  private val appPreferences: AppPreferencesViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setTheme(appPreferences.appTheme.value.getThemeResId(themeType))
  }

  protected open val themeType: ThemeType get() = ThemeType.NORMAL
}

open class AppCompatBackButtonActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    onBackPressedDispatcher.addCallback(this) {
      onSupportNavigateUp()
    }
  }
}