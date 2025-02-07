package io.github.alelk.pws.database.helper

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

fun File.unzip(destDirectory: File) {
  if (!destDirectory.exists()) {
    destDirectory.mkdirs()
  }
  require(destDirectory.isDirectory) { "expected directory: $destDirectory" }
  ZipFile(this).use { zip ->
    zip.entries().asSequence().forEach { entry ->
      zip.getInputStream(entry).use { input ->
        val filePath = destDirectory.resolve(entry.name)
        if (entry.isDirectory) filePath.mkdir()
        else input.copyTo(FileOutputStream(filePath))
      }
    }
  }
}