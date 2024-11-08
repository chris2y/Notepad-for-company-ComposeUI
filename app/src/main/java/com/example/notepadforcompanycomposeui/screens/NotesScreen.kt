package com.example.notepadforcompanycomposeui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.example.notepadforcompanycomposeui.ViewModels.NotesViewModel
import com.example.notepadforcompanycomposeui.data.entities.NotesByDateEntity
import kotlinx.coroutines.launch
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
    val uploadingNotes by viewModel.uploadingNotes.collectAsState()

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
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No notes added", style = MaterialTheme.typography.titleMedium)
            }
        } else {
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
                        isUploading = uploadingNotes.contains(note.noteId),
                        onClick = { onEditNoteClick(dateId, note.noteId) },
                        onUploadClick = {
                            viewModel.viewModelScope.launch {
                                viewModel.uploadNoteToFirestore(note)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: NotesByDateEntity,
    isUploading: Boolean,
    onClick: () -> Unit,
    onUploadClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.companyName,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )

                if (!note.isUploaded) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = onUploadClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload to cloud",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Phone: ${note.phoneNumber}")
            Text(text = "Location: ${note.location}")

            // Optional: Show upload status
            if (!note.isUploaded) {
                Text(
                    text = if (isUploading) "Uploading..." else "Not uploaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private fun formatDate(timeInMillis: Long): String {
    val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    return dateFormat.format(Date(timeInMillis))
}