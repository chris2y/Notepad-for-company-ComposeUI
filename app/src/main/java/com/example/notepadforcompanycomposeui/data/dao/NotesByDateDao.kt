package com.example.notepadforcompanycomposeui.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.notepadforcompanycomposeui.data.entities.NotesByDateEntity

@Dao
interface NotesByDateDao {
    @Query("SELECT * FROM notes_by_date WHERE dateId = :dateId")
    suspend fun getNotesByDateId(dateId: Long): List<NotesByDateEntity>

    @Insert
    suspend fun insert(note: NotesByDateEntity)

    @Update
    suspend fun update(note: NotesByDateEntity)

    @Query("SELECT * FROM notes_by_date WHERE noteId = :noteId")
    suspend fun getNoteById(noteId: Long): NotesByDateEntity
}