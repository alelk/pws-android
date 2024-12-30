package com.alelk.pws.pwapp.fragment.preference

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.alelk.pws.pwapp.R
import com.alelk.pws.pwapp.activity.MainSettingsActivity
import com.alelk.pws.pwapp.model.ImportExportViewModel
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date

class ImportExportPreferenceFragment : PreferenceFragmentCompat() {
  private val viewModel: ImportExportViewModel by viewModels()

  private lateinit var importPreference: Preference
  private lateinit var exportPreference: Preference

  private val coroutineScope = CoroutineScope(Dispatchers.Main)

  private val exportLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      result.data?.data?.let { uri ->
        coroutineScope.launch {
          context?.contentResolver?.openOutputStream(uri)?.use { outputStream ->
            // We need a temporary file to store the exported user data before copying it to the output stream.
            // This is because the export process involves writing data to a file, and using a temporary file
            // allows us to handle the data safely and efficiently before transferring it to the final destination.
            val tempFile = File(context?.cacheDir, "temp_backup.json")
            val isSuccess = withContext(Dispatchers.IO) {
              viewModel.exportDatabase(tempFile)
            }
            if (isSuccess) {
              withContext(Dispatchers.IO) {
                tempFile.inputStream().copyTo(outputStream)
              }
              Toast.makeText(context, R.string.export_success, Toast.LENGTH_SHORT).show()
            } else {
              Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT).show()
            }
            tempFile.delete()
          }
        }
      }
    }
  }

  private val importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      result.data?.data?.let { uri ->
        coroutineScope.launch {
          context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
            val isSuccess = withContext(Dispatchers.IO) {
              viewModel.importDatabase(inputStream, uri)
            }
            if (isSuccess) {
              Toast.makeText(context, R.string.import_success, Toast.LENGTH_SHORT).show()
            } else {
              Toast.makeText(context, R.string.import_failed, Toast.LENGTH_SHORT).show()
            }
          }
        }
      }
    }
  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.pref_import_export, rootKey)
    setHasOptionsMenu(true)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    exportPreference = checkNotNull(findPreference(getString(R.string.pref_export_data_key)))
    importPreference = checkNotNull(findPreference(getString(R.string.pref_import_data_key)))
  }

  override fun onPreferenceTreeClick(preference: Preference): Boolean {
    return when (preference.key) {
      exportPreference.key -> {
        handleExport()
        true
      }

      importPreference.key -> {
        handleImport()
        true
      }

      else -> super.onPreferenceTreeClick(preference)
    }
  }

  private fun handleExport() {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
      addCategory(Intent.CATEGORY_OPENABLE)
      type = "application/json"
      val currentTime = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
      putExtra(Intent.EXTRA_TITLE, "pws_backup_$currentTime.json")
    }
    exportLauncher.launch(intent)
  }

  private fun handleImport() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
      addCategory(Intent.CATEGORY_OPENABLE)
      type = "application/json"
    }
    importLauncher.launch(intent)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val id = item.itemId
    if (id == android.R.id.home) {
      startActivity(Intent(activity, MainSettingsActivity::class.java))
      return true
    }
    return super.onOptionsItemSelected(item)
  }
} 