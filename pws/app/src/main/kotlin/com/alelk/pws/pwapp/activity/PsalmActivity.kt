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

import android.app.SearchManager
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.viewpager.widget.ViewPager
import com.alelk.pws.database.provider.PwsDataProviderContract
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.base.AppCompatThemedActivity
import com.alelk.pws.pwapp.adapter.PsalmTextFragmentStatePagerAdapter
import com.alelk.pws.pwapp.dialog.PsalmPreferencesDialogFragment
import com.alelk.pws.pwapp.dialog.PsalmPreferencesDialogFragment.OnPsalmPreferencesChangedCallbacks
import com.alelk.pws.pwapp.dialog.SearchPsalmNumberDialogFragment
import com.alelk.pws.pwapp.dialog.SearchPsalmNumberDialogFragment.SearchPsalmNumberDialogListener
import com.alelk.pws.pwapp.fragment.PsalmHeaderFragment
import com.alelk.pws.pwapp.fragment.PsalmTextFragment
import com.alelk.pws.pwapp.holder.PsalmHolder
import com.alelk.pws.pwapp.preference.PsalmPreferences
import com.alelk.pws.pwapp.theme.ThemeType
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

/**
 * Psalm Activity
 *
 * Created by Alex Elkin on 25.03.2016.
 */
