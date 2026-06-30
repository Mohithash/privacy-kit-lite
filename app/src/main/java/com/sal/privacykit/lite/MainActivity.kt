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
import com.sal.privacykit.lite.model.LiteAppAssignment
import com.sal.privacykit.lite.model.LiteProfile
import com.sal.privacykit.lite.model.LiteState
import com.sal.privacykit.lite.xposed.XposedConfigExporter

class MainActivity : Activity() {
    private lateinit var store: LiteProfileStore
    private lateinit var root: LinearLayout
    private var state = LiteState(emptyList(), emptyList())
    private var selectedProfileId: Long? = null

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
        state = store.loadState()
        selectedProfileId = state.profiles.firstOrNull()?.id
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
        note("Local LSPosed spoofing with reusable app profiles. No network access, account system, telemetry, browser, AI, or premium code.")
        status()
        actions()

        if (state.profiles.isEmpty()) {
            emptyState()
            return
        }
        profilePicker()
        val profile = state.selectedProfile(selectedProfileId) ?: return
        selectedProfileId = profile.id
        profileTools(profile)
        assignedApps(profile)
        profileEditor(profile)
    }

    private fun status() {
        val connected = LiteServices.xposedConnection.service != null
        val apps = state.assignments.count { it.enabled }
        note(if (connected) "LSPosed service: connected. Exported apps: $apps" else "LSPosed service: not connected. Config is stored locally.")
    }

    private fun actions() {
        row {
            button("New profile") { showProfileNameDialog("New profile", "Lite Profile ${state.profiles.size + 1}") { createProfile(it) } }
            button("Add app") { selectedProfile()?.let(::showAppPicker) ?: toast("Create a profile first") }
            button("Export") { exportProfiles(showToast = true) }
        }
    }

    private fun emptyState() {
        section("No profiles yet")
        note("Create a profile, add installed apps to it, approve LSPosed scope, then restart those app processes.")
    }

    private fun profilePicker() {
        section("Profiles")
        val labels = state.profiles.map { profile ->
            val count = state.assignmentsFor(profile.id).size
            "${profile.name} ($count apps)"
        }
        val selectedIndex = state.profiles.indexOfFirst { it.id == selectedProfileId }.coerceAtLeast(0)
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, labels)
            setSelection(selectedIndex)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val profileId = state.profiles[position].id
                    if (profileId != selectedProfileId) {
                        selectedProfileId = profileId
                        render()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        root.addView(spinner, matchWrap())
    }

    private fun profileTools(profile: LiteProfile) {
        row {
            button("Rename") {
                showProfileNameDialog("Rename profile", profile.name) { name ->
                    updateProfile(profile.copy(name = name.ifBlank { profile.name }))
                }
            }
            button("Duplicate") {
                val copy = store.duplicate(profile, "${profile.name} Copy")
                state = state.copy(profiles = state.profiles + copy)
                selectedProfileId = copy.id
                persistAndRender()
            }
            button("Delete") { confirmDeleteProfile(profile) }
            button("Regenerate") {
                updateProfile(store.regenerate(profile))
                exportProfiles(showToast = false)
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
    }

    private fun assignedApps(profile: LiteProfile) {
        section("Assigned apps")
        val assignments = state.assignmentsFor(profile.id)
        if (assignments.isEmpty()) {
            note("No apps use this profile yet.")
            return
        }
        assignments.forEach { assignment ->
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(8), 0, dp(8))
            }
            box.addView(TextView(this).apply {
                text = "${assignment.label}\n${assignment.packageName}"
                setTextColor(Color.WHITE)
                textSize = 15f
            })
            val enabledSwitch = Switch(this).apply {
                text = "Assignment enabled"
                isChecked = assignment.enabled
                setTextColor(Color.LTGRAY)
                setOnCheckedChangeListener { _, checked ->
                    updateAssignment(assignment.copy(enabled = checked), rerender = false)
                    exportProfiles(showToast = false)
                }
            }
            box.addView(enabledSwitch, matchWrap())
            box.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                button("Scope") { requestScope(assignment.packageName) }
                button("Move") { showMoveAssignmentDialog(assignment) }
                button("Remove") {
                    state = state.copy(assignments = state.assignments.filterNot { it.packageName == assignment.packageName })
                    persistAndRender()
                }
            }, matchWrap())
            root.addView(box, matchWrap())
        }
    }

    private fun profileEditor(profile: LiteProfile) {
        section("Spoof data")
        note("${profile.enabledCount()} generated or custom values are active when this profile and its app assignment are enabled.")
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
                val current = currentProfile(profile.id) ?: return
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
                val current = currentProfile(profile.id) ?: return
                if (current.values[key] == value) return
                updateProfile(current.copy(values = current.values + (key to value)), rerender = false)
                exportProfiles(showToast = false)
            }
        })

        root.addView(row, matchWrap())
    }

    private fun showAppPicker(profile: LiteProfile) {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val assignedPackages = state.assignments.map { it.packageName }.toSet()
        val apps = packageManager.queryIntentActivities(mainIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .map {
                val info = it.activityInfo
                val label = it.loadLabel(packageManager).toString()
                LaunchableApp(info.packageName, label)
            }
            .distinctBy { it.packageName }
            .filterNot { it.packageName == packageName || it.packageName in assignedPackages }
            .sortedBy { it.label.lowercase() }
        if (apps.isEmpty()) {
            toast("No unassigned launchable apps available")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Add app to ${profile.name}")
            .setItems(apps.map { "${it.label} (${it.packageName})" }.toTypedArray()) { _, which ->
                val app = apps[which]
                state = state.copy(
                    assignments = state.assignments + LiteAppAssignment(
                        packageName = app.packageName,
                        label = app.label,
                        profileId = profile.id,
                    ),
                )
                persistAndRender()
                requestScope(app.packageName)
            }
            .show()
    }

    private fun showMoveAssignmentDialog(assignment: LiteAppAssignment) {
        if (state.profiles.size < 2) {
            toast("Create another profile first")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Move app to profile")
            .setItems(state.profiles.map { it.name }.toTypedArray()) { _, which ->
                updateAssignment(assignment.copy(profileId = state.profiles[which].id))
            }
            .show()
    }

    private fun showProfileNameDialog(title: String, initial: String, onDone: (String) -> Unit) {
        val input = EditText(this).apply {
            setText(initial)
            setSingleLine(true)
            selectAll()
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Save") { _, _ -> onDone(input.text?.toString().orEmpty()) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createProfile(name: String) {
        val profile = store.createProfile(name)
        state = state.copy(profiles = state.profiles + profile)
        selectedProfileId = profile.id
        persistAndRender()
    }

    private fun confirmDeleteProfile(profile: LiteProfile) {
        AlertDialog.Builder(this)
            .setTitle("Delete ${profile.name}?")
            .setMessage("Apps assigned to this profile will be removed from the exported LSPosed config.")
            .setPositiveButton("Delete") { _, _ ->
                state = state.copy(
                    profiles = state.profiles.filterNot { it.id == profile.id },
                    assignments = state.assignments.filterNot { it.profileId == profile.id },
                )
                selectedProfileId = state.profiles.firstOrNull()?.id
                persistAndRender()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestScope(packageName: String) {
        LiteServices.xposedConnection.requestScope(packageName) { ok, message ->
            runOnUiThread {
                toast(if (ok) "Scope request approved or already scoped" else message ?: "Open LSPosed and add scope manually")
            }
        }
    }

    private fun exportProfiles(showToast: Boolean): Boolean {
        val ok = XposedConfigExporter.export(LiteServices.xposedConnection, state)
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
        store.saveState(state)
        exportProfiles(showToast = false)
        render()
    }

    private fun updateProfile(profile: LiteProfile, rerender: Boolean = true) {
        state = state.copy(profiles = state.profiles.map { if (it.id == profile.id) profile else it })
        store.saveState(state)
        exportProfiles(showToast = false)
        if (rerender) render()
    }

    private fun updateAssignment(assignment: LiteAppAssignment, rerender: Boolean = true) {
        state = state.copy(assignments = state.assignments.map { if (it.packageName == assignment.packageName) assignment else it })
        store.saveState(state)
        exportProfiles(showToast = false)
        if (rerender) render()
    }

    private fun selectedProfile(): LiteProfile? = state.selectedProfile(selectedProfileId)

    private fun currentProfile(id: Long): LiteProfile? = state.profiles.firstOrNull { it.id == id }

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
