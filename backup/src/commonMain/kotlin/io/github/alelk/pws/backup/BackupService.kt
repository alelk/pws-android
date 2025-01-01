package io.github.alelk.pws.backup

import io.github.alelk.pws.backup.model.Backup
import java.io.File
import java.io.InputStream

class BackupService {
  fun writeAsString(backup: Backup): String = yamlConverter.encodeToString(Backup.serializer(), backup)
  fun readFromString(backupString: String): Backup = yamlConverter.decodeFromString(Backup.serializer(), backupString)
  fun write(backup: Backup, file: File) = file.writeText(writeAsString(backup))
  fun read(file: File): Backup = readFromString(file.readText())
  fun read(inputStream: InputStream): Backup = readFromString(inputStream.reader().readText())
}