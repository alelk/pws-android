package io.github.alelk.pws.android.compose.platform

import android.net.Uri
import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.github.alelk.pws.android.compose.R
import io.github.alelk.pws.domain.telemetry.Telemetry
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.koin.core.context.stopKoin
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

/**
 * Plan 2026-09-30, 05.3: the backup text between "export" and the file choice lives in a temp file
 * (its name survives Activity re-creation), is written to the picked document, and is deleted afterwards;
 * user messages are string resources, exception text goes to telemetry only.
 *
 * Red check: drop `file.delete()` in [BackupFileActions.completeExport] and run
 * `./gradlew :app-compose:testRuDebugUnitTest --tests '*BackupFileActionsTest*'`, then revert.
 */
@RobolectricTest(sdk = [34])
class BackupFileActionsTest :
  FeatureSpec({

    // Robolectric starts the real Application (and Koin) for each test.
    afterTest { runCatching { stopKoin() } }

    class Fixture {
      val cacheDir: File = Files.createTempDirectory("pws-backup-actions").toFile()
      val toasts = mutableListOf<Int>()
      val errors = mutableListOf<String?>()
      val written = ByteArrayOutputStream()
      var input: String? = null
      var exportText: () -> String = { "backup-text" }
      val restored = mutableListOf<String>()
      var restoreFailure: Throwable? = null

      private val telemetry = object : Telemetry {
        override fun recordError(throwable: Throwable, message: String?, attributes: Map<String, String>) {
          errors += message
        }
        override fun log(message: String) = Unit
        override fun event(name: String, params: Map<String, Any?>) = Unit
        override fun setUserProperty(key: String, value: String?) = Unit
      }

      val actions = BackupFileActions(
        cacheDir = cacheDir,
        streams = BackupFileActions.ContentStreams(
          openInput = { input?.let { text -> ByteArrayInputStream(text.toByteArray()) } },
          openOutput = { written },
        ),
        operations = BackupFileActions.Operations(
          exportText = { exportText() },
          restoreFromText = {
            restoreFailure?.let { failure -> throw failure }
            restored += it
          },
        ),
        telemetry = telemetry,
        toaster = { toasts += it },
      )

      val target: Uri = Uri.parse("content://documents/backup.pws")
    }

    feature("export") {
      scenario("backup text goes through a temp file to the picked document, and the file is deleted") {
        val f = Fixture()
        val request = f.actions.prepareExport().shouldNotBeNull()
        File(f.cacheDir, request.tempFileName).readText() shouldBe "backup-text"
        request.suggestedFileName.startsWith("pws_backup_") shouldBe true
        request.suggestedFileName.endsWith(".pws") shouldBe true

        f.actions.completeExport(request.tempFileName, f.target)

        f.written.toString() shouldBe "backup-text"
        File(f.cacheDir, request.tempFileName).exists() shouldBe false
        f.toasts shouldContainExactly listOf(R.string.backup_saved)
      }

      scenario("a cancelled picker only deletes the temp file") {
        val f = Fixture()
        val request = f.actions.prepareExport().shouldNotBeNull()

        f.actions.completeExport(request.tempFileName, null)

        File(f.cacheDir, request.tempFileName).exists() shouldBe false
        f.written.size() shouldBe 0
        f.toasts.shouldBeEmpty()
      }

      scenario("a failed export is reported with a resource message and recorded") {
        val f = Fixture().apply { exportText = { error("secret details") } }

        f.actions.prepareExport().shouldBeNull()

        f.toasts shouldContainExactly listOf(R.string.backup_export_failed)
        f.errors shouldContainExactly listOf("backup_export_failed")
      }

      scenario("a file name outside our temp files is ignored") {
        val f = Fixture()
        val victim = File(f.cacheDir, "important.txt").apply { writeText("keep") }

        f.actions.completeExport("../important.txt", f.target)
        f.actions.completeExport("important.txt", f.target)

        victim.exists() shouldBe true
        f.written.size() shouldBe 0
      }
    }

    feature("import") {
      scenario("the chosen file is restored") {
        val f = Fixture().apply { input = "file-text" }

        f.actions.import(f.target)

        f.restored shouldContainExactly listOf("file-text")
        f.toasts shouldContainExactly listOf(R.string.backup_import_done)
      }

      scenario("an unreadable file or a failed restore shows a resource message") {
        val unreadable = Fixture()
        unreadable.actions.import(unreadable.target)
        unreadable.toasts shouldContainExactly listOf(R.string.backup_import_failed)
        unreadable.errors shouldContainExactly listOf("backup_import_failed")

        val broken = Fixture().apply {
          input = "file-text"
          restoreFailure = IllegalStateException("db is locked")
        }
        broken.actions.import(broken.target)
        broken.toasts shouldContainExactly listOf(R.string.backup_import_failed)
      }
    }
  })
