package com.cherret.zaprett


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cherret.zaprett.ui.screen.HomeScreen
import com.cherret.zaprett.ui.screen.RepoScreen
import com.cherret.zaprett.ui.screen.HostsScreen
import com.cherret.zaprett.ui.screen.SettingsScreen
import com.cherret.zaprett.ui.screen.StrategyScreen
import com.cherret.zaprett.ui.theme.ZaprettTheme
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics

sealed class Screen(val route: String, @StringRes val nameResId: Int, val icon: ImageVector) {
    object home : Screen("home", R.string.title_home, Icons.Default.Home)
    object hosts : Screen("hosts", R.string.title_hosts, Icons.Default.Dashboard)
    object strategies : Screen("strategies", R.string.title_strategies, Icons.Default.Dns)
    object settings : Screen("settings", R.string.title_settings, Icons.Default.Settings)
}
val topLevelRoutes = listOf(Screen.home, Screen.hosts, Screen.strategies, Screen.settings)
val hideNavBar = listOf("repo?source={source}")
class MainActivity : ComponentActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = Firebase.analytics
        enableEdgeToEdge()
        setContent {
            ZaprettTheme {
                val sharedPreferences = remember { getSharedPreferences("settings", MODE_PRIVATE) }
                var showPermissionDialog by remember { mutableStateOf(!Environment.isExternalStorageManager()) }
                var showWelcomeDialog by remember { mutableStateOf(sharedPreferences.getBoolean("welcome_dialog", true)) }
                firebaseAnalytics.setAnalyticsCollectionEnabled(sharedPreferences.getBoolean("send_firebase_analytics", true))
                BottomBar()
                if (showPermissionDialog) {
                    PermissionDialog { showPermissionDialog = false }
                }
                if (showWelcomeDialog) {
                    WelcomeDialog {
                        sharedPreferences.edit { putBoolean("welcome_dialog", false) }
                        showWelcomeDialog = false
                    }
                }
            }
        }
    }

    @Composable
    fun BottomBar() {
        val navController = rememberNavController()
        Scaffold(
            bottomBar = {
                val navBackStackEntry = navController.currentBackStackEntryAsState().value
                val currentDestination = navBackStackEntry?.destination
                if (currentDestination?.route !in hideNavBar) {
                    NavigationBar {
                        topLevelRoutes.forEach { topLevelRoute ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        topLevelRoute.icon,
                                        contentDescription = stringResource(id = topLevelRoute.nameResId)
                                    )
                                },
                                label = { Text(text = stringResource(id = topLevelRoute.nameResId)) },
                                selected = currentDestination?.route == topLevelRoute.route,
                                onClick = {
                                    navController.navigate(topLevelRoute.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController,
                startDestination = Screen.home.route,
                Modifier.padding(innerPadding)
            ) {
                composable(Screen.home.route) { HomeScreen() }
                composable(Screen.hosts.route) { HostsScreen(navController) }
                composable(Screen.strategies.route) { StrategyScreen(navController) }
                composable(Screen.settings.route) { SettingsScreen() }
                composable(route = "repo?source={source}",arguments = listOf(navArgument("source") {})) { backStackEntry ->
                    val source = backStackEntry.arguments?.getString("source")
                    when (source) {
                        "hosts" -> {
                            RepoScreen(navController, ::getAllLists, ::getHostList, "/lists")
                        }
                        "strategies" -> {
                            RepoScreen(navController, ::getAllStrategies, ::getStrategiesList, "/strategies")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun WelcomeDialog(onDismiss: () -> Unit) {
        AlertDialog(
            title = { Text(text = stringResource(R.string.app_name)) },
            text = { Text(text = stringResource(R.string.text_welcome)) },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_continue))
                }
            }
        )
    }

    @Composable
    fun PermissionDialog(onDismiss: () -> Unit) {
        val context = LocalContext.current
        AlertDialog(
            title = { Text(text = stringResource(R.string.error_no_storage_title)) },
            text = { Text(text = stringResource(R.string.error_no_storage_message)) },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        val uri = Uri.fromParts("package", context.packageName, null)
                        intent.data = uri
                        context.startActivity(intent)
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.btn_continue))
                }
            }
        )
    }
}