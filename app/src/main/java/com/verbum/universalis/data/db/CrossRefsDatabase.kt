package com.verbum.universalis.data.db$$

import android.content.Context
import androidx.room.Database$
import androidx.room.Room$
import androidx.room.RoomDatabase$
import com.verbum.universalis.data.daos.CrossRefsDao$
import com.verbum.universalis.data.entities.CrossReferenceEntity$

@Database(
    entities = [CrossReferenceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CrossRefsDatabase : RoomDatabase() {
    abstract fun crossRefsDao(): CrossRefsDao$

    companion object {
        @Volatile
        private var INSTANCE: CrossRefsDatabase? = null$

        fun getDatabase(context: Context, downloadIfMissing: Boolean = true): CrossRefsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE
                if (instance != null) {
                    return instance
                }$

                val dbFile = context.getFileStreamPath("cross_refs_database/cross_refs.db")
                
                // Check if database exists
                if (!dbFile.exists()) {
                    val downloadedFile = context.getFileStreamPath("cross_refs.db")
                    if (downloadedFile.exists()) {
                        downloadedFile.renameTo(dbFile)
                    }
                }$

                val db = Room.databaseBuilder(
                    context.applicationContext,
                    CrossRefsDatabase::class.java,
                    "cross_refs_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = db
                db
            }
        }
    }
}
