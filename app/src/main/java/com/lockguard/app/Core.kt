package com.lockguard.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import java.security.MessageDigest

object Store {
    private fun prefs(c: Context) =
        c.getSharedPreferences("lockguard", Context.MODE_PRIVATE)

    private fun hash(s: String) = MessageDigest.getInstance("SHA-256")
        .digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    fun setPassword(c: Context, p: String) =
        prefs(c).edit().putString("pw", hash(p)).apply()

    fun hasPassword(c: Context) = prefs(c).getString("pw", null) != null

    fun check(c: Context, p: String) = prefs(c).getString("pw", null) == hash(p)

    // Changing a password needs the current one. The first password needs nothing.
    fun changePassword(c: Context, old: String, new: String): Boolean {
        if (hasPassword(c) && !check(c, old)) return false
        setPassword(c, new)
        return true
    }

    fun lockedApps(c: Context): Set<String> =
        HashSet(prefs(c).getStringSet("locked", emptySet()) ?: emptySet())

    fun setLockedApps(c: Context, apps: Set<String>) =
        prefs(c).edit().putStringSet("locked", HashSet(apps)).apply()

    fun fingerprintOn(c: Context) = prefs(c).getBoolean("fp", false)

    fun setFingerprint(c: Context, on: Boolean) =
        prefs(c).edit().putBoolean("fp", on).apply()

    fun setRingtone(c: Context, uri: String?) =
        prefs(c).edit().putString("rt", uri).apply()

    fun ringtone(c: Context): Uri =
        prefs(c).getString("rt", null)?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
}

// Which app is currently unlocked. Lives in memory only, so a reboot or a
// killed process locks everything again.
object Locker {
    var unlockedPkg: String? = null
    var showingFor: String? = null
    var lastShow = 0L
}

object Alarm {
    private var player: MediaPlayer? = null
    var graceUntil = 0L
    var promptShowing = false

    fun isRinging() = player != null

    fun start(c: Context) {
        if (player != null) return
        val ctx = c.applicationContext

        try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(
                AudioManager.STREAM_ALARM,
                am.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                0
            )
        } catch (e: Exception) { }

        player = play(ctx, Store.ringtone(ctx))
            ?: play(ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            ?: play(ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
    }

    private fun play(ctx: Context, uri: Uri?): MediaPlayer? {
        if (uri == null) return null
        return try {
            val p = MediaPlayer()
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            p.setDataSource(ctx, uri)
            p.isLooping = true
            p.prepare()
            p.start()
            p
        } catch (e: Exception) {
            null
        }
    }

    fun stop() {
        try { player?.stop() } catch (e: Exception) { }
        player?.release()
        player = null
    }
}
