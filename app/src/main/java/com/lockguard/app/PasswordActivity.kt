package com.lockguard.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class PasswordActivity : Activity() {
    private var timer: CountDownTimer? = null
    private var unlocked = false
    private lateinit var status: TextView
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        Alarm.promptShowing = true

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.BLACK)
        root.setPadding(60, 160, 60, 60)

        val title = TextView(this)
        title.text = "Enter your password"
        title.setTextColor(Color.WHITE)
        title.textSize = 26f
        root.addView(title)

        status = TextView(this)
        status.setTextColor(Color.RED)
        status.textSize = 18f
        root.addView(status)

        input = EditText(this)
        input.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.setTextColor(Color.WHITE)
        root.addView(input)

        val ok = Button(this)
        ok.text = "Unlock"
        ok.setOnClickListener { submit() }
        root.addView(ok)

        setContentView(root)

        if (Alarm.isRinging()) {
            status.text = "Alarm is active"
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        timer = object : CountDownTimer(15000, 1000) {
            override fun onTick(ms: Long) {
                status.text = "Alarm in ${ms / 1000} seconds"
            }
            override fun onFinish() {
                Alarm.start(this@PasswordActivity)
                status.text = "Alarm is active"
            }
        }.start()
    }

    private fun submit() {
        if (Store.check(this, input.text.toString())) {
            unlocked = true
            timer?.cancel()
            Alarm.stop()
            Alarm.graceUntil = System.currentTimeMillis() + 60000
            finish()
        } else {
            Alarm.start(this)
            status.text = "Wrong password. Alarm is active"
            input.setText("")
        }
    }

    override fun onStop() {
        super.onStop()
        if (!unlocked) Alarm.start(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        Alarm.promptShowing = false
    }

    override fun onBackPressed() { }
}
