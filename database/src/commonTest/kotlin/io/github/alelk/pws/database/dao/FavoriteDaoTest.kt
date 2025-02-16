package io.github.alelk.pws.database.dao

import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.clean
import io.github.alelk.pws.database.entity.favoriteEntity
import io.github.alelk.pws.database.pwsDbForTest
import io.github.alelk.pws.database.withBookEntities
import io.github.alelk.pws.database.withSongEntities
import io.github.alelk.pws.database.withSongNumberEntities
import io.github.alelk.pws.domain.distinctBy
import io.kotest.common.DelicateKotest
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.distinct
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.removeEdgecases
import io.kotest.property.checkAll

@OptIn(DelicateKotest::class)
@DaoTest
class FavoriteDaoTest : FeatureSpec({

  lateinit var db: PwsDatabase

  beforeContainer { db = pwsDbForTest(inMemory = true) }
  afterContainer { db.clean(); db.close() }

  feature("favorite crud") {
    db.withBookEntities(countBooks = 10) {
      db.withSongEntities(countSongs = 500) {
        db.withSongNumberEntities(countSongNumbers = 600) { songNumbers ->

          scenario("insert favorite") {
            checkAll(20, Arb.favoriteEntity(songNumberId = Arb.of(songNumbers.map { it.id })).distinctBy { it.songNumberId }.removeEdgecases()) { favorite ->
              db.favoriteDao().insert(favorite)
              db.favoriteDao().getBySongNumberId(favorite.songNumberId) shouldBe favorite
            }
          }

          scenario("check count") {
            db.favoriteDao().count() shouldBe 20
          }

          scenario("remove all") {
            db.favoriteDao().deleteAll()
            db.favoriteDao().count() shouldBe 0
          }

          scenario("toggle favorite") {
            checkAll(10, Arb.of(songNumbers).distinct()) { songNumber ->
              db.favoriteDao().toggleFavorite(songNumber.id)
              db.favoriteDao().isFavorite(songNumber.id) shouldBe true
              db.favoriteDao().toggleFavorite(songNumber.id)
              db.favoriteDao().isFavorite(songNumber.id) shouldBe false
            }
            db.favoriteDao().count() shouldBe 0
          }
        }
      }
    }
  }

})