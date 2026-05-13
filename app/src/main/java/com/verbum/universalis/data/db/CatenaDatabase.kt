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
                if (instance != null) return instance

                // Downloaded DB may be at filesDir/databases/ or filesDir/ directly
                val sources = listOf(
                    File(context.filesDir, "databases/$DATABASE_NAME"),
                    File(context.filesDir, DATABASE_NAME)
                )
                val roomPath = context.getDatabasePath(DATABASE_NAME)
                val source = sources.firstOrNull { it.exists() }
                if (source != null) {
                    roomPath.parentFile?.mkdirs()
                    source.copyTo(roomPath, overwrite = true)
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

        fun invalidate() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
