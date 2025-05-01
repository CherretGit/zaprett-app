package com.cherret.zaprett.ui.screens

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cherret.zaprett.R
import com.cherret.zaprett.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostsScreen(navController: NavController) {
    val context = LocalContext.current
    var allLists by remember { mutableStateOf(getAllLists()) }
    var activeLists by remember { mutableStateOf(getActiveLists()) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }
    val checked = remember {
        mutableStateMapOf<String, Boolean>().apply {
            allLists.forEach { list -> this[list] = activeLists.contains(list) }
        }
    }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { copySelectedFile(context, it, snackbarHostState, scope) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_hosts),
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
                    isRefreshing = true
                    allLists = getAllLists()
                    activeLists = getActiveLists()
                    checked.clear()
                    allLists.forEach { list ->
                        checked[list] = activeLists.contains(list)
                    }
                    isRefreshing = false
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    contentPadding = paddingValues,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        allLists.isEmpty() != false -> {
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        stringResource(R.string.btn_no_hosts),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        else -> {
                            items(allLists) { item ->
                                HostItem(
                                    item = item,
                                    isChecked = checked[item] == true,
                                    onCheckedChange = { isChecked ->
                                        checked[item] = isChecked
                                        if (isChecked) enableList(item) else disableList(item)
                                        showRestartSnackbar(context, snackbarHostState, scope)
                                    },
                                    onDeleteClick = {
                                        if (deleteHost(item)) {
                                            allLists = getAllLists()
                                            activeLists = getActiveLists()
                                            checked.clear()
                                            allLists.forEach { list ->
                                                checked[list] = activeLists.contains(list)
                                            }
                                        }
                                        showRestartSnackbar(context, snackbarHostState, scope)
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
}

@Composable
private fun HostItem(item: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit, onDeleteClick: () -> Unit) {
    ElevatedCard(
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, top = 25.dp, end = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = item, modifier = Modifier.weight(1f))
            Switch(checked = isChecked, onCheckedChange = onCheckedChange)
        }
        HorizontalDivider(thickness = Dp.Hairline)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            FilledTonalButton(
                onClick = onDeleteClick,
                modifier = Modifier.padding(horizontal = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.btn_remove_host),
                    modifier = Modifier.size(20.dp)
                )
                Text(stringResource(R.string.btn_remove_host))
            }
        }
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
                navController.navigate("hosts_repo") { launchSingleTop = true }
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

private fun addHost(launcher: ActivityResultLauncher<Array<String>>) {
    launcher.launch(arrayOf("text/plain"))
}

private fun copySelectedFile(context: Context, uri: Uri, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
    if (!Environment.isExternalStorageManager()) return

    val contentResolver = context.contentResolver
    val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex != -1) cursor.getString(nameIndex) else "copied_file"
    } ?: "copied_file"

    val outputFile = File(getZaprettPath() + "/lists", fileName)

    try {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(outputFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        showRestartSnackbar(context, snackbarHostState, scope)
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

private fun deleteHost(item: String): Boolean {
    val hostFile = File(item)
    return if (hostFile.exists()) {
        hostFile.delete()
        true
    } else {
        false
    }
}

private fun showRestartSnackbar(context: Context, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
    scope.launch {
        val result = snackbarHostState.showSnackbar(
            context.getString(R.string.pls_restart_snack),
            actionLabel = context.getString(R.string.btn_restart_service)
        )
        if (result == SnackbarResult.ActionPerformed) {
            restartService {}
            snackbarHostState.showSnackbar(context.getString(R.string.snack_reload))
        }
    }
}
