package com.verbum.universalis.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.verbum.universalis.data.daos.CatenaDao
import com.verbum.universalis.data.entities.CatenaCommentaryEntity
import java.io.File

@Database(
    entities = [CatenaCommentaryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CatenaDatabase : RoomDatabase() {
    abstract fun catenaDao(): CatenaDao

    companion object {
        @Volatile
        private var INSTANCE: CatenaDatabase? = null

        private const val DATABASE_NAME = "verbum_catena.db"

        fun getDatabase(context: Context): CatenaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE
                if (instance != null) {
                    return instance
                }

                val dbFile = File(context.filesDir, "databases/$DATABASE_NAME")

                // Check if database exists in filesDir (downloaded location)
                if (!dbFile.exists()) {
                    val downloadedFile = File(context.filesDir, DATABASE_NAME)
                    if (downloadedFile.exists()) {
                        downloadedFile.renameTo(dbFile)
                    }
                }

                val db = Room.databaseBuilder(
                    context.applicationContext,
                    CatenaDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()

                INSTANCE = db
                db
            }
        }

        fun isDatabaseDownloaded(context: Context): Boolean {
            val dbFile = File(context.filesDir, "databases/$DATABASE_NAME")
            val downloadedFile = File(context.filesDir, DATABASE_NAME)
            return dbFile.exists() || downloadedFile.exists()
        }
    }
}
