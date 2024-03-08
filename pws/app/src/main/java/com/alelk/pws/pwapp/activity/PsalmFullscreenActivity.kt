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

import android.annotation.SuppressLint
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import androidx.viewpager.widget.ViewPager
import com.alelk.pws.database.provider.PwsDataProviderContract
import com.alelk.pws.database.provider.PwsDataProviderContract.PsalmNumbers.Book.BookPsalmNumbers.Info.getContentUri
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.base.AppCompatThemedActivity
import com.alelk.pws.pwapp.adapter.PsalmTextFragmentStatePagerAdapter
import com.alelk.pws.pwapp.fragment.PsalmTextFragment
import com.alelk.pws.pwapp.holder.PsalmHolder
import com.alelk.pws.pwapp.preference.PsalmPreferences

class PsalmFullscreenActivity : AppCompatThemedActivity(), PsalmTextFragment.Callbacks {
  private val mHideHandler = Handler()
  private var mContentView: View? = null
  private val mHidePart2Runnable = Runnable {
    mContentView!!.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
      or View.SYSTEM_UI_FLAG_FULLSCREEN
      or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
      or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
      or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
      .let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) it or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        else it
      }
  }
  private var mControlsView: View? = null
  private val mShowPart2Runnable = Runnable {
    val actionBar = supportActionBar
    actionBar?.show()
    mControlsView!!.visibility = View.VISIBLE
  }
  private var mVisible = false
  private val mHideRunnable = Runnable { hide() }
  private val mAddToHistoryHandler = Handler()
  private val mAddToHistoryRunnable = Runnable {
    if (mFragmentStatePagerAdapter == null || mPagerPsalmText == null) return@Runnable
    val fragment =
      mFragmentStatePagerAdapter!!.registeredFragments[mPagerPsalmText!!.currentItem] as PsalmTextFragment
    fragment.addPsalmToHistory()
  }
  private var mPsalmNumberId: Long = 0
  private var mBtnFavorites: Button? = null
  private var mBookPsalmNumberIds: ArrayList<Long>? = null
  private var mPagerPsalmText: ViewPager? = null
  private var mFragmentStatePagerAdapter: PsalmTextFragmentStatePagerAdapter? = null
  private var mPsalmPreferences: PsalmPreferences? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    init()
    setContentView(R.layout.activity_psalm_fullscreen)
    if (supportActionBar != null) supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    mVisible = true
    mControlsView = findViewById(R.id.fullscreen_content_controls)
    mContentView = findViewById(R.id.pager_psalm_text)
    mBtnFavorites = findViewById(R.id.btn_favorites)
    mBtnFavorites!!.setOnClickListener {
      val fragment =
        mFragmentStatePagerAdapter!!.registeredFragments[mPagerPsalmText!!.currentItem] as PsalmTextFragment
      if (fragment.isFavoritePsalm) {
        fragment.removePsalmFromFavorites()
      } else {
        fragment.addPsalmToFavorites()
      }
    }
    mBtnFavorites!!.setOnTouchListener { v: View, event: MotionEvent ->
      if (AUTO_HIDE) {
        delayedHide(AUTO_HIDE_DELAY_MILLIS)
      }
      if (MotionEvent.ACTION_UP == event.action) v.performClick()
      true
    }
    mPagerPsalmText = findViewById(R.id.pager_psalm_text)
    val psalmTextSize = PreferenceManager.getDefaultSharedPreferences(baseContext)
      .getFloat(PsalmTextFragment.KEY_PSALM_TEXT_SIZE, -1f)
    mFragmentStatePagerAdapter = PsalmTextFragmentStatePagerAdapter(
      supportFragmentManager,
      mBookPsalmNumberIds,
      mPsalmPreferences!!
    )
    mPagerPsalmText!!.adapter = mFragmentStatePagerAdapter
    mPagerPsalmText!!.currentItem = mBookPsalmNumberIds!!.indexOf(mPsalmNumberId)
  }

  private fun init() {
    mPsalmNumberId = intent.getLongExtra(KEY_PSALM_NUMBER_ID, -10)
    val preferences = PreferenceManager.getDefaultSharedPreferences(baseContext)
    mPsalmPreferences = PsalmPreferences(
      preferences.getFloat(PsalmTextFragment.KEY_PSALM_TEXT_SIZE, -1f),
      preferences.getBoolean(PsalmTextFragment.KEY_PSALM_TEXT_EXPANDED, true)
    )
    var cursor: Cursor? = null
    try {
      cursor = contentResolver.query(
        getContentUri(mPsalmNumberId),
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
          } catch (ignore: NumberFormatException) {
          }
        }
      }
    } finally {
      cursor?.close()
    }
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
    hide()
  }

  private fun toggle() {
    if (mVisible) {
      hide()
    } else {
      show()
    }
  }

  private fun hide() {
    val actionBar = supportActionBar
    actionBar?.hide()
    mControlsView!!.visibility = View.GONE
    mVisible = false
    mHideHandler.removeCallbacks(mShowPart2Runnable)
    mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
  }

  @SuppressLint("InlinedApi")
  private fun show() {
    // Show the system bar
    mContentView!!.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
      or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    mVisible = true

    // Schedule a runnable to display UI elements after a delay
    mHideHandler.removeCallbacks(mHidePart2Runnable)
    mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
  }

  /**
   * Schedules a call to hide() in [delay] milliseconds, canceling any
   * previously scheduled calls.
   */
  private fun delayedHide(delayMillis: Int) {
    mHideHandler.removeCallbacks(mHideRunnable)
    mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
  }

  override fun onUpdatePsalmInfo(psalmHolder: PsalmHolder?) {
    if (psalmHolder == null ||
      mBookPsalmNumberIds!![mPagerPsalmText!!.currentItem] != psalmHolder.psalmNumberId
    ) return
    val actionBar = supportActionBar
    if (actionBar != null) {
      actionBar.title = "№ " + psalmHolder.psalmNumber + " " + psalmHolder.bookName
    }
    if (psalmHolder.isFavoritePsalm) {
      mBtnFavorites!!.setText(R.string.lbl_remove_from_favorites)
    } else {
      mBtnFavorites!!.setText(R.string.lbl_add_to_favorites)
    }
    mPsalmNumberId = psalmHolder.psalmNumberId
    val intent = Intent()
    intent.putExtra(KEY_PSALM_NUMBER_ID, mPsalmNumberId)
    setResult(RESULT_OK, intent)
    mAddToHistoryHandler.removeCallbacks(mAddToHistoryRunnable)
    mAddToHistoryHandler.postDelayed(mAddToHistoryRunnable, ADD_TO_HISTORY_DELAY.toLong())
  }

  override fun onRequestFullscreenMode() {
    toggle()
  }

  override fun onEditRequest(psalmNumberId: Long) {
    // no edit option in full screen
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home -> finish()
    }
    return super.onOptionsItemSelected(item)
  }

  companion object {
    const val KEY_PSALM_NUMBER_ID = PsalmTextFragment.KEY_PSALM_NUMBER_ID
    private const val AUTO_HIDE = true
    private const val AUTO_HIDE_DELAY_MILLIS = 3000
    private const val UI_ANIMATION_DELAY = 300
    private const val ADD_TO_HISTORY_DELAY = 5000
  }
}