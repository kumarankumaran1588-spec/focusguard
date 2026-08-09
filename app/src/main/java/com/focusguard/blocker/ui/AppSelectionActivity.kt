package com.focusguard.blocker.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.focusguard.blocker.data.PrefsManager
import com.focusguard.blocker.databinding.ActivityAppSelectionBinding

data class AppItem(val label: String, val pkg: String, var checked: Boolean)

class AppSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppSelectionBinding
    private lateinit var prefs: PrefsManager
    private lateinit var adapter: AppListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PrefsManager(this)

        val items = loadLaunchableApps()
        adapter = AppListAdapter(items)
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.btnSave.setOnClickListener {
            val selected = adapter.items.filter { it.checked }.map { it.pkg }.toSet()
            prefs.setBlockedApps(selected)
            finish()
        }
    }

    /** Only list apps the user can actually launch, and hide ourselves. */
    private fun loadLaunchableApps(): MutableList<AppItem> {
        val pm = packageManager
        val alreadyBlocked = prefs.getBlockedApps()
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(launcher, 0)

        val seen = HashSet<String>()
        val list = ArrayList<AppItem>()
        for (ri in resolved) {
            val pkg = ri.activityInfo.packageName
            if (pkg == packageName) continue          // don't let the user block us
            if (!seen.add(pkg)) continue              // de-dupe
            val label = ri.loadLabel(pm).toString()
            list.add(AppItem(label, pkg, alreadyBlocked.contains(pkg)))
        }
        list.sortBy { it.label.lowercase() }
        return list
    }
}
