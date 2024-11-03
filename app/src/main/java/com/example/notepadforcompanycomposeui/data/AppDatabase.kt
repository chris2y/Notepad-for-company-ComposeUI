package com.example.notepadforcompanycomposeui.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.notepadforcompanycomposeui.data.dao.DateDao
import com.example.notepadforcompanycomposeui.data.dao.FirebaseDao
import com.example.notepadforcompanycomposeui.data.dao.NotesByDateDao
import com.example.notepadforcompanycomposeui.data.entities.DateEntity
import com.example.notepadforcompanycomposeui.data.entities.FirebaseEntity
import com.example.notepadforcompanycomposeui.data.entities.NotesByDateEntity

@Database(
    entities = [DateEntity::class, NotesByDateEntity::class, FirebaseEntity::class],
    version = 14,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dateDao(): DateDao
    abstract fun notesByDateDao(): NotesByDateDao
    abstract fun firebaseDao(): FirebaseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notes_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}