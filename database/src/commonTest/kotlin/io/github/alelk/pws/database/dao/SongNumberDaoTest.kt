package io.github.alelk.pws.database.dao

import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.clean
import io.github.alelk.pws.database.entity.bookEntity
import io.github.alelk.pws.database.entity.songEntity
import io.github.alelk.pws.database.entity.songNumberEntity
import io.github.alelk.pws.database.pwsDbForTest
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.removeEdgecases
import io.kotest.property.arbitrary.take

@DaoTest
class SongNumberDaoTest : FeatureSpec({

  lateinit var db: PwsDatabase

  beforeContainer { db = pwsDbForTest(inMemory = true) }
  afterContainer { db.clean(); db.close() }

  feature("song number dao test") {

    val books = Arb.bookEntity().removeEdgecases().take(5).toList().distinctBy { it.id }
    val bookIds = books.map { it.id }
    scenario("insert books") {
      db.bookDao().insert(books)
    }

    val songs = Arb.songEntity().take(100).toList().distinctBy { it.id }
    val songIds = songs.map { it.id }
    scenario("insert songs") {
      db.songDao().insert(songs)
    }

    val songNumbers =
      Arb
        .songNumberEntity(bookId = Arb.of(bookIds), songId = Arb.of(songIds)).take(500)
        .distinctBy { it.songId to it.bookId }
        .distinctBy { it.bookId to it.number }
        .toList()

    scenario("insert song numbers one by one and get by id") {
      songNumbers.take(100).forEach { songNumber ->
        db.songNumberDao().insert(songNumber)
        db.songNumberDao().getById(songNumber.bookId, songNumber.songId) shouldBe songNumber
      }
    }

    scenario("count song numbers") {
      db.songNumberDao().count() shouldBe 100
    }

    scenario("delete song number") {
      songNumbers.drop(100).take(100).forEach { songNumber ->
        db.songNumberDao().insert(songNumber)
        db.songNumberDao().delete(songNumber)
        db.songNumberDao().getById(songNumber.bookId, songNumber.songId) shouldBe null
        db.songNumberDao().count() shouldBe 100
      }
    }

    scenario("delete all song numbers") {
      db.songNumberDao().deleteAll()
      db.songNumberDao().count() shouldBe 0
    }

    scenario("insert song number collection") {
      db.songNumberDao().insert(songNumbers)
    }

    scenario("check count") {
      db.songNumberDao().count() shouldBe songNumbers.size
    }
  }
})