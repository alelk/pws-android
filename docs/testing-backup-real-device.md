# Testing backup/restore on a real device (Google account)

## How it works

`PwsBackupAgent` uses Android **Key-Value Backup** (KVB):

1. `onBackup()` serialises user data (favorites, tags, history, edited songs) to YAML and writes it
   to the KVB stream.
2. Android transports that stream to Google Drive.
3. On restore (first launch after reinstall / `bmgr restore`), `onRestore()` saves the YAML as
   `files/pending_user_restore.yaml`.
4. On next app launch `applyPendingRestoreIfNeeded()` reads the file, applies it, and deletes it.

## Prerequisites

- **Real Android device** (or AVD with Google Play Services — "Google APIs" or "Google Play" image,
  not bare AOSP)
- Google account signed in on the device
- **Settings → System → Backup** → "Back up to Google Drive" enabled
- USB debugging enabled, `adb` connected
- Debuggable APK installed (`./gradlew :app-compose:assembleFullDebug` works fine)

## Step-by-step

### 1. Create test data

Launch the app, then:

- Open a song, toggle it as a favorite
- View a few songs (history)
- Create a tag and assign it to a song

### 2. Trigger a backup

```bash
# Tell the backup manager that our app has new data to back up
adb shell bmgr backup com.alelk.pws.pwapp

# Force the backup job to run immediately (don't wait for scheduler)
adb shell bmgr run
```

Wait 30–60 seconds. Verify it was uploaded:

```bash
adb shell dumpsys backup | grep -A5 "com.alelk.pws.pwapp"
```

### 3. Find the backup set token

```bash
adb shell bmgr list sets
```

Output example:

```
  Restore set token: 0x0000a1b2c3d4e5f6    : OnePlus 9 (John's phone)
```

Copy the hex token.

### 4. Simulate a fresh install

```bash
# Option A: wipe only app data (quicker)
adb shell pm clear com.alelk.pws.pwapp

# Option B: full reinstall (more realistic)
adb uninstall com.alelk.pws.pwapp
adb install output/compose/pws-app-release-<version>-ru.apk
```

### 5. Trigger restore

```bash
adb shell bmgr restore <TOKEN> com.alelk.pws.pwapp
```

Android calls `PwsBackupAgent.onRestore()`, which writes `pending_user_restore.yaml` to the app's
`filesDir`.

### 6. Launch the app

Open the app normally. On startup `applyPendingRestoreIfNeeded()` runs and imports the data. You
should see favorites, tags, and history restored.

## Monitoring

```bash
# Watch backup agent and restore logs
adb logcat -s PwsBackupAgent:V BackupManagerService:V
```

Key log tags: `PwsBackupAgent`, `BackupManagerService`, `FullBackup`.

## Checking the pending file directly

Before launching the app (step 6), you can confirm the file was written:

```bash
adb shell run-as com.alelk.pws.pwapp ls -la files/
adb shell run-as com.alelk.pws.pwapp cat files/pending_user_restore.yaml
```

## Checking backup is enabled / transport

```bash
adb shell bmgr enabled           # should print "Backup Manager currently enabled"
adb shell bmgr list transports   # active transport marked with *
```

The active transport on a device with Google Play Services is typically
`com.google.android.gms/.backup.BackupTransportService`.

## Troubleshooting

| Symptom                                                  | Likely cause                                                                             |
|----------------------------------------------------------|------------------------------------------------------------------------------------------|
| `bmgr run` produces no output and data is not restored   | Device is not signed in or backup is disabled in Settings                                |
| `bmgr restore` exits immediately, no `onRestore` log     | Backup set token is wrong or backup hasn't uploaded yet (wait longer, re-run `bmgr run`) |
| `pending_user_restore.yaml` exists but app shows no data | Check logcat for a parse error — YAML may be malformed                                   |
| `run-as` fails                                           | APK is not debuggable (use debug or `debuggable true` release build)                     |

## What is NOT backed up (by design)

The `data_extraction_rules.xml` / `backup_rules.xml` exclude:

- `pws.db*` — encrypted database (Keystore key is device-specific, unusable on another device)
- `pws_db_security*.xml` — DataStore file holding the Keystore-protected DB password

Only the YAML backup produced by `PwsBackupAgent` is sent to Google.
