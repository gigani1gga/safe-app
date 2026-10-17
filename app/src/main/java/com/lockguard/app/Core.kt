package com.lockguard.app

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
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

    private val handler = Handler(Looper.getMainLooper())
    private var enforceRunnable: Runnable? = null
    private var audioFocusGranted = false
    private var audioFocusRequest: AudioFocusRequest? = null

    fun isRinging() = player != null

    fun start(c: Context) {
        if (player != null) return
        val ctx = c.applicationContext

        try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val afr = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAcceptsDelayedFocusGain(false)
                        .build()
                    val res = am.requestAudioFocus(afr)
                    audioFocusGranted = res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                    audioFocusRequest = afr
                } else {
                    val res = am.requestAudioFocus(
                        null,
                        AudioManager.STREAM_ALARM,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                    )
                    audioFocusGranted = res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                }
            } catch (e: Exception) {
                audioFocusGranted = false
            }

            try {
                am.setStreamVolume(
                    AudioManager.STREAM_ALARM,
                    am.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                    0
                )
            } catch (e: Exception) { }

            enforceRunnable = object : Runnable {
                override fun run() {
                    try {
                        val am2 = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        am2.setStreamVolume(
                            AudioManager.STREAM_ALARM,
                            am2.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                            0
                        )
                    } catch (e: Exception) { }
                    handler.postDelayed(this, 400)
                }
            }
            handler.postDelayed(enforceRunnable!!, 400)
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

        try {
            enforceRunnable?.let { handler.removeCallbacks(it) }
            enforceRunnable = null
        } catch (e: Exception) { }
    }
}
