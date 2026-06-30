package com.sal.privacykit.data.repository

import com.sal.privacykit.data.db.AppDatabase
import com.sal.privacykit.data.db.ProfileEntity
import com.sal.privacykit.data.model.DeviceTemplateCatalog
import com.sal.privacykit.data.model.IdentifierCatalog
import com.sal.privacykit.data.model.IdentifierRuleType
import com.sal.privacykit.data.model.IdentifierValueGenerator
import com.sal.privacykit.data.model.Profile
import com.sal.privacykit.data.model.ProfileMode
import com.sal.privacykit.data.model.ProfileTagColor
import com.sal.privacykit.data.prefs.AppPreferences
import com.sal.privacykit.xposed.XposedConfigExporter
import com.sal.privacykit.xposed.XposedServiceConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private fun encodeRules(rules: Map<String, IdentifierRuleType>): String =
    rules.entries.joinToString(";") { "${it.key}=${it.value.name}" }

private fun decodeRules(raw: String): Map<String, IdentifierRuleType> {
    if (raw.isBlank()) return emptyMap()
    return raw.split(";").mapNotNull { pair ->
        val parts = pair.split("=")
        if (parts.size != 2 || parts[0] !in IdentifierCatalog.allowedKeys) return@mapNotNull null
        val rule = decodeRuleType(parts[1]) ?: return@mapNotNull null
        parts[0] to rule
    }.toMap()
}

private fun decodeRuleType(raw: String): IdentifierRuleType? = when (raw) {
    "RANDOM_PER_LAUNCH", "RANDOM_DAILY" -> IdentifierRuleType.STATIC
    else -> runCatching { IdentifierRuleType.valueOf(raw) }.getOrNull()
}

private fun encodeValues(values: Map<String, String>): String {
    val obj = JSONObject()
    values.filterKeys { it in IdentifierCatalog.allowedKeys }.forEach { (key, value) -> obj.put(key, value) }
    return obj.toString()
}

private fun decodeValues(raw: String): Map<String, String> {
    if (raw.isBlank()) return emptyMap()
    val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap()
    return obj.keys().asSequence()
        .filter { it in IdentifierCatalog.allowedKeys }
        .associateWith { obj.getString(it) }
}

private fun defaultRules(): Map<String, IdentifierRuleType> =
    IdentifierCatalog.allItems.associate { it.key to it.defaultRule }

private fun templateValues(deviceTemplateId: String, customDeviceName: String?, customDeviceManufacturer: String?): Map<String, String> {
    val template = DeviceTemplateCatalog.byId(deviceTemplateId)
    val model = customDeviceName ?: template.name
    val manufacturer = customDeviceManufacturer ?: template.manufacturer
    return mapOf(
        "build_model" to model,
        "build_brand" to manufacturer,
        "build_manufacturer" to manufacturer,
        "build_device" to model.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "device" },
        "build_product" to model.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "product" },
    )
}

private fun defaultValues(
    profileId: Long,
    deviceTemplateId: String,
    customDeviceName: String? = null,
    customDeviceManufacturer: String? = null,
): Map<String, String> {
    val generated = IdentifierCatalog.allItems
        .filter { it.defaultRule == IdentifierRuleType.STATIC }
        .associate { it.key to IdentifierValueGenerator.generate(it.key, IdentifierValueGenerator.seedFor(profileId, it.key)) }
    return generated + templateValues(deviceTemplateId, customDeviceName, customDeviceManufacturer)
}

private fun ProfileEntity.toDomain(): Profile = Profile(
    id = id,
    name = name,
    appId = appId,
    deviceTemplateId = deviceTemplateId,
    profileMode = runCatching { ProfileMode.valueOf(profileMode) }.getOrDefault(ProfileMode.ISOLATED),
    identifierRules = decodeRules(identifierRules).ifEmpty { defaultRules() },
    identifierValues = decodeValues(identifierValues),
    createdAt = createdAt,
    customDeviceName = customDeviceName,
    customDeviceManufacturer = customDeviceManufacturer,
    skipLaunchConfirmation = skipLaunchConfirmation,
    archived = archived,
    tagColor = tagColor?.let { runCatching { ProfileTagColor.valueOf(it) }.getOrNull() },
    notes = notes,
)

fun selectActiveOrFallback(profiles: List<Profile>, activeId: Long?): Profile? {
    val candidates = profiles.filterNot { it.archived }
    if (candidates.isEmpty()) return null
    return candidates.firstOrNull { it.id == activeId } ?: candidates.first()
}

