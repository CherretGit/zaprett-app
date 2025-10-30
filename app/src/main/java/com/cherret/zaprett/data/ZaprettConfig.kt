package com.cherret.zaprett.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZaprettConfig(
    @SerialName("active_lists")
    val activeLists: List<String> = emptyList(),
    @SerialName("active_ipsets")
    val activeIpsets: List<String> = emptyList(),
    @SerialName("active_exclude_lists")
    val activeExcludeLists: List<String> = emptyList(),
    @SerialName("active_exclude_ipsets")
    val activeExcludeIpsets: List<String> = emptyList(),
    @SerialName("list_type")
    val listType: String = "whitelist",
    @SerialName("strategy")
    val strategy: String = "",
    @SerialName("app_list")
    val appList: String = "none",
    @SerialName("whitelist")
    val whitelist: List<String> = emptyList(),
    @SerialName("blacklist")
    val blacklist: List<String> = emptyList()
)
