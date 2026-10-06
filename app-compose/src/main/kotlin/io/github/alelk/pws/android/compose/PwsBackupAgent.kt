package io.github.alelk.pws.android.compose

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.content.Context
import android.os.ParcelFileDescriptor
import io.github.alelk.pws.domain.book.repository.BookReadRepository
import io.github.alelk.pws.domain.booklibrary.repository.InstalledBookObserveRepository
import io.github.alelk.pws.portable.BackupService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext
import java.io.File

class PwsBackupAgent : BackupAgent() {

  override fun onBackup(
    oldState: ParcelFileDescriptor,
    data: BackupDataOutput,
    newState: ParcelFileDescriptor,
  ) {
    // Key/value backup runs the app's Application (and so Koin) normally. Should Koin still be missing, write
    // nothing: the transport keeps the previous backup data of this key.
    val backupManager = GlobalContext.getOrNull()?.get<BackupManager>() ?: return
    val yaml = runBlocking {
      val backup = backupManager.exportBackup(source = "android-backup")
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
      books: BookReadRepository,
      installedBooks: InstalledBookObserveRepository,
      backupManager: BackupManager,
    ) {
      val file = pendingRestoreFile(context)
      if (!file.exists()) return
      // Defer restore until at least one book is installed — restoring user data
      // (favorites, history) against an empty book catalog silently loses all records
      // because song lookup by (bookId, number) returns nothing.
      if (books.count() == 0) return
      val backup = runCatching { BackupService().readFromString(file.readText(Charsets.UTF_8)) }
        .getOrNull() ?: run { file.delete(); return }
      runCatching { backupManager.restoreBackup(backup) }.onFailure { if (it is CancellationException) throw it }
      // Keep the file until every book referenced in the backup is installed so that
      // re-applying on subsequent book installs picks up the remaining records.
      val backupBookIds = (
        (backup.favorites?.map { it.bookId } ?: emptyList()) +
          (backup.history?.map { it.songNumber.bookId } ?: emptyList()) +
          (backup.songs?.map { it.number.bookId } ?: emptyList())
        ).toSet()
      val installedBookIds = installedBooks.observeAll().first().map { it.bookId }.toSet()
      if (backupBookIds.isEmpty() || backupBookIds.all { it in installedBookIds }) file.delete()
    }
  }
}
