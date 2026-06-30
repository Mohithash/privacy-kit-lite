package com.sal.privacykit.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val appId: String,
    val deviceTemplateId: String,
    val profileMode: String,
    val identifierRules: String,
    val createdAt: Long,
    val customDeviceName: String? = null,
    val customDeviceManufacturer: String? = null,
    val skipLaunchConfirmation: Boolean = false,
    val identifierValues: String = "",
    val archived: Boolean = false,
    val tagColor: String? = null,
    val notes: String? = null,
)
