package com.example.notepadforcompanycomposeui.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes_by_date")
data class NotesByDateEntity(
    @PrimaryKey
    var noteId: Long,
    var dateId: Long,
    var noteText: String,
    var phoneNumber: String,
    var companyName: String,
    var email: String,
    var location: String,
    var additionalInfo: String,
    var followUp: String,
    var interestRate: String
)