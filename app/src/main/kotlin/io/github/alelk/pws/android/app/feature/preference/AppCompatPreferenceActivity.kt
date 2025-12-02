package io.github.alelk.pws.android.app.feature.preference

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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