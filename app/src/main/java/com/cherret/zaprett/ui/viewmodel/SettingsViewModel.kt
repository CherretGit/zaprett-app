package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import com.cherret.zaprett.data.AppListType
import com.cherret.zaprett.utils.addPackageToList
import com.cherret.zaprett.utils.getAppList
import com.cherret.zaprett.utils.removePackageFromList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application
    private val prefs = context.getSharedPreferences("settings", MODE_PRIVATE)
    private val _appsList = MutableStateFlow<List<String>>(emptyList())
    val appsList: StateFlow<List<String>> = _appsList
    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()
    private val _currentListType = MutableStateFlow(AppListType.Whitelist)

    init {
        refreshApplications()
    }

    suspend fun getAppIconBitmap(packageName: String): Drawable? = withContext(Dispatchers.IO) {
        val pm: PackageManager = context.packageManager
        val drawable: Drawable = try {
            pm.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            return@withContext null
        }
        return@withContext drawable
    }

    fun getApplicationName(packageName: String) : String? {
        val pm: PackageManager = context.packageManager
        return try {
            val applicationInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun setListType(type: AppListType) {
        _currentListType.value = type
        refreshApplications()
    }

    fun clearList() {
        _appsList.value = emptyList()
        _selectedPackages.value = emptySet()
    }

    fun refreshApplications() {
        val packages = if (prefs.getBoolean("show_system_apps", false)){
            context.packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        }
        else {
            context.packageManager.getInstalledPackages(PackageManager.GET_META_DATA).filter { pkgInfo ->
                (pkgInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) == 0
            }
        }
        val allPackages = packages.map { it.packageName }
        val listType = _currentListType.value

        val result = when (listType) {
            AppListType.Whitelist -> {
                val whitelistSet = getAppList(AppListType.Whitelist, prefs, context)
                val (whitelisted, others) = allPackages.partition { it in whitelistSet }
                _selectedPackages.value = whitelistSet
                (whitelisted + others).filter { it != context.packageName }
            }
            AppListType.Blacklist -> {
                val blacklistSet = getAppList(AppListType.Blacklist, prefs, context)
                val (blacklisted, others) = allPackages.partition { it in blacklistSet }
                _selectedPackages.value = blacklistSet
                (blacklisted + others).filter { it != context.packageName }
            }
        }
        _appsList.value = result
    }

    fun addToList(listType: AppListType, packageName: String) {
        addPackageToList(listType, packageName, prefs, context)
        selectedPackages.value.plus(packageName)
        refreshApplications()
    }

    fun removeFromList(listType: AppListType, packageName: String) {
        removePackageFromList(listType, packageName, prefs, context)
        selectedPackages.value.minus(packageName)
        refreshApplications()
    }

}