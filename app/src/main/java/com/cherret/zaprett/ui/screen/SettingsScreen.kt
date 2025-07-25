package com.cherret.zaprett.ui.screen

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.cherret.zaprett.BuildConfig
import com.cherret.zaprett.byedpi.ByeDpiVpnService
import com.cherret.zaprett.R
import com.cherret.zaprett.data.ServiceStatus
import com.cherret.zaprett.data.AppListType
import com.cherret.zaprett.ui.viewmodel.SettingsViewModel
import com.cherret.zaprett.utils.checkModuleInstallation
import com.cherret.zaprett.utils.checkRoot
import com.cherret.zaprett.utils.getAppsListMode
import com.cherret.zaprett.utils.getStartOnBoot
import com.cherret.zaprett.utils.setAppsListMode
import com.cherret.zaprett.utils.setStartOnBoot
import com.cherret.zaprett.utils.stopService
import androidx.core.content.edit
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel : SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val editor = remember { sharedPreferences.edit() }
    val useModule = remember { mutableStateOf(sharedPreferences.getBoolean("use_module", false)) }
    val updateOnBoot = remember { mutableStateOf(sharedPreferences.getBoolean("update_on_boot", false)) }
    val autoRestart = remember { mutableStateOf(getStartOnBoot()) }
    val autoUpdate = remember { mutableStateOf(sharedPreferences.getBoolean("auto_update", true)) }
    val sendFirebaseAnalytics = remember { mutableStateOf(sharedPreferences.getBoolean("send_firebase_analytics", true)) }
    val ipv6 = remember { mutableStateOf(sharedPreferences.getBoolean("ipv6",false)) }
    val openNoRootDialog = remember { mutableStateOf(false) }
    val openNoModuleDialog = remember { mutableStateOf(false) }
    val showAboutDialog = remember { mutableStateOf(false) }
    val showHostsRepoUrlDialog = remember { mutableStateOf(false) }
    val showStrategyRepoUrlDialog = remember { mutableStateOf(false) }
    val showIPDialog = remember { mutableStateOf(false) }
    val showPortDialog = remember { mutableStateOf(false) }
    val showDNSDialog = remember { mutableStateOf(false) }
    val textDialogValue = remember { mutableStateOf("") }
    val showWhiteDialog = remember { mutableStateOf(false) }
    val showBlackDialog = remember { mutableStateOf(false) }
    val showAppsListsSheet = remember { mutableStateOf(false) }
    val showSystemApps = remember { mutableStateOf(sharedPreferences.getBoolean("show_system_apps", false)) }

    val settingsList = listOf(
        Setting.Section(stringResource(R.string.general_section)),
        Setting.Toggle(
            title = stringResource(R.string.btn_use_root),
            checked = useModule.value,
            onToggle = { isChecked ->
                useModule(
                    context = context,
                    checked = isChecked,
                    updateOnBoot = updateOnBoot,
                    openNoRootDialog = openNoRootDialog,
                    openNoModuleDialog = openNoModuleDialog
                ) { success ->
                    if (success) {
                        useModule.value = isChecked
                        if (!isChecked) stopService {  }
                    }
                }
            }
        ),
        Setting.Toggle(
            title = stringResource(R.string.btn_update_on_boot),
            checked = updateOnBoot.value,
            onToggle = {
                updateOnBoot.value = it
                editor.putBoolean("update_on_boot", it).apply()
            }
        ),
        Setting.Toggle(
            title = stringResource(R.string.btn_autoupdate),
            checked = autoUpdate.value,
            onToggle = {
                autoUpdate.value = it
                editor.putBoolean("auto_update", it).apply()
            }
        ),
        Setting.Toggle(
            title = stringResource(R.string.btn_send_firebase_analytics),
            checked = sendFirebaseAnalytics.value,
            onToggle = {
                sendFirebaseAnalytics.value = it
                editor.putBoolean("send_firebase_analytics", it).apply()
            }
        ),
        Setting.Action(
            title = stringResource(R.string.btn_repository_url_lists),
            onClick = {
                textDialogValue.value = sharedPreferences.getString("hosts_repo_url", "https://raw.githubusercontent.com/CherretGit/zaprett-hosts-repo/refs/heads/main/hosts.json") ?: "https://raw.githubusercontent.com/CherretGit/zaprett-hosts-repo/refs/heads/main/hosts.json"
                showHostsRepoUrlDialog.value = true
            }
        ),
        Setting.Action(
            title = stringResource(R.string.btn_repository_url_strategies),
            onClick = {
                textDialogValue.value = sharedPreferences.getString("strategy_repo_url", "https://raw.githubusercontent.com/CherretGit/zaprett-hosts-repo/refs/heads/main/strategies.json") ?: "https://raw.githubusercontent.com/CherretGit/zaprett-hosts-repo/refs/heads/main/strategies.json"
                showStrategyRepoUrlDialog.value = true
            }
        ),
        Setting.Section(
            title = stringResource(R.string.shared_section)
        ),
        Setting.Action(
            title = stringResource(R.string.btn_applist),
            onClick = {
                showAppsListsSheet.value = true
            }
        ),
        Setting.Action(
            title = stringResource(R.string.btn_whitelist),
            onClick = {
                showWhiteDialog.value = true
            }
        ),
        Setting.Action(
            title = stringResource(R.string.btn_blacklist),
            onClick = {
                showBlackDialog.value = true
            }
        ),
        Setting.Section(stringResource(R.string.byedpi_section)),
        Setting.Toggle(
            title = stringResource(R.string.btn_ipv6),
            checked = ipv6.value,
            onToggle = {
                ipv6.value = it
                editor.putBoolean("ipv6", it).apply()
            }
        ),
        Setting.Action(
            title = stringResource(R.string.btn_ip),
            onClick = {
                textDialogValue.value = sharedPreferences.getString("ip", "127.0.0.1") ?: "127.0.0.1"
                showIPDialog.value = true
            }
        ),
        Setting.Action(
            title = stringResource(R.string.btn_port),
            onClick = {
                textDialogValue.value = sharedPreferences.getString("port", "1080") ?: "1080"
                showPortDialog.value = true
            }
        ),
        Setting.Action(
            title = stringResource(R.string.btn_dns),
            onClick = {
                textDialogValue.value = sharedPreferences.getString("dns", "8.8.8.8") ?: "8.8.8.8"
                showDNSDialog.value = true
            }
        ),
        Setting.Section(
            title = stringResource(R.string.zapret_section)
        ),
        Setting.Toggle(
            title = stringResource(R.string.btn_autorestart),
            checked = autoRestart.value,
            onToggle = {
                if (handleAutoRestart(context, it)) autoRestart.value = it
            }
        )
    )

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

    if (showAboutDialog.value) {
        AboutDialog(onDismiss = { showAboutDialog.value = false })
    }

    if (showHostsRepoUrlDialog.value) {
        TextDialog(stringResource(R.string.btn_repository_url_lists), stringResource(R.string.hint_enter_repository_url_lists), textDialogValue.value, onConfirm = {
            editor.putString("hosts_repo_url", it).apply()
        }, onDismiss = { showHostsRepoUrlDialog.value = false })
    }

    if (showStrategyRepoUrlDialog.value) {
        TextDialog(stringResource(R.string.btn_repository_url_strategies), stringResource(R.string.hint_enter_repository_url_strategies), textDialogValue.value, onConfirm = {
            editor.putString("strategy_repo_url", it).apply()
        }, onDismiss = { showStrategyRepoUrlDialog.value = false })
    }

    if (showIPDialog.value) {
        TextDialog(stringResource(R.string.btn_ip), stringResource(R.string.hint_ip), textDialogValue.value, onConfirm = {
            editor.putString("ip", it).apply()
        }, onDismiss = { showIPDialog.value = false })
    }

    if (showPortDialog.value) {
        TextDialog(stringResource(R.string.btn_port), stringResource(R.string.hint_port), textDialogValue.value, onConfirm = {
            editor.putString("port", it).apply()
        }, onDismiss = { showPortDialog.value = false })
    }

    if (showDNSDialog.value) {
        TextDialog(stringResource(R.string.btn_dns), stringResource(R.string.hint_dns), textDialogValue.value, onConfirm = {
            editor.putString("dns", it).apply()
        }, onDismiss = { showDNSDialog.value = false })
    }

    if (showWhiteDialog.value) {
        ChooseAppsDialog(
            onDismissRequest = {
                showWhiteDialog.value = false
                viewModel.clearList()
            },
            viewModel = viewModel,
            listType = AppListType.Whitelist,
            sharedPreferences,
            showSystemApps
        )
    }

    if (showBlackDialog.value) {
        ChooseAppsDialog(
            onDismissRequest = {
                showBlackDialog.value = false
                viewModel.clearList()
            },
            viewModel = viewModel,
            listType = AppListType.Blacklist,
            sharedPreferences,
            showSystemApps
        )
    }

    if (showAppsListsSheet.value) {
        ListBottomSheet(
            onDismissRequest = {
                showAppsListsSheet.value = false
            },
            prefs = sharedPreferences,
            context
        )
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
                                showAboutDialog.value = true
                            }
                        )
                    }
                },
                windowInsets = WindowInsets(0)
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 25.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(settingsList) { setting ->
                    when (setting) {
                        is Setting.Toggle -> {
                            SettingsItem(
                                title = setting.title,
                                onToggle = setting.onToggle,
                                checked = setting.checked,
                                onCheckedChange = setting.onToggle
                            )
                        }
                        is Setting.Action -> {
                            SettingsTextItem(
                                title = setting.title,
                                setting.onClick
                            )
                        }
                        is Setting.Section -> {
                            SettingsSection(setting.title)
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun SettingsItem(title: String, checked: Boolean, onToggle: (Boolean) -> Unit, onCheckedChange: (Boolean) -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onToggle(!checked) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
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
}

@Composable
private fun SettingsTextItem(title: String, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f)
            )
            Icon(imageVector = Icons.AutoMirrored.Default.ArrowForward, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListBottomSheet(
    onDismissRequest: () -> Unit,
    prefs : SharedPreferences,
    context: Context
){
    val selectedOption = remember { mutableStateOf(getAppsListMode(prefs)) }
    ModalBottomSheet(
        onDismissRequest = { onDismissRequest() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .selectable(
                        selected = selectedOption.value=="none",
                        onClick = {
                            setAppsListMode(prefs, "none")
                            selectedOption.value = "none"
                        },
                        role = Role.RadioButton
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedOption.value.equals("none"),
                    onClick = null
                )
                Text(stringResource(R.string.radio_disabed), modifier = Modifier.padding(start = 8.dp))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .selectable(
                        selected = selectedOption.value=="whitelist",
                        onClick = {
                            setAppsListMode(prefs, "whitelist")
                            selectedOption.value = "whitelist"
                        },
                        role = Role.RadioButton
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedOption.value.equals("whitelist"),
                    onClick = null
                )
                Text(stringResource(R.string.title_whitelist), modifier = Modifier.padding(start = 8.dp))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .selectable(
                        selected = selectedOption.value=="blacklist",
                        onClick = {
                            setAppsListMode(prefs, "blacklist")
                            selectedOption.value = "blacklist"
                        },
                        role = Role.RadioButton
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedOption.value.equals("blacklist"),
                    onClick = null
                )
                Text(stringResource(R.string.title_blacklist), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
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
                        if (ByeDpiVpnService.status == ServiceStatus.Connected) {
                            context.startService(Intent(context, ByeDpiVpnService::class.java).apply {
                                action = "STOP_VPN"
                            })
                        }
                        editor.remove("lists").apply()
                        editor.remove("active_strategy").apply()
                        editor.remove("applist").apply()
                        editor.remove("whitelist").apply()
                        editor.remove("blacklist").apply()
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
private fun TextDialog(title: String, message: String, initialText: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var inputText by remember { mutableStateOf(initialText) }
    AlertDialog(
        title = { Text(text = title) },
        text = {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(message) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )},
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (inputText.isNotEmpty()) {
                    onConfirm(inputText)
                    onDismiss()
                }
                else {
                    onDismiss()
                }
            }
            ) {
                Text(stringResource(R.string.btn_continue))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() }
            ) {
                Text(stringResource(R.string.btn_dismiss))
            }
        }
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        title = { Text(text = stringResource(R.string.about_title)) },
        icon = {Icon(painterResource(R.drawable.ic_launcher_monochrome), contentDescription = stringResource(R.string.app_name), modifier = Modifier
            .size(64.dp))},
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(R.string.about_text, BuildConfig.VERSION_NAME))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW,
                            "https://github.com/CherretGit/zaprett-app".toUri())
                        context.startActivity(intent)
                    }) {
                        Icon(painterResource(R.drawable.github), "GitHub")
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW,
                            "https://t.me/zaprett_module".toUri())
                        context.startActivity(intent)
                    }) {
                        Icon(painterResource(R.drawable.telegram), "Telegram")
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW,
                            "https://matrix.to/#/#zaprett-group:matrix.cherret.ru".toUri())
                        context.startActivity(intent)
                    }) {
                        Icon(painterResource(R.drawable.matrix), "Matrix")
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW,
                            "https://pay.cloudtips.ru/p/672192fd".toUri())
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.AttachMoney, "Donate")
                    }
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = { }
    )
}

