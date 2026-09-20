package com.cosmic.flashcards

import android.app.Application
import com.cosmic.flashcards.data.CosmicDatabase
import com.cosmic.flashcards.data.Repository
import com.cosmic.flashcards.data.Seeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual dependency container.
 *
 * Deliberately not Hilt: one database, one repository, a handful of
 * ViewModels. A DI framework would add an annotation processor and a lot of
 * generated code for no benefit at this size.
 */
class CosmicApp : Application() {

    val database: CosmicDatabase by lazy { CosmicDatabase.get(this) }

    val repository: Repository by lazy {
        Repository(
            cards = database.cardDao(),
            decks = database.deckDao(),
            tags = database.tagDao(),
            reviews = database.reviewDao(),
        )
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Off the main thread: touching the database here would otherwise
        // block the first frame.
        appScope.launch { Seeder.seedIfFirstRun(this@CosmicApp, repository) }
    }
}
