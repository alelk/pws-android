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
package com.alelk.pws.pwapp.activity

import android.os.Bundle
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.base.AppCompatThemedActivity
import com.alelk.pws.pwapp.fragment.preference.RootPreferenceFragment

/**
 * A [PreferenceActivity] that contains main PWS App settings.
 *
 * Created by Alex Elkin on 18.02.2016.
 */
class MainSettingsActivity : AppCompatThemedActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)
//    setupActionBar()
    supportFragmentManager.beginTransaction().replace(R.id.fragment_settings, RootPreferenceFragment()).commit()
  }

//  private fun setupActionBar() {
//    val actionBar = supportActionBar
//    actionBar?.setDisplayHomeAsUpEnabled(true)
//  }
}