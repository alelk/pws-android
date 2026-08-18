package io.github.alelk.pws.database

import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.io.path.createTempDirectory

@RobolectricTest(sdk = 34)
class DiscardHalfCreatedDatabaseTest : FeatureSpec({

  beforeContainer { setupTimberForTest() }

  val passphrase = "test-passphrase".toByteArray()

  fun dbFile(content: String = "not empty"): File {
    val dir = createTempDirectory("pws-db-test").toFile()
    return File(dir, "pws.db").also { it.writeText(content) }
  }

  fun sideFiles(dbFile: File) = listOf("-journal", "-wal", "-shm").map { File(dbFile.path + it) }

  feature("a database left half-created") {

    scenario("is deleted together with its journal/WAL side files") {
      val file = dbFile()
      sideFiles(file).forEach { it.writeText("side") }

      discardHalfCreatedDatabase(file, passphrase) { _, _ -> DatabaseFileState(userVersion = 0, objectCount = 6) } shouldBe true

      file.exists() shouldBe false
      sideFiles(file).forEach { it.exists() shouldBe false }
    }
  }

  feature("a database that may hold user data") {

    scenario("is kept when it carries a schema version") {
      val file = dbFile()
      discardHalfCreatedDatabase(file, passphrase) { _, _ -> DatabaseFileState(userVersion = 15, objectCount = 42) } shouldBe false
      file.exists() shouldBe true
    }

    scenario("is kept when it cannot be read at all — a lost passphrase must not wipe the data") {
      val file = dbFile()
      discardHalfCreatedDatabase(file, passphrase) { _, _ -> null } shouldBe false
      file.exists() shouldBe true
    }
  }

  feature("a database that is not there yet") {

    scenario("an empty file is left to Room") {
      val file = dbFile(content = "")
      discardHalfCreatedDatabase(file, passphrase) { _, _ -> error("state must not be read") } shouldBe false
      file.exists() shouldBe true
    }

    scenario("a missing file is left to Room") {
      val file = dbFile().also { it.delete() }
      discardHalfCreatedDatabase(file, passphrase) { _, _ -> error("state must not be read") } shouldBe false
    }

    scenario("a file with an empty schema is left to Room") {
      val file = dbFile()
      discardHalfCreatedDatabase(file, passphrase) { _, _ -> DatabaseFileState(userVersion = 0, objectCount = 0) } shouldBe false
      file.exists() shouldBe true
    }
  }
})
