package com.cherret.zaprett.ui.screen

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
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
fun HomeScreen(viewModel: HomeViewModel = viewModel(), vpnLauncher: ActivityResultLauncher<Intent>) {
    val context = LocalContext.current
    val sharedPreferences: SharedPreferences = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val requestVpnPermission by viewModel.requestVpnPermission.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val cardText = viewModel.cardText
    val cardIcon = viewModel.cardIcon;
    val changeLog = viewModel.changeLog
    val newVersion = viewModel.newVersion
    val updateAvailable = viewModel.updateAvailable
    val showUpdateDialog = viewModel.showUpdateDialog.value
    val moduleVer = viewModel.moduleVer;
    val nfqwsVer = viewModel.nfqwsVer;
    val byedpiVer = viewModel.byedpiVer;
    val serviceMode = viewModel.serviceMode
    LaunchedEffect(Unit) {
        viewModel.checkForUpdate()
        viewModel.checkServiceStatus()
        viewModel.checkModuleInfo()
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
            Column(modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())) {
                ServiceStatusCard(viewModel, cardText, cardIcon, snackbarHostState, scope)
                UpdateCard(updateAvailable) { viewModel.showUpdateDialog() }
                if (showUpdateDialog) {
                    UpdateDialog(viewModel, changeLog.value.orEmpty(), newVersion) { viewModel.dismissUpdateDialog() }
                }
                ServiceControlButtons(
                    viewModel,
                    sharedPreferences,
                    snackbarHostState,
                    scope
                )
                ModuleInfoCard(moduleVer, nfqwsVer, byedpiVer, serviceMode)
            }
        }
    )
}

@Composable
private fun ServiceStatusCard(viewModel: HomeViewModel, cardText: MutableState<Int>, cardIcon : MutableState<ImageVector>, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
    ElevatedCard(
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, top = 25.dp, end = 10.dp)
            //.height(150.dp)
            .wrapContentHeight(),
        onClick = { viewModel.onCardClick() }
    ) {
        Row (
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        )
        {
            Icon(
                painter = rememberVectorPainter(cardIcon.value),
                modifier = Modifier
                    .width(60.dp)
                    .height(60.dp),
                contentDescription = "icon"
            )
            Text(
                text = stringResource(cardText.value),
                fontFamily = FontFamily(Font(R.font.unbounded, FontWeight.Normal)),
                fontSize = 16.sp,
                //maxLines = 3,
                //overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
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
private fun ServiceControlButtons(viewModel: HomeViewModel, sharedPreferences: SharedPreferences, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
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
    if (sharedPreferences.getBoolean("use_module", false)) {
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
}

@Composable
private fun ModuleInfoCard(
    moduleVer: MutableState<String>,
    nfqwsVer: MutableState<String>,
    byedpiVer: MutableState<String>,
    serviceMode: MutableState<Int>
) {
    ElevatedCard(
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        ModuleInfoItem(Icons.Default.Build, stringResource(R.string.service_mode), stringResource(serviceMode.value))
        HorizontalDivider()
        ModuleInfoItem(Icons.Default.Extension, stringResource(R.string.module_version), moduleVer.value)
        HorizontalDivider()
        ModuleInfoItem(Icons.Default.Dangerous, stringResource(R.string.nfqws_version), nfqwsVer.value)
        HorizontalDivider()
        ModuleInfoItem(Icons.Default.WavingHand, stringResource(R.string.ciadpi_version), byedpiVer.value)
    }
}

@Composable
private fun ModuleInfoItem(
    icon: ImageVector, header : String, value : String
) {
    Row (
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Icon(
            painter = rememberVectorPainter(icon),
            modifier = Modifier
                .size(50.dp)
                .padding(16.dp),
            contentDescription = "icon"
        )
        Column (
            modifier = Modifier
                .padding(horizontal = 16.dp)
        ){
            Text(
                text = header,
                modifier = Modifier
                    .fillMaxWidth(),
                fontSize = 18.sp,
                textAlign = TextAlign.Justify,
            )
            Text(
                text = value
            )
        }
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