package io.github.alelk.pws.android.compose

import io.github.alelk.pws.portable.backup.ExportBackupUseCase
import io.github.alelk.pws.portable.backup.RestoreBackupUseCase
import io.github.alelk.pws.portable.backup.RestoreReport
import io.github.alelk.pws.portable.model.Backup

/**
 * The shell's entry to the backup use cases (`:portable-data`): the file picker ([platform.BackupFileActions]) and
 * Auto Backup ([PwsBackupAgent]) call it. A failed restore is thrown, as it always was; since step 06.2 it has also
 * written nothing (the restore is one transaction).
 */
class BackupManager(private val export: ExportBackupUseCase, private val restore: RestoreBackupUseCase) {

  suspend fun exportBackup(source: String): Backup = export(source)

  suspend fun restoreBackup(backup: Backup): RestoreReport {
    val result = restore(backup)
    return result.fold({ error("Backup restore failed (${it.section}): ${it.message}") }, { it })
  }
}
