package io.github.alelk.pws.database.song_tag

import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.clean
import io.github.alelk.pws.database.core.DaoTest
import io.github.alelk.pws.database.pwsDbForTest
import io.github.alelk.pws.database.withSongEntities
import io.github.alelk.pws.database.withTagEntities
import io.github.alelk.pws.domain.distinctBy
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.take
import io.kotest.property.checkAll

@DaoTest
class SongTagDaoTest : FeatureSpec({

  lateinit var db: PwsDatabase

  beforeContainer { db = pwsDbForTest(inMemory = true) }
  afterContainer { db.clean(); db.close() }

  feature("song tag crud") {
    db.withSongEntities(countSongs = 50) { songs ->
      val songIds = songs.map { it.id }
      db.withTagEntities(countTags = 10) { tags ->
        val tagIds = tags.map { it.id }
        val songTagArb = Arb.Companion.songTagEntity(songId = Arb.Companion.of(songIds), tagId = Arb.Companion.of(tagIds)).distinctBy { it.songId to it.tagId }

        scenario("insert single and get by id") {
          checkAll(10, songTagArb) { snTag ->
              db.songTagDao().insert(snTag)
              db.songTagDao().getById(snTag.songId, snTag.tagId) shouldBe snTag
          }
          db.songTagDao().count() shouldBe 10
        }

        val snTags = songTagArb.take(10).toList()
        scenario("insert batch and get by ids") {
          db.songTagDao().insert(snTags)
          db.songTagDao().count() shouldBe 20
        }

        scenario("delete single") {
          db.songTagDao().delete(snTags.first())
          db.songTagDao().count() shouldBe 19
        }

        scenario("delete batch") {
          db.songTagDao().delete(snTags)
          db.songTagDao().count() shouldBe 10
        }
      }
    }
  }
})