package com.waju.factory.digitalnote.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainTab(val label: String, val icon: ImageVector) {
    NOTES("NOTES", Icons.Outlined.Description),
    SEARCH("SEARCH", Icons.Outlined.Search),
    CANVAS("CANVAS", Icons.Outlined.Edit),
    SETTINGS("SETTINGS", Icons.Outlined.Settings)
}

