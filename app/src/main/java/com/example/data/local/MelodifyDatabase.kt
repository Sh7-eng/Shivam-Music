package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SongEntity::class, PlaylistEntity::class, PlaylistSongCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class MelodifyDatabase : RoomDatabase() {
    abstract fun melodifyDao(): MelodifyDao

    companion object {
        @Volatile
        private var INSTANCE: MelodifyDatabase? = null

        fun getDatabase(context: Context): MelodifyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MelodifyDatabase::class.java,
                    "melodify_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
