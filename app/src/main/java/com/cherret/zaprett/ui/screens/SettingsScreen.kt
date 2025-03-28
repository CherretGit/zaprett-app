package com.cherret.zaprett.ui.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
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
    showNoRootDialog(openNoRootDialog)
    showNoModuleDialog(openNoModuleDialog)
    Scaffold(
        topBar = {
            val primaryColor = MaterialTheme.colorScheme.surfaceVariant
            Box(modifier = Modifier.padding(start = 20.dp, top = 10.dp)) {
                Canvas(modifier = Modifier.size(100.dp, 50.dp)) {
                    rotate(degrees = -30f) {
                        drawOval(
                            color = primaryColor,
                            size = Size(200f, 140f),
                            topLeft = Offset(-30f, -30f)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.title_settings),
                    fontSize = 40.sp,
                    fontFamily = FontFamily(Font(R.font.unbounded, FontWeight.Normal))
                )
            }
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
            ) {
                ElevatedCard(
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 6.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, top = 25.dp, end = 10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_use_root),
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = useModule.value,
                            onCheckedChange = { isChecked ->
                                useModule(
                                    context,
                                    isChecked,
                                    updateOnBoot,
                                    openNoRootDialog,
                                    openNoModuleDialog
                                ) {
                                    if (it) {
                                        useModule.value = isChecked
                                    }
                                }
                            }
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_update_on_boot),
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = updateOnBoot.value,
                            onCheckedChange = { updateOnBoot.value = it; editor.putBoolean("update_on_boot", it).apply()}
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_autorestart),
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = autoRestart.value,
                            onCheckedChange = { if (autoRestart(context, it)) autoRestart.value = it;}
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_autoupdate),
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = autoUpdate.value,
                            onCheckedChange = { autoUpdate.value = it; editor.putBoolean("auto_update", it).apply()}
                        )
                    }
                }
            }
        }
    )
}

fun useModule(context: Context, checked: Boolean, updateOnBoot: MutableState<Boolean>, openNoRootDialog: MutableState<Boolean>, openNoModuleDialog: MutableState<Boolean>, callback: (Boolean) -> Unit): Boolean {
    val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    if (checked) {
        checkRoot {
            if (it) {
                checkModuleInstallation {
                    if (it) {
                        editor.putBoolean("use_module", true).putBoolean("update_on_boot", true).apply()
                        updateOnBoot.value = true
                        callback(true)
                    }
                    else {
                        openNoModuleDialog.value = true
                    }
                }
            }
            else {
                openNoRootDialog.value = true
            }
        }
    }
    else {
        editor.putBoolean("use_module", false).putBoolean("update_on_boot", false).apply()
        updateOnBoot.value = false
        return true
    }
    return false
}

fun autoRestart(context: Context, checked: Boolean): Boolean {
    if (context.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("use_module", false)) {
        setStartOnBoot(checked)
        return true
    }
    return false
}

@Composable
fun showNoRootDialog(openDialog: MutableState<Boolean>) {
    if (openDialog.value) {
        AlertDialog(
            title = {
                Text(text = stringResource(R.string.error_root_title))
            },
            text = {
                Text(text = stringResource(R.string.error_root_message))
            },
            onDismissRequest = {
                openDialog.value = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDialog.value = false
                    }
                ) {
                    Text(stringResource(R.string.btn_continue))
                }
            },
        )
    }
}

@Composable
fun showNoModuleDialog(openDialog: MutableState<Boolean>) {
    if (openDialog.value) {
        AlertDialog(
            title = {
                Text(text = stringResource(R.string.error_no_module_title))
            },
            text = {
                Text(text = stringResource(R.string.error_no_module_message))
            },
            onDismissRequest = {
                openDialog.value = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDialog.value = false
                    }
                ) {
                    Text(stringResource(R.string.btn_continue))
                }
            },
        )
    }
}