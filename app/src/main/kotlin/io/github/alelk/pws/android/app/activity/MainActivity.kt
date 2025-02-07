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

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import io.github.alelk.pws.android.app.activity.base.AppCompatThemedActivity
import io.github.alelk.pws.android.app.theme.ThemeType
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.database.BuildConfig
import timber.log.Timber

@AndroidEntryPoint
open class MainActivity : AppCompatThemedActivity() {

  private val drawerLayout by lazy { findViewById<DrawerLayout>(R.id.layout_main_drawer) }
  private val navController by lazy { (supportFragmentManager.findFragmentById(R.id.app_nav_host_fragment) as NavHostFragment).navController }
  private val navigationView by lazy { findViewById<NavigationView>(R.id.nav_view) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    val fabSearchText = findViewById<FloatingActionButton>(R.id.fab_search_text)
    val fabSearchNumber = findViewById<FloatingActionButton>(R.id.fab_search_number)
    fabSearchText!!.setOnClickListener(onButtonClick)
    fabSearchNumber!!.setOnClickListener(onButtonClick)
    findViewById<View>(R.id.btn_search_song_number).setOnClickListener(onButtonClick)
    findViewById<View>(R.id.btn_search_song_text).setOnClickListener(onButtonClick)
    val appBar = findViewById<AppBarLayout>(R.id.appbar_main)
    appBar.addOnOffsetChangedListener { _: AppBarLayout?, verticalOffset: Int ->
      if (verticalOffset == 0) {
        fabSearchText.hide()
        fabSearchNumber.hide()
      } else {
        fabSearchText.show()
        fabSearchNumber.show()
      }
    }

    val toolbar = findViewById<Toolbar>(R.id.toolbar_main)
    setSupportActionBar(toolbar)
    val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer)
    drawerLayout!!.addDrawerListener(toggle)
    toggle.syncState()

    val collapsingToolbarLayout = findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar_main)

    navigationView.setupWithNavController(navController)
    navController.addOnDestinationChangedListener { _, destination, _ ->
      when (destination.id) {
        R.id.readNowFragment -> {
          appBar.setExpanded(true, true)
          collapsingToolbarLayout.title = getString(R.string.lbl_drawer_main_home)
        }
        R.id.booksFragment -> {
          appBar.setExpanded(false, true)
          collapsingToolbarLayout.title = getString(R.string.lbl_drawer_main_books)
        }
        R.id.historyFragment -> {
          appBar.setExpanded(false, true)
          collapsingToolbarLayout.title = getString(R.string.lbl_drawer_main_history)
        }
        R.id.favoritesFragment -> {
          appBar.setExpanded(false, true)
          collapsingToolbarLayout.title = getString(R.string.lbl_drawer_main_favorite)
        }
        else -> {
          appBar.setExpanded(false, true)
        }
      }
    }

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }
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

  private val onButtonClick = View.OnClickListener { view: View ->
    when (view.id) {
      R.id.btn_search_song_number, R.id.fab_search_number -> {
        val intentSearchNumber = Intent(baseContext, SearchActivity::class.java)
        intentSearchNumber.putExtra(
            SearchActivity.KEY_INPUT_TYPE,
          InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        )
        startActivity(intentSearchNumber)
      }

      R.id.btn_search_song_text, R.id.fab_search_text -> {
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
}