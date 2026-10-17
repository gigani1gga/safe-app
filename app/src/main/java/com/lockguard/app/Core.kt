package com.lockguard.app

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import java.security.MessageDigest

class AdminReceiver : DeviceAdminReceiver()

object Store {
    private fun prefs(c: Context) =
        c.getSharedPreferences("lockguard", Context.MODE_PRIVATE)

    private fun hash(s: String) = MessageDigest.getInstance("SHA-256")
        .digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    fun setPassword(c: Context, p: String) =
        prefs(c).edit().putString("pw", hash(p)).apply()

    fun hasPassword(c: Context) = prefs(c).getString("pw", null) != null

    fun check(c: Context, p: String) = prefs(c).getString("pw", null) == hash(p)

    fun setRingtone(c: Context, uri: String?) =
        prefs(c).edit().putString("rt", uri).apply()

    fun ringtone(c: Context): Uri =
        prefs(c).getString("rt", null)?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
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
                am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0
            )
        } catch (e: Exception) { }
        try {
            val p = MediaPlayer()
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            p.setDataSource(ctx, Store.ringtone(ctx))
            p.isLooping = true
            p.prepare()
            p.start()
            player = p
        } catch (e: Exception) {
            player = null
        }
    }

    fun stop() {
        try { player?.stop() } catch (e: Exception) { }
        player?.release()
        player = null
    }
}
