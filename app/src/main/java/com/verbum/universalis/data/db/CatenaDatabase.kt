package com.verbum.universalis.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.verbum.universalis.data.daos.CatenaDao
import com.verbum.universalis.data.entities.CatenaCommentaryEntity
import com.verbum.universalis.data.entities.FatherMetaEntity
import java.io.File

@Database(
    entities = [CatenaCommentaryEntity::class, FatherMetaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CatenaDatabase : RoomDatabase() {
    abstract fun catenaDao(): CatenaDao

    companion object {
        @Volatile
        private var INSTANCE: CatenaDatabase? = null
        
        private const val DATABASE_NAME = "catena.sqlite"

        fun getDatabase(context: Context, downloadIfMissing: Boolean = true): CatenaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE
                if (instance != null) {
                    return instance
                }

                val dbFile = File(context.filesDir, "databases/$DATABASE_NAME")
                
                // Check if database exists, if not and downloadIfMissing is true, 
                // we should have already downloaded it before calling this
                if (!dbFile.exists()) {
                    // Try to find it in the files dir (downloaded location)
                    val downloadedFile = File(context.filesDir, DATABASE_NAME)
                    if (downloadedFile.exists()) {
                        downloadedFile.renameTo(dbFile)
                    }
                }

                val db = Room.databaseBuilder(
                    context.applicationContext,
                    CatenaDatabase::class.java,
                    "catena.sqlite"
                )
                // Since this is a pre-existing database, we need to handle schema mismatches
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
