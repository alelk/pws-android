# Data Security

Two independent protection layers defend hymn/psalm content against the two main threats:
extracting assets from a decompiled APK, and copying the database file from a rooted device.

---

## Threat model

| Threat              | What the attacker does                                    | Mitigation                                                 |
|---------------------|-----------------------------------------------------------|------------------------------------------------------------|
| APK decompilation   | Unpacks the APK, reads `.dbz.enc` files from `assets/db/` | AES-256-CBC encryption, key not readable as a plain string |
| Root device DB copy | Copies `pws.X.Y.Z.db` from `/data/data/…/databases/`      | SQLCipher encryption, key stored in Android Keystore       |

---

## Layer 1 — Asset encryption (APK)

Song books are bundled as `assets/db/<name>-<version>.dbz.enc`.

**Wire format (PWSB):**
```
MAGIC(4) | VERSION(1) | HMAC-SHA256(32) | IV(16) | AES-256-CBC ciphertext
```

**Decryption key (`DB_DECRYPT_KEY`):**
- Debug: a fixed public key committed to source control — anyone can build and run the app.
- Prod: injected at build time via the `DB_DECRYPT_KEY_PROD` GitHub Actions secret (never committed).

**XOR obfuscation (T-10):**
`build.gradle.kts` converts the hex key into two randomly-generated `byte[]` arrays
(`BuildConfig.DB_KEY_MASKED` + `BuildConfig.DB_KEY_MASK`) on every Gradle run.
`DbKeyConfig.keyHex()` XORs them at runtime to reconstruct the key.
The 64-char hex string never appears as a DEX string literal, so `strings`/`dexdump` tools
cannot extract it directly. The random mask changes with every build, producing a different
byte pattern in each APK.

**R8 / ProGuard:**
`app-compose` has `isMinifyEnabled = true`. R8 inlines byte-array constants at call sites and
removes the `BuildConfig` class — adding a second layer that makes static analysis harder.

**Key code paths:**
- `BundleCrypto` — encrypt/decrypt (`portable-data` module)
- `DbKeyConfig` — runtime key reconstruction (`data/db-android`)
- `initDatabase()` — first-launch asset extraction and `sqlcipher_export` into encrypted DB

---

## Layer 2 — Database encryption (on-device)

The running database `pws.X.Y.Z.db` is encrypted with **SQLCipher** (`net.zetetic:sqlcipher-android:4.16.0`).

**Passphrase generation:**
`KeyManager.getOrCreatePassphrase(context)` generates 32 cryptographically-random bytes on
the first launch, encodes them as a 64-char hex string, and stores it in
`EncryptedSharedPreferences` (backed by Android Keystore AES-256-GCM).
On every subsequent launch the same passphrase is returned.

**Room integration:**
```kotlin
Room.databaseBuilder(context, PwsDatabase::class.java, DATABASE_NAME)
    .openHelperFactory(SupportOpenHelperFactory(passphrase))
    .build()
```

**Security properties:**
- The passphrase is unique per device — it never leaves the Keystore.
- Copying the `.db` file to another device is useless without that device's Keystore entry.
- The asset decryption key and the DB passphrase are completely independent:
  breaking one layer does not compromise the other.
- The passphrase survives app updates; it is lost only on factory reset or `Clear data`
  (at which point the database itself is already gone).

**Key code paths:**
- `KeyManager` — passphrase lifecycle (`data/db-android/…/security/`)
- `PwsDatabaseProvider` — Room builder wiring
- `initDatabase()` — first launch: decrypts asset → writes temp file → `sqlcipher_export` → encrypted DB

---

## Database version history

| DB file                         | Encryption          | Notes                             |
|---------------------------------|---------------------|-----------------------------------|
| `pws.1.x.x.db` … `pws.2.0.0.db` | none                | legacy                            |
| `pws.3.2.2.db`                  | none (plain SQLite) | last unencrypted version          |
| `pws.3.3.0.db`                  | SQLCipher AES-256   | current; passphrase from Keystore |

---

## Migration path

**3.2.2 → 3.3.0 (existing users):**

1. `initDatabase()` creates `pws.3.3.0.db` (encrypted) from assets.
2. `migrateDataFromPrevDatabase()` opens `pws.3.2.2.db` as plain SQLite via
   `openReadOnlyDatabase()`, copies favourites / history / tags / edited songs, then deletes
   the old file.

**Future migrations (3.3.x → 3.4.x):**

`openReadOnlyDatabase()` detects `SQLITE_NOTADB` errors and logs a warning.
When implementing the next version bump, extend it to open the old file via
SQLCipher + `KeyManager.getOrCreatePassphrase()`.

---

## CI / build secrets

| Secret                    | Where set                             | Purpose                   |
|---------------------------|---------------------------------------|---------------------------|
| `DB_DECRYPT_KEY_PROD`     | GitHub → Settings → Secrets → Actions | Prod asset decryption key |
| `RELEASE_KEYSTORE_BASE64` | GitHub → Settings → Secrets → Actions | APK signing keystore      |

The prod key is passed to Gradle as the `DB_DECRYPT_KEY_PROD` environment variable in
`.github/workflows/release-build.yml`. Debug builds always use the public debug key.
