package com.alelk.pws.pwapp.activity


import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.base.AppCompatThemedActivity
import com.alelk.pws.pwapp.adapter.CategoriesAdapter
import com.alelk.pws.pwapp.dialog.CategoryDialog
import com.alelk.pws.pwapp.loader.CategoryLoader
import com.alelk.pws.pwapp.model.Category


class CategoriesActivity : AppCompatThemedActivity() {

  private var isEditMode = false
  private lateinit var mCategoryDialog: CategoryDialog
  private lateinit var mCategoriesAdapter: CategoriesAdapter
  private lateinit var mCategoryLoader: CategoryLoader
  private lateinit var editCategoriesButton: Button
  private lateinit var recyclerView: RecyclerView
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_categories)

    mCategoryDialog = CategoryDialog(this)
    mCategoryLoader = CategoryLoader(this)
    mCategoriesAdapter = CategoriesAdapter(this)

    recyclerView = findViewById(R.id.categories_recycler_view)
    recyclerView.adapter = mCategoriesAdapter
    recyclerView.layoutManager = LinearLayoutManager(this)

    editCategoriesButton = findViewById(R.id.button_edit_categories)
    editCategoriesButton.setOnClickListener { onEditButton() }

    val categories = mCategoryLoader.loadData()
    mCategoriesAdapter.swapData(categories.toList())
  }

  private fun onEditButton() {
    isEditMode = !isEditMode
    editCategoriesButton.text = if (isEditMode) getString(R.string.lbl_done) else getString(R.string.lbl_edit_categories)
    mCategoriesAdapter.switchEditMode(isEditMode)
    invalidateOptionsMenu()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_song_categories, menu)
    return true
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    val addCategoryItem = menu.findItem(R.id.action_add_category)
    addCategoryItem.isVisible = !isEditMode
    return super.onPrepareOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_add_category -> {
        addCategory()
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }

  fun onCategorySelect(category: Category) {
    intent = Intent(this, CategoryPsalmsActivity::class.java)
    intent.putExtra(CategoryPsalmsActivity.CATEGORY_ID, category.id)
    intent.putExtra(CategoryPsalmsActivity.CATEGORY_NAME, category.name)
    startActivity(intent)
  }

  private fun addCategory() {
    mCategoryDialog.showAddCategoryDialog {
      if (isNameUnique(it)) return@showAddCategoryDialog
      val categories = mCategoryLoader.saveData(it)
      reloadActvivity(categories.toList())
    }
  }

  fun editCategory(category: Category) {
    mCategoryDialog.showEditCategoryDialog(category) {
      if (isNameUnique(it)) return@showEditCategoryDialog
      val categories = mCategoryLoader.updateData(it)
      reloadActvivity(categories.toList())
    }
  }

  fun editCategoryColor(category: Category) {
    mCategoryDialog.showSelectColorDialog(category.color) {
      val categories = mCategoryLoader.updateData(Category(category.id, category.name, color = it, category.priority))
      reloadActvivity(categories.toList())
    }
  }

  fun deleteCategory(category: Category) {
    mCategoryDialog.showDeleteCategoryDialog(category) {
      val categories = mCategoryLoader.deleteData(it)
      reloadActvivity(categories.toList())
    }
  }

  private fun isNameUnique(category: Category): Boolean {
    if (!mCategoryLoader.isCategoryNameUnique(category.name)) {
      mCategoryDialog.showWarningUniqueNameDialog()
      return true
    }
    return false
  }

  private fun reloadActvivity(categories: List<Category>) {
    mCategoriesAdapter.swapData(categories)
    this.recreate()
  }
}