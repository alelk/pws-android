package io.github.alelk.pws.backup.model

import io.github.alelk.pws.domain.model.Tonality
import java.util.Locale

data class Song(
  val locale: Locale,
  val name: String,
  val text: String,
  val tonalities: List<Tonality>?,
  val bibleRef: String?
)