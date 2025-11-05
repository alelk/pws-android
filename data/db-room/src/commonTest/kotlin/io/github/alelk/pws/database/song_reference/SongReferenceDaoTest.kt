package io.github.alelk.pws.database.song_reference

import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.clean
import io.github.alelk.pws.database.core.DaoTest
import io.github.alelk.pws.database.song.songEntity
import io.github.alelk.pws.database.pwsDbForTest
import io.github.alelk.pws.database.song.SongEntity
import io.github.alelk.pws.domain.distinctBy
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.removeEdgecases
import io.kotest.property.arbitrary.take
import io.kotest.property.checkAll

@DaoTest
class SongReferenceDaoTest : FeatureSpec({

  lateinit var db: PwsDatabase

  beforeContainer { db = pwsDbForTest(inMemory = true) }
  afterContainer { db.clean(); db.close() }

  feature("song song reference dao crud test") {

    lateinit var songs: List<SongEntity>
    scenario("insert songs") {
      val songsToInsert = Arb.Companion.songEntity().removeEdgecases().take(50).distinctBy { it.id }.toList()
      db.songDao().insert(songsToInsert)
      songs = db.songDao().getByIds(songsToInsert.map { it.id })
    }
    val songIds = songs.map { it.id }

    val songRefArb =
      Arb.Companion.songSongReferenceEntity(
        songId = Arb.Companion.of(songIds).removeEdgecases(),
        refSongId = Arb.Companion.of(songIds).removeEdgecases()
      ).removeEdgecases().distinctBy { it.refSongId to it.songId }

    scenario("insert song song reference and get by id") {
      val refs = songRefArb.take(20).toList()
      refs.forEach { reference ->
        db.songReferenceDao().insert(reference)
        db.songReferenceDao().getById(reference.songId, reference.refSongId) shouldBe reference
      }
    }

    db.songReferenceDao().deleteAll()

    scenario("insert multiple song song references and get by ids") {
      val refs = songRefArb.take(20).toList()
      db.songReferenceDao().insertAll(refs)
      db.songReferenceDao().count() shouldBe 20
    }

    scenario("delete song song reference") {
        checkAll(10, songRefArb) { reference ->
            db.songReferenceDao().insert(reference)
            db.songReferenceDao().delete(reference)
            db.songReferenceDao().getById(reference.songId, reference.refSongId) shouldBe null
        }
    }
  }
})