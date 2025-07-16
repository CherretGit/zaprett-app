package com.cherret.zaprett.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.cherret.zaprett.R
import com.cherret.zaprett.byedpi.ByeDpiVpnService
import com.cherret.zaprett.byedpi.ServiceStatus
import com.cherret.zaprett.utils.download
import com.cherret.zaprett.utils.getActiveStrategy
import com.cherret.zaprett.utils.getBinVersion
import com.cherret.zaprett.utils.getChangelog
import com.cherret.zaprett.utils.getModuleVersion
import com.cherret.zaprett.utils.getStatus
import com.cherret.zaprett.utils.getUpdate
import com.cherret.zaprett.utils.installApk
import com.cherret.zaprett.utils.registerDownloadListener
import com.cherret.zaprett.utils.restartService
import com.cherret.zaprett.utils.startService
import com.cherret.zaprett.utils.stopService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _requestVpnPermission = MutableStateFlow(false)
    val requestVpnPermission = _requestVpnPermission.asStateFlow()
    var cardText = mutableIntStateOf(R.string.status_not_availible) // MVP temporarily(maybe)
        private set
    var cardIcon = mutableStateOf(Icons.AutoMirrored.Filled.Help)
        private set

    var moduleVer = mutableStateOf(context.getString(R.string.unknown_text))
        private set

    var nfqwsVer = mutableStateOf(context.getString(R.string.unknown_text))
        private set

    var byedpiVer = mutableStateOf("0.17.2")
        private set

    var serviceMode = mutableIntStateOf(R.string.service_mode_ciadpi)
        private set

    var changeLog = mutableStateOf<String?>(null)
        private set

    var newVersion = mutableStateOf<String?>(null)
        private set

    var updateAvailable = mutableStateOf(false)
        private set

    var downloadUrl = mutableStateOf<String?>(null)
        private set

    var showUpdateDialog = mutableStateOf(false)

    fun checkForUpdate() {
        if (prefs.getBoolean("auto_update", true)) {
            getUpdate {
                if (it != null) {
                    downloadUrl.value = it.downloadUrl.toString()
                    getChangelog(it.changelogUrl.toString()) { log -> changeLog.value = log }
                    newVersion.value = it.version
                    updateAvailable.value = true
                }
            }
        }
    }

    fun checkServiceStatus() {
        if (prefs.getBoolean("use_module", false) && prefs.getBoolean("update_on_boot", false)) {
            getStatus { isEnabled ->
                if (isEnabled){
                    cardText.intValue = R.string.status_enabled
                    cardIcon.value = Icons.Filled.CheckCircle
                }
                else {
                    cardText.intValue = R.string.status_disabled
                    cardIcon.value = Icons.Filled.Cancel
                }
            }
        }
        else {
            if (ByeDpiVpnService.status == ServiceStatus.Connected){
                cardText.intValue = R.string.status_enabled
                cardIcon.value = Icons.Filled.CheckCircle
            }
            else {
                cardText.intValue = R.string.status_disabled
                cardIcon.value = Icons.Filled.Cancel
            }
        }
    }

    fun onCardClick() {
        if (prefs.getBoolean("use_module", false)) {
            getStatus { isEnabled ->
                if (isEnabled){
                    cardText.intValue = R.string.status_enabled
                    cardIcon.value = Icons.Filled.CheckCircle
                }
                else {
                    cardText.intValue = R.string.status_disabled
                    cardIcon.value = Icons.Filled.Cancel
                }
            }
        } else {
            if (ByeDpiVpnService.status == ServiceStatus.Connected){
                cardText.intValue = R.string.status_enabled
                cardIcon.value = Icons.Filled.CheckCircle
            }
            else {
                cardText.intValue = R.string.status_disabled
                cardIcon.value = Icons.Filled.Cancel
            }
        }
    }

    fun startVpn() {
        ContextCompat.startForegroundService(context, Intent(context, ByeDpiVpnService::class.java).apply { action = "START_VPN" })
    }

    fun onBtnStartService(snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        if (prefs.getBoolean("use_module", false)) {
            getStatus { isEnabled ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(
                            if (isEnabled) R.string.snack_already_started else R.string.snack_starting_service
                        )
                    )
                }
                if (!isEnabled) startService {}
            }
        } else {
            if (ByeDpiVpnService.status == ServiceStatus.Disconnected || ByeDpiVpnService.status == ServiceStatus.Failed) {
                if (getActiveStrategy(prefs).isNotEmpty()) {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.snack_starting_service))
                    }
                    _requestVpnPermission.value = true
                }
                else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_no_strategy_selected),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            else {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.snack_already_started))
                }
            }
        }
    }

    fun clearVpnPermissionRequest() {
        _requestVpnPermission.value = false
    }

    fun onBtnStopService(snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        if (prefs.getBoolean("use_module", false)) {
            getStatus { isEnabled ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(
                            if (isEnabled) R.string.snack_stopping_service else R.string.snack_no_service
                        )
                    )
                }
                if (isEnabled) stopService {}
            }
        } else {
            if (ByeDpiVpnService.status == ServiceStatus.Connected) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.snack_stopping_service)
                    )
                }
                context.startService(Intent(context, ByeDpiVpnService::class.java).apply {
                    action = "STOP_VPN"
                })
            }
            else {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.snack_no_service))
                }
            }
        }
    }

    fun onBtnRestart(snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        if (prefs.getBoolean("use_module", false)) {
            restartService {}
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.snack_reload))
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.snack_module_disabled))
            }
        }
    }

    fun checkModuleInfo() {
        if (prefs.getBoolean("use_module", false)) {
            getModuleVersion { value ->
                moduleVer.value = value
            }
            getBinVersion { value ->
                nfqwsVer.value = value
            }
            serviceMode.intValue = R.string.service_mode_nfqws;
        }
    }

    fun showUpdateDialog() {
        showUpdateDialog.value = true
    }

    fun dismissUpdateDialog() {
        showUpdateDialog.value = false
    }

    fun onUpdateConfirm() {
        showUpdateDialog.value = false
        if (context.packageManager.canRequestPackageInstalls()){
            val id = download(context, downloadUrl.value.orEmpty())
            registerDownloadListener(context, id) { uri ->
                installApk(context, uri)
            }
        }
        else {
            val packageUri = Uri.fromParts("package", context.packageName, null)
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun parseArgs(ip: String, port: String, lines: List<String>): Array<String> {
        val regex = Regex("""--?\S+(?:=(?:[^"'\s]+|"[^"]*"|'[^']*'))?|[^\s]+""")
        val parsedArgs = lines
            .flatMap { line -> regex.findAll(line).map { it.value } }
        return arrayOf("ciadpi", "--ip", ip, "--port", port) + parsedArgs
    }

}