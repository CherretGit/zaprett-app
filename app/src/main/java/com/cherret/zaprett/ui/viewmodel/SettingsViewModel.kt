package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.runtime.MutableState
import androidx.lifecycle.AndroidViewModel
import com.cherret.zaprett.byedpi.ByeDpiVpnService
import com.cherret.zaprett.data.AppListType
import com.cherret.zaprett.data.ServiceStatus
import com.cherret.zaprett.data.ServiceType
import com.cherret.zaprett.utils.addPackageToList
import com.cherret.zaprett.utils.checkModuleInstallation
import com.cherret.zaprett.utils.checkRoot
import com.cherret.zaprett.utils.getAppList
import com.cherret.zaprett.utils.getServiceType
import com.cherret.zaprett.utils.getStartOnBoot
import com.cherret.zaprett.utils.removePackageFromList
import com.cherret.zaprett.utils.setServiceType
import com.cherret.zaprett.utils.setStartOnBoot
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
    private val _serviceType = MutableStateFlow(ServiceType.byedpi)
    val serviceType: StateFlow<ServiceType> = _serviceType

    private val _autoRestart = MutableStateFlow(false)
    val autoRestart: StateFlow<Boolean> = _autoRestart

    init {
        refreshApplications()
        _serviceType.value = getServiceType(prefs)
        getStartOnBoot(prefs) { value ->
            _autoRestart.value = value
        }
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

    fun changeServiceType(context: Context, serviceType: ServiceType, openNoRootDialog: MutableState<Boolean>, openNoModuleDialog: MutableState<Boolean>) {
        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        when (serviceType) {
            ServiceType.nfqws, ServiceType.nfqws2 -> {
                checkRoot { hasRoot ->
                    if (hasRoot) {
                        checkModuleInstallation { hasModule ->
                            if (hasModule) {
                                setServiceType(sharedPreferences, serviceType)
                                if (ByeDpiVpnService.status == ServiceStatus.Connected) {
                                    context.startService(Intent(context, ByeDpiVpnService::class.java).apply {
                                        action = "STOP_VPN"
                                    })
                                }
                                _serviceType.value = serviceType
                            } else {
                                openNoModuleDialog.value = true
                            }
                        }
                    } else {
                        openNoRootDialog.value = true
                    }
                }
            }
            ServiceType.byedpi -> {
                setServiceType(sharedPreferences, serviceType)
                _serviceType.value = ServiceType.byedpi
            }
        }
    }

    fun handleAutoRestart(context: Context) {
        val sharedPreferences = context.getSharedPreferences("settings", MODE_PRIVATE)
        if (getServiceType(sharedPreferences) != ServiceType.byedpi) {
            setStartOnBoot(prefs) { value ->
                _autoRestart.value = value
            }
        }
    }
}