// Pull the exported backup file from the device to the host.
// The file is saved by the Android file picker (CreateDocument) to the Downloads folder
// with a timestamped name: /sdcard/Download/pws_backup_YYYY-MM-DD_HH-mm-ss.pws
// Host path: /tmp/pws_backup.pws

var hostPath = "/tmp/pws_backup.pws"

var Runtime = Java.type('java.lang.Runtime')
var BufferedReader = Java.type('java.io.BufferedReader')
var InputStreamReader = Java.type('java.io.InputStreamReader')

function execOut(cmd) {
  var p = Runtime.getRuntime().exec(["sh", "-c", cmd])
  var reader = new BufferedReader(new InputStreamReader(p.getInputStream()))
  var line = reader.readLine()
  p.waitFor()
  return line ? line.trim() : ""
}

var devicePath = execOut("adb shell 'ls -t /sdcard/Download/pws_backup_*.pws 2>/dev/null | head -1'")
if (!devicePath) {
  throw new Error("No backup file found in /sdcard/Download/ — was the file picker confirmed?")
}

var p = Runtime.getRuntime().exec(["adb", "pull", devicePath, hostPath])
var exitCode = p.waitFor()
if (exitCode !== 0) {
  throw new Error("adb pull failed with exit code " + exitCode + " (device path: " + devicePath + ")")
}
output.backupPulled = "true"
