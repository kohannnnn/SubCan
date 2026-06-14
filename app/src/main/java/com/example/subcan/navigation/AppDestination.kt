package com.example.subcan.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
) {
    HOME(
        label = "ホーム",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        route = "home"
    ),
    CALENDAR(
        label = "カレンダー",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        route = "calendar"
    ),
    ANALYTICS(
        label = "アナリティクス",
        selectedIcon = Icons.Filled.Analytics,
        unselectedIcon = Icons.Outlined.Analytics,
        route = "analytics"
    ),
    SETTINGS(
        label = "設定",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        route = "settings"
    )
}

object Routes {
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val ANALYTICS = "analytics"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val PRESET = "preset"
    const val EDITOR = "editor?id={id}&templateId={templateId}"
    const val DETAIL = "detail/{id}"

    fun editor(id: Long? = null, templateId: Long? = null): String =
        "editor?id=${id ?: -1}&templateId=${templateId ?: -1}"
    fun detail(id: Long): String = "detail/$id"

    val topLevelRoutes = listOf(HOME, CALENDAR, ANALYTICS, SETTINGS)
}
