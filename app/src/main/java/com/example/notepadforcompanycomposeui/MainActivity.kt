package com.example.notepadforcompanycomposeui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.notepadforcompanycomposeui.screens.MapScreen
import com.example.notepadforcompanycomposeui.ui.theme.NotepadForCompanyComposeUITheme
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.material.icons.filled.Add
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.notepadforcompanycomposeui.screens.AddNoteScreen
import com.example.notepadforcompanycomposeui.screens.DateListScreen
import com.example.notepadforcompanycomposeui.screens.NotesScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // All permissions granted, proceed with location updates
        } else {
            // Handle permission denial
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLocationPermissions()
        setContent {
            NotepadForCompanyComposeUITheme {
                MainScreen()
            }
        }
    }

    private fun requestLocationPermissions() {
        val context = this
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Home,
        Screen.Map
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // Show the bottom bar only on the Home and Map screens
            if (currentRoute == Screen.Home.route || currentRoute == Screen.Map.route) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.route) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
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
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
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
}



sealed class Screen(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
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