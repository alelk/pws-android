package io.github.alelk.pws.database.dao

import android.os.Build
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.pwsDbForTest
import io.github.alelk.pws.database.withBookEntities
import io.github.alelk.pws.database.withSongEntities
import io.github.alelk.pws.database.withSongNumberEntities
import io.github.alelk.pws.database.withTagEntities
import io.github.alelk.pws.database.common.entity.songNumberTagEntity
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.take
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.toList

@RobolectricTest(sdk = Build.VERSION_CODES.M)
class SongNumberTagDaoTest : FeatureSpec({

  lateinit var db: PwsDatabase

  beforeContainer { db = pwsDbForTest() }
  afterContainer { db.close() }

  feature("song number tag crud") {
    db.withBookEntities(countBooks = 5) { books ->
      db.withSongEntities(countSongs = 50) { songs ->
        val bookIds = books.map { it.id!! }
        val songIds = songs.map { it.id }
        db.withSongNumberEntities(countSongNumbers = 100, Arb.of(bookIds), Arb.of(songIds)) { songNumbers ->
          db.withTagEntities(countTags = 12) { tags ->
            val songNumberIds = songNumbers.map { it.id!! }
            val tagIds = tags.map { it.id }

            val snTagArb = Arb.songNumberTagEntity(songNumberId = Arb.of(songNumberIds), tagId = Arb.of(tagIds))

            scenario("insert single and get by id") {
              checkAll(10, snTagArb) { snTag ->
                db.songNumberTagDao().insert(snTag)
                db.songNumberTagDao().getById(snTag.songNumberId, snTag.tagId) shouldBe snTag
              }
              db.songNumberTagDao().count() shouldBe 10
            }

            val snTags = snTagArb.take(10).toList()
            scenario("insert batch and get by ids") {
              db.songNumberTagDao().insert(snTags)
              db.songNumberTagDao().count() shouldBe 20
            }

            scenario("delete single") {
              db.songNumberTagDao().delete(snTags.first())
              db.songNumberTagDao().count() shouldBe 19
            }

            scenario("delete batch") {
              db.songNumberTagDao().delete(snTags)
              db.songNumberTagDao().count() shouldBe 10
            }

            db.songNumberTagDao().insert(snTags + snTagArb.take(30).toList())
            db.songNumberTagDao().count() shouldBe 50

            val allSnTags = db.songNumberTagDao().getAll().toList()
            allSnTags.count() shouldBe 50

            scenario("get by book external id") {
              checkAll(10, Arb.of(books.map { it.externalId })) { bookExternalId ->
                val book = books.find { it.externalId == bookExternalId }!!
                val bookSongNumbers = songNumbers.filter { it.bookId == book.id }
                val bookTags = allSnTags.filter { it.songNumberId in bookSongNumbers.map { sn -> sn.id } }
                val expected = bookTags.associateWith { snt -> bookSongNumbers.first { it.id == snt.songNumberId } }
                val actual = db.songNumberTagDao().getByBookExternalId(bookExternalId)
                actual shouldBe expected
              }
            }

          }
        }
      }
    }
  }
})