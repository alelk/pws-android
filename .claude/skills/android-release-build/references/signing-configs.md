# Signing configs

A signing key is the app's identity on a device: an update installs only if it is signed with the
same certificate as what is installed. Lose the key or sign with the wrong one and the "update"
installs for nobody. Leak it and anyone can ship an update to your users.

*Mine:* a release artifact signed with the wrong key (another flavor's, the debug key) or not signed
at all, reaching a store upload while every Gradle task is green.

## Where credentials come from

| Source                                   | Use                     | Note                                                         |
|------------------------------------------|-------------------------|--------------------------------------------------------------|
| `~/.gradle/gradle.properties`            | developer machine       | outside the repo; seen by `findProperty()`                   |
| `ORG_GRADLE_PROJECT_<name>` env          | CI (preferred)          | Gradle maps it to project property `<name>`; nothing on the command line |
| `-P<name>=…`                             | CI (acceptable)         | visible in the process list; GitHub masks secrets in logs     |
| repo `gradle.properties`, `build.gradle.kts` | **never**           | committed                                                    |
| `local.properties`                       | not for `findProperty()` | Gradle does **not** expose it as project properties — `findProperty()` silently returns `null` (pws-android found this the hard way with the AppMetrica key). Load it with `Properties()` if you must |

Use property names without dots (`releasePlayStorePassword`) so the `ORG_GRADLE_PROJECT_` form is a
valid env name. `.gitignore`: `*.jks`, `*.keystore`, `keystore.properties`, `local.properties`.

## Absent credentials

Local: create the signing config only when all values are present; otherwise log one lifecycle line
and leave the flavor unsigned. AGP then emits `app-<flavor>-release-unsigned.apk` — enough to
measure R8/DEX, not to install (sign it with the debug key via `apksigner sign --ks
~/.android/debug.keystore --ks-pass pass:android` for a smoke test).

CI: `-PrequireReleaseSigning=true` turns any missing value into a configuration failure. Without it,
a missing secret produces an unsigned artifact that a later step may upload or attach to a release.

## Per-flavor assignment and precedence

The merged variant takes **build type's `signingConfig` first, then the flavor's**. So:

- `release` build type: **no** `signingConfig`. Each flavor sets its own.
- `debug` build type: keeps the default debug config — debug builds stay installable side by side.
- One global `buildTypes.release.signingConfig = …` signs every store with one key.

pws-android assigns `productFlavors.getByName("uk").signingConfig = …` *inside* the
`buildTypes.release {}` block. It works (it sets the flavor property), but reads like a build-type
setting; put it in the flavor block.

Need a non-release variant signed with a release key (e.g. a debuggable build that installs **over**
the published app to test an upgrade, with `run-as` access)? Override one variant only, and only when
the key is configured:

```kotlin
androidComponents {
  onVariants(selector().withName("storeBDebug")) { variant ->
    if (storeBSigningConfigured) variant.signingConfig.setConfig(android.signingConfigs.getByName("releaseStoreB"))
  }
}
```

## How many keystores

- Same owner, same store account, independent apps → **one keystore, one alias per app** is fine
  (pws-android: `ru`/`full` and `uk` share `release.jks` with aliases `pws` / `pws-uk`).
- A store whose installed base is pinned to a **different** certificate (an app taken over from a
  fork, a store without key-upload service) → **its own keystore**, its own secrets
  (pws-android: `release-rustore`, `RELEASE_KEYSTORE_RUSTORE_BASE64`).
- With Play App Signing, the key in CI is the *upload* key; Google holds the app-signing key. Other
  stores may sign with what you upload — know which case each flavor is in.

## CI

```yaml
- name: Decode keystore — storeB
  run: echo "${{ secrets.RELEASE_KEYSTORE_STOREB_BASE64 }}" | base64 --decode > "$RUNNER_TEMP/storeb.jks"

- name: Build storeB release
  env:
    ORG_GRADLE_PROJECT_releaseStoreBKeystorePath: ${{ runner.temp }}/storeb.jks
    ORG_GRADLE_PROJECT_releaseStoreBStorePassword: ${{ secrets.RELEASE_STORE_PASSWORD_STOREB }}
    ORG_GRADLE_PROJECT_releaseStoreBKeyPassword: ${{ secrets.RELEASE_KEY_PASSWORD_STOREB }}
    ORG_GRADLE_PROJECT_releaseStoreBKeyAlias: ${{ secrets.RELEASE_KEY_ALIAS_STOREB }}
  run: ./gradlew :app:assembleStoreBRelease :app:bundleStoreBRelease -PrequireReleaseSigning=true
```

Workflow structure, permissions, pinning and release publishing →
[`gradle-release-system`](../../gradle-release-system/SKILL.md) (`references/ci-hardening.md`).

## Verify the artifact's certificate

The Gradle config says which key *should* sign; the artifact says which key *did*. For every flavor
with a pinned certificate, compare the SHA-256 of **both** APK and AAB before upload:

```bash
norm() { tr -d ': ' | tr '[:lower:]' '[:upper:]'; }
EXPECTED=$(echo "$EXPECTED_SHA256" | norm)
APK_SHA=$(apksigner verify --print-certs "$APK" | grep -m1 'certificate SHA-256 digest' | sed 's/.*digest: //' | norm)
AAB_SHA=$(keytool -printcert -jarfile "$AAB" | grep -m1 'SHA256:' | sed 's/.*SHA256: //' | norm)
[[ "$APK_SHA" == "$EXPECTED" && "$AAB_SHA" == "$EXPECTED" ]] || { echo "::error::wrong signing key"; exit 1; }
```

The expected fingerprint is public (it is in every installed APK) and may live in the workflow.

## Invariants beside the key

The same "installs as an update" logic pins `applicationId`, the `versionCode` floor, the `minSdk`
ceiling (raising it strands users on old devices) and content-provider authorities. pws-android
checks them in `VerifyRustoreInvariantsTask`, wired to `preRustoreReleaseBuild` — it fails before
anything compiles. Template: [`../templates/app-flavors-signing.gradle.kts.snippet`](../templates/app-flavors-signing.gradle.kts.snippet).
