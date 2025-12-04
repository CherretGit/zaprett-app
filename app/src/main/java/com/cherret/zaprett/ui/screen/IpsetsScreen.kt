package com.cherret.zaprett.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cherret.zaprett.R
import com.cherret.zaprett.ui.component.ListSwitchItem
import com.cherret.zaprett.ui.viewmodel.HostsViewModel
import com.cherret.zaprett.ui.viewmodel.IpsetViewModel
import com.cherret.zaprett.utils.getHostListMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpsetsScreen(navController: NavController, viewModel: IpsetViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val allLists = viewModel.allItems
    val checked = viewModel.checked
    val isRefreshing = viewModel.isRefreshing
    val showPermissionDialog by viewModel.showNoPermissionDialog.collectAsState()
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            if (getHostListMode(prefs) == "whitelist") viewModel.copySelectedFile(context, "/ipset/include", it)
            else viewModel.copySelectedFile(context, "/ipset/exclude", it) }
    }
    val error by viewModel.errorFlow.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    if (error.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                viewModel.clearError()
            },
            title = { Text(stringResource(R.string.error_text)) },
            text = {
                Text(stringResource(R.string.error_unknown))
            },
            dismissButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip: ClipData = ClipData.newPlainText("Error log", error)
                    clipboard.setPrimaryClip(clip)
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
                        Toast.makeText(context, context.getString(R.string.log_copied), Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(stringResource(R.string.btn_copy_log))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearError()
                }) {
                    Text(stringResource(R.string.btn_continue))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_ipset),
                        fontSize = 40.sp,
                        fontFamily = FontFamily(Font(R.font.unbounded, FontWeight.Normal))
                    )
                },
                windowInsets = WindowInsets(0)
            )
        },
        content = { paddingValues ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    viewModel.refresh()
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding() + 80.dp
                    ),
                    modifier = Modifier.navigationBarsPadding().fillMaxSize()
                ) {
                    item {
                        IpsetTypeChoose(viewModel, prefs)
                    }
                    when {
                        allLists.isEmpty() -> {
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        stringResource(R.string.empty_list),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        else -> {
                            items(allLists) { item ->
                                ListSwitchItem (
                                    item = item,
                                    isChecked = checked[item] == true,
                                    onCheckedChange = { isChecked ->
                                        viewModel.onCheckedChange(item, isChecked, snackbarHostState, scope)
                                    },
                                    onDeleteClick = {
                                        viewModel.deleteItem(item, snackbarHostState, scope)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingMenu(navController, filePickerLauncher)
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    )
    if (showPermissionDialog) {
        AlertDialog(
            title = { Text(text = stringResource(R.string.error_no_storage_title)) },
            text = { Text(text = stringResource(R.string.no_storage_permission_message)) },
            onDismissRequest = {
                viewModel.hideNoPermissionDialog()
                navController.popBackStack()
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.hideNoPermissionDialog()
                    navController.popBackStack()
                }) {
                    Text(stringResource(R.string.btn_continue))
                }
            }
        )
    }
}

@Composable
private fun FloatingMenu(navController: NavController, launcher: ActivityResultLauncher<Array<String>>) {
    var expanded by remember { mutableStateOf(false) }
    FloatingActionButton(
        modifier = Modifier.size(80.dp),
        onClick = { expanded = !expanded }
    ) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.btn_add_host))
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.btn_download_host)) },
            onClick = {
                expanded = false
                navController.navigate("repo?source=ipsets") { launchSingleTop = true }
            },
            leadingIcon = {
                Icon(Icons.Default.Download, contentDescription = stringResource(R.string.btn_download_host))
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.btn_add_host)) },
            onClick = {
                expanded = false
                addHost(launcher)
            },
            leadingIcon = {
                Icon(Icons.Default.UploadFile, contentDescription = stringResource(R.string.btn_add_host))
            }
        )
    }
}
@Composable
fun IpsetTypeChoose(viewModel: IpsetViewModel, prefs : SharedPreferences) {
    val listType = remember { mutableStateOf(getHostListMode(prefs))}
    val options = listOf(stringResource(R.string.title_whitelist), stringResource(R.string.title_blacklist))
    val selectedIndex = if (listType.value == "whitelist") 0 else 1

    SingleChoiceSegmentedButtonRow (
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                onClick = {
                    listType.value = if (index == 0) "whitelist" else "blacklist"
                    viewModel.setListType(listType.value)
                },
                selected = index == selectedIndex,
                label = {
                    Text(
                        label
                    )
                }
            )
        }
    }
}
private fun addHost(launcher: ActivityResultLauncher<Array<String>>) {
    launcher.launch(arrayOf("text/plain"))
}