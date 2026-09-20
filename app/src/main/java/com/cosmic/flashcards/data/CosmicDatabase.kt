package com.cosmic.flashcards.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cosmic.flashcards.data.dao.CardDao
import com.cosmic.flashcards.data.dao.DeckDao
import com.cosmic.flashcards.data.dao.ReviewDao
import com.cosmic.flashcards.data.dao.TagDao
import com.cosmic.flashcards.data.entity.CardEntity
import com.cosmic.flashcards.data.entity.CardTagCrossRef
import com.cosmic.flashcards.data.entity.DeckEntity
import com.cosmic.flashcards.data.entity.ReviewEntity
import com.cosmic.flashcards.data.entity.TagEntity

@Database(
    entities = [
        DeckEntity::class,
        TagEntity::class,
        CardEntity::class,
        CardTagCrossRef::class,
        ReviewEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class CosmicDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao
    abstract fun deckDao(): DeckDao
    abstract fun tagDao(): TagDao
    abstract fun reviewDao(): ReviewDao

    companion object {
        @Volatile
        private var INSTANCE: CosmicDatabase? = null

        fun get(context: Context): CosmicDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CosmicDatabase::class.java,
                    "cosmic.db",
                )
                    // Foreign keys drive deck-delete (SET NULL) and tag cleanup.
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
