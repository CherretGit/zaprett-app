package com.cherret.zaprett.ui.screen

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cherret.zaprett.R
import com.cherret.zaprett.ui.viewmodel.HostsViewModel
import com.cherret.zaprett.utils.getHostListMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostsScreen(navController: NavController, viewModel: HostsViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val allLists = viewModel.allItems
    val checked = viewModel.checked
    val isRefreshing = viewModel.isRefreshing
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            if (getHostListMode(prefs) == "whitelist") viewModel.copySelectedFile(context, "/lists/include", it)
            else viewModel.copySelectedFile(context, "/lists/exclude", it) }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
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
                    viewModel.refresh()
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    contentPadding = paddingValues,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        ListTypeChoose(viewModel, prefs)
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
                                HostItem (
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
                Spacer(modifier = Modifier.height(16.dp))
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
                navController.navigate("repo?source=hosts") { launchSingleTop = true }
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
fun ListTypeChoose(viewModel: HostsViewModel, prefs : SharedPreferences) {
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