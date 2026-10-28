package io.github.alelk.pws.build

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

// Release invariants of the rustore build: it must install as an update over the published RuStore
// fork (2.3.1, versionCode 38) — see plan 2026-09-29, §3 (I2–I5).
object RustoreInvariants {
  const val APPLICATION_ID = "io.github.alelk.pws.app"
  const val DB_AUTHORITY = "io.github.alelk.pws.database"
  const val LAST_FORK_VERSION_CODE = 38
  const val MAX_MIN_SDK = 23
}

/**
 * Fails the rustore release build when it could not be installed as an update of the RuStore fork
 * (or would clash with its content provider). Wired into `preRustoreReleaseBuild`.
 */
abstract class VerifyRustoreInvariantsTask : DefaultTask() {
  @get:Input
  abstract val applicationId: Property<String>

  @get:Input
  abstract val versionCode: Property<Int>

  @get:Input
  abstract val minSdk: Property<Int>

  @get:Input
  abstract val dbAuthority: Property<String>

  @TaskAction
  fun verify() {
    val problems = buildList {
      if (applicationId.get() != RustoreInvariants.APPLICATION_ID)
        add("applicationId = ${applicationId.get()}, must be ${RustoreInvariants.APPLICATION_ID}")
      if (versionCode.get() <= RustoreInvariants.LAST_FORK_VERSION_CODE)
        add("versionCode = ${versionCode.get()}, must be > ${RustoreInvariants.LAST_FORK_VERSION_CODE} (last RuStore fork release)")
      if (minSdk.get() > RustoreInvariants.MAX_MIN_SDK)
        add("minSdk = ${minSdk.get()}, must be <= ${RustoreInvariants.MAX_MIN_SDK} (fork users would lose updates)")
      if (dbAuthority.get() != RustoreInvariants.DB_AUTHORITY)
        add("db_authority = ${dbAuthority.get()}, must be ${RustoreInvariants.DB_AUTHORITY}")
    }
    check(problems.isEmpty()) {
      "rustore release invariants violated (plan 2026-09-29 §3):\n - " + problems.joinToString("\n - ")
    }
    logger.lifecycle("rustore invariants OK: ${applicationId.get()} vc=${versionCode.get()} minSdk=${minSdk.get()}")
  }
}
