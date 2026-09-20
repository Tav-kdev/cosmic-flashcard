package com.cosmic.flashcards.data

import com.cosmic.flashcards.data.dao.CardDao
import com.cosmic.flashcards.data.dao.DeckDao
import com.cosmic.flashcards.data.dao.ReviewDao
import com.cosmic.flashcards.data.dao.TagDao
import com.cosmic.flashcards.data.entity.CardEntity
import com.cosmic.flashcards.data.entity.CardWithRelations
import com.cosmic.flashcards.data.entity.DeckEntity
import com.cosmic.flashcards.data.entity.Mastery
import com.cosmic.flashcards.data.entity.ReviewEntity
import com.cosmic.flashcards.data.entity.TagEntity
import com.cosmic.flashcards.domain.ContentHash
import com.cosmic.flashcards.domain.ParsedCard
import com.cosmic.flashcards.domain.Scheduler
import java.util.Locale
import java.util.concurrent.TimeUnit

/** What an import actually did — surfaced to the user after the fact. */
data class ImportResult(
    val created: Int,
    val duplicatesInDb: Int,
    val duplicatesInPaste: Int,
) {
    val skipped: Int get() = duplicatesInDb + duplicatesInPaste
    val total: Int get() = created + skipped
}

/** Outcome of saving a card — the UI needs to distinguish a duplicate. */
sealed interface SaveResult {
    data class Ok(val id: Long) : SaveResult
    data object Duplicate : SaveResult
    data class Invalid(val message: String) : SaveResult
}

