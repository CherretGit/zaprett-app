package com.cherret.zaprett.utils

import android.Manifest
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.cherret.zaprett.data.AppListType
import com.cherret.zaprett.data.ListType
import com.cherret.zaprett.data.RepoManifest
import com.cherret.zaprett.data.ServiceType
import com.cherret.zaprett.data.StorageData
import com.cherret.zaprett.data.ZaprettConfig
import com.topjohnwu.superuser.Shell
import io.ktor.client.plugins.cache.storage.FileStorage
import kotlinx.io.files.FileNotFoundException
import kotlinx.serialization.json.Json
import java.io.File

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
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

fun checkStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
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

fun startService(callback: (String) -> Unit) {
    Shell.cmd("zaprett start 2>&1").submit { result ->
        callback(
            if (result.isSuccess) ""
            else result.out.joinToString("\n")
        )
    }
}


fun stopService(callback: (String) -> Unit) {
    Shell.cmd("zaprett stop 2>&1").submit { result ->
        callback(
            if (result.isSuccess) ""
            else result.out.joinToString("\n")
        )
    }
}

fun restartService(callback: (String) -> Unit) {
    Shell.cmd("zaprett restart 2>&1").submit { result ->
        callback(
            if (result.isSuccess) ""
            else result.out.joinToString("\n")
        )
    }
}

fun getModuleVersion(callback: (String) -> Unit) {
    Shell.cmd("zaprett --version").submit { result ->
        callback(result.out.first())
    }
}

fun getNfqwsVersion(callback: (String) -> Unit) {
    Shell.cmd("zaprett nfqws-version").submit { result ->
        callback(result.out.first())
    }
}

fun getNfqws2Version(callback: (String) -> Unit) {
    Shell.cmd("zaprett nfqws2-version").submit { result ->
        callback(result.out.first())
    }
}

fun getConfigFile(): File {
    return getZaprettPath().resolve("config.json")
}

fun setStartOnBoot(prefs: SharedPreferences, callback: (Boolean) -> Unit) {
    if (getServiceType(prefs) != ServiceType.byedpi) {
        Shell.cmd("zaprett set-autostart").submit { result ->
            if (result.out.isNotEmpty() && result.out.toString().contains("true")) callback(true) else callback(false)
        }
    }
}

fun getStartOnBoot(prefs: SharedPreferences, callback: (Boolean) -> Unit) {
    if (getServiceType(prefs) != ServiceType.byedpi) {
        Shell.cmd("zaprett get-autostart").submit { result ->
            if (result.out.isNotEmpty() && result.out.toString().contains("true")) callback(true) else callback(false)
        }
    } else {
        callback(false)
    }
}

fun getZaprettPath(): File {
    return Environment.getExternalStorageDirectory().resolve("zaprett")
}

fun getManifestsPath(): File {
    return getZaprettPath().resolve("manifests")
}
fun getValidManifests(listsDir: File): Array<StorageData> {
    return (listsDir.listFiles()
        ?.mapNotNull { file ->
            if (!file.isFile || file.extension.lowercase() != "json") return@mapNotNull null
            parseManifestFromFile(file).getOrNull()?.takeIf {
                File(it.file).exists()
            }
        }
        ?.toTypedArray()
        ?: emptyArray())
}

fun getAllLists(): Array<StorageData> {
    val listsDir = getManifestsPath().resolve("lists/include")
    return getValidManifests(listsDir)
}

fun getAllIpsets(): Array<StorageData> {
    val listsDir = getManifestsPath().resolve("ipset/include")
    return getValidManifests(listsDir)
}

fun getAllExcludeLists(): Array<StorageData> {
    val listsDir = getManifestsPath().resolve("lists/exclude")
    return getValidManifests(listsDir)
}

fun getAllExcludeIpsets(): Array<StorageData> {
    val listsDir = getManifestsPath().resolve("ipset/exclude")
    return getValidManifests(listsDir)
}

fun getAllNfqwsStrategies(): Array<StorageData> {
    val listsDir = getManifestsPath().resolve("strategies/nfqws")
    return getValidManifests(listsDir)
}

fun getAllNfqws2Strategies(): Array<StorageData> {
    val listsDir = getManifestsPath().resolve("strategies/nfqws2")
    return getValidManifests(listsDir)
}

fun getAllByeDPIStrategies(): Array<StorageData> {
    val listsDir = getManifestsPath().resolve("strategies/byedpi")
    return getValidManifests(listsDir)
}

fun getAllStrategies(sharedPreferences: SharedPreferences): Array<StorageData> {
    return when (getServiceType(sharedPreferences)) {
        ServiceType.nfqws -> getAllNfqwsStrategies()
        ServiceType.nfqws2 -> getAllNfqws2Strategies()
        ServiceType.byedpi -> getAllByeDPIStrategies()
    }
}

