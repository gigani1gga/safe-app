package com.lockguard.app

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 24f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }

        root.addView(title, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 24 })

        val enableBtn = Button(this).apply {
            text = "Enable device-owner enforcement"
            setOnClickListener { onEnableClicked() }
        }

        root.addView(enableBtn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        setContentView(root)
    }

    private fun onEnableClicked() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val admin = ComponentName(this, AdminReceiver::class.java)

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
            Toast.makeText(this, msg.toString(), Toast.LENGTH_LONG).show()
        }
    }
}
