package com.cherret.zaprett.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cherret.zaprett.BuildConfig
import com.cherret.zaprett.R
import com.cherret.zaprett.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val cardText = viewModel.cardText
    val changeLog = viewModel.changeLog
    val newVersion = viewModel.newVersion
    val updateAvailable = viewModel.updateAvailable
    val showUpdateDialog = viewModel.showUpdateDialog.value

    LaunchedEffect(Unit) {
        viewModel.checkForUpdate()
        viewModel.checkServiceStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 40.sp,
                        fontFamily = FontFamily(Font(R.font.unbounded, FontWeight.Normal))
                    )
                },
                windowInsets = WindowInsets(0)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                ServiceStatusCard(viewModel, cardText, snackbarHostState, scope)
                UpdateCard(updateAvailable) { viewModel.showUpdateDialog() }
                if (showUpdateDialog) {
                    UpdateDialog(viewModel, changeLog.value.orEmpty(), newVersion) { viewModel.dismissUpdateDialog() }
                }
                ServiceControlButtons(viewModel, snackbarHostState, scope)
            }
        }
    )
}

@Composable
private fun ServiceStatusCard(viewModel: HomeViewModel, cardText: MutableState<Int>, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
    ElevatedCard(
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, top = 25.dp, end = 10.dp)
            .width(240.dp)
            .height(150.dp),
        onClick = { viewModel.onCardClick(snackbarHostState, scope) }
    ) {
        Text(
            text = stringResource(cardText.value),
            fontFamily = FontFamily(Font(R.font.unbounded, FontWeight.Normal)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun UpdateCard(updateAvailable: MutableState<Boolean>, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = updateAvailable.value,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        ElevatedCard(
            elevation = CardDefaults.cardElevation(6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, top = 10.dp, end = 10.dp)
                .width(140.dp)
                .height(70.dp),
            onClick = onClick
        ) {
            Text(
                text = stringResource(R.string.update_available),
                fontFamily = FontFamily(Font(R.font.unbounded, FontWeight.Normal)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ServiceControlButtons(viewModel: HomeViewModel, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
    FilledTonalButton(
        onClick = { viewModel.onBtnStartService(snackbarHostState, scope) },
        modifier = Modifier
            .padding(horizontal = 5.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = stringResource(R.string.btn_start_service),
            modifier = Modifier.size(20.dp)
        )
        Text(stringResource(R.string.btn_start_service))
    }
    FilledTonalButton(
        onClick = { viewModel.onBtnStopService(snackbarHostState, scope) },
        modifier = Modifier
            .padding(horizontal = 5.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Stop,
            contentDescription = stringResource(R.string.btn_stop_service),
            modifier = Modifier.size(20.dp)
        )
        Text(stringResource(R.string.btn_stop_service))
    }
    FilledTonalButton(
        onClick = { viewModel.onBtnRestart(snackbarHostState, scope) },
        modifier = Modifier
            .padding(horizontal = 5.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.RestartAlt,
            contentDescription = stringResource(R.string.btn_restart_service),
            modifier = Modifier.size(20.dp)
        )
        Text(stringResource(R.string.btn_restart_service))
    }
}

@Composable
fun UpdateDialog(viewModel: HomeViewModel, changeLog: String, newVersion: MutableState<String?>, onDismiss: () -> Unit) {
    AlertDialog(
        title = { Text(text = stringResource(R.string.update_available)) },
        text = {
            Text(
                text = stringResource(R.string.alert_version, BuildConfig.VERSION_NAME, newVersion.value.toString(), changeLog)
            )
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                viewModel.onUpdateConfirm()
            }) {
                Text(stringResource(R.string.btn_update))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_dismiss))
            }
        }
    )
}
