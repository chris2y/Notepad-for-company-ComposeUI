package com.example.notepadforcompanycomposeui.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val icon: ImageVector) {
    object Home : Screen("Home", Icons.Filled.Home)
    object Map : Screen("Map", Icons.Filled.Place)
    object Notes : Screen("notes/{dateId}", Icons.Filled.Home) {
        fun createRoute(dateId: Long) = "notes/$dateId"
    }
    object AddNote : Screen("add-note/{dateId}?noteId={noteId}", Icons.Filled.Add) {
        fun createRoute(dateId: Long, noteId: Long? = null) =
            if (noteId != null) "add-note/$dateId?noteId=$noteId"
            else "add-note/$dateId"
    }
}
