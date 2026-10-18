package com.lockguard.app

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

class GuardService : AccessibilityService() {
    private var lastTrigger = 0L
    private val handler = Handler(Looper.getMainLooper())

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> trigger(false)
                Intent.ACTION_SCREEN_OFF -> Locker.unlockedPkg = null
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT ->
                    handler.postDelayed({ checkForeground() }, 500)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val f = IntentFilter()
        f.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        f.addAction(Intent.ACTION_SCREEN_OFF)
        f.addAction(Intent.ACTION_SCREEN_ON)
        f.addAction(Intent.ACTION_USER_PRESENT)
        registerReceiver(receiver, f)
    }

    private fun isLocked(): Boolean {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return km.isKeyguardLocked
    }

    // ---- power menu / quick settings / airplane mode guard ----

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

    // ---- app lock ----

    private fun imePackage(): String? {
        val id = Settings.Secure.getString(
            contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: return null
        return id.substringBefore('/')
    }

    private fun requireUnlock(pkg: String) {
        if (Locker.unlockedPkg == pkg) return
        val now = System.currentTimeMillis()
        if (Locker.showingFor == pkg && now - Locker.lastShow < 1500) return
        Locker.showingFor = pkg
        Locker.lastShow = now
        val i = Intent(this, AppLockActivity::class.java)
        i.putExtra("target", pkg)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    }

    private fun evaluate(pkg: String, cls: String?) {
        if (!Store.hasPassword(this)) return
        val c = (cls ?: "").lowercase()

        // Recents screen: leaving an app that way counts as leaving it.
        if (c.contains("recents")) {
            Locker.unlockedPkg = null
            return
        }

        // This app's own screens are protected too, except the two lock screens.
        if (pkg == packageName) {
            if (c.endsWith("applockactivity") || c.endsWith("passwordactivity")) return
            requireUnlock(pkg)
            return
        }

        // Keyboards and system pop-ups do not count as leaving the app.
        if (pkg == "android" || pkg.contains("systemui") || pkg == imePackage()) return

        if (Store.lockedApps(this).contains(pkg)) {
            requireUnlock(pkg)
        } else {
            Locker.unlockedPkg = null
        }
    }

    private fun checkForeground() {
        val pkg = rootInActiveWindow?.packageName?.toString() ?: return
        if (pkg == packageName) return
        evaluate(pkg, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            evaluate(pkg, event.className?.toString())
        }

        // power menu / quick settings guard
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
        try { unregisterReceiver(receiver) } catch (e: Exception) { }
        super.onDestroy()
    }
}
