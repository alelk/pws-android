package io.github.alelk.pws.database.dao

import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.database.clean
import io.github.alelk.pws.database.entity.historyEntity
import io.github.alelk.pws.database.pwsDbForTest
import io.github.alelk.pws.database.withBookEntities
import io.github.alelk.pws.database.withSongEntities
import io.github.alelk.pws.database.withSongNumberEntities
import io.github.alelk.pws.domain.distinctBy
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.removeEdgecases
import io.kotest.property.checkAll

@DaoTest
class HistoryDaoTest : FeatureSpec({

  lateinit var db: PwsDatabase

  beforeContainer { db = pwsDbForTest(inMemory = true) }
  afterContainer { db.clean(); db.close() }

  feature("history crud") {
    db.withBookEntities(countBooks = 10) {
      db.withSongEntities(countSongs = 500) {
        db.withSongNumberEntities(countSongNumbers = 600) { songNumbers ->
          scenario("insert history") {
            checkAll(
              20,
              Arb.historyEntity(id = null, songNumberId = Arb.of(songNumbers.map { it.id })).distinctBy { it.songNumberId }.removeEdgecases()
            ) { history ->
              val id = db.historyDao().insert(history)
              db.historyDao().getById(id) shouldBe history.copy(id = id)
            }
          }

          scenario("check count") {
            db.historyDao().count() shouldBe 20
          }

          scenario("remove all") {
            db.historyDao().deleteAll()
            db.historyDao().count() shouldBe 0
          }
        }
      }
    }
  }

})