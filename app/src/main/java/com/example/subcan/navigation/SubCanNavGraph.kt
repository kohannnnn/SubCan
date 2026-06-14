package com.example.subcan.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.subcan.ui.analytics.AnalyticsRoute
import com.example.subcan.ui.calendar.CalendarRoute
import com.example.subcan.ui.detail.DetailRoute
import com.example.subcan.ui.editor.EditorRoute
import com.example.subcan.ui.history.HistoryRoute
import com.example.subcan.ui.home.HomeRoute
import com.example.subcan.ui.preset.PresetSelectRoute
import com.example.subcan.ui.settings.SettingsRoute

@Composable
fun SubCanNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeRoute(
                onAddClick = {
                    navController.navigate(Routes.PRESET)
                },
                onSubscriptionClick = { id ->
                    navController.navigate(Routes.detail(id))
                }
            )
        }

        composable(Routes.CALENDAR) {
            CalendarRoute(
                onSubscriptionClick = { id ->
                    navController.navigate(Routes.detail(id))
                }
            )
        }

        composable(Routes.ANALYTICS) {
            AnalyticsRoute()
        }

        composable(Routes.SETTINGS) {
            SettingsRoute(
                onHistoryClick = {
                    navController.navigate(Routes.HISTORY)
                }
            )
        }

        composable(Routes.HISTORY) {
            HistoryRoute(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PRESET) {
            PresetSelectRoute(
                onTemplateSelect = { templateId ->
                    navController.navigate(Routes.editor(templateId = templateId)) {
                        popUpTo(Routes.PRESET) { inclusive = true }
                    }
                },
                onCustom = {
                    navController.navigate(Routes.editor()) {
                        popUpTo(Routes.PRESET) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(
                navArgument("id") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            DetailRoute(
                subscriptionId = id,
                onBack = { navController.popBackStack() },
                onEdit = { editId ->
                    navController.navigate(Routes.editor(id = editId))
                },
                onArchived = {
                    navController.navigate(Routes.HISTORY) {
                        popUpTo(Routes.DETAIL) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("templateId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: -1L
            val templateId = backStackEntry.arguments?.getLong("templateId") ?: -1L
            EditorRoute(
                subscriptionId = if (id > 0) id else null,
                templateId = if (templateId > 0) templateId else null,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
