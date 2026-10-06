package io.github.alelk.pws.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Copies a minified variant's R8 `mapping.txt` out of `build/` into a stable, release-labelled
 * location so it can be uploaded to the AppMetrica console (Settings → "Mapping files"). Without a
 * mapping, release crash reports arrive obfuscated and are effectively unreadable.
 *
 * We deliberately do not use the official AppMetrica Gradle plugin: its current release (1.0.1)
 * drives the removed `com.android.build.gradle.api.ApplicationVariant` API and does not work on
 * AGP 9. Staging the file is AGP-version-proof; the upload itself is a manual (or CI) step,
 * documented in docs/monitoring.md.
 */
abstract class StageMappingFileTask : DefaultTask() {
  @get:InputFile
  abstract val mappingFile: RegularFileProperty

  @get:OutputFile
  abstract val stagedFile: RegularFileProperty

  @TaskAction
  fun stage() {
    val target = stagedFile.get().asFile
    target.parentFile?.mkdirs()
    mappingFile.get().asFile.copyTo(target, overwrite = true)
    logger.lifecycle("AppMetrica: mapping staged at ${target.absolutePath} — upload it to the AppMetrica console for this release")
  }
}
