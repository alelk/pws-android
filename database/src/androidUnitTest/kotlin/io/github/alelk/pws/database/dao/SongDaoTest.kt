package io.github.alelk.pws.database.dao

import android.os.Build
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.pwsDbForTest
import io.github.alelk.pws.database.entity.songEntity
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.removeEdgecases
import io.kotest.property.arbitrary.take
import io.kotest.property.checkAll

@RobolectricTest(sdk = Build.VERSION_CODES.M)
class SongDaoTest : FeatureSpec({

  lateinit var db: PwsDatabase

  beforeContainer { db = pwsDbForTest() }
  afterContainer { db.close() }

  feature("song dao crud test") {
    scenario("insert songs one by one and get by id") {
      checkAll(10, Arb.songEntity().removeEdgecases()) { song ->
        db.songDao().insert(song) shouldBe song.id
        db.songDao().getById(song.id) shouldBe song
      }
    }

    scenario("count songs") {
      db.songDao().count() shouldBe 10
    }

    scenario("delete song") {
      checkAll(10, Arb.songEntity()) { song ->
        db.songDao().insert(song)
        db.songDao().delete(song)
        db.songDao().getById(song.id) shouldBe null
        db.songDao().count() shouldBe 10
      }
    }

    scenario("delete all songs") {
      db.songDao().deleteAll()
      db.songDao().count() shouldBe 0
    }
  }

  feature("insert/get/delete song collection") {
    val songs = Arb.songEntity().removeEdgecases().take(20).toList().distinctBy { it.id }

    scenario("insert song collection") {
      db.songDao().insert(songs) shouldBe songs.map { it.id }
    }

    scenario("check count") {
      db.songDao().count() shouldBe songs.size
    }

    scenario("get songs collection by ids") {
      val found = db.songDao().getByIds(songs.map { it.id })
      found.size shouldBe songs.size
      found shouldContainExactlyInAnyOrder songs
    }

    scenario("delete songs collection") {
      val extraSong = Arb.songEntity().next()
      db.songDao().insert(extraSong)
      db.songDao().delete(songs)
      db.songDao().count() shouldBe 1
      db.songDao().getById(extraSong.id) shouldBe extraSong
    }
  }
})