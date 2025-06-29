package com.cherret.zaprett

import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import com.topjohnwu.superuser.Shell
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Properties
import androidx.core.content.edit

fun checkRoot(callback: (Boolean) -> Unit) {
    Shell.cmd("ls /").submit { result ->
        callback(result.isSuccess)
    }
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
    return File(Environment.getExternalStorageDirectory(), "zaprett/config")
}

fun setStartOnBoot(startOnBoot: Boolean) {
    val configFile = getConfigFile()
    if (configFile.exists()) {
        val props = Properties()
        try {
            FileInputStream(configFile).use { input ->
                props.load(input)
            }
            props.setProperty("autorestart", startOnBoot.toString())
            FileOutputStream(configFile).use { output ->
                props.store(output, "Don't place '/' in end of directory! Example: /sdcard")
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}

fun getStartOnBoot(): Boolean {
    val configFile = getConfigFile()
    val props = Properties()
    return try {
        if (configFile.exists()) {
            FileInputStream(configFile).use { input ->
                props.load(input)
            }
            props.getProperty("autorestart", "false").toBoolean()
        } else {
            false
        }
    } catch (_: IOException) {
        false
    }
}

fun getZaprettPath(): String {
    val props = Properties()
    val configFile = getConfigFile()
    if (configFile.exists()) {
        return try {
            FileInputStream(configFile).use { input ->
                props.load(input)
            }
            props.getProperty("zaprettdir", Environment.getExternalStorageDirectory().path + "/zaprett")
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
    return Environment.getExternalStorageDirectory().path + "/zaprett"
}

fun getAllLists(): Array<String> {
    val listsDir = File("${getZaprettPath()}/lists/")
    if (listsDir.exists() && listsDir.isDirectory) {
        val onlyNames = listsDir.list() ?: return emptyArray()
        return onlyNames.map { "$listsDir/$it" }.toTypedArray()
    }
    return emptyArray()
}

fun getAllNfqwsStrategies(): Array<String> {
    val listsDir = File("${getZaprettPath()}/strategies/nfqws")
    if (listsDir.exists() && listsDir.isDirectory) {
        val onlyNames = listsDir.list() ?: return emptyArray()
        return onlyNames.map { "$listsDir/$it" }.toTypedArray()
    }
    return emptyArray()
}

fun getAllByeDPIStrategies(): Array<String> {
    val listsDir = File("${getZaprettPath()}/strategies/byedpi")
    if (listsDir.exists() && listsDir.isDirectory) {
        val onlyNames = listsDir.list() ?: return emptyArray()
        return onlyNames.map { "$listsDir/$it" }.toTypedArray()
    }
    return emptyArray()
}

fun getActiveLists(sharedPreferences: SharedPreferences): Array<String> {
    if (sharedPreferences.getBoolean("use_module", false)) {
        val configFile = File("${getZaprettPath()}/config")
        if (configFile.exists()) {
            val props = Properties()
            return try {
                FileInputStream(configFile).use { input ->
                    props.load(input)
                }
                val activeLists = props.getProperty("activelists", "")
                Log.d("Active lists", activeLists)
                if (activeLists.isNotEmpty()) activeLists.split(",")
                    .toTypedArray() else emptyArray()
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
        return emptyArray()
    }
    else {
        return sharedPreferences.getStringSet("lists", emptySet())?.toTypedArray() ?: emptyArray()
    }
}

fun getActiveNfqwsStrategies(): Array<String> {
    val configFile = File("${getZaprettPath()}/config")
    if (configFile.exists()) {
        val props = Properties()
        return try {
            FileInputStream(configFile).use { input ->
                props.load(input)
            }
            val activeStrategies = props.getProperty("strategy", "")
            Log.d("Active strategies", activeStrategies)
            if (activeStrategies.isNotEmpty()) activeStrategies.split(",").toTypedArray() else emptyArray()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
    return emptyArray()
}

fun getActiveByeDPIStrategies(sharedPreferences: SharedPreferences): Array<String> {
    val path = sharedPreferences.getString("active_strategy", "")
    if (!path.isNullOrBlank() && File(path).exists()) {
        return arrayOf(path)
    }
    return emptyArray()
}

fun getActiveStrategy(sharedPreferences: SharedPreferences): List<String> {
    val path = sharedPreferences.getString("active_strategy", "")
    if (!path.isNullOrBlank() && File(path).exists()) {
        return File(path).readLines()
    }
    return emptyList()
}

fun enableList(path: String, sharedPreferences: SharedPreferences) {
    if (sharedPreferences.getBoolean("use-module", false)) {
        val props = Properties()
        val configFile = getConfigFile()
        try {
            if (configFile.exists()) {
                FileInputStream(configFile).use { input ->
                    props.load(input)
                }
            }
            val activeLists = props.getProperty("activelists", "")
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableList()
            if (path !in activeLists) {
                activeLists.add(path)
            }
            props.setProperty("activelists", activeLists.joinToString(","))
            FileOutputStream(configFile).use { output ->
                props.store(output, "Don't place '/' in end of directory! Example: /sdcard")
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
    else {
        val currentSet = sharedPreferences.getStringSet("lists", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (path !in currentSet) {
            currentSet.add(path)
            sharedPreferences.edit { putStringSet("lists", currentSet) }
        }
    }
}

fun enableStrategy(path: String, sharedPreferences: SharedPreferences) {
    if (sharedPreferences.getBoolean("use_module", false)) {
        val props = Properties()
        val configFile = getConfigFile()
        try {
            if (configFile.exists()) {
                FileInputStream(configFile).use { input ->
                    props.load(input)
                }
            }
            val activeStrategies = props.getProperty("strategy", "")
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableList()
            if (path !in activeStrategies) {
                activeStrategies.add(path)
            }
            props.setProperty("strategy", activeStrategies.joinToString(","))
            FileOutputStream(configFile).use { output ->
                props.store(output, "Don't place '/' in end of directory! Example: /sdcard")
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
    else {
        sharedPreferences.edit { putString("active_strategy", path) }
    }
}

fun disableList(path: String, sharedPreferences: SharedPreferences) {
    if (sharedPreferences.getBoolean("use-module", false)) {
        val props = Properties()
        val configFile = getConfigFile()
        try {
            if (configFile.exists()) {
                FileInputStream(configFile).use { input ->
                    props.load(input)
                }
            }
            val activeLists = props.getProperty("activelists", "")
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableList()
            if (path in activeLists) {
                activeLists.remove(path)
            }
            props.setProperty("activelists", activeLists.joinToString(","))
            FileOutputStream(configFile).use { output ->
                props.store(output, "Don't place '/' in end of directory! Example: /sdcard")
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
    else {
        val currentSet = sharedPreferences.getStringSet("lists", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (path in currentSet) {
            currentSet.remove(path)
            sharedPreferences.edit { putStringSet("lists", currentSet) }
        }
        if (currentSet.isEmpty()) {
            sharedPreferences.edit { remove("lists") }
        }
    }
}

fun disableStrategy(path: String, sharedPreferences: SharedPreferences) {
    if (sharedPreferences.getBoolean("use_module", false)) {
        val props = Properties()
        val configFile = getConfigFile()
        try {
            if (configFile.exists()) {
                FileInputStream(configFile).use { input ->
                    props.load(input)
                }
            }
            val activeStrategies = props.getProperty("strategy", "")
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableList()
            if (path in activeStrategies) {
                activeStrategies.remove(path)
            }
            props.setProperty("strategy", activeStrategies.joinToString(","))
            FileOutputStream(configFile).use { output ->
                props.store(output, "Don't place '/' in end of directory! Example: /sdcard")
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
    else {
        sharedPreferences.edit { remove("active_strategy") }
    }
}
