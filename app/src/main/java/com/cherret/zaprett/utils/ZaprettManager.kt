package com.cherret.zaprett.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import androidx.core.content.edit
import com.cherret.zaprett.data.AppListType
import com.cherret.zaprett.data.ZaprettConfig
import com.topjohnwu.superuser.Shell
import kotlinx.serialization.json.Json
import java.io.File

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    coerceInputValues = true
}

private fun readConfig(): ZaprettConfig {
    val configFile = getConfigFile()
    if (!configFile.exists()) {
        return ZaprettConfig()
    }
    return try {
        val content = configFile.readText()
        if (content.isBlank()) ZaprettConfig() else json.decodeFromString<ZaprettConfig>(content)
    } catch (e: Exception) {
        Log.e("ZaprettManager", "Error reading config, returning defaults", e)
        ZaprettConfig()
    }
}

private fun writeConfig(config: ZaprettConfig) {
    val configFile = getConfigFile()
    try {
        configFile.parentFile?.mkdirs()
        val content = json.encodeToString(config)
        configFile.writeText(content)
    } catch (e: Exception) {
        Log.e("ZaprettManager", "Error writing config", e)
    }
}

fun checkRoot(callback: (Boolean) -> Unit) {
    Shell.getShell().isRoot.let { callback(it) }
}

fun checkModuleInstallation(callback: (Boolean) -> Unit) {
    Shell.cmd("zaprett").submit { result ->
        callback(result.out.toString().contains("zaprett"))
    }
}

fun getStatus(callback: (Boolean) -> Unit) {
    Shell.cmd("zaprett status").submit { result ->
        callback(result.out.toString().contains("working"))
    }
}

fun startService(callback: (Boolean) -> Unit) {
    Shell.cmd("zaprett start").submit { result ->
        callback(result.isSuccess)
    }
}

fun stopService(callback: (Boolean) -> Unit) {
    Shell.cmd("zaprett stop").submit { result ->
        callback(result.isSuccess)
    }
}

fun restartService(callback: (Boolean) -> Unit) {
    Shell.cmd("zaprett restart").submit { result ->
        callback(result.isSuccess)
    }
}

fun getModuleVersion(callback: (String) -> Unit) {
    Shell.cmd("zaprett module-ver").submit { result ->
        if (result.out.isNotEmpty()) callback(result.out.first()) else "undefined"
    }
}

fun getBinVersion(callback: (String) -> Unit) {
    Shell.cmd("zaprett bin-ver").submit { result ->
        if (result.out.isNotEmpty()) callback(result.out.first()) else "undefined"
    }
}

fun getConfigFile(): File {
    return File(Environment.getExternalStorageDirectory(), "zaprett/config.json")
}

fun setStartOnBoot(prefs: SharedPreferences, callback: (Boolean) -> Unit) {
    if (prefs.getBoolean("use_module", false)) {
        Shell.cmd("zaprett autostart").submit { result ->
            if (result.out.isNotEmpty() && result.out.toString().contains("true")) callback(true) else callback(false)
        }
    }
}

fun getStartOnBoot(prefs: SharedPreferences, callback: (Boolean) -> Unit) {
    if (prefs.getBoolean("use_module", false)) {
        Shell.cmd("zaprett get-autostart").submit { result ->
            if (result.out.isNotEmpty() && result.out.toString().contains("true")) callback(true) else callback(false)
        }
    } else {
        callback(false)
    }
}

fun getZaprettPath(): String {
    return Environment.getExternalStorageDirectory().path + "/zaprett"
}

fun getAllLists(): Array<String> {
    val listsDir = File("${getZaprettPath()}/lists/include")
    return listsDir.listFiles { file -> file.isFile && file.extension.lowercase() == "txt" }
        ?.map { it.absolutePath }
        ?.toTypedArray()
        ?: emptyArray()
}

fun getAllIpsets(): Array<String> {
    val listsDir = File("${getZaprettPath()}/ipset/include")
    return listsDir.listFiles { file -> file.isFile && file.extension.lowercase() == "txt" }
        ?.map { it.absolutePath }
        ?.toTypedArray()
        ?: emptyArray()
}

fun getAllExcludeLists(): Array<String> {
    val listsDir = File("${getZaprettPath()}/lists/exclude/")
    return listsDir.listFiles { file -> file.isFile && file.extension.lowercase() == "txt" }
        ?.map { it.absolutePath }
        ?.toTypedArray()
        ?: emptyArray()
}