fun getAllBin(): Array<StorageData> {
    val listsDir = getManifestsPath().resolve("bin")
    return getValidManifests(listsDir)
}

fun getActiveLists(sharedPreferences: SharedPreferences): Array<StorageData> {
    if (getServiceType(sharedPreferences) != ServiceType.byedpi) {
        return readConfig().activeLists.mapNotNull { parseManifestFromFile(File(it)).getOrNull() }.toTypedArray()
    } else {
        return sharedPreferences.getStringSet("lists", emptySet())!!.mapNotNull { parseManifestFromFile(File(it)).getOrNull() }.toTypedArray()
    }
}

fun getServiceType(sharedPreferences: SharedPreferences): ServiceType {
    val serviceType = runCatching { enumValueOf<ServiceType>(sharedPreferences.getString("service_type", ServiceType.byedpi.name) ?: ServiceType.byedpi.name) }.getOrDefault(ServiceType.byedpi)
    return if ( serviceType != ServiceType.byedpi) {
        readConfig().serviceType
    }
    else {
        serviceType
    }
}

fun setServiceType(sharedPreferences: SharedPreferences, serviceType: ServiceType) {
    if (serviceType != ServiceType.byedpi) {
        val config = readConfig()
        val newConfig = config.copy(serviceType = serviceType)
        writeConfig(newConfig)
    }
    sharedPreferences.edit {
        putString("service_type", serviceType.name)
    }
}

fun getActiveIpsets(sharedPreferences: SharedPreferences): Array<StorageData> {
    if (getServiceType(sharedPreferences) != ServiceType.byedpi) {
        return readConfig().activeIpsets.mapNotNull { parseManifestFromFile(File(it)).getOrNull() }.toTypedArray()
    } else return sharedPreferences.getStringSet("ipsets", emptySet())!!.mapNotNull { parseManifestFromFile(File(it)).getOrNull() }.toTypedArray()
}

fun getActiveExcludeLists(sharedPreferences: SharedPreferences): Array<StorageData> {
    if (getServiceType(sharedPreferences) != ServiceType.byedpi) {
        return readConfig().activeExcludeLists.mapNotNull { parseManifestFromFile(File(it)).getOrNull() }.toTypedArray()
    } else {
        return sharedPreferences.getStringSet("exclude_lists", emptySet())!!.mapNotNull { parseManifestFromFile(File(it)).getOrNull() }.toTypedArray()
    }
}

fun getActiveExcludeIpsets(sharedPreferences: SharedPreferences): Array<StorageData> {
    if (getServiceType(sharedPreferences) != ServiceType.byedpi) {
        return readConfig().activeExcludeIpsets.mapNotNull { parseManifestFromFile(File(it)).getOrNull() }.toTypedArray()
    } else return sharedPreferences.getStringSet("exclude_ipsets", emptySet())!!.mapNotNull { parseManifestFromFile(File(it)).getOrNull() }.toTypedArray()
}

fun getActiveNfqwsStrategy(): Result<StorageData> {
    return parseManifestFromFile(File(readConfig().strategy))
}

fun getActiveNfqws2Strategy(): Result<StorageData> {
    return parseManifestFromFile(File(readConfig().strategyNfqws2))
}

fun getActiveByeDPIStrategy(sharedPreferences: SharedPreferences): Result<StorageData> {
    val path = sharedPreferences.getString("active_strategy", "")?: ""
//    if (!path.isNullOrBlank() && File(path).exists()) {
//        return arrayOf(path)
//    }
//    return emptyArray()
    return parseManifestFromFile(File(path))

}

fun getActiveByeDPIStrategyContent(sharedPreferences: SharedPreferences): List<String> {
    val path = sharedPreferences.getString("active_strategy", "")?: ""
    if (path.isBlank()) return emptyList()
    val manifest = parseManifestFromFile(File(path)).getOrNull()
    val file = manifest?.file?.let { File(it) }
    return if (file?.exists() == true) file.readLines()
    else emptyList()
}

fun getActiveStrategy(sharedPreferences: SharedPreferences): Result<StorageData> {
    return when(getServiceType(sharedPreferences)) {
        ServiceType.byedpi -> getActiveByeDPIStrategy(sharedPreferences)
        ServiceType.nfqws -> getActiveNfqwsStrategy()
        ServiceType.nfqws2 -> getActiveNfqws2Strategy()
    }
}

fun enableList(path: String, sharedPreferences: SharedPreferences) {
    if (getServiceType(sharedPreferences) != ServiceType.byedpi) {
        val config = readConfig()
        val isWhitelist = getHostListMode(sharedPreferences) == ListType.whitelist
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
        val key = if (getHostListMode(sharedPreferences) == ListType.whitelist) "lists" else "exclude_lists"
        val currentSet = sharedPreferences.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (path !in currentSet) {
            currentSet.add(path)
            sharedPreferences.edit { putStringSet(key, currentSet) }
        }
    }
}

