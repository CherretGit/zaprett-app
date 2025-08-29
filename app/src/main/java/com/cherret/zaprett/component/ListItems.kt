package com.cherret.zaprett.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cherret.zaprett.R
import com.cherret.zaprett.ui.viewmodel.BaseRepoViewModel
import com.cherret.zaprett.utils.RepoItemInfo

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
