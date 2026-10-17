package com.lockguard.app

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        DeviceAdminUtil.applyEnforcement(context)
        Toast.makeText(context, "Admin enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        DeviceAdminUtil.clearEnforcement(context)
        Toast.makeText(context, "Admin disabled", Toast.LENGTH_SHORT).show()
    }
}