@Composable
private fun ChooseAppsDialog(
    onDismissRequest: () -> Unit,
    viewModel: SettingsViewModel,
    listType: AppListType,
    prefs: SharedPreferences,
    showSystemApps : MutableState<Boolean>
) {
    val appsList by viewModel.appsList.collectAsState()
    val selectedPackages by viewModel.selectedPackages.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val filteredList = remember(searchQuery, appsList) {
        if (searchQuery.isBlank()) appsList
        else appsList.filter { it.contains(searchQuery, ignoreCase = true) }
    }
    var expanded by remember { mutableStateOf(false) }
    val title = if (listType == AppListType.Whitelist) stringResource(R.string.title_whitelist) else stringResource(R.string.title_blacklist)
    LaunchedEffect(listType) {
        viewModel.setListType(listType)
    }
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Box {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.btn_show_system_apps),
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Checkbox(
                                            checked = showSystemApps.value,
                                            onCheckedChange = null
                                        )
                                    }
                                },
                                onClick = {
                                    prefs.edit { putBoolean("show_system_apps", !showSystemApps.value) }
                                    showSystemApps.value = !showSystemApps.value
                                    viewModel.refreshApplications()
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Row {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.search_field)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(filteredList) {
                        AppItem(viewModel(), it, selectedPackages.contains(it), { isChecked ->
                            if (isChecked){ viewModel.addToList(listType, it) }
                            else { viewModel.removeFromList(listType, it) }
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onDismissRequest() },
                        modifier = Modifier.padding(8.dp, top = 4.dp),
                    ) {
                        Text(text = stringResource(R.string.btn_continue))
                    }
                }
            }
        }
    }
}

@Composable
private fun AppItem(viewModel: SettingsViewModel, packageName : String, enabled : Boolean, onCheckedChange: (Boolean) -> Unit) {
    var bitmap by remember { mutableStateOf<Drawable?>(null) }
    LaunchedEffect(packageName) {
        bitmap = viewModel.getAppIconBitmap(packageName)
    }
    Row (
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = bitmap,
            contentDescription = null,
            modifier = Modifier
                .padding(4.dp)
        )
        Text(
            text = viewModel.getApplicationName(packageName) ?: "unknown",
            modifier = Modifier
                .weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 18.sp
        )
        Switch(
                checked = enabled,
                onCheckedChange = onCheckedChange
        )
    }
}

sealed class Setting {
    data class Toggle(val title: String, val checked: Boolean, val onToggle: (Boolean) -> Unit) : Setting()
    data class Action(val title: String, val onClick: () -> Unit) : Setting()
    data class Section(val title: String) : Setting()
}