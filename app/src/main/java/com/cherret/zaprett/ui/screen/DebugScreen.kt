package com.cherret.zaprett.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cherret.zaprett.R
import com.cherret.zaprett.data.Setting
import com.cherret.zaprett.ui.component.SettingsActionItem
import com.cherret.zaprett.ui.component.SettingsItem
import com.cherret.zaprett.ui.component.SettingsSection
import com.cherret.zaprett.ui.component.TextDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val editor = remember { sharedPreferences.edit() }
    val showUpdateUrlDialog = remember { mutableStateOf(false) }
    val textDialogValue = remember { mutableStateOf("") }
    val settingsList = listOf(
        Setting.Action(
            title = stringResource(R.string.btn_update_repository_url),
            onClick = {
                textDialogValue.value = sharedPreferences.getString("update_repo_url", "https://raw.githubusercontent.com/CherretGit/zaprett-app/refs/heads/main/update.json")?: "https://raw.githubusercontent.com/CherretGit/zaprett-app/refs/heads/main/update.json"
                showUpdateUrlDialog.value = true
            }
        ),
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_debug),
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
                windowInsets = WindowInsets(0)
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 25.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(settingsList) { setting ->
                    when (setting) {
                        is Setting.Toggle -> {
                            SettingsItem(
                                title = setting.title,
                                onToggle = setting.onToggle,
                                checked = setting.checked,
                                onCheckedChange = setting.onToggle
                            )
                        }
                        is Setting.Action -> {
                            SettingsActionItem(
                                title = setting.title,
                                setting.onClick
                            )
                        }
                        is Setting.Section -> {
                            SettingsSection(setting.title)
                        }
                    }
                }
            }
        }
    )
    if (showUpdateUrlDialog.value) {
        TextDialog(stringResource(R.string.btn_update_repository_url), stringResource(R.string.hint_enter_update_repository_url), textDialogValue.value, onConfirm = {
            editor.putString("update_repo_url", it).apply()
        }, onDismiss = { showUpdateUrlDialog.value = false })
    }
}