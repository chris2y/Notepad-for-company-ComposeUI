package com.example.notepadforcompanycomposeui.data.dataclass

data class UploadedNote(
    val noteId: Long,
    val dateId: Long,
    val noteText: String,
    val phoneNumber: String,
    val companyName: String,
    val email: String,
    val location: String,
    val additionalInfo: String,
    val followUp: String,
    val interestRate: String,
    val latitude: Double,
    val longitude: Double
)