package com.cherret.zaprett.ui.component

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cherret.zaprett.R
import com.cherret.zaprett.data.StrategyCheckResult
import com.cherret.zaprett.data.StrategyTestingStatus
import com.cherret.zaprett.ui.viewmodel.BaseRepoViewModel
import com.cherret.zaprett.utils.RepoItemInfo
import com.cherret.zaprett.utils.disableStrategy
import com.cherret.zaprett.utils.enableStrategy
import com.cherret.zaprett.utils.getActiveStrategy
import kotlinx.coroutines.launch

@Composable
fun ListSwitchItem(item: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit, onDeleteClick: () -> Unit) {
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
fun RepoItem(
    item: RepoItemInfo,
    viewModel: BaseRepoViewModel,
    isInstalling: Map<String, Boolean>,
    isUpdateInstalling: Map<String, Boolean>,
    isUpdate: Map<String, Boolean>,
    modifier: Modifier = Modifier
) {
    val isInstalled = viewModel.isItemInstalled(item)
    val installing = isInstalling[item.name] == true
    val updating = isUpdateInstalling[item.name] == true

    ElevatedCard(
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
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


@Composable
fun StrategySelectionItem(strategy : StrategyCheckResult, prefs : SharedPreferences, context : Context, snackbarHostState : SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    ElevatedCard (
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        onClick = {
            if (strategy.status == StrategyTestingStatus.Completed && strategy.domains.isNotEmpty()) {
                expanded = !expanded
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 25.dp, bottom = 0.dp)
    ) {
        Column (
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        {
            Row {
                Text(
                    text = strategy.path,
                    modifier = Modifier
                       .weight(1f)
                )
                FilledTonalIconButton(
                    onClick = {
                        if (getActiveStrategy(prefs).isNotEmpty()) disableStrategy(getActiveStrategy(prefs)[0], prefs)
                        enableStrategy(strategy.path, prefs)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.strategy_applied)
                            )
                        }
                    },
                    enabled = strategy.status == StrategyTestingStatus.Completed
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "apply"
                    )
                }
            }
            Row {
                Text(
                    text = stringResource(strategy.status.resId),
                    modifier = Modifier
                        .weight(1f),
                    fontSize = 12.sp,
                )
            }
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .weight(1f),

                    progress = {
                        strategy.progress
                    },
                    color = ProgressIndicatorDefaults.linearColor,
                    trackColor = ProgressIndicatorDefaults.linearTrackColor,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
                Text(
                    text = "${(strategy.progress*100).toInt()}%",
                    modifier = Modifier
                        .padding(start = 16.dp),

                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Card (
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.selection_available_domains)
                )
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(strategy.domains) { item ->
                        Card(
                            elevation = CardDefaults.cardElevation(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = item,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}