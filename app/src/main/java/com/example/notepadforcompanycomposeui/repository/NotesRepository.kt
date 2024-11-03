package com.example.notepadforcompanycomposeui.repository

import com.example.notepadforcompanycomposeui.data.dao.DateDao
import com.example.notepadforcompanycomposeui.data.dao.FirebaseDao
import com.example.notepadforcompanycomposeui.data.dao.NotesByDateDao
import com.example.notepadforcompanycomposeui.data.entities.DateEntity
import com.example.notepadforcompanycomposeui.data.entities.FirebaseEntity
import com.example.notepadforcompanycomposeui.data.entities.NotesByDateEntity
import javax.inject.Inject

class NotesRepository @Inject constructor(
    private val notesByDateDao: NotesByDateDao,
    private val dateDao: DateDao,
    private val firebaseDao: FirebaseDao
) {
    suspend fun getNotesByDateId(dateId: Long): List<NotesByDateEntity> {
        return notesByDateDao.getNotesByDateId(dateId)
    }

    suspend fun insertNote(note: NotesByDateEntity) {
        notesByDateDao.insert(note)
    }

    suspend fun updateNote(note: NotesByDateEntity) {
        notesByDateDao.update(note)
    }

    // Add this new method
    suspend fun getNoteById(noteId: Long): NotesByDateEntity {
        return notesByDateDao.getNoteById(noteId)
    }



    suspend fun insertFirebaseLocation(location: FirebaseEntity) {
        firebaseDao.insert(location)
    }

    suspend fun getUnsyncedLocations(): List<FirebaseEntity> {
        return firebaseDao.getUnsynced()
    }


    suspend fun getAllDates(): List<DateEntity> {
        return dateDao.getAllDates()
    }

    suspend fun insertDate(date: DateEntity) {
        dateDao.insert(date)
    }

    suspend fun getDatesBetween(startTime: Long, endTime: Long): List<DateEntity> {
        return dateDao.getDatesBetween(startTime, endTime)
    }
}
