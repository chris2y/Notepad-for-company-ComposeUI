package com.example.notepadforcompanycomposeui.ui.navigation


import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.notepadforcompanycomposeui.screens.AddNoteScreen
import com.example.notepadforcompanycomposeui.screens.DateListScreen
import com.example.notepadforcompanycomposeui.screens.MapScreen
import com.example.notepadforcompanycomposeui.screens.NotesScreen

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            DateListScreen(
                onDateClick = { dateId ->
                    navController.navigate(Screen.Notes.createRoute(dateId))
                }
            )
        }
        composable(
            route = Screen.Notes.route,
            arguments = listOf(navArgument("dateId") { type = NavType.LongType })
        ) { backStackEntry ->
            val dateId = backStackEntry.arguments?.getLong("dateId") ?: return@composable
            NotesScreen(
                dateId = dateId,
                onNavigateBack = { navController.popBackStack() },
                onAddNoteClick = { dateId ->
                    navController.navigate(Screen.AddNote.createRoute(dateId))
                },
                onEditNoteClick = { dateId, noteId ->
                    navController.navigate(Screen.AddNote.createRoute(dateId, noteId))
                }
            )
        }
        composable(
            route = Screen.AddNote.route,
            arguments = listOf(
                navArgument("dateId") { type = NavType.LongType },
                navArgument("noteId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val dateId = backStackEntry.arguments?.getLong("dateId") ?: return@composable
            val noteId = backStackEntry.arguments?.getLong("noteId")?.takeIf { it != -1L }
            AddNoteScreen(
                dateId = dateId,
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Map.route) { MapScreen() }
    }
}
