
package com.lockguard.app

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent

class GuardService : AccessibilityService() {
    private var lastTrigger = 0L

    private val airplaneReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            trigger(false)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        registerReceiver(
            airplaneReceiver,
            IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        )
    }

    private fun isLocked(): Boolean {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return km.isKeyguardLocked
    }

    private fun trigger(closeFirst: Boolean) {
        if (!Store.hasPassword(this)) return
        if (!isLocked()) return
        if (Alarm.promptShowing) return
        val now = System.currentTimeMillis()
        if (now < Alarm.graceUntil) return
        if (now - lastTrigger < 2000) return
        lastTrigger = now
        if (closeFirst) performGlobalAction(GLOBAL_ACTION_BACK)
        val i = Intent(this, PasswordActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg != "android" && !pkg.contains("systemui")) return

        val cls = event.className?.toString()?.lowercase() ?: ""
        val txt = event.text.joinToString(" ").lowercase()

        val powerMenu = cls.contains("globalactions") ||
            cls.contains("powermenu") ||
            cls.contains("shutdown") ||
            txt.contains("power off") ||
            txt.contains("power menu") ||
            txt.contains("shut down")

        val quickSettings = cls.contains("qspanel") ||
            cls.contains("quicksettings") ||
            cls.contains("qscontainer") ||
            txt.contains("airplane")

        if (powerMenu || quickSettings) trigger(true)
    }

    override fun onInterrupt() { }

    override fun onDestroy() {
        try { unregisterReceiver(airplaneReceiver) } catch (e: Exception) { }
        super.onDestroy()
    }
}
