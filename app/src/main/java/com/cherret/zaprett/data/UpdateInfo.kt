package com.cherret.zaprett.data

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
    val version: String,
    val versionCode: Int,
    val downloadUrl: String,
    val changelogUrl: String
)

data class UpdateData(
    val updateInfo: UpdateInfo,
    val changelog: String
)