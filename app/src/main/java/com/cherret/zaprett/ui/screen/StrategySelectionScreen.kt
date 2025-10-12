package com.cherret.zaprett.ui.screen

import android.content.Context.MODE_PRIVATE
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cherret.zaprett.R
import com.cherret.zaprett.ui.component.StrategySelectionItem
import com.cherret.zaprett.ui.viewmodel.StrategySelectionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategySelectionScreen(navController: NavController, viewModel : StrategySelectionViewModel = viewModel()){
    val snackbarHostState = remember { SnackbarHostState() }
    val strategyStates = viewModel.strategyStates
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", MODE_PRIVATE)
    var showDialog = remember { mutableStateOf(false) }

    if (showDialog.value) {
        InfoAlert { showDialog.value = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_selection),
                        fontSize = 40.sp,
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
                modifier = Modifier
                    .padding(paddingValues)
            ) {
                item {
                    Row (
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    )
                    {
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

