package com.cosmic.flashcards.data

import android.content.Context
import com.cosmic.flashcards.data.entity.CardEntity
import com.cosmic.flashcards.data.entity.DeckEntity
import com.cosmic.flashcards.data.entity.Mastery
import com.cosmic.flashcards.data.entity.TagEntity
import com.cosmic.flashcards.domain.ContentHash

/**
 * Puts one small deck in place on first launch, so the app opens with
 * something to study instead of an empty vault.
 *
 * Runs once ever: the flag is kept even if the user deletes every card, so
 * clearing the deck out doesn't bring it back.
 */
object Seeder {

    private const val PREFS = "cosmic_prefs"
    private const val KEY_SEEDED = "seeded_v1"

    private val WELCOME = listOf(
        "How does Cosmic decide when to show a card again?" to
            "It uses SM-2. Each answer adjusts the card's ease factor and multiplies its interval, so cards you know drift further apart.",
        "What do the four grade buttons mean?" to
            "Again (blanked), Hard (struggled), Good (recalled), Easy (instant). The number under each one is the interval that answer would schedule.",
        "What's the difference between Normal and Exam mode?" to
            "Normal works your due queue and brings missed cards back before you finish. Exam runs a fixed set straight through, no repeats.",
        "How do I add a lot of cards at once?" to
            "Import. Paste 'front | back' lines — or Q:/A: blocks, CSV, or blank-line-separated blocks — and it works out the format.",
        "Will it import the same card twice?" to
            "No. Every card is fingerprinted by its text, ignoring case and spacing, so re-importing the same paste is safe.",
        "Where is my data stored?" to
            "In a SQLite database on this device only. The app requests no permissions and sends nothing anywhere.",
    )

    suspend fun seedIfFirstRun(context: Context, repo: Repository) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SEEDED, false)) return

        // Mark first so a crash mid-seed can't cause a duplicate run.
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()

        if (repo.cards.allHashes().isNotEmpty()) return

        val deckId = repo.decks.insert(
            DeckEntity(name = "Welcome to Cosmic", emoji = "✦", accent = "cyan")
        )
        val tagId = repo.tags.insert(TagEntity(name = "cosmic", accent = "purple"))

        val now = System.currentTimeMillis()
        val cards = WELCOME.map { (front, back) ->
            CardEntity(
                deckId = deckId,
                front = front,
                back = back,
                contentHash = ContentHash.of(front, back),
                createdAt = now,
                updatedAt = now,
                mastery = Mastery(dueAt = now),
            )
        }
        val ids = repo.cards.insertAll(cards).filter { it != -1L }
        ids.forEach { repo.cards.setTags(it, listOf(tagId)) }
    }
}
