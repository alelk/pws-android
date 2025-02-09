package io.github.alelk.pws.database.dao

import android.os.Build
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.entity.SongEntity
import io.github.alelk.pws.database.pwsDbForTest
import io.github.alelk.pws.domain.distinctBy
import io.github.alelk.pws.database.entity.songEntity
import io.github.alelk.pws.database.entity.songSongReferenceEntity
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.removeEdgecases
import io.kotest.property.arbitrary.take
import io.kotest.property.checkAll

@RobolectricTest(sdk = Build.VERSION_CODES.M)
class SongSongReferenceDaoTest : FeatureSpec({

  lateinit var db: PwsDatabase

  beforeContainer { db = pwsDbForTest() }
  afterContainer { db.close() }

  feature("song song reference dao crud test") {

    lateinit var songs: List<SongEntity>
    scenario("insert songs") {
      val ids = db.songDao().insert(Arb.songEntity().removeEdgecases().take(50).distinctBy { it.id }.toList())
      songs = db.songDao().getByIds(ids)
    }
    val songIds = songs.map { it.id }

    val songRefArb =
      Arb.songSongReferenceEntity(songId = Arb.of(songIds), refSongId = Arb.of(songIds)).removeEdgecases().distinctBy { it.refSongId to it.songId }

    scenario("insert song song reference and get by id") {
      val refs = songRefArb.take(20).toList()
      refs.forEach { reference ->
        val id = db.songSongReferenceDao().insert(reference)
        db.songSongReferenceDao().getById(id) shouldBe reference.copy(id = id)
      }
    }

    db.songSongReferenceDao().deleteAll()

    scenario("insert multiple song song references and get by ids") {
      val refs = songRefArb.take(20).toList().distinctBy { it.refSongId to it.songId }
      val ids = db.songSongReferenceDao().insertAll(refs)
      ids shouldHaveSize refs.size
      db.songSongReferenceDao().getByIds(ids).map { it.copy(id = null) } shouldContainExactlyInAnyOrder refs
    }

    scenario("delete song song reference") {
      checkAll(10, songRefArb) { reference ->
        val id = db.songSongReferenceDao().insert(reference)
        db.songSongReferenceDao().delete(reference.copy(id = id))
        db.songSongReferenceDao().getById(id) shouldBe null
      }
    }
  }
})