class Repository(
    val cards: CardDao,
    val decks: DeckDao,
    val tags: TagDao,
    val reviews: ReviewDao,
) {

    // ------------------------------------------------------------- cards

    suspend fun saveCard(
        existing: CardEntity?,
        front: String,
        back: String,
        deckId: Long?,
        tagIds: List<Long>,
        newTagNames: String,
    ): SaveResult {
        val f = front.trim()
        val b = back.trim()
        if (f.isEmpty()) return SaveResult.Invalid("The front can't be empty.")
        if (b.isEmpty()) return SaveResult.Invalid("The back can't be empty.")

        val hash = ContentHash.of(f, b)
        val clash = cards.idForHash(hash)
        if (clash != null && clash != existing?.id) return SaveResult.Duplicate

        val now = System.currentTimeMillis()
        val id: Long
        if (existing == null) {
            val inserted = cards.insert(
                CardEntity(
                    deckId = deckId,
                    front = f,
                    back = b,
                    contentHash = hash,
                    createdAt = now,
                    updatedAt = now,
                    // A new card is due immediately, so it is studyable at once.
                    mastery = Mastery(dueAt = now),
                )
            )
            if (inserted == -1L) return SaveResult.Duplicate
            id = inserted
        } else {
            // Editing the text must not reset the schedule.
            cards.update(
                existing.copy(
                    front = f,
                    back = b,
                    deckId = deckId,
                    contentHash = hash,
                    updatedAt = now,
                )
            )
            id = existing.id
        }

        val allTagIds = (tagIds + resolveTagNames(newTagNames)).distinct()
        cards.setTags(id, allTagIds)
        return SaveResult.Ok(id)
    }

    suspend fun deleteCard(id: Long) = cards.deleteById(id)

    // ------------------------------------------------------------ import

    suspend fun import(
        parsed: List<ParsedCard>,
        deckId: Long?,
        tagIds: List<Long>,
        newTagNames: String,
    ): ImportResult {
        val existing = cards.allHashes().toHashSet()
        val seen = HashSet<String>()
        var dupDb = 0
        var dupPaste = 0
        val toCreate = mutableListOf<CardEntity>()
        val now = System.currentTimeMillis()

        for (item in parsed) {
            val hash = ContentHash.of(item.front, item.back)
            when {
                hash in existing -> dupDb++
                hash in seen -> dupPaste++
                else -> {
                    seen.add(hash)
                    toCreate.add(
                        CardEntity(
                            deckId = deckId,
                            front = item.front,
                            back = item.back,
                            contentHash = hash,
                            createdAt = now,
                            updatedAt = now,
                            mastery = Mastery(dueAt = now),
                        )
                    )
                }
            }
        }

        if (toCreate.isEmpty()) return ImportResult(0, dupDb, dupPaste)

        val allTagIds = (tagIds + resolveTagNames(newTagNames)).distinct()
        val ids = cards.insertAll(toCreate).filter { it != -1L }
        for (id in ids) {
            if (allTagIds.isNotEmpty()) cards.setTags(id, allTagIds)
        }
        return ImportResult(ids.size, dupDb, dupPaste)
    }

    /** How many of these would be new, without writing anything. */
    suspend fun previewImport(parsed: List<ParsedCard>): List<Pair<ParsedCard, Boolean>> {
        val existing = cards.allHashes().toHashSet()
        val seen = HashSet<String>()
        return parsed.map { item ->
            val hash = ContentHash.of(item.front, item.back)
            val dupe = hash in existing || hash in seen
            seen.add(hash)
            item to dupe
        }
    }

    // -------------------------------------------------------------- decks

    suspend fun saveDeck(
        existing: DeckEntity?,
        name: String,
        emoji: String,
        accent: String,
    ): SaveResult {
        val n = name.trim()
        if (n.isEmpty()) return SaveResult.Invalid("Give the deck a name.")
        val clash = decks.byName(n)
        if (clash != null && clash.id != existing?.id) {
            return SaveResult.Invalid("You already have a deck with that name.")
        }
        return if (existing == null) {
            SaveResult.Ok(decks.insert(DeckEntity(name = n, emoji = emoji, accent = accent)))
        } else {
            decks.update(existing.copy(name = n, emoji = emoji, accent = accent))
            SaveResult.Ok(existing.id)
        }
    }

    /** Deleting a deck keeps its cards; the FK sets their deckId to NULL. */
    suspend fun deleteDeck(id: Long) = decks.deleteById(id)

    // --------------------------------------------------------------- tags

    suspend fun saveTag(existing: TagEntity?, name: String, accent: String): SaveResult {
        val n = normalizeTag(name)
        if (n.isEmpty()) return SaveResult.Invalid("Give the tag a name.")
        val clash = tags.byName(n)
        if (clash != null && clash.id != existing?.id) {
            return SaveResult.Invalid("That tag already exists.")
        }
        return if (existing == null) {
            SaveResult.Ok(tags.insert(TagEntity(name = n, accent = accent)))
        } else {
            tags.update(existing.copy(name = n, accent = accent))
            SaveResult.Ok(existing.id)
        }
    }

    suspend fun deleteTag(id: Long) = tags.deleteById(id)

    private fun normalizeTag(raw: String) =
        raw.trim().removePrefix("#").trim().lowercase(Locale.ROOT)

    /** Turns "a, b, c" into tag ids, creating any that don't exist yet. */
    private suspend fun resolveTagNames(raw: String): List<Long> {
        if (raw.isBlank()) return emptyList()
        val out = mutableListOf<Long>()
        for (part in raw.split(",")) {
            val name = normalizeTag(part)
            if (name.isEmpty()) continue
            val existing = tags.byName(name)
            out += existing?.id ?: tags.insert(TagEntity(name = name))
        }
        return out
    }

    // -------------------------------------------------------------- study

    /**
     * Grade a card: advance its SM-2 schedule, persist it, and log the review.
     * Returns the new interval in days.
     */
    suspend fun grade(card: CardEntity, quality: Int, mode: String): Int {
        val now = System.currentTimeMillis()
        val next = Scheduler.apply(card.mastery.toScheduling(), quality)
        val lapses = card.mastery.lapses + if (quality < 3) 1 else 0

        cards.update(
            card.copy(
                mastery = Mastery(
                    easeFactor = next.easeFactor,
                    intervalDays = next.intervalDays,
                    repetitions = next.repetitions,
                    dueAt = now + TimeUnit.DAYS.toMillis(next.intervalDays.toLong()),
                    lastReviewedAt = now,
                    lapses = lapses,
                )
            )
        )
        reviews.insert(
            ReviewEntity(
                cardId = card.id,
                quality = quality,
                intervalDays = next.intervalDays,
                mode = mode,
                reviewedAt = now,
            )
        )
        return next.intervalDays
    }

    suspend fun cardsByIds(ids: List<Long>): List<CardWithRelations> = cards.byIds(ids)
}