class ProfileRepository(
    private val database: AppDatabase,
    private val serviceConnection: XposedServiceConnection,
    private val appPreferences: AppPreferences,
) {
    private val dao = database.profileDao()

    fun observeAll(): Flow<List<Profile>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeForApp(appId: String): Flow<List<Profile>> =
        dao.observeForApp(appId).map { list -> list.map { it.toDomain() } }

    fun observeById(id: Long): Flow<Profile?> = dao.observeById(id).map { it?.toDomain() }

    suspend fun activeOrFallbackProfile(appId: String): Profile? {
        val activeId = appPreferences.activeProfileId(appId).first()
        return selectActiveOrFallback(observeForApp(appId).first(), activeId)
    }

    suspend fun createProfile(
        name: String,
        appId: String,
        deviceTemplateId: String,
    ): Long {
        val id = dao.insert(
            ProfileEntity(
                name = name,
                appId = appId,
                deviceTemplateId = deviceTemplateId,
                profileMode = ProfileMode.ISOLATED.name,
                identifierRules = encodeRules(defaultRules()),
                createdAt = System.currentTimeMillis(),
            ),
        )
        dao.update(dao.observeById(id).first()!!.copy(identifierValues = encodeValues(defaultValues(id, deviceTemplateId))))
        serviceConnection.requestScope(appId)
        appPreferences.setActiveProfile(appId, id)
        syncExport()
        return id
    }

    suspend fun createCustomProfile(
        name: String,
        appId: String,
        customDeviceName: String,
        customDeviceManufacturer: String,
    ): Long {
        val id = dao.insert(
            ProfileEntity(
                name = name,
                appId = appId,
                deviceTemplateId = "custom_device",
                profileMode = ProfileMode.ISOLATED.name,
                identifierRules = encodeRules(defaultRules()),
                createdAt = System.currentTimeMillis(),
                customDeviceName = customDeviceName,
                customDeviceManufacturer = customDeviceManufacturer,
            ),
        )
        dao.update(
            dao.observeById(id).first()!!.copy(
                identifierValues = encodeValues(defaultValues(id, "custom_device", customDeviceName, customDeviceManufacturer)),
            ),
        )
        serviceConnection.requestScope(appId)
        appPreferences.setActiveProfile(appId, id)
        syncExport()
        return id
    }

    suspend fun cloneProfile(source: Profile, targetAppId: String): Long {
        val id = dao.insert(
            toEntity(source).copy(
                id = 0,
                appId = targetAppId,
                name = "${source.name} Copy",
                createdAt = System.currentTimeMillis(),
            ),
        )
        serviceConnection.requestScope(targetAppId)
        appPreferences.setActiveProfile(targetAppId, id)
        syncExport()
        return id
    }

    suspend fun rename(profile: Profile, newName: String) {
        dao.update(toEntity(profile).copy(name = newName))
        syncExport()
    }

    suspend fun updateDeviceTemplate(profile: Profile, deviceTemplateId: String) {
        val templateOverrides = templateValues(deviceTemplateId, null, null)
            .filterKeys { key -> profile.identifierRules[key] != IdentifierRuleType.CUSTOM }
        val updatedValues = profile.identifierValues.toMutableMap().apply { putAll(templateOverrides) }
        dao.update(
            toEntity(profile).copy(
                deviceTemplateId = deviceTemplateId,
                customDeviceName = null,
                customDeviceManufacturer = null,
                identifierValues = encodeValues(updatedValues),
            ),
        )
        syncExport()
    }

    suspend fun updateIdentifierRule(profile: Profile, key: String, rule: IdentifierRuleType) {
        if (key !in IdentifierCatalog.allowedKeys) return
        val updatedRules = profile.identifierRules.toMutableMap().apply { put(key, rule) }
        val updatedValues = profile.identifierValues.toMutableMap()
        if (rule == IdentifierRuleType.STATIC && updatedValues[key] == null) {
            updatedValues[key] = IdentifierValueGenerator.generate(key, IdentifierValueGenerator.seedFor(profile.id, key))
        }
        dao.update(toEntity(profile).copy(identifierRules = encodeRules(updatedRules), identifierValues = encodeValues(updatedValues)))
        syncExport()
    }

    suspend fun updateIdentifierValue(profile: Profile, key: String, value: String) {
        if (key !in IdentifierCatalog.allowedKeys) return
        val updatedValues = profile.identifierValues.toMutableMap().apply { put(key, value) }
        dao.update(toEntity(profile).copy(identifierValues = encodeValues(updatedValues)))
        syncExport()
    }

    suspend fun setSkipLaunchConfirmation(profile: Profile, skip: Boolean) {
        dao.update(toEntity(profile).copy(skipLaunchConfirmation = skip))
        syncExport()
    }

    suspend fun setArchived(profile: Profile, archived: Boolean) {
        dao.update(toEntity(profile).copy(archived = archived))
        syncExport()
    }

    suspend fun setTagColor(profile: Profile, color: ProfileTagColor?) {
        dao.update(toEntity(profile).copy(tagColor = color?.name))
    }

    suspend fun setNotes(profile: Profile, notes: String?) {
        dao.update(toEntity(profile).copy(notes = notes?.takeIf { it.isNotBlank() }))
    }

    suspend fun delete(profile: Profile) {
        dao.delete(toEntity(profile))
        syncExport()
    }

    suspend fun setActiveProfile(profile: Profile) {
        appPreferences.setActiveProfile(profile.appId, profile.id)
        syncExport()
    }

    suspend fun resync() = syncExport()

    private fun toEntity(profile: Profile): ProfileEntity = ProfileEntity(
        id = profile.id,
        name = profile.name,
        appId = profile.appId,
        deviceTemplateId = profile.deviceTemplateId,
        profileMode = profile.profileMode.name,
        identifierRules = encodeRules(profile.identifierRules.filterKeys { it in IdentifierCatalog.allowedKeys }),
        createdAt = profile.createdAt,
        customDeviceName = profile.customDeviceName,
        customDeviceManufacturer = profile.customDeviceManufacturer,
        skipLaunchConfirmation = profile.skipLaunchConfirmation,
        identifierValues = encodeValues(profile.identifierValues),
        archived = profile.archived,
        tagColor = profile.tagColor?.name,
        notes = profile.notes,
    )

    suspend fun exportToJson(): String {
        val array = JSONArray()
        observeAll().first().forEach { profile ->
            array.put(
                JSONObject().apply {
                    put("id", profile.id)
                    put("name", profile.name)
                    put("appId", profile.appId)
                    put("deviceTemplateId", profile.deviceTemplateId)
                    put("identifierRules", encodeRules(profile.identifierRules))
                    put("identifierValues", encodeValues(profile.identifierValues))
                    put("createdAt", profile.createdAt)
                    put("customDeviceName", profile.customDeviceName)
                    put("customDeviceManufacturer", profile.customDeviceManufacturer)
                    put("skipLaunchConfirmation", profile.skipLaunchConfirmation)
                    put("archived", profile.archived)
                    put("tagColor", profile.tagColor?.name)
                    put("notes", profile.notes)
                },
            )
        }
        return JSONObject().put("profiles", array).toString()
    }

    suspend fun importFromJson(json: String): Int {
        val root = JSONObject(json)
        val array = root.optJSONArray("profiles") ?: return 0
        var count = 0
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            dao.insert(
                ProfileEntity(
                    name = obj.getString("name"),
                    appId = obj.getString("appId"),
                    deviceTemplateId = obj.getString("deviceTemplateId"),
                    profileMode = obj.optString("profileMode", ProfileMode.ISOLATED.name),
                    identifierRules = encodeRules(decodeRules(obj.optString("identifierRules", ""))),
                    identifierValues = encodeValues(decodeValues(obj.optString("identifierValues", ""))),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    customDeviceName = obj.takeIf { it.has("customDeviceName") && !it.isNull("customDeviceName") }?.getString("customDeviceName"),
                    customDeviceManufacturer = obj.takeIf { it.has("customDeviceManufacturer") && !it.isNull("customDeviceManufacturer") }?.getString("customDeviceManufacturer"),
                    skipLaunchConfirmation = obj.optBoolean("skipLaunchConfirmation", false),
                    archived = obj.optBoolean("archived", false),
                    tagColor = obj.takeIf { it.has("tagColor") && !it.isNull("tagColor") }?.getString("tagColor"),
                    notes = obj.takeIf { it.has("notes") && !it.isNull("notes") }?.getString("notes"),
                ),
            )
            count++
        }
        syncExport()
        return count
    }

    suspend fun backfillMissingStaticValues() {
        val profiles = observeAll().first()
        var changed = false
        profiles.forEach { profile ->
            val missingKeys = profile.identifierRules
                .filterValues { it == IdentifierRuleType.STATIC }
                .keys
                .filter { it in IdentifierCatalog.allowedKeys && profile.identifierValues[it].isNullOrBlank() }
            if (missingKeys.isEmpty()) return@forEach
            val updatedValues = profile.identifierValues.toMutableMap()
            missingKeys.forEach { key ->
                updatedValues[key] = IdentifierValueGenerator.generate(key, IdentifierValueGenerator.seedFor(profile.id, key))
            }
            dao.update(toEntity(profile).copy(identifierValues = encodeValues(updatedValues)))
            changed = true
        }
        if (changed) syncExport()
    }

    suspend fun fixTemplateMismatchedValues() {
        val profiles = observeAll().first()
        var changed = false
        profiles.forEach { profile ->
            val expected = templateValues(profile.deviceTemplateId, profile.customDeviceName, profile.customDeviceManufacturer)
            val toFix = expected.filter { (key, value) ->
                profile.identifierRules[key] == IdentifierRuleType.STATIC && profile.identifierValues[key] != value
            }
            if (toFix.isEmpty()) return@forEach
            val updatedValues = profile.identifierValues.toMutableMap().apply { putAll(toFix) }
            dao.update(toEntity(profile).copy(identifierValues = encodeValues(updatedValues)))
            changed = true
        }
        if (changed) syncExport()
    }

    suspend fun profileBelongsToApp(profileId: Long, appId: String): Boolean =
        dao.findAppId(profileId) == appId

    private suspend fun syncExport() {
        val profiles = observeAll().first()
        val activeByApp = profiles.map { it.appId }.distinct().associateWith { appPreferences.activeProfileId(it).first() }
            .mapNotNull { (appId, id) -> if (id != null) appId to id else null }.toMap()
        val customHooksJson = appPreferences.customHooksJson.first()
        XposedConfigExporter.export(serviceConnection, profiles, activeByApp, customHooksJson)
    }
}
