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

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.alelk.pws.database.BuildConfig
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.base.AppCompatThemedActivity
import com.alelk.pws.pwapp.fragment.BooksFragment
import com.alelk.pws.pwapp.fragment.FavoritesFragment
import com.alelk.pws.pwapp.fragment.HistoryFragment
import com.alelk.pws.pwapp.fragment.ReadNowFragment
import com.alelk.pws.pwapp.theme.ThemeType
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import timber.log.Timber

open class MainActivity : AppCompatThemedActivity(), NavigationView.OnNavigationItemSelectedListener {
  private var mDrawerLayout: DrawerLayout? = null
  private var mNavigationItemId = R.id.drawer_main_home
  private var mFabSearchText: FloatingActionButton? = null
  private var mFabSearchNumber: FloatingActionButton? = null
  private var mAppBar: AppBarLayout? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    if (savedInstanceState != null) {
      mNavigationItemId = savedInstanceState.getInt(KEY_NAVIGATION_ITEM_ID)
    }
    mFabSearchText = findViewById(R.id.fab_search_text)
    mFabSearchNumber = findViewById(R.id.fab_search_number)
    mFabSearchText!!.setOnClickListener(onButtonClick)
    mFabSearchNumber!!.setOnClickListener(onButtonClick)
    findViewById<View>(R.id.btn_search_psalm_number).setOnClickListener(onButtonClick)
    findViewById<View>(R.id.btn_search_psalm_text).setOnClickListener(onButtonClick)
    mDrawerLayout = findViewById(R.id.layout_main_drawer)
    mAppBar = findViewById(R.id.appbar_main)
    mAppBar!!.addOnOffsetChangedListener { _: AppBarLayout?, verticalOffset: Int ->
      if (verticalOffset == 0) {
        mFabSearchText!!.hide()
        mFabSearchNumber!!.hide()
      } else {
        mFabSearchText!!.show()
        mFabSearchNumber!!.show()
      }
    }
    val toolbar = findViewById<Toolbar>(R.id.toolbar_main)
    setSupportActionBar(toolbar)
    val toggle = ActionBarDrawerToggle(
      this,
      mDrawerLayout,
      toolbar,
      R.string.open_drawer,
      R.string.close_drawer
    )
    mDrawerLayout!!.addDrawerListener(toggle)
    toggle.syncState()
    (findViewById<View>(R.id.nav_view) as NavigationView).setNavigationItemSelectedListener(this)
    displayFragment()

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putInt(KEY_NAVIGATION_ITEM_ID, mNavigationItemId)
  }

  override fun onResume() {
    super.onResume()
    displayFragment()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val id = item.itemId
    if (id == R.id.action_settings) {
      val intent = Intent(this, MainSettingsActivity::class.java)
      startActivity(intent)
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  @Deprecated("Deprecated in Java")
  override fun onBackPressed() {
    if (mDrawerLayout!!.isDrawerOpen(GravityCompat.START)) {
      mDrawerLayout!!.closeDrawer(GravityCompat.START)
    } else {
      super.onBackPressed()
    }
  }

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    val id = item.itemId
    var result = false
    val intent: Intent
    when (id) {
      R.id.drawer_main_history, R.id.drawer_main_favorite, R.id.drawer_main_books -> {
        mAppBar!!.setExpanded(false, true)
        mNavigationItemId = id
        displayFragment()
        result = true
      }

      R.id.drawer_main_home -> {
        mAppBar!!.setExpanded(true, true)
        mNavigationItemId = id
        displayFragment()
        result = true
      }

      R.id.drawer_main_settings -> {
        intent = Intent(this, MainSettingsActivity::class.java)
        startActivity(intent)
        result = true
      }

      R.id.drawer_main_categories -> {
        intent = Intent(this, TagsActivity::class.java)
        startActivity(intent)
        result = true
      }
    }
    val drawer = findViewById<DrawerLayout>(R.id.layout_main_drawer)
    drawer.closeDrawer(GravityCompat.START)
    return result
  }

  private fun displayFragment() {
    val fragmentTransaction = supportFragmentManager.beginTransaction()
    var fragment: Fragment? = null
    var titleResId = R.string.app_name
    when (mNavigationItemId) {
      R.id.drawer_main_home -> {
        fragment = ReadNowFragment()
        titleResId = R.string.lbl_drawer_main_home
      }

      R.id.drawer_main_books -> {
        fragment = BooksFragment()
        titleResId = R.string.lbl_drawer_main_books
      }

      R.id.drawer_main_history -> {
        fragment = HistoryFragment()
        titleResId = R.string.lbl_drawer_main_history
      }

      R.id.drawer_main_favorite -> {
        fragment = FavoritesFragment()
        titleResId = R.string.lbl_drawer_main_favorite
      }
    }
    if (fragment == null) return
    val collapsingToolbarLayout =
      findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar_main)
    collapsingToolbarLayout.title = getString(titleResId)
    fragmentTransaction.replace(R.id.fragment_main_container, fragment)
    fragmentTransaction.addToBackStack(null)
    fragmentTransaction.commit()
  }

  private val onButtonClick = View.OnClickListener { view: View ->
    when (view.id) {
      R.id.btn_search_psalm_number, R.id.fab_search_number -> {
        val intentSearchNumber = Intent(baseContext, SearchActivity::class.java)
        intentSearchNumber.putExtra(
          SearchActivity.KEY_INPUT_TYPE,
          InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        )
        startActivity(intentSearchNumber)
      }

      R.id.btn_search_psalm_text, R.id.fab_search_text -> {
        val intentSearchText = Intent(baseContext, SearchActivity::class.java)
        intentSearchText.putExtra(
          SearchActivity.KEY_INPUT_TYPE,
          InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
        )
        startActivity(intentSearchText)
      }
    }
  }
  override val themeType: ThemeType
    get() = ThemeType.NO_ACTION_BAR

  companion object {
    private const val KEY_NAVIGATION_ITEM_ID = "navigationItemId"
  }
}