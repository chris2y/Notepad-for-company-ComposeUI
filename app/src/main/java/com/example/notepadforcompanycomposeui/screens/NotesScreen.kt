package com.example.notepadforcompanycomposeui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notepadforcompanycomposeui.ViewModels.NotesViewModel
import com.example.notepadforcompanycomposeui.data.entities.NotesByDateEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    dateId: Long,
    onNavigateBack: () -> Unit,
    onAddNoteClick: (Long) -> Unit,
    onEditNoteClick: (Long, Long) -> Unit,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val notes by viewModel.notes.collectAsState()

    LaunchedEffect(dateId) {
        viewModel.loadNotesByDateId(dateId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(formatDate(dateId)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddNoteClick(dateId) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Note")
            }
        }
    ) { paddingValues ->
        // Check if notes are empty
        if (notes.isEmpty()) {
            // Show message if there are no notes
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No notes added", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            // Show notes in a LazyColumn
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onEditNoteClick(dateId, note.noteId) }
                    )
                }
            }
        }
    }
}


@Composable
private fun NoteCard(
    note: NotesByDateEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick // Make the card clickable
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = note.companyName,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Phone: ${note.phoneNumber}")
            Text(text = "Location: ${note.location}")
        }
    }
}

private fun formatDate(timeInMillis: Long): String {
    val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    return dateFormat.format(Date(timeInMillis))
}