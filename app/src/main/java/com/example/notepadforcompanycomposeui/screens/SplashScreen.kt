package com.example.notepadforcompanycomposeui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Note
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()

    // Set a timer to automatically navigate away from the splash screen
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000) // Delay for 2 seconds
        onSplashComplete()
    }

    // Display the splash screen with different background based on theme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (isDarkTheme) Color(0xFF121212) else MaterialTheme.colorScheme.primary
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Note,
                    contentDescription = "Notes Icon",
                    tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(100.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Notes App",
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