fun getAllExcludeIpsets(): Array<String> {
    val listsDir = File("${getZaprettPath()}/ipset/exclude/")
    return listsDir.listFiles { file -> file.isFile && file.extension.lowercase() == "txt" }
        ?.map { it.absolutePath }
        ?.toTypedArray()
        ?: emptyArray()
}

fun getAllNfqwsStrategies(): Array<String> {
    val listsDir = File("${getZaprettPath()}/strategies/nfqws")
    return listsDir.listFiles { file -> file.isFile && file.extension.lowercase() == "txt" }
        ?.map { it.absolutePath }
        ?.toTypedArray()
        ?: emptyArray()
}

fun getAllByeDPIStrategies(): Array<String> {
    val listsDir = File("${getZaprettPath()}/strategies/byedpi")
    return listsDir.listFiles { file -> file.isFile && file.extension.lowercase() == "txt" }
        ?.map { it.absolutePath }
        ?.toTypedArray()
        ?: emptyArray()
}

fun getAllStrategies(sharedPreferences: SharedPreferences): Array<String> {
    return if (sharedPreferences.getBoolean("use_module", false)) getAllNfqwsStrategies()
    else getAllByeDPIStrategies()
}

fun getActiveLists(sharedPreferences: SharedPreferences): Array<String> {
    if (sharedPreferences.getBoolean("use_module", false)) {
        return readConfig().activeLists.toTypedArray()
    } else {
        return sharedPreferences.getStringSet("lists", emptySet())?.toTypedArray() ?: emptyArray()
    }
}

fun getActiveIpsets(sharedPreferences: SharedPreferences): Array<String> {
    if (sharedPreferences.getBoolean("use_module", false)) {
        return readConfig().activeIpsets.toTypedArray()
    } else return sharedPreferences.getStringSet("ipsets", emptySet())?.toTypedArray() ?: emptyArray()
}

fun getActiveExcludeLists(sharedPreferences: SharedPreferences): Array<String> {
    if (sharedPreferences.getBoolean("use_module", false)) {
        return readConfig().activeExcludeLists.toTypedArray()
    } else {
        return sharedPreferences.getStringSet("exclude_lists", emptySet())?.toTypedArray() ?: emptyArray()
    }
}

fun getActiveExcludeIpsets(sharedPreferences: SharedPreferences): Array<String> {
    if (sharedPreferences.getBoolean("use_module", false)) {
        return readConfig().activeExcludeIpsets.toTypedArray()
    } else return sharedPreferences.getStringSet("exclude_ipsets", emptySet())?.toTypedArray() ?: emptyArray()
}

fun getActiveNfqwsStrategy(): Array<String> {
    val strategy = readConfig().strategy
    return if (strategy.isNotBlank()) arrayOf(strategy) else emptyArray()
}

fun getActiveByeDPIStrategy(sharedPreferences: SharedPreferences): Array<String> {
    val path = sharedPreferences.getString("active_strategy", "")
    if (!path.isNullOrBlank() && File(path).exists()) {
        return arrayOf(path)
    }
    return emptyArray()
}

fun getActiveByeDPIStrategyContent(sharedPreferences: SharedPreferences): List<String> {
    val path = sharedPreferences.getString("active_strategy", "")
    if (!path.isNullOrBlank() && File(path).exists()) {
        return File(path).readLines()
    }
    return emptyList()
}

fun getActiveStrategy(sharedPreferences: SharedPreferences): Array<String> {
    return if (sharedPreferences.getBoolean("use_module", false)) getActiveNfqwsStrategy()
    else getActiveByeDPIStrategy(sharedPreferences)
}

fun enableList(path: String, sharedPreferences: SharedPreferences) {
    if (sharedPreferences.getBoolean("use_module", false)) {
        val config = readConfig()
        val isWhitelist = getHostListMode(sharedPreferences) == "whitelist"
        val currentLists = if (isWhitelist) config.activeLists else config.activeExcludeLists
        if (path !in currentLists) {
            val updatedLists = currentLists + path
            val newConfig = if (isWhitelist) {
                config.copy(activeLists = updatedLists)
            } else {
                config.copy(activeExcludeLists = updatedLists)
            }
            writeConfig(newConfig)
        }
    } else {
        val key = if (getHostListMode(sharedPreferences) == "whitelist") "lists" else "exclude_lists"
        val currentSet = sharedPreferences.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (path !in currentSet) {
            currentSet.add(path)
            sharedPreferences.edit { putStringSet(key, currentSet) }
        }
    }
}

