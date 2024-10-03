package com.alelk.pws.pwapp.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alelk.pws.database.entity.TagEntity
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.base.AppCompatThemedActivity
import com.alelk.pws.pwapp.adapter.TagsAdapter
import com.alelk.pws.pwapp.dialog.TagDialog
import com.alelk.pws.pwapp.model.TagViewModel
import kotlinx.coroutines.launch

class TagsActivity : AppCompatThemedActivity() {

  private var isEditMode = false
  private lateinit var tagDialog: TagDialog
  private lateinit var tagsAdapter: TagsAdapter
  private lateinit var editTagsButton: Button
  private lateinit var recyclerView: RecyclerView
  private val tagViewModel: TagViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_categories)

    tagDialog = TagDialog(this)
    tagsAdapter = TagsAdapter(
      onTagSelect = { tag -> this.onTagSelect(tag) },
      onEditTag = { tag -> this.editTag(tag) },
      onDeleteTag = { tag -> this.deleteTag(tag) },
      onEditTagColor = { tag -> this.editTagColor(tag) }
    )

    recyclerView = findViewById(R.id.categories_recycler_view)
    recyclerView.adapter = tagsAdapter
    recyclerView.layoutManager = LinearLayoutManager(this)

    editTagsButton = findViewById(R.id.button_edit_categories)
    editTagsButton.setOnClickListener { onEditButton() }

    tagViewModel.allTags.observe(this) { tags ->
      tagsAdapter.swapData(tags)
    }
  }

  private fun onEditButton() {
    isEditMode = !isEditMode
    editTagsButton.text = if (isEditMode) getString(R.string.lbl_done) else getString(R.string.lbl_edit_categories)
    tagsAdapter.switchEditMode(isEditMode)
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
        addTag()
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun onTagSelect(tag: TagEntity) {
    intent = Intent(this, TagSongsActivity::class.java)
    intent.putExtra(TagSongsActivity.TAG_ID, tag.id.toString())
    intent.putExtra(TagSongsActivity.TAG_NAME, tag.name)
    startActivity(intent)
  }

  private fun addTag() {
    lifecycleScope.launch {
      val nextTagId = tagViewModel.getNextCustomTagId()
      tagDialog.showAddTagDialog(nextTagId) {
        lifecycleScope.launch {
          if (tagViewModel.findByName(it.name).isEmpty())
            tagViewModel.addTag(it)
          else
            tagDialog.showWarningUniqueNameDialog()
        }
      }
    }
  }

  private fun editTag(tag: TagEntity) {
    tagDialog.showEditTagDialog(tag) {
      lifecycleScope.launch {
        if (tagViewModel.findByName(it.name).filterNot { it.id == tag.id }.isEmpty())
          tagViewModel.updateTag(it)
        else
          tagDialog.showWarningUniqueNameDialog()
      }
    }
  }

  private fun editTagColor(tag: TagEntity) {
    tagDialog.showSelectColorDialog(tag.color) {
      lifecycleScope.launch {
        tagViewModel.updateTag(tag.copy(color = it))
      }
    }
  }

  private fun deleteTag(tag: TagEntity) {
    tagDialog.showDeleteTagDialog(tag) {
      lifecycleScope.launch {
        tagViewModel.deleteTag(tag)
      }
    }
  }
}