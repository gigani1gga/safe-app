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
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var scroll: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(48, 96, 48, 48)

        val title = TextView(this)
        title.text = "Security App setup"
        title.textSize = 24f
        root.addView(title)

        val oldPw = EditText(this)
        oldPw.hint = "Current password (empty the first time)"
        oldPw.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        root.addView(oldPw)

        val newPw = EditText(this)
        newPw.hint = "New password (4+ characters)"
        newPw.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        root.addView(newPw)

        fun addButton(label: String, action: () -> Unit): Button {
            val b = Button(this)
            b.text = label
            b.setOnClickListener { action() }
            root.addView(b)
            return b
        }

        addButton("1. Set or change password") {
            val n = newPw.text.toString()
            if (n.length < 4) {
                Toast.makeText(this, "New password is too short", Toast.LENGTH_SHORT).show()
            } else if (Store.changePassword(this, oldPw.text.toString(), n)) {
                oldPw.setText("")
                newPw.setText("")
                Locker.unlockedPkg = packageName
                Toast.makeText(this, "Password saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Current password is wrong", Toast.LENGTH_SHORT).show()
            }
        }

        addButton("2. Choose apps to lock") {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }

        val fp = addButton("") { }
        fun refreshFp() {
            fp.text = "3. Fingerprint unlock: " + if (Store.fingerprintOn(this)) "ON" else "OFF"
        }
        fp.setOnClickListener {
            Store.setFingerprint(this, !Store.fingerprintOn(this))
            refreshFp()
        }
        refreshFp()

        addButton("4. Choose alarm sound") {
            val i = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
            i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Store.ringtone(this))
            startActivityForResult(i, 1)
        }

        addButton("5. Allow display over other apps") {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        addButton("6. Enable device admin") {
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

        addButton("7. Turn on accessibility service") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        addButton("Test the alarm password screen") {
            startActivity(Intent(this, PasswordActivity::class.java))
        }

        scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    // This screen asks for the password too, so nobody can edit the lock list.
    override fun onResume() {
        super.onResume()
        if (Store.hasPassword(this) && Locker.unlockedPkg != packageName) {
            scroll.visibility = View.INVISIBLE
            Locker.showingFor = packageName
            Locker.lastShow = System.currentTimeMillis()
            val i = Intent(this, AppLockActivity::class.java)
            i.putExtra("target", packageName)
            startActivity(i)
        } else {
            scroll.visibility = View.VISIBLE
        }
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
