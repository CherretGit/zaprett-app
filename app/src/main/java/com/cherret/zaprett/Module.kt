package com.cherret.zaprett

import android.os.Environment
import android.util.Log
import com.topjohnwu.superuser.Shell
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Properties

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
    } catch (e: IOException) {
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
            props.getProperty("zaprettdir", "/sdcard/zaprett")
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
    return "/sdcard/zaprett"
}

fun getAllLists(): Array<String> {
    val listsDir = File("${getZaprettPath()}/lists/")
    if (listsDir.exists() && listsDir.isDirectory) {
        val onlyNames = listsDir.list() ?: return emptyArray()
        return onlyNames.map { "$listsDir/$it" }.toTypedArray()
    }
    return emptyArray()
}

fun getActiveLists(): Array<String> {
    val configFile = File("${getZaprettPath()}/config")
    if (configFile.exists()) {
        val props = Properties()
        return try {
            FileInputStream(configFile).use { input ->
                props.load(input)
            }
            val activeLists = props.getProperty("activelists", "")
            Log.d("Active lists", activeLists)
            if (activeLists.isNotEmpty()) activeLists.split(",").toTypedArray() else emptyArray()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
    return emptyArray()
}

fun enableList(path: String) {
    val props = Properties()
    val configFile = getConfigFile()
    try {
        if (configFile.exists()) {
            FileInputStream(configFile).use { input ->
                props.load(input)
            }
        }
        val activeLists = props.getProperty("activelists", "").split(",").toMutableList()
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

fun disableList(path: String) {
    val props = Properties()
    val configFile = getConfigFile()
    try {
        if (configFile.exists()) {
            FileInputStream(configFile).use { input ->
                props.load(input)
            }
        }
        val activeLists = props.getProperty("activelists", "").split(",").toMutableList()
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