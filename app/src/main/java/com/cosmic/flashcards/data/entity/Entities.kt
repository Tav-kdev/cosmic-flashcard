package com.cosmic.flashcards.data.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.cosmic.flashcards.domain.SchedulingState
import com.cosmic.flashcards.domain.StatsCalc

/**
 * All timestamps are epoch millis (Long) rather than a date type — no type
 * converters, nothing to get wrong, and SQLite sorts and compares them directly.
 */

@Entity(
    tableName = "decks",
    indices = [Index(value = ["name"], unique = true)]
)
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "🗂️",
    val accent: String = "cyan",
    val parentId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val accent: String = "purple",
)

/**
 * Scheduling state, embedded straight into the card row.
 *
 * The web app keeps this in a separate 1:1 table; here it's inlined because it
 * is always needed alongside the card and the join buys nothing.
 */
data class Mastery(
    val easeFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val dueAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null,
    val lapses: Int = 0,
) {
    val stage: String
        get() = when {
            repetitions == 0 -> "new"
            intervalDays >= StatsCalc.MASTERED_INTERVAL_DAYS -> "mastered"
            else -> "learning"
        }

    /** 0–100 progress toward the long-interval mark. */
    val strengthPct: Int
        get() = StatsCalc.percent(intervalDays, StatsCalc.MASTERED_INTERVAL_DAYS)

    fun toScheduling() = SchedulingState(easeFactor, intervalDays, repetitions)
}

@Entity(
    tableName = "cards",
    indices = [
        Index(value = ["contentHash"], unique = true),
        Index(value = ["deckId"]),
        Index(value = ["dueAt"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.SET_NULL,
        )
    ]
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long? = null,
    val front: String,
    val back: String,
    val contentHash: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @Embedded val mastery: Mastery = Mastery(),
)

@Entity(
    tableName = "card_tags",
    primaryKeys = ["cardId", "tagId"],
    indices = [Index(value = ["tagId"])],
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ]
)
data class CardTagCrossRef(
    val cardId: Long,
    val tagId: Long,
)

@Entity(
    tableName = "reviews",
    indices = [Index(value = ["reviewedAt"]), Index(value = ["cardId"])],
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val quality: Int,
    val intervalDays: Int,
    val mode: String,
    val reviewedAt: Long,
)

/** A card with its deck and tags resolved — what the UI actually renders. */
data class CardWithRelations(
    @Embedded val card: CardEntity,
    @Relation(parentColumn = "deckId", entityColumn = "id")
    val deck: DeckEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = CardTagCrossRef::class,
            parentColumn = "cardId",
            entityColumn = "tagId",
        )
    )
    val tags: List<TagEntity>,
)

/** A deck plus its live counts, for the deck list and dashboard tiles. */
data class DeckWithCounts(
    @Embedded val deck: DeckEntity,
    @ColumnInfo(name = "cardCount") val cardCount: Int,
    @ColumnInfo(name = "dueCount") val dueCount: Int,
    @ColumnInfo(name = "masteredCount") val masteredCount: Int,
) {
    val masteredPct: Int get() = StatsCalc.percent(masteredCount, cardCount)
}

/** A tag plus its live counts. */
data class TagWithCounts(
    @Embedded val tag: TagEntity,
    @ColumnInfo(name = "cardCount") val cardCount: Int,
    @ColumnInfo(name = "dueCount") val dueCount: Int,
)
