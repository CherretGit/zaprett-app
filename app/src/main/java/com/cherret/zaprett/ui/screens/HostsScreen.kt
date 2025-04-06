package com.cherret.zaprett.ui.screens

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cherret.zaprett.R
import com.cherret.zaprett.disableList
import com.cherret.zaprett.enableList
import com.cherret.zaprett.getActiveLists
import com.cherret.zaprett.getAllLists
import com.cherret.zaprett.getZaprettPath
import com.cherret.zaprett.restartService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostsScreen() {
    val context = LocalContext.current
    var allLists by remember { mutableStateOf(getAllLists()) }
    var activeLists by remember { mutableStateOf(getActiveLists()) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }
    val checked = remember {
        mutableStateMapOf<String, Boolean>().apply {
            allLists.forEach { list ->
                this[list] = activeLists.contains(list)
            }
        }
    }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                copySelectedFile(context, it, snackbarHostState, scope)
            }
        }
    )
    Scaffold(
        topBar = {
            val primaryColor = MaterialTheme.colorScheme.surfaceVariant
            Box(modifier = Modifier.padding(start = 20.dp, top = 10.dp)) {
                Canvas(modifier = Modifier.size(100.dp, 50.dp)) {
                    rotate(degrees = -30f) {
                        drawOval(
                            color = primaryColor,
                            size = Size(200f, 140f),
                            topLeft = Offset(-30f, -30f)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.title_hosts),
                    fontSize = 40.sp,
                    fontFamily = FontFamily(Font(R.font.unbounded, FontWeight.Normal))
                )
            }
        },
        content = {  paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
            ) {
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
                    modifier = Modifier
                ) {
                    LazyColumn (
                        contentPadding = PaddingValues(bottom = 25.dp)
                    ){
                        items(allLists) { item ->
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
                                        text = item,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = checked[item] == true,
                                        onCheckedChange = { isChecked ->
                                            checked[item] = isChecked
                                            if (isChecked) {
                                                enableList(item)
                                            } else {
                                                disableList(item)
                                            }
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    context.getString(R.string.pls_restart_snack),
                                                    actionLabel = context.getString(R.string.btn_restart_service)
                                                )
                                                when (result) {
                                                    SnackbarResult.ActionPerformed -> {
                                                        restartService {}
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar(
                                                                context.getString(
                                                                    R.string.snack_reload
                                                                )
                                                            )
                                                        }
                                                    }
                                                    SnackbarResult.Dismissed -> {}
                                                }
                                            }
                                        }
                                    )
                                }
                                HorizontalDivider(thickness = Dp.Hairline)
                                Row (modifier = Modifier
                                    .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End) {
                                    FilledTonalButton(
                                        onClick = {
                                            if (deleteHost(item)) {
                                                allLists = getAllLists()
                                                activeLists = getActiveLists()
                                                checked.clear()
                                                allLists.forEach { list ->
                                                    checked[list] = activeLists.contains(list)
                                                }
                                            }
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    context.getString(R.string.pls_restart_snack),
                                                    actionLabel = context.getString(R.string.btn_restart_service)
                                                )
                                                when (result) {
                                                    SnackbarResult.ActionPerformed -> {
                                                        restartService {}
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar(
                                                                context.getString(
                                                                    R.string.snack_reload
                                                                )
                                                            )
                                                        }
                                                    }
                                                    SnackbarResult.Dismissed -> {}
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .padding(start = 5.dp, end = 5.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.btn_remove_host),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            stringResource(R.string.btn_remove_host)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(modifier = Modifier
                .size(80.dp, 80.dp),
                onClick = {addHost(filePickerLauncher)}) {
                Icon(Icons.Default.Add, contentDescription = "Restart")
            }
        },
        snackbarHost = {SnackbarHost(hostState = snackbarHostState)}
    )
}

fun addHost(launcher: ActivityResultLauncher<Array<String>>) {
    launcher.launch(arrayOf("text/plain"))
}

fun copySelectedFile(context: Context, uri: Uri?, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {// AI Generated
    if (Environment.isExternalStorageManager()) {
        if (uri == null) return
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
            scope.launch {
                val result = snackbarHostState.showSnackbar(context.getString(R.string.pls_restart_snack), actionLabel = context.getString(R.string.btn_restart_service))
                    when (result) {
                        SnackbarResult.ActionPerformed -> {
                            restartService{}
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.snack_reload))
                            }
                        }
                        SnackbarResult.Dismissed -> {}
                    }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

fun deleteHost(item: String): Boolean {
    val hostFile = File(item)
    if (hostFile.exists()) {
        hostFile.delete()
        return true
    }
    return false
}

