package io.github.alelk.pws.database

import timber.log.Timber

fun setupTimberForTest() {
  Timber.plant(object : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
      println("$tag: $message")
    }
  })
}