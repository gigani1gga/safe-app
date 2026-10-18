package com.lockguard.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

class AppPickerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pm = packageManager
        val launch = Intent(Intent.ACTION_MAIN)
        launch.addCategory(Intent.CATEGORY_LAUNCHER)

        val apps = pm.queryIntentActivities(launch, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .filter { it.first != packageName }
            .sortedBy { it.second.lowercase() }

        val saved = Store.lockedApps(this)

        val list = ListView(this)
        list.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        list.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_multiple_choice,
            apps.map { it.second }
        )
        for (i in apps.indices) {
            list.setItemChecked(i, saved.contains(apps[i].first))
        }
        list.setOnItemClickListener { _, _, _, _ ->
            val chosen = HashSet<String>()
            for (i in apps.indices) {
                if (list.isItemChecked(i)) chosen.add(apps[i].first)
            }
            Store.setLockedApps(this, chosen)
        }

        val heading = TextView(this)
        heading.text = "Tick the apps to lock"
        heading.textSize = 22f
        heading.setPadding(48, 96, 48, 24)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.addView(heading)
        root.addView(list)
        setContentView(root)
    }
}
