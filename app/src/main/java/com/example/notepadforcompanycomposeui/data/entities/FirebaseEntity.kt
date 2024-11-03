package com.example.notepadforcompanycomposeui.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "firebase_locations")
data class FirebaseEntity(
    @PrimaryKey
    var id: Long,
    var location: String,
    var companyName: String,
    var synced: Boolean,
    var longitude: Double? = null,
    var latitude: Double? = null
) {
    constructor() : this(0, "", "", false)
}