// Pull the exported backup file from the device to the host.
// Device path: /sdcard/Android/data/<appId>/files/pws_backup.yaml
// Host path:   /tmp/pws_backup.yaml
// APP_ID is injected as a Maestro global variable from the flow's env block.
var appId = (typeof APP_ID !== 'undefined') ? APP_ID : "com.alelk.pws.pwapp"
var devicePath = "/sdcard/Android/data/" + appId + "/files/pws_backup.yaml"
var hostPath = "/tmp/pws_backup.yaml"

var p = java.lang.Runtime.getRuntime().exec(["adb", "pull", devicePath, hostPath])
var exitCode = p.waitFor()
if (exitCode !== 0) {
  throw new Error("adb pull failed with exit code " + exitCode + " (device path: " + devicePath + ")")
}
output.backupPulled = "true"
