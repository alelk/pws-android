# Verify the artifact, not the sources

A release ships bytes. Checks that read `src/` or the Gradle configuration say what *should* be in
the jar; a check that opens the built jar says what *is*. The second kind is the one that catches a
developer's `application-local.yaml` (with real secrets) riding into a published image because a
resource directory, an IDE run configuration or a copy task put it on the classpath once.

*Mine:* a release jar or image that carries the builder's local configuration — secrets of whoever
built it — while every source-level test is green.

## Three locks, layered

1. **Not on the classpath.** Local overrides are read from the working directory at runtime (or an
   explicit path), never from `src/main/resources`; `.gitignore` covers them.
2. **Excluded at packaging.** `shadowJar { exclude("application-local*.yaml") }` — so even a
   misplaced file does not travel.
3. **Verified after packaging.** A task opens the jar `shadowJar` produced and fails if any entry's
   file name starts with `application-local`. CI runs it right after `shadowJar`:
   `./gradlew :server:app:shadowJar :server:app:verifyReleaseJar`.
   Snippet: [`templates/shared/verify-release-jar.gradle.kts.snippet`](../templates/shared/verify-release-jar.gradle.kts.snippet).

The image-context script repeats lock 3 for anyone building an image by hand
([`single-workflow-release-image.md`](single-workflow-release-image.md)).

## Unambiguous artifact names

With the Shadow plugin the thin `jar` and the fat `shadowJar` may share a base name; then whichever
task ran last decides what a release glob (`app-*.jar`) or an image script picks up. Give the thin
jar its own base name:

```kotlin
tasks.jar { archiveBaseName = "server-app-thin" }
tasks.shadowJar { archiveBaseName = "app"; archiveClassifier = "" }
```

And check the other way round in the image script: a jar without the server engine's classes is the
thin one — refuse it.

## Keep the fat jar out of the everyday gate

`./gradlew build` should verify the product build without making every edit pay for a fat jar:
remove `shadowJar` from `assemble`'s dependencies and call it explicitly in the release job.

```kotlin
tasks.named("assemble") { setDependsOn(dependsOn.filterNot { it == tasks.shadowJar }) }
```

## Other artifact checks worth the same treatment

| Artifact | Check |
|----------|-------|
| shadow jar | has the server engine classes; has the build-metadata resources the image reads (version, schema fingerprint) |
| web tarball | has `index.html`; source maps removed before the image (or never shipped) |
| image | build arg that labels it (fingerprint/version) non-empty — `RUN test -n "$ARG"` fails the build instead of producing an empty label |

## Evidence

family-planner `server/app/build.gradle.kts` (`VerifyReleaseJarTask`, `tasks.jar { archiveBaseName =
"server-app-thin" }` after finding H5 where the last-run task decided the glob), step 66 K13 "risk
two", `deploy/bin/image-context.sh`, `deploy/Dockerfile` (`SCHEMA_FINGERPRINT` empty = build error).