fun enableIpset(path: String, sharedPreferences: SharedPreferences) {
    if (sharedPreferences.getBoolean("use_module", false)) {
        val config = readConfig()
        val isWhitelist = getHostListMode(sharedPreferences) == "whitelist"
        val currentIpsets = if (isWhitelist) config.activeIpsets else config.activeExcludeIpsets
        if (path !in currentIpsets) {
            val updatedIpsets = currentIpsets + path
            val newConfig = if (isWhitelist) {
                config.copy(activeIpsets = updatedIpsets)
            } else {
                config.copy(activeExcludeIpsets = updatedIpsets)
            }
            writeConfig(newConfig)
        }
    } else {
        val key = if (getHostListMode(sharedPreferences) == "whitelist") "ipsets" else "exclude_ipsets"
        val currentSet = sharedPreferences.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (path !in currentSet) {
            currentSet.add(path)
            sharedPreferences.edit { putStringSet(key, currentSet) }
        }
    }
}

fun enableStrategy(path: String, sharedPreferences: SharedPreferences) {
    if (sharedPreferences.getBoolean("use_module", false)) {
        val config = readConfig()
        if (config.strategy != path) {
            writeConfig(config.copy(strategy = path))
        }
    } else {
        sharedPreferences.edit { putString("active_strategy", path) }
    }
}

fun disableList(path: String, sharedPreferences: SharedPreferences) {
    if (sharedPreferences.getBoolean("use_module", false)) {
        val config = readConfig()
        val isWhitelist = getHostListMode(sharedPreferences) == "whitelist"
        val currentLists = if (isWhitelist) config.activeLists else config.activeExcludeLists
        if (path in currentLists) {
            val updatedLists = currentLists.filter { it != path }
            val newConfig = if (isWhitelist) {
                config.copy(activeLists = updatedLists)
            } else {
                config.copy(activeExcludeLists = updatedLists)
            }
            writeConfig(newConfig)
        }
    } else {
        val key = if (getHostListMode(sharedPreferences) == "whitelist") "lists" else "exclude_lists"
        val currentSet = sharedPreferences.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (path in currentSet) {
            currentSet.remove(path)
            sharedPreferences.edit { putStringSet(key, currentSet) }
        }
        if (currentSet.isEmpty()) {
            sharedPreferences.edit { remove(key) }
        }
    }
}

fun disableIpset(path: String, sharedPreferences: SharedPreferences) {
    if (sharedPreferences.getBoolean("use_module", false)) {
        val config = readConfig()
        val isWhitelist = getHostListMode(sharedPreferences) == "whitelist"
        val currentIpsets = if (isWhitelist) config.activeIpsets else config.activeExcludeIpsets
        if (path in currentIpsets) {
            val updatedIpsets = currentIpsets.filter { it != path }
            val newConfig = if (isWhitelist) {
                config.copy(activeIpsets = updatedIpsets)
            } else {
                config.copy(activeExcludeIpsets = updatedIpsets)
            }
            writeConfig(newConfig)
        }
    } else {
        val key = if (getHostListMode(sharedPreferences) == "whitelist") "ipsets" else "exclude_ipsets"
        val currentSet = sharedPreferences.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (path in currentSet) {
            currentSet.remove(path)
            sharedPreferences.edit { putStringSet(key, currentSet) }
        }
        if (currentSet.isEmpty()) {
            sharedPreferences.edit { remove(key) }
        }
    }
}

fun disableStrategy(path: String, sharedPreferences: SharedPreferences) {
    if (sharedPreferences.getBoolean("use_module", false)) {
        val config = readConfig()
        if (config.strategy == path) {
            writeConfig(config.copy(strategy = ""))
        }
    } else {
        sharedPreferences.edit { remove("active_strategy") }
    }
}

fun addPackageToList(listType: AppListType, packageName: String, prefs: SharedPreferences, context: Context) {
    if (prefs.getBoolean("use_module", false)) {
        val config = readConfig()
        if (listType == AppListType.Whitelist) {
            if (packageName !in config.whitelist) {
                writeConfig(config.copy(whitelist = config.whitelist + packageName))
            }
        } else if (listType == AppListType.Blacklist) {
            if (packageName !in config.blacklist) {
                writeConfig(config.copy(blacklist = config.blacklist + packageName))
            }
        }
    } else {
        val prefs = context.getSharedPreferences("settings", MODE_PRIVATE)
        if (listType == AppListType.Whitelist) {
            val set = prefs.getStringSet("whitelist", emptySet())?.toMutableSet() ?: mutableSetOf()
            set.add(packageName)
            prefs.edit().putStringSet("whitelist", set).apply()
        }
        if (listType == AppListType.Blacklist) {
            val set = prefs.getStringSet("blacklist", emptySet())?.toMutableSet() ?: mutableSetOf()
            set.add(packageName)
            prefs.edit().putStringSet("blacklist", set).apply()
        }
    }
}

