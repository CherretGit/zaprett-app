package com.cherret.zaprett.ui.screens

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cherret.zaprett.R
import com.cherret.zaprett.download
import com.cherret.zaprett.getChangelog
import com.cherret.zaprett.getStatus
import com.cherret.zaprett.getUpdate
import com.cherret.zaprett.installApk
import com.cherret.zaprett.registerDownloadListener
import com.cherret.zaprett.restartService
import com.cherret.zaprett.startService
import com.cherret.zaprett.stopService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("settings", MODE_PRIVATE) }
    val cardText = remember { mutableStateOf(R.string.status_not_availible) }
    val changeLog = remember { mutableStateOf<String?>(null) }
    val updateAvailable = remember {mutableStateOf(false)}
    val downloadUrl = remember { mutableStateOf<String?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        if (sharedPreferences.getBoolean("auto_update", true)) {
            getUpdate(context) {
                if (it != null) {
                    downloadUrl.value = it.downloadUrl.toString()
                    getChangelog(it.changelogUrl.toString()) {
                        changeLog.value = it
                    }
                    updateAvailable.value = true
                }
            }
        }
        if (sharedPreferences.getBoolean("use_module", false) && sharedPreferences.getBoolean("update_on_boot", false)) {
            getStatus {
                if (it) {
                    cardText.value = R.string.status_enabled
                }
                else {
                    cardText.value = R.string.status_disabled
                }
            }

        }
    }
    Scaffold(
        topBar = {
            val primaryColor = MaterialTheme.colorScheme.surfaceVariant
            Box(modifier = Modifier.padding(start = 20.dp, top = 10.dp)) {
                Canvas(modifier = Modifier.size(100.dp, 50.dp)) {
                    rotate(degrees = -30f) {
                        drawOval(
                            color = primaryColor,
                            size = Size(200f, 140f),
                            topLeft = Offset(-20f, -20f)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.app_name),
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
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, top = 25.dp, end = 10.dp)
                        .size(width = 240.dp, height = 150.dp),
                    onClick = { onCardClick(context, cardText, snackbarHostState, scope) }
                ) {
                    Text(
                        text = stringResource(cardText.value),
                        fontFamily = FontFamily(Font(R.font.unbounded, FontWeight.Normal)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                if (updateAvailable.value) {
                    ElevatedCard(
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 6.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, top = 10.dp, end = 10.dp)
                            .size(width = 140.dp, height = 70.dp),
                        onClick = {
                            showUpdateDialog = true
                        }
                    )
                    {
                        Text(
                            text = stringResource(R.string.update_available),
                            fontFamily = FontFamily(Font(R.font.unbounded, FontWeight.Normal)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (showUpdateDialog) {
                    UpdateDialog(context, downloadUrl.value.toString(), changeLog.value.toString(), onDismiss = { showUpdateDialog = false })
                }
                FilledTonalButton(
                    onClick = { onBtnStartService(context, snackbarHostState, scope) },
                    modifier = Modifier
                        .padding(start = 5.dp, top = 8.dp, end = 5.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.btn_start_service),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        stringResource(R.string.btn_start_service)
                    )
                }
                FilledTonalButton(
                    onClick = { onBtnStopService(context, snackbarHostState, scope) },
                    modifier = Modifier
                        .padding(start = 5.dp, top = 8.dp, end = 5.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = stringResource(R.string.btn_stop_service),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        stringResource(R.string.btn_stop_service)
                    )
                }
                FilledTonalButton(
                    onClick = { onBtnRestart(context, snackbarHostState, scope) },
                    modifier = Modifier
                        .padding(start = 5.dp, top = 8.dp, end = 5.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = stringResource(R.string.btn_restart_service),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        stringResource(R.string.btn_restart_service)
                    )
                }
            }
        },
        snackbarHost = {SnackbarHost(hostState = snackbarHostState)}
    )
}
fun onCardClick(context: Context, cardText: MutableState<Int>, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
    val sharedPreferences = context.getSharedPreferences("settings", MODE_PRIVATE)
    if (sharedPreferences.getBoolean("use_module", false)) {
        getStatus {
            if (it) {
                cardText.value = R.string.status_enabled
            }
            else {
                cardText.value = R.string.status_disabled
            }
        }

    }
    else {
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.snack_module_disabled))
        }
    }
}

fun onBtnStartService(context: Context, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
    if (context.getSharedPreferences("settings", MODE_PRIVATE).getBoolean("use_module", false)) {
        getStatus {
            if (it) {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.snack_already_started))
                }
            } else {
                startService {}
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.snack_starting_service))
                }
            }
        }
    } else {
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.snack_module_disabled))
        }
    }
}

fun onBtnStopService(context: Context, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
    if (context.getSharedPreferences("settings", MODE_PRIVATE).getBoolean("use_module", false)) {
        getStatus {
            if (it) {
                stopService{}
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.snack_stopping_service))
                }
            }
            else {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.snack_no_service))
                }
            }
        }
    }
    else {
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.snack_module_disabled))
        }
    }
}

fun onBtnRestart(context: Context, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
    if (context.getSharedPreferences("settings", MODE_PRIVATE).getBoolean("use_module", false)) {
        restartService{}
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.snack_reload))
        }
    }
    else {
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.snack_module_disabled))
        }
    }
}



@Composable
fun UpdateDialog(context: Context, downloadUrl: String, changeLog: String, onDismiss: () -> Unit) {
    AlertDialog(
        title = { Text(text = stringResource(R.string.update_available)) },
        text = { Text(text = changeLog) },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                val downloadId = download(context, downloadUrl)
                registerDownloadListener(context, downloadId) { uri ->
                    installApk(context, uri)
                }
            }) {
                Text(stringResource(R.string.btn_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_dismiss))
            }
        }
    )
}