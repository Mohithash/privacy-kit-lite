package com.sal.privacykit.lite

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.sal.privacykit.lite.data.LiteProfileStore
import com.sal.privacykit.lite.model.IdentifierCatalog
import com.sal.privacykit.lite.model.IdentifierRuleType
import com.sal.privacykit.lite.model.IdentifierValueGenerator
import com.sal.privacykit.lite.model.LiteProfile
import com.sal.privacykit.lite.xposed.XposedConfigExporter

class MainActivity : Activity() {
    private lateinit var store: LiteProfileStore
    private lateinit var root: LinearLayout
    private var profiles: List<LiteProfile> = emptyList()
    private var selectedPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LiteProfileStore(this)
        LiteServices.xposedConnection.onStateChanged = {
            runOnUiThread {
                if (LiteServices.xposedConnection.service != null) {
                    exportProfiles(showToast = false)
                }
                render()
            }
        }
        profiles = store.load()
        selectedPackage = profiles.firstOrNull()?.packageName
        render()
    }

    override fun onDestroy() {
        LiteServices.xposedConnection.onStateChanged = null
        super.onDestroy()
    }

    private fun render() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(24))
            setBackgroundColor(Color.rgb(16, 20, 24))
        }
        setContentView(ScrollView(this).apply { addView(root) })

        title("Privacy Kit Lite")
        note("Local LSPosed spoofing. No network access, account system, telemetry, browser, AI, or premium code.")
        status()
        actions()

        if (profiles.isEmpty()) {
            emptyState()
        } else {
            profilePicker()
            selectedProfile()?.let(::profileEditor)
        }
    }

    private fun status() {
        val connected = LiteServices.xposedConnection.service != null
        note(if (connected) "LSPosed service: connected" else "LSPosed service: not connected")
    }

    private fun actions() {
        row {
            button("Add app") { showAppPicker() }
            button("Export") { exportProfiles(showToast = true) }
        }
    }

    private fun emptyState() {
        section("No apps selected")
        note("Add an installed app, approve its LSPosed scope, then restart that app process.")
    }

    private fun profilePicker() {
        section("Selected app")
        val labels = profiles.map { "${it.label} (${it.packageName})" }
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, labels)
            setSelection(profiles.indexOfFirst { it.packageName == selectedPackage }.coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val packageName = profiles[position].packageName
                    if (packageName != selectedPackage) {
                        selectedPackage = packageName
                        render()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        root.addView(spinner, matchWrap())
    }

    private fun profileEditor(profile: LiteProfile) {
        section(profile.label)
        note("${profile.packageName} - ${profile.enabledCount()} spoofed values")
        row {
            button("Request scope") {
                LiteServices.xposedConnection.requestScope(profile.packageName) { ok, message ->
                    runOnUiThread {
                        toast(if (ok) "Scope request approved or already scoped" else message ?: "Scope request failed")
                    }
                }
            }
            button("Regenerate") {
                updateProfile(store.regenerate(profile))
                exportProfiles(showToast = false)
            }
            button("Delete") {
                profiles = profiles.filterNot { it.packageName == profile.packageName }
                selectedPackage = profiles.firstOrNull()?.packageName
                persistAndRender()
            }
        }

        val enabledSwitch = Switch(this).apply {
            setText(R.string.profile_enabled)
            isChecked = profile.enabled
            setTextColor(Color.WHITE)
            setOnCheckedChangeListener { _, checked ->
                updateProfile(profile.copy(enabled = checked), rerender = false)
                exportProfiles(showToast = false)
            }
        }
        root.addView(enabledSwitch, matchWrap())

        IdentifierCatalog.items.forEach { item ->
            identifierRow(profile, item.key, item.label)
        }
    }

    private fun identifierRow(profile: LiteProfile, key: String, label: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(10))
        }
        row.addView(TextView(this).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 15f
        })

        val ruleSpinner = Spinner(this)
        val rules = IdentifierRuleType.values().toList()
        ruleSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, rules.map(::ruleLabel))
        ruleSpinner.setSelection(rules.indexOf(profile.rules[key] ?: IdentifierRuleType.REAL))
        row.addView(ruleSpinner, matchWrap())

        val valueEdit = EditText(this).apply {
            setText(profile.values[key].orEmpty())
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            hint = "Enter a custom value"
            isEnabled = profile.rules[key] == IdentifierRuleType.CUSTOM
        }
        row.addView(valueEdit, matchWrap())

        ruleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val rule = rules[position]
                valueEdit.isEnabled = rule == IdentifierRuleType.CUSTOM
                val current = currentProfile(profile.packageName) ?: return
                if (current.rules[key] == rule) return
                val values = if (rule == IdentifierRuleType.STATIC) {
                    val seed = IdentifierValueGenerator.seedFor(current.id + System.nanoTime(), key)
                    current.values + (key to IdentifierValueGenerator.generate(key, seed))
                } else {
                    current.values
                }
                val updated = current.copy(rules = current.rules + (key to rule), values = values)
                updateProfile(updated, rerender = false)
                if (rule == IdentifierRuleType.STATIC) {
                    valueEdit.setText(values[key].orEmpty())
                }
                exportProfiles(showToast = false)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        valueEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(text: Editable?) {
                val value = text?.toString().orEmpty()
                val current = currentProfile(profile.packageName) ?: return
                if (current.values[key] == value) return
                updateProfile(current.copy(values = current.values + (key to value)), rerender = false)
                exportProfiles(showToast = false)
            }
        })

        root.addView(row, matchWrap())
    }

    private fun showAppPicker() {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(mainIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .map {
                val info = it.activityInfo
                val label = it.loadLabel(packageManager).toString()
                LaunchableApp(info.packageName, label)
            }
            .distinctBy { it.packageName }
            .filterNot { it.packageName == packageName || profiles.any { profile -> profile.packageName == it.packageName } }
            .sortedBy { it.label.lowercase() }
        if (apps.isEmpty()) {
            toast("No launchable apps available")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Add app")
            .setItems(apps.map { "${it.label} (${it.packageName})" }.toTypedArray()) { _, which ->
                val app = apps[which]
                val profile = store.create(app.packageName, app.label)
                profiles = (profiles + profile).sortedBy { it.label.lowercase() }
                selectedPackage = app.packageName
                persistAndRender()
                LiteServices.xposedConnection.requestScope(app.packageName) { ok, message ->
                    runOnUiThread {
                        toast(if (ok) "Scope request approved or already scoped" else message ?: "Open LSPosed and add scope manually")
                    }
                }
            }
            .show()
    }

    private fun exportProfiles(showToast: Boolean): Boolean {
        val ok = XposedConfigExporter.export(LiteServices.xposedConnection, profiles)
        if (showToast) {
            val message = when {
                ok -> "Exported to LSPosed"
                LiteServices.xposedConnection.service == null -> "LSPosed service is not connected"
                else -> "LSPosed export failed"
            }
            toast(message)
        }
        return ok
    }

    private fun persistAndRender() {
        store.save(profiles)
        exportProfiles(showToast = false)
        render()
    }

    private fun updateProfile(profile: LiteProfile, rerender: Boolean = true) {
        profiles = profiles.map { if (it.packageName == profile.packageName) profile else it }
        store.save(profiles)
        if (rerender) render()
    }

    private fun selectedProfile(): LiteProfile? = profiles.firstOrNull { it.packageName == selectedPackage } ?: profiles.firstOrNull()

    private fun currentProfile(packageName: String): LiteProfile? = profiles.firstOrNull { it.packageName == packageName }

    private fun ruleLabel(rule: IdentifierRuleType): String = when (rule) {
        IdentifierRuleType.REAL -> "Use real value"
        IdentifierRuleType.STATIC -> "Generated value"
        IdentifierRuleType.CUSTOM -> "Custom value"
    }

    private fun section(text: String) {
        root.addView(TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 20f
            setPadding(0, dp(18), 0, dp(8))
        }, matchWrap())
    }

    private fun title(text: String) {
        root.addView(TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 26f
            setPadding(0, 0, 0, dp(8))
        }, matchWrap())
    }

    private fun note(text: String) {
        root.addView(TextView(this).apply {
            this.text = text
            setTextColor(Color.LTGRAY)
            textSize = 14f
            setPadding(0, dp(4), 0, dp(4))
        }, matchWrap())
    }

    private fun row(content: LinearLayout.() -> Unit) {
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
            content()
        }, matchWrap())
    }

    private fun LinearLayout.button(text: String, onClick: () -> Unit) {
        addView(Button(this@MainActivity).apply {
            this.text = text
            setOnClickListener { onClick() }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(6)
        })
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun matchWrap(): ViewGroup.LayoutParams =
        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private data class LaunchableApp(val packageName: String, val label: String)
}
