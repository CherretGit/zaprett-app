package com.cherret.zaprett.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.cherret.zaprett.RepoItemInfo
import com.cherret.zaprett.R
import com.cherret.zaprett.download
import com.cherret.zaprett.getFileSha256
import com.cherret.zaprett.getZaprettPath
import com.cherret.zaprett.registerDownloadListenerHost
import com.cherret.zaprett.ui.viewmodel.BaseRepoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoScreen(navController: NavController, viewModel: BaseRepoViewModel) {
    val context = LocalContext.current
    val hostLists = viewModel.hostLists.value
    val isUpdate = viewModel.isUpdate
    val isInstalling = viewModel.isInstalling
    val isUpdateInstalling = viewModel.isUpdateInstalling
    val isRefreshing = viewModel.isRefreshing.value
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.refresh()
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
        content = { paddingValues ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    contentPadding = paddingValues,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (hostLists.isEmpty()) {
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
                    } else {
                        items(hostLists) { item ->
                            val isInstalled = viewModel.isItemInstalled(item)
                            val installing = isInstalling[item.name] == true
                            val updating = isUpdateInstalling[item.name] == true

                            ElevatedCard(
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 10.dp, top = 25.dp, end = 10.dp)
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(text = item.name, modifier = Modifier.weight(1f))
                                    }

                                    Row(
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
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        if (isUpdate[item.name] == true && isInstalled) {
                                            FilledTonalButton(
                                                onClick = { viewModel.update(item) },
                                                enabled = !updating,
                                                modifier = Modifier.padding(horizontal = 5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Update,
                                                    contentDescription = stringResource(R.string.btn_remove_host),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    if (updating) stringResource(R.string.btn_updating_host)
                                                    else stringResource(R.string.btn_update_host)
                                                )
                                            }
                                        }

                                        FilledTonalButton(
                                            onClick = { viewModel.install(item) },
                                            enabled = !installing && !isInstalled,
                                            modifier = Modifier.padding(horizontal = 5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.InstallMobile,
                                                contentDescription = stringResource(R.string.btn_remove_host),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                when {
                                                    installing -> stringResource(R.string.btn_installing_host)
                                                    isInstalled -> stringResource(R.string.btn_installed_host)
                                                    else -> stringResource(R.string.btn_install_host)
                                                }
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    )
}
