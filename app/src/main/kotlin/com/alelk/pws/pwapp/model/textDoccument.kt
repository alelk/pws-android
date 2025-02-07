package com.alelk.pws.pwapp.model

import com.alelk.pws.database.util.PwsSongUtil

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
      this.song.locale,
      this.song.lyric,
      this.song.bibleRef,
      this.song.name,
      this.songNumber.number,
      this.song.author?.name,
      this.song.translator?.name,
      this.song.composer?.name,
      "<p><b><i><a href='https://play.google.com/store/apps/details?id=com.alelk.pws.pwapp'>P&W Songs: ${this.book.name}</a></i></b></p>"
    )