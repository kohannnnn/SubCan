package com.example.subcan

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.subcan.navigation.Routes
import com.example.subcan.navigation.SubCanNavGraph
import com.example.subcan.navigation.TopDestination
import com.example.subcan.notification.NotificationScheduler
import com.example.subcan.ui.theme.SubCanTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 通知スケジュールを開始
        NotificationScheduler.scheduleDailyCheck(this)

        lifecycleScope.launch {
            (application as SubCanApplication).repository.syncSubscriptions()
        }

        setContent {
            SubCanTheme {
                SubCanApp()
            }
        }
    }
}

@Composable
fun SubCanApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // トップレベル画面かどうかを判定
    val isTopLevel = currentRoute in Routes.topLevelRoutes
    val currentTopDestination = TopDestination.entries.find { it.route == currentRoute }
        ?: TopDestination.HOME
    val activity = context.findActivity()

    BackHandler(
        enabled = isTopLevel && currentRoute != Routes.HOME && activity != null
    ) {
        activity?.moveTaskToBack(true)
    }

    if (isTopLevel) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                TopDestination.entries.forEach { destination ->
                    val selected = destination == currentTopDestination
                    item(
                        icon = {
                            Icon(
                                imageVector = if (selected) {
                                    destination.selectedIcon
                                } else {
                                    destination.unselectedIcon
                                },
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) },
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
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
        ) {
            SubCanNavGraph(navController = navController)
        }
    } else {
        SubCanNavGraph(navController = navController)
    }
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
