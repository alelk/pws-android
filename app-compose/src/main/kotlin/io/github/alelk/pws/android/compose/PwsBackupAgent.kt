package io.github.alelk.pws.android.compose

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.PwsDatabaseProvider
import io.github.alelk.pws.portable.BackupService
import kotlinx.coroutines.runBlocking
import java.io.File

class PwsBackupAgent : BackupAgent() {

  override fun onBackup(
    oldState: ParcelFileDescriptor,
    data: BackupDataOutput,
    newState: ParcelFileDescriptor,
  ) {
    val db = PwsDatabaseProvider.getDatabase(applicationContext)
    val dataStore = applicationContext.appSettingsDataStore()
    val yaml = runBlocking {
      val backup = BackupManager(db, dataStore).exportBackup(source = "android-backup")
      BackupService().writeAsString(backup)
    }
    val bytes = yaml.toByteArray(Charsets.UTF_8)
    data.writeEntityHeader(KEY, bytes.size)
    data.writeEntityData(bytes, bytes.size)
  }

  override fun onRestore(
    data: BackupDataInput,
    appVersionCode: Int,
    newState: ParcelFileDescriptor,
  ) {
    while (data.readNextHeader()) {
      if (data.key == KEY) {
        val bytes = ByteArray(data.dataSize)
        data.readEntityData(bytes, 0, data.dataSize)
        pendingRestoreFile(applicationContext).writeBytes(bytes)
      } else {
        data.skipEntityData()
      }
    }
  }

  companion object {
    private const val KEY = "user_data_v1"

    fun pendingRestoreFile(context: Context): File =
      File(context.filesDir, "pending_user_restore.yaml")

    suspend fun applyPendingRestoreIfNeeded(
      context: Context,
      db: PwsDatabase,
      dataStore: DataStore<Preferences>,
    ) {
      val file = pendingRestoreFile(context)
      if (!file.exists()) return
      runCatching {
        val backup = BackupService().readFromString(file.readText(Charsets.UTF_8))
        BackupManager(db, dataStore).restoreBackup(backup)
      }
      file.delete()
    }
  }
}