fun enableIpset(path: String, sharedPreferences: SharedPreferences) {
    if (getServiceType(sharedPreferences) != ServiceType.byedpi) {
        val config = readConfig()
        val isWhitelist = getHostListMode(sharedPreferences) == ListType.whitelist
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
        val key = if (getHostListMode(sharedPreferences) == ListType.whitelist) "ipsets" else "exclude_ipsets"
        val currentSet = sharedPreferences.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (path !in currentSet) {
            currentSet.add(path)
            sharedPreferences.edit { putStringSet(key, currentSet) }
        }
    }
}

fun enableStrategy(path: String, sharedPreferences: SharedPreferences) {
    when(getServiceType(sharedPreferences)) {
        ServiceType.nfqws -> {
            val config = readConfig()
            if (config.strategy != path) {
                writeConfig(config.copy(strategy = path))
            }
        }
        ServiceType.nfqws2 -> {
            val config = readConfig()
            if (config.strategyNfqws2 != path) {
                writeConfig(config.copy(strategyNfqws2 = path))
            }
        }
        ServiceType.byedpi -> {
            sharedPreferences.edit { putString("active_strategy", path)}
        }
    }
}

fun disableList(path: String, sharedPreferences: SharedPreferences) {
    if (getServiceType(sharedPreferences) != ServiceType.byedpi) {
        val config = readConfig()
        val isWhitelist = getHostListMode(sharedPreferences) == ListType.whitelist
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
        val key = if (getHostListMode(sharedPreferences) == ListType.whitelist) "lists" else "exclude_lists"
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
    if (getServiceType(sharedPreferences) != ServiceType.byedpi) {
        val config = readConfig()
        val isWhitelist = getHostListMode(sharedPreferences) == ListType.whitelist
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
        val key = if (getHostListMode(sharedPreferences) == ListType.whitelist) "ipsets" else "exclude_ipsets"
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
    when(getServiceType(sharedPreferences)) {
        ServiceType.nfqws -> {
            val config = readConfig()
            if (config.strategy == path) {
                writeConfig(config.copy(strategy = ""))
            }
        }
        ServiceType.nfqws2 -> {
            val config = readConfig()
            if (config.strategyNfqws2 == path) {
                writeConfig(config.copy(strategyNfqws2 = ""))
            }
        }
        ServiceType.byedpi -> {
            sharedPreferences.edit { remove("active_strategy") }
        }
    }
}

fun addPackageToList(listType: AppListType, packageName: String, prefs: SharedPreferences, context: Context) {
    if (getServiceType(prefs) != ServiceType.byedpi) {
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
    if (getServiceType(prefs) != ServiceType.byedpi) {
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
    if (getServiceType(prefs) != ServiceType.byedpi) {
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
    if (getServiceType(sharedPreferences) != ServiceType.byedpi) {
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
    if (getServiceType(prefs) != ServiceType.byedpi) {
        val applist = readConfig().appList
        Log.d("App list", "Equals to $applist")
        return if (applist == "whitelist" || applist == "blacklist" || applist == "none") applist
        else "none"
    } else {
        return prefs.getString("app_list", "none")!!
    }
}

fun setAppsListMode(prefs: SharedPreferences, mode: String) {
    if (getServiceType(prefs) != ServiceType.byedpi) {
        val config = readConfig()
        writeConfig(config.copy(appList = mode))
    } else {
        prefs.edit { putString("app_list", mode) }
    }
    Log.d("App List", "Changed to $mode")
}

fun setHostListMode(prefs: SharedPreferences, mode: ListType) {
    if (getServiceType(prefs) != ServiceType.byedpi) {
        val config = readConfig()
        writeConfig(config.copy(listType = mode))
    } else {
        prefs.edit { putString("list_type", mode.name) }
    }
    Log.d("App List", "Changed to $mode")
}

fun getHostListMode(prefs: SharedPreferences): ListType {
    if (getServiceType(prefs) != ServiceType.byedpi) {
        val hostlist = readConfig().listType
        return hostlist
    } else {
        return runCatching { enumValueOf<ListType>(prefs.getString("list_type", ListType.whitelist.name) ?: ListType.whitelist.name) }.getOrDefault(ListType.whitelist)
    }
}

fun parseManifestFromFile(file: File): Result<StorageData> {
    return runCatching {
        if (!file.isFile || !file.canRead()) throw IllegalArgumentException("can't find manifest file")
        val manifest = Json.decodeFromString<StorageData>(file.readText())
        manifest.manifestPath = file.path
        manifest
    }
}