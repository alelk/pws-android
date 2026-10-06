package io.github.alelk.pws.build

import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Downloads the seed bundles for one variant from the catalog and writes them into
 * `<outputDir>/seed-books/`, which AGP wires into the variant's merged assets. Bundle file names
 * (`{bookId}-{bundleVariant}-{catalogVersion}.book.yaml.gz.enc`) are resolved from the catalog's
 * top-level `version`; each download is verified against the catalog `checksum` (SHA-256).
 *
 * Up-to-date checking is keyed on the declared inputs (book IDs / bundle variant / catalog URLs).
 * The remote catalog version is not an input, so run `clean` (or bump the seed list) to force a
 * refresh when the catalog publishes newer bundles.
 */
abstract class DownloadSeedBundlesTask : DefaultTask() {
  @get:Input
  abstract val bookIds: ListProperty<String>

  @get:Input
  abstract val bundleVariant: Property<String>

  @get:Input
  abstract val catalogUrls: ListProperty<String>

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @TaskAction
  fun download() {
    val seedDir = outputDir.get().dir("seed-books").asFile
    seedDir.deleteRecursively()
    seedDir.mkdirs()

    val ids = bookIds.get()
    if (ids.isEmpty()) return
    val variant = bundleVariant.get()
    val urls = catalogUrls.get()

    // Fetch the catalog from the first reachable mirror.
    val catalog = urls.firstNotNullOfOrNull { url ->
      runCatching { url to String(fetch(url), Charsets.UTF_8) }
        .onFailure { logger.warn("Seed: catalog fetch failed from $url: ${it.message}") }
        .getOrNull()
    } ?: error("Seed: cannot fetch catalog for variant '$variant' from any mirror: $urls")
    val (catalogUrl, catalogJson) = catalog

    @Suppress("UNCHECKED_CAST")
    val parsed = groovy.json.JsonSlurper().parseText(catalogJson) as Map<String, Any?>
    val catalogVersion = parsed["version"] as? String
      ?: error("Seed: catalog has no 'version' field ($catalogUrl)")

    @Suppress("UNCHECKED_CAST")
    val books = parsed["books"] as? List<Map<String, Any?>>
      ?: error("Seed: catalog has no 'books' array ($catalogUrl)")

    val checksumById: Map<String, String> = books.associate { entry ->
      @Suppress("UNCHECKED_CAST")
      val book = entry["book"] as Map<String, Any?>
      (book["id"] as String) to (entry["checksum"] as String)
    }

    val base = catalogUrl.substringBeforeLast('/')
    ids.forEach { id ->
      val expected = checksumById[id]
        ?: error("Seed: book '$id' not found in catalog for variant '$variant' ($catalogUrl)")
      val fileName = "$id-$variant-$catalogVersion.book.yaml.gz.enc"
      val bytes = fetch("$base/$fileName")
      val actual = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }
      check(actual == expected) {
        "Seed: checksum mismatch for $fileName — expected $expected, got $actual"
      }
      seedDir.resolve(fileName).writeBytes(bytes)
      logger.lifecycle("Seed: baked $fileName (${bytes.size} bytes) into assets/seed-books/")
    }
  }

  private fun fetch(url: String): ByteArray {
    val conn = URI(url).toURL().openConnection() as HttpURLConnection
    conn.setRequestProperty("User-Agent", "pws-android-build/1.0 (+github.com/alelk/pws-android)")
    conn.setRequestProperty("Accept", "application/octet-stream, application/json")
    conn.connectTimeout = 30_000
    conn.readTimeout = 120_000
    conn.instanceFollowRedirects = true
    try {
      val code = conn.responseCode
      check(code in 200..299) { "HTTP $code for $url" }
      return conn.inputStream.use { it.readBytes() }
    } finally {
      conn.disconnect()
    }
  }
}