fun removePackageFromList(listType: AppListType, packageName: String, prefs: SharedPreferences, context: Context) {
    if (prefs.getBoolean("use_module", false)) {
        val config = readConfig()
        if (listType == AppListType.Whitelist) {
            if (packageName in config.whitelist) {
                writeConfig(config.copy(whitelist = config.whitelist.filter { it != packageName }))
            }
        } else if (listType == AppListType.Blacklist) {
            if (packageName in config.blacklist) {
                writeConfig(config.copy(blacklist = config.blacklist.filter { it != packageName }))
            }
        }
    } else {
        val prefs = context.getSharedPreferences("settings", MODE_PRIVATE)
        if (listType == AppListType.Whitelist) {
            val set = prefs.getStringSet("whitelist", emptySet())?.toMutableSet() ?: mutableSetOf()
            set.remove(packageName)
            prefs.edit().putStringSet("whitelist", set).apply()
        }
        if (listType == AppListType.Blacklist) {
            val set = prefs.getStringSet("blacklist", emptySet())?.toMutableSet() ?: mutableSetOf()
            set.remove(packageName)
            prefs.edit().putStringSet("blacklist", set).apply()
        }
    }
}

fun isInList(listType: AppListType, packageName: String, prefs: SharedPreferences, context: Context): Boolean {
    if (prefs.getBoolean("use_module", false)) {
        val config = readConfig()
        return if (listType == AppListType.Whitelist) {
            packageName in config.whitelist
        } else {
            packageName in config.blacklist
        }
    } else {
        val prefs = context.getSharedPreferences("settings", MODE_PRIVATE)
        if (listType == AppListType.Whitelist) {
            val whitelist = prefs.getStringSet("whitelist", emptySet()) ?: emptySet()
            return packageName in whitelist
        } else {
            val blacklist = prefs.getStringSet("blacklist", emptySet()) ?: emptySet()
            return packageName in blacklist
        }
    }
}

fun getAppList(listType: AppListType, sharedPreferences: SharedPreferences, context: Context): Set<String> {
    if (sharedPreferences.getBoolean("use_module", false)) {
        val config = readConfig()
        return if (listType == AppListType.Whitelist) {
            config.whitelist.toSet()
        } else {
            config.blacklist.toSet()
        }
    } else {
        return if (listType == AppListType.Whitelist) context.getSharedPreferences("settings", MODE_PRIVATE)
            .getStringSet("whitelist", emptySet()) ?: emptySet()
        else context.getSharedPreferences("settings", MODE_PRIVATE)
            .getStringSet("blacklist", emptySet()) ?: emptySet()
    }
}

fun getAppsListMode(prefs: SharedPreferences): String {
    if (prefs.getBoolean("use_module", false)) {
        val applist = readConfig().appList
        Log.d("App list", "Equals to $applist")
        return if (applist == "whitelist" || applist == "blacklist" || applist == "none") applist
        else "none"
    } else {
        return prefs.getString("app_list", "none")!!
    }
}

fun setAppsListMode(prefs: SharedPreferences, mode: String) {
    if (prefs.getBoolean("use_module", false)) {
        val config = readConfig()
        writeConfig(config.copy(appList = mode))
    } else {
        prefs.edit { putString("app_list", mode) }
    }
    Log.d("App List", "Changed to $mode")
}

fun setHostListMode(prefs: SharedPreferences, mode: String) {
    if (prefs.getBoolean("use_module", false)) {
        val config = readConfig()
        writeConfig(config.copy(listType = mode))
    } else {
        prefs.edit { putString("list_type", mode) }
    }
    Log.d("App List", "Changed to $mode")
}

fun getHostListMode(prefs: SharedPreferences): String {
    if (prefs.getBoolean("use_module", false)) {
        val hostlist = readConfig().listType
        return if (hostlist == "whitelist" || hostlist == "blacklist") hostlist
        else "whitelist"
    } else {
        return prefs.getString("list_type", "whitelist")!!
    }
}