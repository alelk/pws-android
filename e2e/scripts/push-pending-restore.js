// Push the backup file from host to device as a pending restore.
// 1. Push to the app's own external files dir (readable by app-UID via run-as; avoids scoped-storage issues on API 30+).
// 2. Copy into the app's private files dir via `adb shell run-as` (requires debuggable build).
// 3. Clean up the staging file.
//
// APP_ID is injected as a Maestro global variable from the flow's env block.
var appId = (typeof APP_ID !== 'undefined') ? APP_ID : "com.alelk.pws.pwapp"
var hostPath = "/tmp/pws_backup.yaml"
var sdcardTmp = "/sdcard/Android/data/" + appId + "/files/pws_restore_tmp.yaml"
var appFile = "files/pending_user_restore.yaml"

function exec(cmd) {
  var p = java.lang.Runtime.getRuntime().exec(cmd)
  var code = p.waitFor()
  return code
}

var pushCode = exec(["adb", "push", hostPath, sdcardTmp])
if (pushCode !== 0) {
  throw new Error("adb push failed with exit code " + pushCode)
}

var cpCode = exec(["adb", "shell", "run-as", appId, "cp", sdcardTmp, appFile])
if (cpCode !== 0) {
  throw new Error("adb shell run-as cp failed with exit code " + cpCode + " (requires debuggable build)")
}

exec(["adb", "shell", "rm", sdcardTmp])
output.pendingRestoreInjected = "true"
