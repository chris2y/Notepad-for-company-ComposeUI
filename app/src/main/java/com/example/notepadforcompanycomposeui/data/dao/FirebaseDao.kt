package com.example.notepadforcompanycomposeui.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.notepadforcompanycomposeui.data.entities.FirebaseEntity

@Dao
interface FirebaseDao {
    @Insert
    suspend fun insert(firebaseEntity: FirebaseEntity)

    @Update
    suspend fun update(firebaseEntity: FirebaseEntity)

    @Query("SELECT * FROM firebase_locations WHERE synced = 0")
    suspend fun getUnsynced(): List<FirebaseEntity>

    @Query("SELECT * FROM firebase_locations WHERE id = :id")
    suspend fun getFirebaseEntityById(id: Long): FirebaseEntity
}