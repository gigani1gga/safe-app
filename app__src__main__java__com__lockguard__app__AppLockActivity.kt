package com.lockguard.app

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.hardware.biometrics.BiometricPrompt
import android.os.Bundle
import android.os.CancellationSignal
import android.text.InputType
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class AppLockActivity : Activity() {
    private var target = ""
    private var done = false
    private var fpShowing = false
    private var cancelSignal: CancellationSignal? = null
    private lateinit var heading: TextView
    private lateinit var status: TextView
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.BLACK)
        root.setPadding(60, 200, 60, 60)

        heading = TextView(this)
        heading.setTextColor(Color.WHITE)
        heading.textSize = 26f
        root.addView(heading)

        status = TextView(this)
        status.setTextColor(Color.RED)
        status.textSize = 18f
        root.addView(status)

        input = EditText(this)
        input.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.setTextColor(Color.WHITE)
        input.hint = "Password"
        input.setHintTextColor(Color.GRAY)
        root.addView(input)

        val ok = Button(this)
        ok.text = "Unlock"
        ok.setOnClickListener { submit() }
        root.addView(ok)

        if (Store.fingerprintOn(this)) {
            val fp = Button(this)
            fp.text = "Use fingerprint"
            fp.setOnClickListener { startFingerprint() }
            root.addView(fp)
        }

        setContentView(root)
        readTarget(intent)

        if (Store.fingerprintOn(this)) {
            root.postDelayed({ if (!done) startFingerprint() }, 300)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            setIntent(intent)
            readTarget(intent)
        }
    }

    private fun readTarget(i: Intent?) {
        target = i?.getStringExtra("target") ?: packageName
        Locker.showingFor = target
        heading.text = "Unlock " + labelOf(target)
    }

    private fun labelOf(pkg: String): String {
        return try {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            pkg
        }
    }

    private fun startFingerprint() {
        if (fpShowing) return
        try {
            cancelSignal?.cancel()
            val sig = CancellationSignal()
            cancelSignal = sig
            fpShowing = true
            val prompt = BiometricPrompt.Builder(this)
                .setTitle("Unlock " + labelOf(target))
                .setNegativeButton(
                    "Use password",
                    mainExecutor,
                    DialogInterface.OnClickListener { _, _ -> fpShowing = false }
                )
                .build()
            prompt.authenticate(
                sig,
                mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult?
                    ) {
                        fpShowing = false
                        unlock()
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence?
                    ) {
                        fpShowing = false
                    }
                }
            )
        } catch (e: Exception) {
            fpShowing = false
        }
    }

    private fun submit() {
        if (Store.check(this, input.text.toString())) {
            unlock()
        } else {
            status.text = "Wrong password"
            input.setText("")
        }
    }

    private fun unlock() {
        if (done) return
        done = true
        cancelSignal?.cancel()
        Locker.unlockedPkg = target
        finish()
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN)
        home.addCategory(Intent.CATEGORY_HOME)
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(home)
        finish()
    }

    override fun onStop() {
        super.onStop()
        // Leaving the lock screen (home, recents, screen off) closes it, so
        // the next visit to the app asks again.
        if (!done && !fpShowing && !isChangingConfigurations) finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelSignal?.cancel()
        if (Locker.showingFor == target) Locker.showingFor = null
    }

    override fun onBackPressed() {
        goHome()
    }
}
