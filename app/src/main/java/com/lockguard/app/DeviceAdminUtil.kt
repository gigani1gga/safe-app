package com.lockguard.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import android.widget.Toast

object DeviceAdminUtil {
    private val RESTRICTIONS = arrayOf(
        UserManager.DISALLOW_CONFIG_WIFI,
        UserManager.DISALLOW_CONFIG_LOCATION,
        UserManager.DISALLOW_CONFIG_TETHERING,
        UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS,
        UserManager.DISALLOW_CONFIG_BLUETOOTH
    )

    fun applyEnforcement(context: Context) {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val admin = ComponentName(context, AdminReceiver::class.java)

        if (dpm == null) {
            Toast.makeText(context, "DevicePolicyManager not available", Toast.LENGTH_LONG).show()
            return
        }

        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            Toast.makeText(context, "App is not device owner — cannot enforce", Toast.LENGTH_LONG).show()
            return
        }

        for (r in RESTRICTIONS) {
            try { dpm.addUserRestriction(admin, r) } catch (ex: Exception) { /* best-effort */ }
        }

        // Optionally: disable keyguard features / force additional policies if desired.
        Toast.makeText(context, "Device-owner restrictions applied", Toast.LENGTH_SHORT).show()
    }

    fun clearEnforcement(context: Context) {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val admin = ComponentName(context, AdminReceiver::class.java)

        if (dpm == null) return

        for (r in RESTRICTIONS) {
            try { dpm.clearUserRestriction(admin, r) } catch (ex: Exception) { /* best-effort */ }
        }

        Toast.makeText(context, "Device-owner restrictions cleared", Toast.LENGTH_SHORT).show()
    }
}
