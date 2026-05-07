package com.verbum.universalis.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.verbum.universalis.data.local.dao.*
import com.verbum.universalis.data.local.entities.*

@Database(
    entities = [BookEntity::class, VerseEntity::class, TextEntity::class, InterlinearWordEntity::class, LexiconEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VerbumDatabase : RoomDatabase() {
    abstract fun verseDao(): VerseDao
    abstract fun interlinearDao(): InterlinearDao
    abstract fun lexiconDao(): LexiconDao
}
