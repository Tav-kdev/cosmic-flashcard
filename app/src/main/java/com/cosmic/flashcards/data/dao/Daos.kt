package com.cosmic.flashcards.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.cosmic.flashcards.data.entity.CardEntity
import com.cosmic.flashcards.data.entity.CardTagCrossRef
import com.cosmic.flashcards.data.entity.CardWithRelations
import com.cosmic.flashcards.data.entity.DeckEntity
import com.cosmic.flashcards.data.entity.DeckWithCounts
import com.cosmic.flashcards.data.entity.ReviewEntity
import com.cosmic.flashcards.data.entity.TagEntity
import com.cosmic.flashcards.data.entity.TagWithCounts
import kotlinx.coroutines.flow.Flow

/** Sort order for the card list. Values are passed into SQL, so keep them stable. */
object CardSort {
    const val RECENT = 0
    const val DUE = 1
    const val ALPHA = 2
}

/** State filter for the card list. */
object CardState {
    const val ANY = 0
    const val DUE = 1
    const val NEW = 2
    const val LEARNING = 3
    const val MASTERED = 4
}

@Dao
interface CardDao {

    @Transaction
    @Query(
        """
        SELECT * FROM cards
        WHERE (:query IS NULL OR front LIKE '%' || :query || '%' OR back LIKE '%' || :query || '%')
          AND (:deckId IS NULL OR deckId = :deckId)
          AND (:unsortedOnly = 0 OR deckId IS NULL)
          AND (:tagId IS NULL OR id IN (SELECT cardId FROM card_tags WHERE tagId = :tagId))
          AND (:state = 0
               OR (:state = 1 AND dueAt <= :now)
               OR (:state = 2 AND repetitions = 0)
               OR (:state = 3 AND repetitions > 0 AND intervalDays < 21)
               OR (:state = 4 AND intervalDays >= 21))
        ORDER BY
          CASE WHEN :sort = 1 THEN dueAt END ASC,
          CASE WHEN :sort = 2 THEN front END COLLATE NOCASE ASC,
          CASE WHEN :sort = 0 THEN createdAt END DESC,
          id DESC
        """
    )
    fun filter(
        query: String?,
        deckId: Long?,
        unsortedOnly: Int,
        tagId: Long?,
        state: Int,
        sort: Int,
        now: Long,
    ): Flow<List<CardWithRelations>>

    @Transaction
    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun byId(id: Long): CardWithRelations?

