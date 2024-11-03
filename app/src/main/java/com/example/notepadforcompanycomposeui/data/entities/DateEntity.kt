package com.example.notepadforcompanycomposeui.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DateEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var currentTimeMillis: Long
)