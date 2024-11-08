package com.example.notepadforcompanycomposeui.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notepadforcompanycomposeui.data.entities.DateEntity
import com.example.notepadforcompanycomposeui.data.entities.FirebaseEntity
import com.example.notepadforcompanycomposeui.data.entities.NotesByDateEntity
import com.example.notepadforcompanycomposeui.repository.NotesRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NotesRepository
) : ViewModel() {

    private val _notes = MutableStateFlow<List<NotesByDateEntity>>(emptyList())
    val notes: StateFlow<List<NotesByDateEntity>> = _notes

    private val _dates = MutableStateFlow<List<DateEntity>>(emptyList())
    val dates: StateFlow<List<DateEntity>> = _dates

    fun loadNotesByDateId(dateId: Long) {
        viewModelScope.launch {
            _notes.value = repository.getNotesByDateId(dateId)
        }
    }

    suspend fun getNoteById(noteId: Long): NotesByDateEntity? {
        return repository.getNoteById(noteId)
    }

    fun updateNote(note: NotesByDateEntity) {
        viewModelScope.launch {
            repository.updateNote(note)
            loadNotesByDateId(note.dateId)
        }
    }
    fun insertDate(date: DateEntity) {
        viewModelScope.launch {
            repository.insertDate(date)
            // Refresh the dates list after insertion
            val currentTime = System.currentTimeMillis()
            loadDatesBetween(0, currentTime + (365L * 24 * 60 * 60 * 1000))
        }
    }

    fun insertNote(note: NotesByDateEntity) {
        viewModelScope.launch {
            repository.insertNote(note)
            loadNotesByDateId(note.dateId)
        }
    }

    fun loadDatesBetween(startTime: Long, endTime: Long) {
        viewModelScope.launch {
            _dates.value = repository.getDatesBetween(startTime, endTime)
        }
    }

    fun insertFirebaseLocation(location: FirebaseEntity) {
        viewModelScope.launch {
            repository.insertFirebaseLocation(location)
        }
    }


    private val _showToast = MutableStateFlow<String?>(null)
    val showToast: StateFlow<String?> = _showToast

    fun saveCurrentDate() {
        viewModelScope.launch {
            val currentTimeMillis = System.currentTimeMillis()
            val dateEntity = DateEntity(currentTimeMillis = currentTimeMillis)

            if (isDateSavedForToday()) {
                _showToast.value = "Date already added"
            } else {
                repository.insertDate(dateEntity)
                loadSavedDates()
            }
            // Reset toast after showing
            _showToast.value = null
        }
    }

    private suspend fun isDateSavedForToday(): Boolean {
        val startOfDay = getStartOfDay(System.currentTimeMillis())
        val endOfDay = getEndOfDay(System.currentTimeMillis())
        val dates = repository.getDatesBetween(startOfDay, endOfDay)
        return dates.isNotEmpty()
    }

    private fun getStartOfDay(currentTimeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getEndOfDay(currentTimeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun loadSavedDates() {
        viewModelScope.launch {
            val dateEntities = repository.getAllDates()
            _dates.value = dateEntities.sortedByDescending { it.currentTimeMillis }
        }
    }

    init {
        loadSavedDates()
    }


    private val firestore = FirebaseFirestore.getInstance()
    private val _uploadingNotes = MutableStateFlow<Set<Long>>(emptySet())
    val uploadingNotes: StateFlow<Set<Long>> = _uploadingNotes

    fun uploadNoteToFirestore(note: NotesByDateEntity) {
        viewModelScope.launch {
            try {
                // Add note ID to uploading set
                _uploadingNotes.value = _uploadingNotes.value + note.noteId

                // Convert note to HashMap for Firestore
                val noteMap = hashMapOf(
                    "noteId" to note.noteId,
                    "dateId" to note.dateId,
                    "noteText" to note.noteText,
                    "phoneNumber" to note.phoneNumber,
                    "companyName" to note.companyName,
                    "email" to note.email,
                    "location" to note.location,
                    "additionalInfo" to note.additionalInfo,
                    "followUp" to note.followUp,
                    "interestRate" to note.interestRate,
                    "latitude" to note.latitude,
                    "longitude" to note.longitude,
                    "timestamp" to FieldValue.serverTimestamp()
                )

                // Upload to Firestore
                firestore.collection("notes")
                    .document(note.noteId.toString())
                    .set(noteMap)
                    .await()

                // Update local database
                val updatedNote = note.copy(isUploaded = true)
                repository.updateNote(updatedNote)

                // Refresh notes list
                loadNotesByDateId(note.dateId)
            } catch (e: Exception) {
                _showToast.value = "Upload failed: ${e.message}"
            } finally {
                // Remove note ID from uploading set
                _uploadingNotes.value = _uploadingNotes.value - note.noteId
            }
        }
    }
}