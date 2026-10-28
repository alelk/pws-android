/**
 * The local Docker daemon's own API version, asked once per Gradle daemon via `docker version` and
 * memoized. Test tasks pass it to Testcontainers' client (`-Dapi.version=…`):
 *
 * docker-java has no real negotiation — left unset it falls back to a long-stale built-in version
 * that a current Docker Engine rejects outright. A hand-picked replacement goes stale the same way
 * at the next engine upgrade (it bit one project twice), so ask the engine that is actually running
 * here — laptop, hosted runner, self-hosted runner. No `docker` on PATH → `null` → no override; the
 * build then fails later and more clearly, at the Testcontainers call site.
 *
 * Configuration cache: starting a process at configuration time is reported as a problem once
 * `org.gradle.configuration-cache=true`. Then read it through Gradle instead:
 * `providers.exec { commandLine("docker", "version", "--format", "{{.Server.APIVersion}}"); isIgnoreExitValue = true }
 *   .standardOutput.asText.map(String::trim)` and pass it with a `CommandLineArgumentProvider`.
 */
object DockerApiVersion {
    val detected: String? by lazy {
        runCatching {
            ProcessBuilder("docker", "version", "--format", "{{.Server.APIVersion}}")
                .redirectErrorStream(true)
                .start()
                .let { process ->
                    val output = process.inputStream.bufferedReader().readText().trim()
                    process.waitFor()
                    output.takeIf { process.exitValue() == 0 && it.isNotBlank() }
                }
        }.getOrNull()
    }
}
