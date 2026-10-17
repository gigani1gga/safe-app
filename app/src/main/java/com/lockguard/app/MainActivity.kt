package com.lockguard.app

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(48, 96, 48, 48)

        val title = TextView(this)
        title.text = "Security App setup"
        title.textSize = 24f
        root.addView(title)

        val pw = EditText(this)
        pw.hint = "Choose a password (4+ characters)"
        pw.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        root.addView(pw)

        fun addButton(label: String, action: () -> Unit) {
            val b = Button(this)
            b.text = label
            b.setOnClickListener { action() }
            root.addView(b)
        }

        addButton("1. Save password") {
            if (pw.text.length >= 4) {
                Store.setPassword(this, pw.text.toString())
                Toast.makeText(this, "Password saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Too short", Toast.LENGTH_SHORT).show()
            }
        }

        addButton("2. Choose alarm sound") {
            val i = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
            i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Store.ringtone(this))
            startActivityForResult(i, 1)
        }

        addButton("3. Allow display over other apps") {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        addButton("4. Enable device admin") {
            val i = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            i.putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                ComponentName(this, AdminReceiver::class.java)
            )
            i.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Stops the app being uninstalled without your say."
            )
            startActivity(i)
        }

        addButton("5. Turn on accessibility service") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        addButton("Test the password screen") {
            startActivity(Intent(this, PasswordActivity::class.java))
        }

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val uri = data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            Store.setRingtone(this, uri?.toString())
            Toast.makeText(this, "Alarm sound saved", Toast.LENGTH_SHORT).show()
        }
    }
}
