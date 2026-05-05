package com.verbun.universalis.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.verbun.universalis.data.daos.*
import com.verbun.universalis.data.entities.*

@Database(
    entities = [BookEntity::class, VerseEntity::class, TextEntity::class, InterlinearWordEntity::class, LexiconEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun verseDao(): VerseDao
    abstract fun interlinearDao(): InterlinearDao
    abstract fun lexiconDao(): LexiconDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "verbum_database"
                )
                .createFromAsset("verbum_seed.db")
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
