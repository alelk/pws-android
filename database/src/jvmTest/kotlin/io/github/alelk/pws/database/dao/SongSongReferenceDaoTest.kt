package io.github.alelk.pws.database.dao

import io.github.alelk.pws.database.clean
import io.github.alelk.pws.domain.distinctBy
import io.github.alelk.pws.database.common.entity.SongEntity
import io.github.alelk.pws.database.common.entity.songEntity
import io.github.alelk.pws.database.common.entity.songSongReferenceEntity
import io.github.alelk.pws.database.pwsDbForTest
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.removeEdgecases
import io.kotest.property.arbitrary.take
import io.kotest.property.checkAll

class SongSongReferenceDaoTest : FeatureSpec({

  val db = pwsDbForTest(inMemory = true)
  val songDao = db.songDao()
  val songSongReferenceDao = db.songSongReferenceDao()

  beforeContainer { db.clean() }
  afterContainer { db.clean() }

  feature("song song reference dao crud test") {

    lateinit var songs: List<SongEntity>
    scenario("insert songs") {
      val ids = songDao.insert(Arb.songEntity().removeEdgecases().take(50).distinctBy { it.id }.toList())
      songs = songDao.getByIds(ids)
    }
    val songIds = songs.map { it.id }

    val songRefArb =
      Arb.songSongReferenceEntity(songId = Arb.of(songIds), refSongId = Arb.of(songIds)).removeEdgecases().distinctBy { it.refSongId to it.songId }

    scenario("insert song song reference and get by id") {
      val refs = songRefArb.take(20).toList()
      refs.forEach { reference ->
        val id = songSongReferenceDao.insert(reference)
        songSongReferenceDao.getById(id) shouldBe reference.copy(id = id)
      }
    }

    songSongReferenceDao.deleteAll()

    scenario("insert multiple song song references and get by ids") {
      val refs = songRefArb.take(20).toList().distinctBy { it.refSongId to it.songId }
      val ids = songSongReferenceDao.insertAll(refs)
      ids shouldHaveSize refs.size
      songSongReferenceDao.getByIds(ids).map { it.copy(id = null) } shouldContainExactlyInAnyOrder refs
    }

    scenario("delete song song reference") {
      checkAll(10, songRefArb) { reference ->
        val id = songSongReferenceDao.insert(reference)
        songSongReferenceDao.delete(reference.copy(id = id))
        songSongReferenceDao.getById(id) shouldBe null
      }
    }
  }
})