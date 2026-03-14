package com.cherret.zaprett.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class StorageData(
    val schema: Int,
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val dependencies: List<String> = emptyList(),
    val file: String,

){
    @Transient
    var manifestPath: String = ""
}