    @Transaction
    @Query("SELECT * FROM cards WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<CardWithRelations>

    @Query("SELECT id FROM cards WHERE dueAt <= :now ORDER BY dueAt ASC LIMIT :limit")
    suspend fun dueCardIds(now: Long, limit: Int): List<Long>

    @Query(
        """
        SELECT id FROM cards
        WHERE dueAt <= :now AND deckId = :deckId
        ORDER BY dueAt ASC LIMIT :limit
        """
    )
    suspend fun dueCardIdsInDeck(now: Long, deckId: Long, limit: Int): List<Long>

    @Query(
        """
        SELECT c.id FROM cards c
        INNER JOIN card_tags ct ON ct.cardId = c.id
        WHERE c.dueAt <= :now AND ct.tagId = :tagId
        ORDER BY c.dueAt ASC LIMIT :limit
        """
    )
    suspend fun dueCardIdsWithTag(now: Long, tagId: Long, limit: Int): List<Long>

    /** Exam mode: any card in scope, due or not. */
    @Query(
        """
        SELECT c.id FROM cards c
        WHERE (:deckId IS NULL OR c.deckId = :deckId)
          AND (:tagId IS NULL OR c.id IN (SELECT cardId FROM card_tags WHERE tagId = :tagId))
          AND (:dueOnly = 0 OR c.dueAt <= :now)
        ORDER BY
          CASE WHEN :order = 1 THEN c.createdAt END ASC,
          CASE WHEN :order = 2 THEN c.createdAt END DESC,
          CASE WHEN :order = 0 THEN c.id END ASC
        """
    )
    suspend fun examCardIds(
        deckId: Long?,
        tagId: Long?,
        dueOnly: Int,
        order: Int,
        now: Long,
    ): List<Long>

    @Query("SELECT COUNT(*) FROM cards")
    fun totalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE dueAt <= :now")
    fun dueCount(now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE dueAt <= :now")
    suspend fun dueCountNow(now: Long): Int

    @Query("SELECT COUNT(*) FROM cards WHERE intervalDays >= 21")
    fun masteredCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE repetitions = 0")
    fun newCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE repetitions > 0 AND intervalDays < 21")
    fun learningCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId IS NULL")
    fun unsortedCount(): Flow<Int>

    @Query("SELECT contentHash FROM cards")
    suspend fun allHashes(): List<String>

    @Query("SELECT id FROM cards WHERE contentHash = :hash LIMIT 1")
    suspend fun idForHash(hash: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(card: CardEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(cards: List<CardEntity>): List<Long>

    @Update
    suspend fun update(card: CardEntity)

    @Delete
    suspend fun delete(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteById(id: Long)

    // --- tag links ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkTags(refs: List<CardTagCrossRef>)

    @Query("DELETE FROM card_tags WHERE cardId = :cardId")
    suspend fun clearTags(cardId: Long)

    @Transaction
    suspend fun setTags(cardId: Long, tagIds: List<Long>) {
        clearTags(cardId)
        if (tagIds.isNotEmpty()) {
            linkTags(tagIds.map { CardTagCrossRef(cardId, it) })
        }
    }
}

@Dao
interface DeckDao {

    @Query("SELECT * FROM decks ORDER BY name COLLATE NOCASE ASC")
    fun all(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks ORDER BY name COLLATE NOCASE ASC")
    suspend fun allNow(): List<DeckEntity>

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun byId(id: Long): DeckEntity?

    @Query("SELECT * FROM decks WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun byName(name: String): DeckEntity?

    @Query(
        """
        SELECT d.*,
               (SELECT COUNT(*) FROM cards c WHERE c.deckId = d.id) AS cardCount,
               (SELECT COUNT(*) FROM cards c WHERE c.deckId = d.id AND c.dueAt <= :now) AS dueCount,
               (SELECT COUNT(*) FROM cards c WHERE c.deckId = d.id AND c.intervalDays >= 21) AS masteredCount
        FROM decks d
        ORDER BY dueCount DESC, cardCount DESC, d.name COLLATE NOCASE ASC
        """
    )
    fun withCounts(now: Long): Flow<List<DeckWithCounts>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(deck: DeckEntity): Long

    @Update
    suspend fun update(deck: DeckEntity)

    @Query("DELETE FROM decks WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun all(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun byId(id: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun byName(name: String): TagEntity?

    @Query(
        """
        SELECT t.*,
               (SELECT COUNT(*) FROM card_tags ct WHERE ct.tagId = t.id) AS cardCount,
               (SELECT COUNT(*) FROM card_tags ct
                  INNER JOIN cards c ON c.id = ct.cardId
                WHERE ct.tagId = t.id AND c.dueAt <= :now) AS dueCount
        FROM tags t
        ORDER BY cardCount DESC, t.name COLLATE NOCASE ASC
        """
    )
    fun withCounts(now: Long): Flow<List<TagWithCounts>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ReviewDao {

    @Insert
    suspend fun insert(review: ReviewEntity)

    @Query("SELECT reviewedAt FROM reviews WHERE reviewedAt >= :since")
    fun timestampsSince(since: Long): Flow<List<Long>>

    @Query("SELECT reviewedAt FROM reviews WHERE reviewedAt >= :since")
    suspend fun timestampsSinceNow(since: Long): List<Long>

    @Query("SELECT COUNT(*) FROM reviews WHERE reviewedAt >= :since")
    fun countSince(since: Long): Flow<Int>

    @Insert
    suspend fun insertAll(reviews: List<ReviewEntity>)
}
