package com.cherret.zaprett.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cherret.zaprett.R
import com.cherret.zaprett.checkModuleInstallation
import com.cherret.zaprett.checkRoot
import com.cherret.zaprett.getStartOnBoot
import com.cherret.zaprett.setStartOnBoot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val editor = remember { sharedPreferences.edit() }

    val useModule = remember { mutableStateOf(sharedPreferences.getBoolean("use_module", false)) }
    val updateOnBoot = remember { mutableStateOf(sharedPreferences.getBoolean("update_on_boot", false)) }
    val autoRestart = remember { mutableStateOf(getStartOnBoot()) }
    val autoUpdate = remember { mutableStateOf(sharedPreferences.getBoolean("auto_update", true)) }
    val openNoRootDialog = remember { mutableStateOf(false) }
    val openNoModuleDialog = remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    if (openNoRootDialog.value) {
        InfoDialog(
            title = stringResource(R.string.error_root_title),
            message = stringResource(R.string.error_root_message),
            onDismiss = { openNoRootDialog.value = false }
        )
    }

    if (openNoModuleDialog.value) {
        InfoDialog(
            title = stringResource(R.string.error_no_module_title),
            message = stringResource(R.string.error_no_module_message),
            onDismiss = { openNoModuleDialog.value = false }
        )
    }

    if (showAboutDialog) {
        AboutDialog(context, onDismiss = { showAboutDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_settings),
                        fontSize = 40.sp,
                        fontFamily = FontFamily(Font(R.font.unbounded, FontWeight.Normal))
                    )
                },
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.about_title)) },
                            onClick = {
                                expanded = false
                                showAboutDialog = true
                            }
                        )
                    }
                },
                windowInsets = WindowInsets(0)
            )
        },
        content = { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                ElevatedCard(
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 25.dp)
                ) {
                    SettingsItem(
                        title = stringResource(R.string.btn_use_root),
                        checked = useModule.value,
                        onCheckedChange = { isChecked ->
                            useModule(
                                context = context,
                                checked = isChecked,
                                updateOnBoot = updateOnBoot,
                                openNoRootDialog = openNoRootDialog,
                                openNoModuleDialog = openNoModuleDialog
                            ) { success ->
                                if (success) useModule.value = isChecked
                            }
                        }
                    )
                    SettingsItem(
                        title = stringResource(R.string.btn_update_on_boot),
                        checked = updateOnBoot.value,
                        onCheckedChange = {
                            updateOnBoot.value = it
                            editor.putBoolean("update_on_boot", it).apply()
                        }
                    )
                    SettingsItem(
                        title = stringResource(R.string.btn_autorestart),
                        checked = autoRestart.value,
                        onCheckedChange = {
                            if (handleAutoRestart(context, it)) autoRestart.value = it
                        }
                    )
                    SettingsItem(
                        title = stringResource(R.string.btn_autoupdate),
                        checked = autoUpdate.value,
                        onCheckedChange = {
                            autoUpdate.value = it
                            editor.putBoolean("auto_update", it).apply()
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun SettingsItem(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private fun useModule(context: Context, checked: Boolean, updateOnBoot: MutableState<Boolean>, openNoRootDialog: MutableState<Boolean>, openNoModuleDialog: MutableState<Boolean>, callback: (Boolean) -> Unit) {
    val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    if (checked) {
        checkRoot { hasRoot ->
            if (hasRoot) {
                checkModuleInstallation { hasModule ->
                    if (hasModule) {
                        editor.putBoolean("use_module", true)
                            .putBoolean("update_on_boot", true)
                            .apply()
                        updateOnBoot.value = true
                        callback(true)
                    } else {
                        openNoModuleDialog.value = true
                    }
                }
            } else {
                openNoRootDialog.value = true
            }
        }
    } else {
        editor.putBoolean("use_module", false)
            .putBoolean("update_on_boot", false)
            .apply()
        updateOnBoot.value = false
        callback(true)
    }
}

private fun handleAutoRestart(context: Context, checked: Boolean): Boolean {
    val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    return if (sharedPreferences.getBoolean("use_module", false)) {
        setStartOnBoot(checked)
        true
    } else {
        false
    }
}

@Composable
private fun InfoDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        title = { Text(text = title) },
        text = { Text(text = message) },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_continue))
            }
        }
    )
}

@Composable
private fun AboutDialog(context: Context, onDismiss: () -> Unit) {
    AlertDialog(
        title = { Text(text = stringResource(R.string.about_title)) },
        icon = {Icon(painterResource(R.drawable.ic_launcher_monochrome), contentDescription = stringResource(R.string.app_name), modifier = Modifier
            .size(64.dp))},
        text = { Text(text = stringResource(R.string.about_text, context.packageManager.getPackageInfo(context.packageName, 0).versionName.toString())) },
        onDismissRequest = onDismiss,
        confirmButton = { }
    )
}