// Insert (or replace) the button code where you build the UI in MainActivity.onCreate()

addButton("Enable device-owner enforcement") {
    val dpm = getSystemService(android.app.admin.DevicePolicyManager::class.java)
    val admin = android.content.ComponentName(this, AdminReceiver::class.java)

    if (dpm != null && dpm.isDeviceOwnerApp(packageName)) {
        // App is device owner — apply restrictions immediately
        DeviceAdminUtil.applyEnforcement(this)
    } else {
        // Not device owner — show provisioning instructions
        val msg = StringBuilder()
        msg.append("App is not device owner. To enable full enforcement you must provision the app as device owner.\n\n")
        msg.append("Provisioning (ADB, requires factory-reset / fresh device):\n")
        msg.append("1) Factory-reset the device (required on many devices).\n")
        msg.append("2) On the device (or emulator), run:\n")
        msg.append("   adb shell dpm set-device-owner \"com.lockguard.app/.AdminReceiver\"\n\n")
        msg.append("After successful provisioning, open the app and tap this button again to apply enforcement.\n")
        msg.append("To remove: adb shell dpm remove-active-admin com.lockguard.app/.AdminReceiver (or factory reset)\n")
        android.widget.Toast.makeText(this, msg.toString(), android.widget.Toast.LENGTH_LONG).show()
    }
}
