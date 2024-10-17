package com.alelk.pws.database.dao

import android.os.Build
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import com.alelk.pws.database.PwsDatabase
import com.alelk.pws.database.pwsDbForTest
import io.github.alelk.pws.database.common.entity.tagEntity
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.take
import io.kotest.property.checkAll

@RobolectricTest(sdk = Build.VERSION_CODES.M)
class TagDaoTest : FeatureSpec({

  lateinit var db: PwsDatabase

  beforeContainer { db = pwsDbForTest() }
  afterContainer { db.close() }

  feature("tag dao crud") {

    scenario("insert a tag") {
      checkAll(10, Arb.tagEntity()) { tag ->
        db.tagDao().insert(tag)
      }
      db.tagDao().count() shouldBe 10
    }

    val tags = Arb.tagEntity().take(10).toList()
    scenario("insert batch") {
      db.tagDao().insert(tags)
      db.tagDao().count() shouldBe 20
    }

    scenario("get tag by id") {
      checkAll(5, Arb.of(tags)) { tag ->
        db.tagDao().getById(tag.id) shouldBe tag
      }
    }

    scenario("get batch by ids") {
      db.tagDao().getByIds(tags.map { it.id }) shouldContainExactlyInAnyOrder tags
    }

    scenario("update one") {
      db.tagDao().update(tags.first().copy(name = "tag new name - 1"))
      db.tagDao().getById(tags.first().id)?.name shouldBe "tag new name - 1"
    }

    scenario("update batch") {
      db.tagDao().update(tags.mapIndexed { idx, tag -> tag.copy(name = "tag new name - $idx") })
      val updated = db.tagDao().getByIds(tags.map { it.id })
      updated shouldHaveSize 10
      updated.forEach { t -> t.name shouldStartWith "tag new name" }
    }

    scenario("delete one") {
      db.tagDao().delete(tags.first())
      db.tagDao().getById(tags.first().id) shouldBe null
      db.tagDao().count() shouldBe 19
    }

    scenario("delete batch") {
      db.tagDao().delete(tags)
      db.tagDao().getByIds(tags.map { it.id }) shouldHaveSize 0
      db.tagDao().count() shouldBe 10
    }

    scenario("delete all") {
      db.tagDao().deleteAll()
      db.tagDao().count() shouldBe 0
    }
  }
})