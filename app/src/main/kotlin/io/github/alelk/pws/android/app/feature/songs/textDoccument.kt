package io.github.alelk.pws.android.app.feature.songs

import io.github.alelk.pws.android.app.feature.songs.PwsSongUtil
import java.util.Locale

val SongInfo.textDocument: String
  get() {
    val s = this
    return buildString {
      appendLine("№${s.songNumber.number} - ${s.song.name}")
      appendLine()
      s.song.bibleRef?.let {
        appendLine(it)
        appendLine()
      }
      appendLine(s.song.lyric)
      appendLine()
      appendLine("P&W Songs: ${s.book.name}")
    }
  }

val SongInfo.textDocumentHtml: String
  get() =
    PwsSongUtil.songTextToPrettyHtml(
      Locale.forLanguageTag(this.song.locale.value),
      this.song.lyric,
      this.song.bibleRef?.text,
      this.song.name,
      this.songNumber.number,
      this.song.author?.name,
      this.song.translator?.name,
      this.song.composer?.name,
      "<p><b><i><a href='https://play.google.com/store/apps/details?id=com.alelk.pws.pwapp'>P&W Songs: ${this.book.name}</a></i></b></p>"
    )