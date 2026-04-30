package com.waju.factory.digitalnote.navigation

sealed interface AppRoute {
    val route: String

    data object Notes : AppRoute {
        override val route: String = "notes"
    }

    data object Search : AppRoute {
        override val route: String = "search"
    }

    data object Settings : AppRoute {
        override val route: String = "settings"
    }

    data object Canvas : AppRoute {
        override val route: String = "canvas/{noteId}"
        fun create(noteId: Int): String = "canvas/$noteId"
    }

    data object Editor : AppRoute {
        override val route: String = "editor/{noteId}"
        fun create(noteId: Int): String = "editor/$noteId"
    }

    data object Chat : AppRoute {
        override val route: String = "chat/{noteId}"
        fun create(noteId: Int): String = "chat/$noteId"
    }

    data object ChatAttachmentViewer : AppRoute {
        override val route: String = "chat_attachment_viewer/{noteId}/{messageId}"
        fun create(noteId: Int, messageId: Long): String = "chat_attachment_viewer/$noteId/$messageId"
    }
}

data class BottomDestination(
    val tab: MainTab,
    val baseRoute: String
)

val bottomDestinations = listOf(
    BottomDestination(MainTab.NOTES, AppRoute.Notes.route),
    BottomDestination(MainTab.SEARCH, AppRoute.Search.route),
    BottomDestination(MainTab.CANVAS, "canvas"),
    BottomDestination(MainTab.SETTINGS, AppRoute.Settings.route)
)

