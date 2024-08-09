package com.alelk.pws.pwapp.activity

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.base.AppCompatThemedActivity
import com.alelk.pws.pwapp.adapter.PsalmInfoAdapter
import com.alelk.pws.pwapp.loader.CategoryLoader
import com.alelk.pws.pwapp.model.PsalmInfo

class CategoryPsalmsActivity : AppCompatThemedActivity() {
  private lateinit var recyclerView: RecyclerView
  private lateinit var categoryLoader: CategoryLoader
  private lateinit var psalmInfoAdapter: PsalmInfoAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_category_psalms)

    val categoryId = intent.getLongExtra(CATEGORY_ID, -1)
    val categoryName = intent.getStringExtra(CATEGORY_NAME)
    setTitle(getString(R.string.title_activity_category_psalms) + " $categoryName")

    categoryLoader = CategoryLoader(this)
    val psalmInfoList = categoryLoader.loadPsalmsByCategory(categoryId.toString())
    psalmInfoAdapter = PsalmInfoAdapter(this)

    recyclerView = findViewById(R.id.category_psalms_recycler_view)
    recyclerView.adapter = psalmInfoAdapter
    recyclerView.layoutManager = LinearLayoutManager(this)

    psalmInfoAdapter.swapData(psalmInfoList)
  }

  fun onPsalmSelected(data: PsalmInfo) {
    val intentPsalmView = Intent(baseContext, PsalmActivity::class.java)
    intentPsalmView.putExtra(PsalmActivity.KEY_PSALM_NUMBER_ID, data.psalmNumberId)
    startActivity(intentPsalmView)
  }

  companion object {
    const val CATEGORY_NAME = "category_name"
    const val CATEGORY_ID = "category_id"
  }
}