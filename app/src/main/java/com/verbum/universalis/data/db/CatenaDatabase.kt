package com.verbum.universalis.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.verbum.universalis.data.daos.CatenaDao
import com.verbum.universalis.data.entities.*

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

        fun getDatabase(context: Context, downloadIfMissing: Boolean = true): CatenaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE
                if (instance != null) {
                    return instance
                }

                val dbFile = context.getFileStreamPath("catena_database/catena.db")
                
                // Check if database exists
                if (!dbFile.exists()) {
                    val downloadedFile = context.getFileStreamPath("catena.db")
                    if (downloadedFile.exists()) {
                        downloadedFile.renameTo(dbFile)
                    }
                }

                val db = Room.databaseBuilder(
                    context.applicationContext,
                    CatenaDatabase::class.java,
                    "catena_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = db
                db
            }
        }
    }
}
