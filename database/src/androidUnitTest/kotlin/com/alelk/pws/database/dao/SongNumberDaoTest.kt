package com.alelk.pws.database.dao

import android.os.Build
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import com.alelk.pws.database.PwsDatabase
import com.alelk.pws.database.pwsDbForTest
import com.alelk.pws.database.withBookEntities
import com.alelk.pws.database.withSongEntities
import com.alelk.pws.database.withSongNumberEntities
import io.github.alelk.pws.database.common.entity.SongNumberEntity
import io.github.alelk.pws.database.common.entity.bookEntity
import io.github.alelk.pws.database.common.entity.songEntity
import io.github.alelk.pws.database.common.entity.songNumberEntity
import io.kotest.common.DelicateKotest
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.distinct
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.removeEdgecases
import io.kotest.property.arbitrary.take
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.firstOrNull

@OptIn(DelicateKotest::class)
@RobolectricTest(sdk = Build.VERSION_CODES.M)
class SongNumberDaoTest : FeatureSpec({

  lateinit var db: PwsDatabase

  beforeContainer { db = pwsDbForTest() }
  afterContainer { db.close() }

  feature("song number dao test") {

    lateinit var bookIds: List<Long>
    scenario("insert books") {
      bookIds = db.bookDao().insert(Arb.bookEntity().removeEdgecases().take(5).toList().distinctBy { it.id }.distinctBy { it.externalId })
    }

    lateinit var songIds: List<Long>
    scenario("insert songs") {
      songIds = db.songDao().insert(Arb.songEntity().take(100).toList().distinctBy { it.id })
    }

    val songNumbers =
      Arb
        .songNumberEntity(bookId = Arb.of(bookIds), songId = Arb.of(songIds)).take(500)
        .distinctBy { it.songId to it.bookId }
        .distinctBy { it.bookId to it.number }
        .toList()

    scenario("insert song numbers one by one and get by id") {
      songNumbers.take(100).forEach { songNumber ->
        val id = db.songNumberDao().insert(songNumber)
        id shouldNotBe null
        db.songNumberDao().getById(id).firstOrNull() shouldBe songNumber.copy(id = id)
      }
    }

    scenario("count song numbers") {
      db.songNumberDao().count() shouldBe 100
    }

    scenario("delete song number") {
      songNumbers.drop(100).take(100).forEach { songNumber ->
        val id = db.songNumberDao().insert(songNumber)
        db.songNumberDao().delete(songNumber.copy(id = id))
        db.songNumberDao().count() shouldBe 100
      }
    }

    scenario("delete all song numbers") {
      db.songNumberDao().deleteAll()
      db.songNumberDao().count() shouldBe 0
    }

    lateinit var songNumberIds: List<Long>
    scenario("insert song number collection") {
      songNumberIds = db.songNumberDao().insert(songNumbers)
      songNumberIds shouldHaveSize songNumbers.size
    }

    scenario("check count") {
      db.songNumberDao().count() shouldBe songNumbers.size
    }

    lateinit var savedSongNumbers: List<SongNumberEntity>
    scenario("get song numbers collection by ids") {
      savedSongNumbers = db.songNumberDao().getByIds(songNumberIds)
      savedSongNumbers.map { it.copy(id = null) } shouldContainExactlyInAnyOrder songNumbers
    }
  }

  feature("use book external id in queries") {
    db.withBookEntities(countBooks = 5) { books ->
      db.withSongEntities(countSongs = 100) { songs ->
        db.withSongNumberEntities(countSongNumbers = 150) { songNumbers ->
          val bookExternalIdArb = Arb.of(books.map { it.externalId })
          val songNumberArb = Arb.of(songNumbers.map { it.number })
          val bookExternalIdAndSongNumberArb = Arb.pair(bookExternalIdArb, songNumberArb).distinct()

          scenario("get song numbers by book external id and song number") {
            checkAll(150, bookExternalIdAndSongNumberArb) { (bookExternalId, songNumber) ->
              val result = db.songNumberDao().getByBookExternalIdAndSongNumber(bookExternalId, songNumber)
              val book = books.find { it.externalId == bookExternalId }
              val expected = songNumbers.filter { it.bookId == checkNotNull(book).id }.filter { it.number == songNumber }
              expected.size shouldBeLessThanOrEqual 1
              if (expected.isNotEmpty()) {
                result?.first shouldBe expected.firstOrNull()
                result?.second shouldBe book
              } else {
                result shouldBe null
              }
            }
          }

          scenario("delete song numbers by book external id and song number") {
            checkAll(150, bookExternalIdAndSongNumberArb) { (bookExternalId, songNumber) ->
              db.songNumberDao().deleteByBookExternalIdAndSongNumber(bookExternalId, songNumber)
              db.songNumberDao().getByBookExternalIdAndSongNumber(bookExternalId, songNumber) shouldBe null
            }
          }
        }
      }
    }
  }
})