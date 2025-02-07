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
package io.github.alelk.pws.android.app.activity

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import android.os.Bundle
import android.view.MenuInflater
import android.view.View
import androidx.annotation.LayoutRes
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * A [android.preference.PreferenceActivity] which supports AppCompat.
 *
 * @author Alex Elkin
 */
@AndroidEntryPoint
abstract class AppCompatPreferenceActivity : AppCompatActivity() {
  private var mDelegate: AppCompatDelegate? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    delegate.installViewFactory()
    delegate.onCreate(savedInstanceState)
    super.onCreate(savedInstanceState)
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
    delegate.onPostCreate(savedInstanceState)
  }

  override fun getSupportActionBar(): ActionBar? = delegate.supportActionBar

  override fun getMenuInflater(): MenuInflater {
    return delegate.menuInflater
  }

  override fun setContentView(@LayoutRes layoutResID: Int) {
    delegate.setContentView(layoutResID)
  }

  override fun setContentView(view: View) {
    delegate.setContentView(view)
  }

  override fun setContentView(view: View, params: ViewGroup.LayoutParams) {
    delegate.setContentView(view, params)
  }

  override fun addContentView(view: View, params: ViewGroup.LayoutParams) {
    delegate.addContentView(view, params)
  }

  override fun onPostResume() {
    super.onPostResume()
    delegate.onPostResume()
  }

  override fun onTitleChanged(title: CharSequence, color: Int) {
    super.onTitleChanged(title, color)
    delegate.setTitle(title)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    delegate.onConfigurationChanged(newConfig)
  }

  @Deprecated("Deprecated in Java")
  override fun onStop() {
    super.onStop()
    delegate.onStop()
  }

  @Deprecated("Deprecated in Java")
  override fun onDestroy() {
    super.onDestroy()
    delegate.onDestroy()
  }

  override fun invalidateOptionsMenu() {
    delegate.invalidateOptionsMenu()
  }

  override fun getDelegate(): AppCompatDelegate {
    if (mDelegate == null) {
      mDelegate = AppCompatDelegate.create(this, null)
    }
    return mDelegate!!
  }
}