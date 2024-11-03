package com.example.notepadforcompanycomposeui.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.notepadforcompanycomposeui.data.entities.DateEntity

@Dao
interface DateDao {
    @Insert
    suspend fun insert(dateEntity: DateEntity)

    @Query("SELECT * FROM DateEntity")
    suspend fun getAllDates(): List<DateEntity>

    @Query("SELECT * FROM DateEntity WHERE currentTimeMillis = :millis LIMIT 1")
    suspend fun getDateByMillis(millis: Long): DateEntity

    @Query("SELECT * FROM DateEntity WHERE currentTimeMillis BETWEEN :startTime AND :endTime")
    suspend fun getDatesBetween(startTime: Long, endTime: Long): List<DateEntity>

}