class PsalmActivity : AppCompatThemedActivity(), PsalmTextFragment.Callbacks,
  SearchPsalmNumberDialogListener, OnPsalmPreferencesChangedCallbacks {
  private var mPsalmNumberId = -1L
  private var mPsalmPreferences: PsalmPreferences? = null
  private var mPagerPsalmText: ViewPager? = null
  private var mPsalmHeaderFragment: PsalmHeaderFragment? = null
  private var mFabFavorite: FloatingActionButton? = null
  private var mBookPsalmNumberIds: ArrayList<Long>? = null
  private var mPsalmTextPagerAdapter: PsalmTextFragmentStatePagerAdapter? = null
  private val mAddToHistoryHandler = Handler()
  private val mAddToHistoryRunnable = Runnable {
    if (mPsalmTextPagerAdapter == null || mPagerPsalmText == null) return@Runnable
    val fragment =
      mPsalmTextPagerAdapter?.registeredFragments?.get(mPagerPsalmText!!.currentItem) as PsalmTextFragment?
    fragment?.addPsalmToHistory()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    init()
    setContentView(R.layout.activity_psalm)
    val mToolbar = findViewById<Toolbar>(R.id.toolbar_psalm)
    mFabFavorite = findViewById(R.id.fab_psalm)
    mFabFavorite!!.setOnClickListener(FabFavoritesOnClick())
    setSupportActionBar(mToolbar)
    if (supportActionBar != null) {
      supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }
    mPsalmHeaderFragment =
      supportFragmentManager.findFragmentById(R.id.fragment_psalm_header) as PsalmHeaderFragment?
    if (mPsalmHeaderFragment == null) {
      mPsalmHeaderFragment = PsalmHeaderFragment()
      supportFragmentManager.beginTransaction()
        .add(R.id.fragment_psalm_header, mPsalmHeaderFragment!!).commit()
    }
    mPagerPsalmText = findViewById(R.id.pager_psalm_text)
    mPsalmTextPagerAdapter = PsalmTextFragmentStatePagerAdapter(
      supportFragmentManager,
      mBookPsalmNumberIds,
      mPsalmPreferences!!
    )
    mPagerPsalmText!!.adapter = mPsalmTextPagerAdapter
    mPagerPsalmText!!.currentItem = mBookPsalmNumberIds!!.indexOf(mPsalmNumberId)
  }

  private fun init() {
    if (mPsalmNumberId >= 0) return
    mPsalmNumberId = intent.getLongExtra(KEY_PSALM_NUMBER_ID, -10L)
    val preferences = PreferenceManager.getDefaultSharedPreferences(baseContext)
    mPsalmPreferences = PsalmPreferences(
      preferences.getFloat(PsalmTextFragment.KEY_PSALM_TEXT_SIZE, -1.0f),
      preferences.getBoolean(PsalmTextFragment.KEY_PSALM_TEXT_EXPANDED, true)
    )
    var cursor: Cursor? = null
    try {
      cursor = contentResolver.query(
        PwsDataProviderContract.PsalmNumbers.Book.BookPsalmNumbers.Info.getContentUri(mPsalmNumberId),
        null,
        null,
        null,
        null
      )
      if (cursor != null && cursor.moveToFirst()) {
        val psalmNumberIdsList = cursor.getString(
          cursor.getColumnIndex(PwsDataProviderContract.PsalmNumbers.Book.BookPsalmNumbers.Info.COLUMN_PSALMNUMBERID_LIST)
        )
          .split(",").toTypedArray()
        mBookPsalmNumberIds = ArrayList(psalmNumberIdsList.size)
        for (id in psalmNumberIdsList) {
          try {
            mBookPsalmNumberIds!!.add(id.toLong())
          } catch (ignored: NumberFormatException) {
          }
        }
      }
    } finally {
      cursor?.close()
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_psalm, menu)
    val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager?
    val searchView = menu.findItem(R.id.menu_search).actionView as SearchView
    if (searchManager != null) searchView.setSearchableInfo(
      searchManager.getSearchableInfo(
        componentName
      )
    )
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val id = item.itemId
    if (id == R.id.menu_jump) {
      val searchNumberDialog: DialogFragment =
        SearchPsalmNumberDialogFragment.newInstance(mPsalmNumberId)
      searchNumberDialog.show(
        supportFragmentManager,
        SearchPsalmNumberDialogFragment::class.java.simpleName
      )
      return true
    } else if (id == R.id.action_settings || id == R.id.menu_text_size) {
      if (mPsalmPreferences!!.textSize < 0) {
        val fragment =
          mPsalmTextPagerAdapter!!.registeredFragments[mPagerPsalmText!!.currentItem] as PsalmTextFragment
        mPsalmPreferences!!.textSize = fragment.psalmTextSize
      }
      val psalmPreferencesDialog: DialogFragment =
        PsalmPreferencesDialogFragment.newInstance(mPsalmPreferences!!)
      psalmPreferencesDialog.show(
        supportFragmentManager,
        PsalmPreferencesDialogFragment::class.java.simpleName
      )
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onUpdatePsalmInfo(psalmHolder: PsalmHolder?) {
    if (psalmHolder == null ||
      mBookPsalmNumberIds!![mPagerPsalmText!!.currentItem] != psalmHolder.psalmNumberId
    ) return
    val collapsingToolbarLayout =
      findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar_psalm)
    collapsingToolbarLayout.title = "№ " + psalmHolder.psalmNumber
    mPsalmHeaderFragment!!.updateUi(
      psalmHolder.psalmName,
      psalmHolder.bookName,
      psalmHolder.bibleRef
    )
    drawFavoriteFabIcon(psalmHolder.isFavoritePsalm)
    mAddToHistoryHandler.removeCallbacks(mAddToHistoryRunnable)
    mAddToHistoryHandler.postDelayed(mAddToHistoryRunnable, ADD_TO_HISTORY_DELAY.toLong())
  }

  override fun onRequestFullscreenMode() {
    val intent = Intent(this, PsalmFullscreenActivity::class.java)
    intent.putExtra(
      PsalmFullscreenActivity.KEY_PSALM_NUMBER_ID,
      mBookPsalmNumberIds!![mPagerPsalmText!!.currentItem]
    )
    startActivityForResult(intent, REQUEST_CODE_FULLSCREEN_ACTIVITY)
  }

  override fun onEditRequest(psalmNumberId: Long) {
    val intent = Intent(this, PsalmEditActivity::class.java)
    intent.putExtra(PsalmEditActivity.KEY_PSALM_NUMBER_ID, psalmNumberId)
    startActivityForResult(intent, REQUEST_CODE_EDIT_ACTIVITY)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      REQUEST_CODE_FULLSCREEN_ACTIVITY -> {
        if (resultCode != RESULT_OK || data == null) return
        mPsalmNumberId = data.getLongExtra(PsalmFullscreenActivity.KEY_PSALM_NUMBER_ID, -1)
        mPagerPsalmText!!.currentItem = mBookPsalmNumberIds!!.indexOf(mPsalmNumberId)
      }

      REQUEST_CODE_EDIT_ACTIVITY -> {
        if (data == null) return
        mPsalmNumberId = data.getLongExtra(KEY_PSALM_NUMBER_ID, -1L)
        mPagerPsalmText!!.currentItem = mBookPsalmNumberIds!!.indexOf(mPsalmNumberId)
        if (resultCode == RESULT_OK) {
          val fragment =
            mPsalmTextPagerAdapter!!.registeredFragments[mPagerPsalmText!!.currentItem] as PsalmTextFragment
          fragment.reloadUi()
        }
      }
    }
  }

  override fun onPositiveButtonClick(psalmNumberId: Long) {
    mPagerPsalmText!!.currentItem = mBookPsalmNumberIds!!.indexOf(psalmNumberId)
  }

  override fun onNegativeButtonClick() {}
  override fun onPreferencesChanged(preferences: PsalmPreferences?) {
    if (preferences != null) {
      val fragment =
        mPsalmTextPagerAdapter!!.registeredFragments[mPagerPsalmText!!.currentItem] as PsalmTextFragment
      fragment.applyPsalmPreferences(preferences)
    }
  }

  override fun onApplyPreferences(preferences: PsalmPreferences?) {
    mPsalmPreferences = preferences
    PreferenceManager.getDefaultSharedPreferences(baseContext)
      .edit()
      .putFloat(PsalmTextFragment.KEY_PSALM_TEXT_SIZE, preferences!!.textSize)
      .putBoolean(PsalmTextFragment.KEY_PSALM_TEXT_EXPANDED, preferences.isExpandPsalmText)
      .apply()
    val fragment =
      mPsalmTextPagerAdapter!!.registeredFragments[mPagerPsalmText!!.currentItem] as PsalmTextFragment
    fragment.applyPsalmPreferences(preferences)
    mPsalmTextPagerAdapter?.applyPsalmPreferences(preferences)
  }

  override fun onCancelPreferences(previousPreferences: PsalmPreferences?) {
    mPsalmPreferences = previousPreferences
    val fragment =
      mPsalmTextPagerAdapter!!.registeredFragments[mPagerPsalmText!!.currentItem] as PsalmTextFragment
    fragment.applyPsalmPreferences(previousPreferences!!)
  }

  private inner class FabFavoritesOnClick : View.OnClickListener {
    override fun onClick(v: View) {
      val fragment =
        mPsalmTextPagerAdapter!!.registeredFragments[mPagerPsalmText!!.currentItem] as PsalmTextFragment
      if (fragment.isFavoritePsalm) {
        fragment.removePsalmFromFavorites()
        Snackbar.make(v, R.string.msg_removed_from_favorites, Snackbar.LENGTH_SHORT)
          .setAction("Action", null).show()
      } else {
        fragment.addPsalmToFavorites()
        Snackbar.make(v, R.string.msg_added_to_favorites, Snackbar.LENGTH_SHORT)
          .setAction("Action", null).show()
      }
    }
  }

  private fun drawFavoriteFabIcon(isFavoritePsalm: Boolean) {
    if (isFavoritePsalm) {
      mFabFavorite!!.setImageResource(R.drawable.ic_favorite_white_24dp)
    } else {
      mFabFavorite!!.setImageResource(R.drawable.ic_favorite_border_white_24dp)
    }
  }

  override val themeType: ThemeType
    get() = ThemeType.NO_ACTION_BAR

  companion object {
    const val KEY_PSALM_NUMBER_ID = "psalmNumberId"
    private const val REQUEST_CODE_FULLSCREEN_ACTIVITY = 1
    private const val REQUEST_CODE_EDIT_ACTIVITY = 2
    private const val ADD_TO_HISTORY_DELAY = 5000
  }
}