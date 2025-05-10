package com.cherret.zaprett.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.navigation.NavController
import com.cherret.zaprett.HostsInfo
import com.cherret.zaprett.R
import com.cherret.zaprett.download
import com.cherret.zaprett.getFileSha256
import com.cherret.zaprett.getZaprettPath
import com.cherret.zaprett.registerDownloadListenerHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostsRepoScreen(navController: NavController, getAllLists: () -> Array<String>, getHostList: ((List<HostsInfo>?) -> Unit) -> Unit, targetPath: String) {
    val context = LocalContext.current
    var allLists by remember { mutableStateOf(getAllLists()) }
    var hostLists by remember { mutableStateOf<List<HostsInfo>?>(null) }
    val isUpdate = remember { mutableStateMapOf<String, Boolean>() }
    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        getHostList {
            hostLists = it
        }
    }
    LaunchedEffect(hostLists) {
        if (hostLists != null) {
            withContext(Dispatchers.IO) {
                hostLists!!.forEach { item ->
                    isUpdate[item.name] = !allLists.any { getFileSha256(File(it)) == item.hash }
                }
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_repo),
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
                windowInsets = WindowInsets(0)
            )
        },
        content = {  paddingValues ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    getHostList {
                        hostLists = it
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    contentPadding = paddingValues,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        hostLists?.isEmpty() != false -> {
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
                            items(hostLists.orEmpty()) { item ->
                                var isButtonEnabled by remember { mutableStateOf(!allLists.any { File(it).name == item.name }) }
                                var isInstalling by remember { mutableStateOf(false) }
                                var isButtonUpdateEnabled by remember { mutableStateOf(true) }
                                var isUpdateInstalling by remember { mutableStateOf(false) }
                                ElevatedCard(
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = 6.dp
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
                                            text = item.name,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.title_author, item.author),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp)
                                    ) {
                                        Text(
                                            text = item.description,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    HorizontalDivider(thickness = Dp.Hairline)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        if (isUpdate[item.name] == true && allLists.any { File(it).name == item.name }) {
                                            FilledTonalButton(
                                                onClick = {
                                                    isUpdateInstalling = true
                                                    isButtonUpdateEnabled = false
                                                    val downloadId = download(context, item.url)
                                                    registerDownloadListenerHost(
                                                        context,
                                                        downloadId
                                                    ) { uri ->
                                                        val sourceFile = File(uri.path!!)
                                                        val targetFile = File(
                                                            getZaprettPath() + targetPath,
                                                            uri.lastPathSegment!!
                                                        )
                                                        sourceFile.copyTo(targetFile, overwrite = true)
                                                        sourceFile.delete()
                                                        isUpdateInstalling = false
                                                        getHostList {
                                                            hostLists = it
                                                        }
                                                        isUpdate[item.name] = false
                                                    }
                                                },
                                                enabled = isButtonUpdateEnabled,
                                                modifier = Modifier
                                                    .padding(start = 5.dp, end = 5.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Update,
                                                    contentDescription = stringResource(R.string.btn_remove_host),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    if (!isUpdateInstalling) stringResource(R.string.btn_update_host) else stringResource(
                                                        R.string.btn_updating_host
                                                    )
                                                )
                                            }
                                        }
                                        FilledTonalButton(
                                            onClick = {
                                                isInstalling = true
                                                isButtonEnabled = false
                                                val downloadId = download(context, item.url)
                                                registerDownloadListenerHost(
                                                    context,
                                                    downloadId
                                                ) { uri ->
                                                    val sourceFile = File(uri.path!!)
                                                    val targetFile = File(
                                                        getZaprettPath() + targetPath,
                                                        uri.lastPathSegment!!
                                                    )
                                                    sourceFile.copyTo(targetFile, overwrite = true)
                                                    sourceFile.delete()
                                                    isInstalling = false
                                                    getHostList {
                                                        hostLists = it
                                                    }
                                                }
                                            },
                                            enabled = isButtonEnabled,
                                            modifier = Modifier
                                                .padding(start = 5.dp, end = 5.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.InstallMobile,
                                                contentDescription = stringResource(R.string.btn_remove_host),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                if (isButtonEnabled) stringResource(R.string.btn_install_host) else if (isInstalling) stringResource(
                                                    R.string.btn_installing_host
                                                ) else stringResource(R.string.btn_installed_host)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = {SnackbarHost(hostState = snackbarHostState)}
    )
}