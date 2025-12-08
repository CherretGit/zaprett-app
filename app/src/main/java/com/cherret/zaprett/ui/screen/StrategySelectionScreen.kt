package com.cherret.zaprett.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cherret.zaprett.R
import com.cherret.zaprett.ui.component.StrategySelectionItem
import com.cherret.zaprett.ui.viewmodel.StrategySelectionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategySelectionScreen(navController: NavController, vpnLauncher: ActivityResultLauncher<Intent>, viewModel : StrategySelectionViewModel = viewModel()){
    val snackbarHostState = remember { SnackbarHostState() }
    val strategyStates = viewModel.strategyStates
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", MODE_PRIVATE)
    var showDialog = remember { mutableStateOf(false) }
    val requestVpnPermission by viewModel.requestVpnPermission.collectAsState()
    val error by viewModel.errorFlow.collectAsState()

    if (showDialog.value) {
        InfoAlert { showDialog.value = false }
    }

    LaunchedEffect(requestVpnPermission) {
        if (requestVpnPermission) {
            val intent = VpnService.prepare(context)
            if (intent != null) {
                vpnLauncher.launch(intent)
            } else {
                viewModel.startVpn()
                viewModel.clearVpnPermissionRequest()
            }
        }
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
                        text = stringResource(R.string.title_selection),
                        fontSize = 30.sp,
                        fontFamily = FontFamily(Font(R.font.unbounded, FontWeight.Normal))
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            showDialog.value = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "info"
                        )
                    }
                },
                windowInsets = WindowInsets(0)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { paddingValues ->
            LazyColumn (
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + 40.dp
                ),
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxSize()
            ) {
                item {
                    Row (
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    )
                    {
                        NoHostsCard(viewModel.noHostsCard)
                        FilledTonalButton(
                            onClick = {
                                viewModel.viewModelScope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.begin_selection_snack)
                                    )
                                    viewModel.performTest()
                                }
                            }
                        ) {
                            Text(stringResource(R.string.begin_selection))
                        }
                    }
                }
                when {
                    strategyStates.isEmpty() -> {
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
                        items(strategyStates, key = { it.path }) { item ->
                            StrategySelectionItem(item, prefs, context, snackbarHostState)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun InfoAlert(onDismiss: () -> Unit) {
    AlertDialog(
        title = { Text(text = stringResource(R.string.strategy_selection_info_title)) },
        text = { Text(text = stringResource(R.string.strategy_selection_info_msg)) },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_continue))
            }
        }
    )
}

@Composable
private fun NoHostsCard(noHostsCard: MutableState<Boolean>) {
    if (noHostsCard.value) {
        AlertDialog(
            title = { Text(text = stringResource(R.string.selection_no_hosts_title)) },
            text = { Text(text = stringResource(R.string.selection_no_hosts_message)) },
            onDismissRequest = {
                noHostsCard.value = false
            },
            confirmButton = {
                TextButton(onClick = { noHostsCard.value = false }) {
                    Text(stringResource(R.string.btn_continue))
                }
            }
        )
    }
}