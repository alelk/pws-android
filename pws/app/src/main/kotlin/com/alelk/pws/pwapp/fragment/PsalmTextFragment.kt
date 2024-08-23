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
package com.alelk.pws.pwapp.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.util.TypedValue
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.database.data.Tonality.Companion.getInstanceBySignature
import com.alelk.pws.database.provider.PwsDataProviderContract
import com.alelk.pws.database.table.PwsFavoritesTable
import com.alelk.pws.database.table.PwsHistoryTable
import com.alelk.pws.database.util.PwsPsalmUtil
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.PsalmActivity
import com.alelk.pws.pwapp.activity.PsalmFullscreenActivity
import com.alelk.pws.pwapp.adapter.ReferredPsalmsRecyclerViewAdapter
import com.alelk.pws.pwapp.dialog.EditPsalmCategoryDialog
import com.alelk.pws.pwapp.holder.PsalmHolder
import com.alelk.pws.pwapp.loader.CategoryLoader
import com.alelk.pws.pwapp.model.Category
import com.alelk.pws.pwapp.preference.PsalmPreferences
import com.alelk.pws.pwapp.view.CategoryView
import com.google.android.flexbox.FlexboxLayout
import java.util.Locale
import java.util.SortedSet

/**
 * Created by Alex Elkin on 18.04.2015.
 *
 * The Activity who are the host of this fragment should implement Callbacks interface.
 */
class PsalmTextFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
  private val SELECTION_ARGS = arrayOfNulls<String>(1)
  private val CONTENT_VALUES_FAVORITES = ContentValues(1)
  private val CONTENT_VALUES_HISTORY = ContentValues(1)
  private var callbacks: Callbacks? = null
  private var cvTonalities: CardView? = null
  private var cvPsalmInfo: CardView? = null
  private var vPsalmText: TextView? = null
  private var vPsalmInfo: TextView? = null
  private var vPsalmTonalities: TextView? = null
  private var cvCategories: CardView? = null
  private var vPsalmCategories: FlexboxLayout? = null
  private var mReferredPsalmsAdapter: ReferredPsalmsRecyclerViewAdapter? = null
  private var mPsalmHolder: PsalmHolder? = null
  private var mPsalmNumberId: Long = -1
  private var mPsalmPreferences: PsalmPreferences? = null
  private var mCategoryLoader: CategoryLoader? = null
  private var isAddedToHistory = false
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)
    val v = inflater.inflate(R.layout.fragment_psalm_text, null)
    cvTonalities = v.findViewById(R.id.cv_tonalities)
    cvPsalmInfo = v.findViewById(R.id.cv_psalm_info)
    vPsalmText = v.findViewById(R.id.txt_psalm_text)
    cvCategories = v.findViewById(R.id.cv_categories)
    vPsalmCategories = v.findViewById(R.id.categories)
    vPsalmInfo = v.findViewById(R.id.txt_psalm_info)
    vPsalmTonalities = v.findViewById(R.id.txt_psalm_tonalities)
    val activity = activity ?: return v
    mReferredPsalmsAdapter =
      ReferredPsalmsRecyclerViewAdapter({ psalmNumberId: Long ->
        val intentPsalmView = Intent(activity.baseContext, PsalmActivity::class.java)
        intentPsalmView.putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, psalmNumberId)
        startActivity(intentPsalmView)
      }, -1.0f)
    val rvReferredPsalms = v.findViewById<RecyclerView>(R.id.rv_referred_psalms)
    rvReferredPsalms.adapter = mReferredPsalmsAdapter
    rvReferredPsalms.isNestedScrollingEnabled = false
    rvReferredPsalms.layoutManager = LinearLayoutManager(activity.applicationContext)
    psalmTextSize = mPsalmPreferences!!.textSize
    mCategoryLoader = CategoryLoader(activity)
    updateUi()
    retainInstance = true
    LoaderManager.getInstance(this).initLoader(PWS_REFERRED_PSALMS_LOADER, null, this)
    setHasOptionsMenu(true)
    registerForContextMenu(vPsalmText!!)
    vPsalmText?.setOnClickListener { callbacks!!.onRequestFullscreenMode() }
    return v
  }

  private fun init() {
    if (mPsalmNumberId < 0) return
    SELECTION_ARGS[0] = mPsalmNumberId.toString()
    CONTENT_VALUES_FAVORITES.put(
      PwsFavoritesTable.COLUMN_PSALMNUMBERID,
      mPsalmNumberId.toString()
    )
    CONTENT_VALUES_HISTORY.put(PwsHistoryTable.COLUMN_PSALMNUMBERID, mPsalmNumberId.toString())
  }

  private fun loadData() {
    val METHOD_NAME = "loadData"
    if (mPsalmNumberId < 0) {
      return
    }
    var cursor: Cursor? = null
    val activity = activity ?: return
    try {
      cursor = activity.contentResolver.query(
        PwsDataProviderContract.PsalmNumbers.Psalm.getContentUri(mPsalmNumberId),
        null,
        null,
        null,
        null
      )
      if (cursor == null || !cursor.moveToFirst()) {
        return
      }
      mPsalmHolder = PsalmHolder(cursor, isFavoritePsalm)
      Log.v(
        LOG_TAG,
        METHOD_NAME + ": The psalm data successfully loaded: " + "mPsalmHolder=" + mPsalmHolder.toString()
      )
    } finally {
      cursor?.close()
    }
  }

  fun updateUi() {
    if (mPsalmNumberId < 0 || mPsalmHolder == null) {
      return
    }
    val psalmTextHtml = PwsPsalmUtil.psalmTextToHtml(
      Locale(mPsalmHolder!!.psalmLocale),
      mPsalmHolder!!.psalmText,
      mPsalmPreferences!!.isExpandPsalmText
    )
    if (Build.VERSION.SDK_INT >= 24) {
      vPsalmText!!.text = Html.fromHtml(psalmTextHtml, Html.FROM_HTML_MODE_LEGACY)
    } else {
      vPsalmText!!.text = Html.fromHtml(psalmTextHtml)
    }
    val psalmInfoHtml = PwsPsalmUtil.buildPsalmInfoHtml(
      Locale(
        mPsalmHolder!!.psalmLocale
      ),
      mPsalmHolder!!.psalmAuthor,
      mPsalmHolder!!.psalmTranslator,
      mPsalmHolder!!.psalmComposer
    )
    if (psalmInfoHtml == null) {
      cvPsalmInfo!!.visibility = View.GONE
    } else if (Build.VERSION.SDK_INT >= 24) {
      vPsalmInfo!!.text = Html.fromHtml(psalmInfoHtml, Html.FROM_HTML_MODE_LEGACY)
    } else {
      vPsalmInfo!!.text = Html.fromHtml(psalmInfoHtml)
    }
    var tonalities: String? = null
    val tonsArray = mPsalmHolder!!.psalmTonalities
    var i = 0
    while (tonsArray != null && i < tonsArray.size) {
      val tonality = getInstanceBySignature(tonsArray[i])
      if (tonality == null) {
        i++
        continue
      }
      tonalities = (if (tonalities == null) "" else ", ") + tonality.getLabel(requireActivity())
      i++
    }
    if (tonalities.isNullOrEmpty()) {
      cvTonalities!!.visibility = View.GONE
    } else {
      cvTonalities!!.visibility = View.VISIBLE
      vPsalmTonalities!!.text = tonalities
    }
    callbacks!!.onUpdatePsalmInfo(mPsalmHolder)

    // setup view for categories
    addCategoriesToView(mCategoryLoader!!.loadCategoriesForPsalm(mPsalmNumberId.toString()))
  }

  private fun addCategoriesToView(categories: SortedSet<Category>) {
    vPsalmCategories!!.removeAllViews()
    if (categories.size == 0) {
      cvCategories!!.visibility = View.GONE
      return
    } else {
      cvCategories!!.visibility = View.VISIBLE
    }
    for (category in categories) {
      val categoryView = CategoryView(requireActivity(), category)
      categoryView.addMode()
      vPsalmCategories!!.addView(categoryView)
    }
    vPsalmCategories!!.setOnClickListener(onEditCategoriesResult())
  }

  fun reloadUi() {
    loadData()
    updateUi()
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    callbacks = context as Callbacks
    if (arguments != null) {
      mPsalmNumberId = requireArguments().getLong(KEY_PSALM_NUMBER_ID, -10L)
      mPsalmPreferences = PsalmPreferences(
        requireArguments().getFloat(KEY_PSALM_TEXT_SIZE, -1f),
        requireArguments().getBoolean(KEY_PSALM_TEXT_EXPANDED, true)
      )
    }
    init()
    loadData()
  }

  override fun setMenuVisibility(menuVisible: Boolean) {
    super.setMenuVisibility(menuVisible)
    if (menuVisible && callbacks != null) {
      callbacks!!.onUpdatePsalmInfo(mPsalmHolder)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    val activity = activity ?: return
    activity.menuInflater.inflate(R.menu.menu_psalm_text, menu)
    if (activity.javaClass == PsalmFullscreenActivity::class.java) {
      val editMenu = menu.findItem(R.id.menu_edit);
      editMenu.setVisible(false)
    }
  }

  override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
    super.onCreateContextMenu(menu, v, menuInfo)
    val activity = activity ?: return
    activity.menuInflater.inflate(R.menu.menu_psalm_text_context, menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.menu_share) {
      val intent = Intent(Intent.ACTION_SEND)
      intent.putExtra(Intent.EXTRA_TEXT, psalmDocument)
      intent.putExtra(Intent.EXTRA_HTML_TEXT, psalmHtmlDocument)
      intent.type = "text/plain"
      startActivity(intent)
      return true
    }
    if (item.itemId == R.id.menu_edit) {
      callbacks!!.onEditRequest(mPsalmHolder!!.psalmNumberId)
    }
    if (R.id.menu_edit_categories == item.itemId) {
      onEditCategoriesResult().invoke(requireView())
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onContextItemSelected(item: MenuItem): Boolean {
    if (R.id.menu_copy == item.itemId) {
      if (mPsalmHolder == null) return false
      val activity = activity ?: return false
      val clipboardManager =
        activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          ?: return false
      val clip =
        ClipData.newHtmlText(getString(R.string.app_name), psalmDocument, psalmHtmlDocument)
      clipboardManager.setPrimaryClip(clip)
      return true
    }
    return super.onContextItemSelected(item)
  }

  private val psalmDocument: String?
    private get() {
      if (mPsalmHolder == null) return null
      val sb = StringBuilder()
      sb.append("№").append(mPsalmHolder!!.psalmNumber).append(" - ")
        .append(mPsalmHolder!!.psalmName).append("\n\n")
      if (mPsalmHolder!!.bibleRef != null) sb.append(mPsalmHolder!!.bibleRef)
        .append("\n\n")
      sb.append(mPsalmHolder!!.psalmText)
      sb.append("\n\n").append("P&W Songs: ").append(mPsalmHolder!!.bookName)
      return sb.toString()
    }
  private val psalmHtmlDocument: String?
    private get() = if (mPsalmHolder == null) null else PwsPsalmUtil.psalmTextToPrettyHtml(
      Locale(mPsalmHolder!!.psalmLocale),
      mPsalmHolder!!.psalmText,
      mPsalmHolder!!.bibleRef,
      mPsalmHolder!!.psalmName,
      mPsalmHolder!!.psalmNumber,
      mPsalmHolder!!.psalmAuthor,
      mPsalmHolder!!.psalmTranslator,
      mPsalmHolder!!.psalmComposer,
      "<p><b><i><a href='https://play.google.com/store/apps/details?id=" +
              "com.alelk.pws.pwapp'>P&W Songs: " + mPsalmHolder!!.bookName + "</a></i></b></p>"
    )

  fun addPsalmToHistory() {
    val METHOD_NAME = "addPsalmToHistory"
    if (mPsalmNumberId < 0 || isAddedToHistory || activity == null) return
    val contentResolver = requireActivity().contentResolver ?: return
    val cursor =
      contentResolver.query(
        PwsDataProviderContract.History.Last.CONTENT_URI,
        null,
        null,
        null,
        null
      )
    try {
      if (cursor != null && cursor.moveToFirst()) {
        if (cursor.getLong(cursor.getColumnIndex(PwsDataProviderContract.History.Last.COLUMN_PSALMNUMBER_ID)) == mPsalmNumberId) {
          Log.d(
            LOG_TAG,
            "$METHOD_NAME: The psalm already present in history table as a recent item"
          )
          isAddedToHistory = true
          return
        }
      }
      requireActivity().contentResolver.insert(
        PwsDataProviderContract.History.CONTENT_URI,
        CONTENT_VALUES_HISTORY
      )
      isAddedToHistory = true
    } finally {
      cursor?.close()
    }
  }

  fun applyPsalmPreferences(preferences: PsalmPreferences) {
    psalmTextSize = preferences.textSize
    mPsalmPreferences = preferences
    updateUi()
  }

  var psalmTextSize: Float
    get() = vPsalmText!!.textSize
    private set(psalmTextSize) {
      if (psalmTextSize < 10 || psalmTextSize > 100) return
      if (vPsalmText != null) vPsalmText!!.setTextSize(
        TypedValue.COMPLEX_UNIT_PX,
        psalmTextSize
      )
      if (vPsalmTonalities != null) vPsalmTonalities!!.setTextSize(
        TypedValue.COMPLEX_UNIT_PX,
        psalmTextSize / 1.5f
      )
      if (vPsalmInfo != null) vPsalmInfo!!.setTextSize(
        TypedValue.COMPLEX_UNIT_PX,
        psalmTextSize / 1.5f
      )
    }

  /**
   * Add psalm to favorites table
   */
  fun addPsalmToFavorites() {
    val METHOD_NAME = "addPsalmToFavorites"
    if (mPsalmNumberId < 0) return
    val activity = activity ?: return
    activity.contentResolver.insert(
      PwsDataProviderContract.Favorites.CONTENT_URI,
      CONTENT_VALUES_FAVORITES
    )
    mPsalmHolder!!.isFavoritePsalm = isFavoritePsalm
    callbacks!!.onUpdatePsalmInfo(mPsalmHolder)
    callbacks!!.onUpdatePsalmInfo(mPsalmHolder)
    Log.v(LOG_TAG, METHOD_NAME + ": Result: isFavoritePsalm=" + mPsalmHolder!!.isFavoritePsalm)
  }

  /**
   * Remove psalm from favorites table
   */
  fun removePsalmFromFavorites() {
    val METHOD_NAME = "removePsalmFromFavorites"
    if (mPsalmNumberId < 0) return
    val activity = activity ?: return
    activity.contentResolver.delete(
      PwsDataProviderContract.Favorites.CONTENT_URI,
      SELECTION_FAVORITES_PSALM_NUMBER_MATCH,
      SELECTION_ARGS
    )
    mPsalmHolder!!.isFavoritePsalm = false
    callbacks!!.onUpdatePsalmInfo(mPsalmHolder)
    Log.v(LOG_TAG, METHOD_NAME + ": Result: isFavoritePsalm=" + mPsalmHolder!!.isFavoritePsalm)
  }

  /**
   * Check if it is favorite psalm
   * @return true if favorites table contains psalm, false otherwise
   */
  val isFavoritePsalm: Boolean
    get() {
      if (mPsalmNumberId < 0) return false
      val activity = activity ?: return false
      val cursor = activity.contentResolver.query(
        PwsDataProviderContract.Favorites.CONTENT_URI,
        null,
        SELECTION_FAVORITES_PSALM_NUMBER_MATCH,
        SELECTION_ARGS,
        null
      )
      return cursor.use { cursor ->
        cursor != null && cursor.moveToFirst()
      }
    }

  override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
    return if (i == PWS_REFERRED_PSALMS_LOADER) CursorLoader(
      requireActivity().baseContext,
      PwsDataProviderContract.PsalmNumbers.ReferencePsalms.getContentUri(mPsalmNumberId),
      null,
      null,
      null,
      null
    ) else throw java.lang.IllegalStateException("unable to create cursor loader")
  }

  override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
    if (loader.id == PWS_REFERRED_PSALMS_LOADER) {
      mReferredPsalmsAdapter!!.swapCursor(cursor)
    }
  }

  override fun onLoaderReset(loader: Loader<Cursor>) {
    mReferredPsalmsAdapter!!.swapCursor(null)
  }

  private fun onEditCategoriesResult(): (v: View) -> Unit {
    return {
      val assignedCategoriesOrigin =
        mCategoryLoader!!.loadCategoriesForPsalm(mPsalmNumberId.toString())
      val allCategories = mCategoryLoader!!.loadData()
      EditPsalmCategoryDialog(requireActivity()).showEditCategoryDialog(
        assignedCategoriesOrigin,
        allCategories
      ) { assignedCategories ->
        val assignedCategoriesToPsalm = assignedCategories - assignedCategoriesOrigin
        val unAssignedCategoriesFromPsalm = assignedCategoriesOrigin - assignedCategories
        mCategoryLoader!!.updateCategoriesForPsalm(
          mPsalmNumberId.toString(),
          assignedCategoriesToPsalm,
          unAssignedCategoriesFromPsalm
        )
        vPsalmCategories!!.removeAllViews()
        addCategoriesToView(assignedCategories)
      }
    }
  }

  /**
   * Interface which should be implemented by activity who are the host of this fragment.
   */
  interface Callbacks {
    /**
     * This method is called when any psalm information is changed.
     * @param psalmHolder Psalm Holder
     */
    fun onUpdatePsalmInfo(psalmHolder: PsalmHolder?)
    fun onRequestFullscreenMode()
    fun onEditRequest(psalmNumberId: Long)
  }

  companion object {
    private const val PWS_REFERRED_PSALMS_LOADER = 60
    private val LOG_TAG = PsalmTextFragment::class.java.simpleName
    const val KEY_PSALM_NUMBER_ID = "com.alelk.pws.pwapp.psalmNumberId"
    const val KEY_PSALM_TEXT_SIZE = "com.alelk.pws.pwapp.psalmTextSize"
    const val KEY_PSALM_TEXT_EXPANDED = "com.alelk.pws.pwapp.psalmTextExpanded"
    private val SELECTION_FAVORITES_PSALM_NUMBER_MATCH: String =
      PwsDataProviderContract.Favorites.COLUMN_PSALMNUMBER_ID + " = ?"

    /**
     * Create new instance of PsalmTextFragment with attached psalmNumberId argument
     * @param psalmNumberId psalmNumber Id
     * @param preferences psalm text preferences
     * @return instance of PsalmTextFragment
     */
    fun newInstance(psalmNumberId: Long, preferences: PsalmPreferences): PsalmTextFragment {
      val args = Bundle()
      args.putLong(KEY_PSALM_NUMBER_ID, psalmNumberId)
      args.putFloat(KEY_PSALM_TEXT_SIZE, preferences.textSize)
      args.putBoolean(KEY_PSALM_TEXT_EXPANDED, preferences.isExpandPsalmText)
      val fragment = PsalmTextFragment()
      fragment.arguments = args
      return fragment
    }
  }
}