package com.cherret.zaprett.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cherret.zaprett.R
import com.cherret.zaprett.ui.component.RepoItem
import com.cherret.zaprett.ui.viewmodel.BaseRepoViewModel
import kotlinx.serialization.SerializationException
import java.io.IOException

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
    val error by viewModel.errorFlow.collectAsState()
    val downloadError by viewModel.downloadErrorFlow.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.clearError()
                navController.popBackStack()
            },
            title = { Text(stringResource(R.string.error_text)) },
            text = {
                Text(
                    when (error) {
                        is IOException -> stringResource(R.string.error_server_data)
                        is SerializationException -> stringResource(R.string.error_processing_data)
                        else -> stringResource(R.string.error_unknown)
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip: ClipData = ClipData.newPlainText("Error log", error?.message)
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
                    navController.popBackStack()
                }) {
                    Text(stringResource(R.string.btn_continue))
                }
            }
        )
    }

    if (downloadError != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.clearDownloadError()
            },
            title = { Text(stringResource(R.string.error_text)) },
            text = {
                Text(stringResource(R.string.download_error))
            },
            dismissButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip: ClipData = ClipData.newPlainText("Error log", downloadError)
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
                    viewModel.clearDownloadError()
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
                    modifier = Modifier.navigationBarsPadding().fillMaxSize()
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
                            RepoItem(
                                item = item,
                                viewModel = viewModel,
                                isInstalling = isInstalling,
                                isUpdateInstalling = isUpdateInstalling,
                                isUpdate = isUpdate
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    )
}
