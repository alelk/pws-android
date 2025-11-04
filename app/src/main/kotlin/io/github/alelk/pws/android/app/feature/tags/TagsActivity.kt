package io.github.alelk.pws.android.app.feature.tags

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.android.app.AppCompatThemedActivity
import io.github.alelk.pws.android.app.feature.tags.TagsAdapter
import io.github.alelk.pws.android.app.feature.tags.TagDialog
import io.github.alelk.pws.database.tag.TagEntity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TagsActivity : AppCompatThemedActivity() {

  private var isEditMode = false
  private lateinit var tagDialog: TagDialog
  private lateinit var tagsAdapter: TagsAdapter
  private lateinit var editTagsButton: Button
  private lateinit var recyclerView: RecyclerView
  private val tagsViewModel: TagsViewModel by viewModels()

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

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        tagsViewModel.allTags.filterNotNull().collectLatest { tags ->
          tagsAdapter.swapData(tags)
        }
      }
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
    intent.putExtra(TagSongsActivity.Companion.TAG_ID, tag.id.toString())
    intent.putExtra(TagSongsActivity.Companion.TAG_NAME, tag.name)
    startActivity(intent)
  }

  private fun addTag() {
    lifecycleScope.launch {
      val nextTagId = tagsViewModel.getNextCustomTagId()
      tagDialog.showAddTagDialog(nextTagId) {
        lifecycleScope.launch {
          if (tagsViewModel.findByName(it.name).isEmpty())
            tagsViewModel.addTag(it)
          else
            tagDialog.showWarningUniqueNameDialog()
        }
      }
    }
  }

  private fun editTag(tag: TagEntity) {
    tagDialog.showEditTagDialog(tag) {
      lifecycleScope.launch {
        if (tagsViewModel.findByName(it.name).filterNot { it.id == tag.id }.isEmpty())
          tagsViewModel.updateTag(it)
        else
          tagDialog.showWarningUniqueNameDialog()
      }
    }
  }

  private fun editTagColor(tag: TagEntity) {
    tagDialog.showSelectColorDialog(tag.color) {
      lifecycleScope.launch {
        tagsViewModel.updateTag(tag.copy(color = it))
      }
    }
  }

  private fun deleteTag(tag: TagEntity) {
    tagDialog.showDeleteTagDialog(tag) {
      lifecycleScope.launch {
        tagsViewModel.deleteTag(tag)
      }
    }
  }
}