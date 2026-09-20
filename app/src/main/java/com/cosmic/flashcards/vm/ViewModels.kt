package com.cosmic.flashcards.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cosmic.flashcards.CosmicApp
import com.cosmic.flashcards.data.ImportResult
import com.cosmic.flashcards.data.Repository
import com.cosmic.flashcards.data.SaveResult
import com.cosmic.flashcards.data.dao.CardSort
import com.cosmic.flashcards.data.dao.CardState
import com.cosmic.flashcards.data.entity.CardEntity
import com.cosmic.flashcards.data.entity.CardWithRelations
import com.cosmic.flashcards.data.entity.DeckEntity
import com.cosmic.flashcards.data.entity.DeckWithCounts
import com.cosmic.flashcards.data.entity.TagEntity
import com.cosmic.flashcards.data.entity.TagWithCounts
import com.cosmic.flashcards.domain.ActivityDay
import com.cosmic.flashcards.domain.ImportParser
import com.cosmic.flashcards.domain.ParsedCard
import com.cosmic.flashcards.domain.Scheduler
import com.cosmic.flashcards.domain.StatsCalc
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** Builds every ViewModel from the application container. */
object CosmicViewModelFactory {

    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { DashboardViewModel(app().repository) }
        initializer { CardsViewModel(app().repository) }
        initializer { CardEditViewModel(app().repository) }
        initializer { DecksViewModel(app().repository) }
        initializer { TagsViewModel(app().repository) }
        initializer { ImportViewModel(app().repository) }
        initializer { StudyViewModel(app().repository) }
    }

    private fun CreationExtras.app(): CosmicApp =
        this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CosmicApp
}

private fun nowMillis() = System.currentTimeMillis()
private fun zone(): ZoneId = ZoneId.systemDefault()

// ------------------------------------------------------------------ dashboard

data class DashboardState(
    val total: Int = 0,
    val due: Int = 0,
    val mastered: Int = 0,
    val learning: Int = 0,
    val newCards: Int = 0,
    val deckCount: Int = 0,
    val streak: Int = 0,
    val reviewedToday: Int = 0,
    val activity: List<ActivityDay> = emptyList(),
    val decks: List<DeckWithCounts> = emptyList(),
    val tags: List<TagWithCounts> = emptyList(),
    val loading: Boolean = true,
) {
    val totalDisplay get() = StatsCalc.compact(total)
    val dueDisplay get() = StatsCalc.compact(due)
    val masteredDisplay get() = StatsCalc.compact(mastered)
    val masteredPct get() = StatsCalc.percent(mastered, total)
    val progressPct get() = StatsCalc.percent(mastered + learning, total)
    val orbitStatus get() = StatsCalc.orbitStatus(total, due, streak, masteredPct)
}

class DashboardViewModel(private val repo: Repository) : ViewModel() {

    private val now = nowMillis()
    private val since = now - TimeUnit.DAYS.toMillis(400)

    val state: StateFlow<DashboardState> = combine(
        repo.cards.totalCount(),
        repo.cards.dueCount(now),
        repo.cards.masteredCount(),
        repo.cards.learningCount(),
        repo.cards.newCount(),
    ) { total, due, mastered, learning, newCards ->
        intArrayOf(total, due, mastered, learning, newCards)
    }.combine(repo.decks.withCounts(now)) { counts, decks ->
        counts to decks
    }.combine(repo.tags.withCounts(now)) { (counts, decks), tags ->
        Triple(counts, decks, tags)
    }.combine(repo.reviews.timestampsSince(since)) { (counts, decks, tags), reviews ->
        val today = LocalDate.now(zone())
        DashboardState(
            total = counts[0],
            due = counts[1],
            mastered = counts[2],
            learning = counts[3],
            newCards = counts[4],
            deckCount = decks.size,
            streak = StatsCalc.streak(reviews, zone(), today),
            reviewedToday = reviews.count { StatsCalc.millisToDate(it, zone()) == today },
            activity = StatsCalc.activity(reviews, zone(), today),
            decks = decks.take(4),
            tags = tags.filter { it.cardCount > 0 }.take(5),
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())
}

// ---------------------------------------------------------------------- cards

data class CardFilters(
    val query: String = "",
    val deckId: Long? = null,
    val unsortedOnly: Boolean = false,
    val tagId: Long? = null,
    val state: Int = CardState.ANY,
    val sort: Int = CardSort.RECENT,
) {
    val isActive: Boolean
        get() = query.isNotBlank() || deckId != null || unsortedOnly ||
            tagId != null || state != CardState.ANY
}

@OptIn(ExperimentalCoroutinesApi::class)
class CardsViewModel(private val repo: Repository) : ViewModel() {

    private val _filters = MutableStateFlow(CardFilters())
    val filters: StateFlow<CardFilters> = _filters.asStateFlow()

    /** Which card has its answer revealed inline, if any. */
    private val _revealed = MutableStateFlow<Set<Long>>(emptySet())
    val revealed: StateFlow<Set<Long>> = _revealed.asStateFlow()

    val decks: StateFlow<List<DeckEntity>> =
        repo.decks.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tags: StateFlow<List<TagEntity>> =
        repo.tags.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cards: StateFlow<List<CardWithRelations>> = _filters
        .flatMapLatest { f ->
            repo.cards.filter(
                query = f.query.trim().ifBlank { null },
                deckId = f.deckId,
                unsortedOnly = if (f.unsortedOnly) 1 else 0,
                tagId = f.tagId,
                state = f.state,
                sort = f.sort,
                now = nowMillis(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(q: String) { _filters.value = _filters.value.copy(query = q) }
    fun setDeck(id: Long?, unsorted: Boolean = false) {
        _filters.value = _filters.value.copy(deckId = id, unsortedOnly = unsorted)
    }
    fun setTag(id: Long?) { _filters.value = _filters.value.copy(tagId = id) }
    fun setState(s: Int) { _filters.value = _filters.value.copy(state = s) }
    fun setSort(s: Int) { _filters.value = _filters.value.copy(sort = s) }
    fun clearFilters() { _filters.value = CardFilters() }

    fun toggleReveal(id: Long) {
        _revealed.value = _revealed.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
    }

    fun delete(id: Long) = viewModelScope.launch { repo.deleteCard(id) }
}

// ------------------------------------------------------------------ card edit

data class CardEditState(
    val loading: Boolean = true,
    val existing: CardEntity? = null,
    val front: String = "",
    val back: String = "",
    val deckId: Long? = null,
    val selectedTagIds: Set<Long> = emptySet(),
    val newTags: String = "",
    val error: String? = null,
    val saved: Boolean = false,
)

class CardEditViewModel(private val repo: Repository) : ViewModel() {

    private val _state = MutableStateFlow(CardEditState())
    val state: StateFlow<CardEditState> = _state.asStateFlow()

    val decks: StateFlow<List<DeckEntity>> =
        repo.decks.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tags: StateFlow<List<TagEntity>> =
        repo.tags.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** cardId <= 0 means "new card". */
    fun load(cardId: Long, presetDeckId: Long? = null) {
        viewModelScope.launch {
            if (cardId <= 0L) {
                _state.value = CardEditState(loading = false, deckId = presetDeckId)
            } else {
                val found = repo.cards.byId(cardId)
                _state.value = if (found == null) {
                    CardEditState(loading = false, error = "That card no longer exists.")
                } else {
                    CardEditState(
                        loading = false,
                        existing = found.card,
                        front = found.card.front,
                        back = found.card.back,
                        deckId = found.card.deckId,
                        selectedTagIds = found.tags.map { it.id }.toSet(),
                    )
                }
            }
        }
    }

    fun setFront(v: String) { _state.value = _state.value.copy(front = v, error = null) }
    fun setBack(v: String) { _state.value = _state.value.copy(back = v, error = null) }
    fun setDeck(id: Long?) { _state.value = _state.value.copy(deckId = id) }
    fun setNewTags(v: String) { _state.value = _state.value.copy(newTags = v) }

    fun toggleTag(id: Long) {
        val next = _state.value.selectedTagIds.toMutableSet()
        if (!next.add(id)) next.remove(id)
        _state.value = _state.value.copy(selectedTagIds = next)
    }

    fun save(onDone: (Boolean) -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            when (val result = repo.saveCard(
                existing = s.existing,
                front = s.front,
                back = s.back,
                deckId = s.deckId,
                tagIds = s.selectedTagIds.toList(),
                newTagNames = s.newTags,
            )) {
                is SaveResult.Ok -> { _state.value = s.copy(saved = true, error = null); onDone(true) }
                is SaveResult.Duplicate -> {
                    _state.value = s.copy(error = "You already have an identical card — nothing was saved.")
                    onDone(false)
                }
                is SaveResult.Invalid -> {
                    _state.value = s.copy(error = result.message); onDone(false)
                }
            }
        }
    }

    /** Save and immediately reset for the next card. */
    fun saveAndAddAnother(onResult: (Boolean) -> Unit) {
        save { ok ->
            if (ok) {
                _state.value = CardEditState(loading = false, deckId = _state.value.deckId)
            }
            onResult(ok)
        }
    }

    fun delete(onDone: () -> Unit) {
        val existing = _state.value.existing ?: return
        viewModelScope.launch { repo.deleteCard(existing.id); onDone() }
    }
}

// ---------------------------------------------------------------------- decks

class DecksViewModel(private val repo: Repository) : ViewModel() {

    val decks: StateFlow<List<DeckWithCounts>> =
        repo.decks.withCounts(nowMillis())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unsortedCount: StateFlow<Int> =
        repo.cards.unsortedCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _editing = MutableStateFlow<DeckEntity?>(null)
    val editing: StateFlow<DeckEntity?> = _editing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun startEdit(deck: DeckEntity?) { _editing.value = deck; _error.value = null }

    fun save(existing: DeckEntity?, name: String, emoji: String, accent: String, onDone: () -> Unit) {
        viewModelScope.launch {
            when (val r = repo.saveDeck(existing, name, emoji, accent)) {
                is SaveResult.Ok -> { _error.value = null; onDone() }
                is SaveResult.Invalid -> _error.value = r.message
                SaveResult.Duplicate -> _error.value = "That deck already exists."
            }
        }
    }

    fun delete(id: Long) = viewModelScope.launch { repo.deleteDeck(id) }
    fun clearError() { _error.value = null }
    suspend fun deckById(id: Long) = repo.decks.byId(id)
}

// ----------------------------------------------------------------------- tags

class TagsViewModel(private val repo: Repository) : ViewModel() {

    val tags: StateFlow<List<TagWithCounts>> =
        repo.tags.withCounts(nowMillis())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun save(existing: TagEntity?, name: String, accent: String, onDone: () -> Unit) {
        viewModelScope.launch {
            when (val r = repo.saveTag(existing, name, accent)) {
                is SaveResult.Ok -> { _error.value = null; onDone() }
                is SaveResult.Invalid -> _error.value = r.message
                SaveResult.Duplicate -> _error.value = "That tag already exists."
            }
        }
    }

    fun delete(id: Long) = viewModelScope.launch { repo.deleteTag(id) }
    fun clearError() { _error.value = null }
}

// --------------------------------------------------------------------- import

data class ImportState(
    val text: String = "",
    val preview: List<Pair<ParsedCard, Boolean>> = emptyList(),
    val deckId: Long? = null,
    val selectedTagIds: Set<Long> = emptySet(),
    val newTags: String = "",
    val result: ImportResult? = null,
    val message: String? = null,
) {
    val newCount get() = preview.count { !it.second }
    val dupeCount get() = preview.count { it.second }
}

class ImportViewModel(private val repo: Repository) : ViewModel() {

    private val _state = MutableStateFlow(ImportState())
    val state: StateFlow<ImportState> = _state.asStateFlow()

    val decks: StateFlow<List<DeckEntity>> =
        repo.decks.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tags: StateFlow<List<TagEntity>> =
        repo.tags.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setText(v: String) {
        _state.value = _state.value.copy(text = v, message = null)
        viewModelScope.launch {
            val parsed = ImportParser.parse(v)
            // Cap the preview; the import itself still handles the whole paste.
            _state.value = _state.value.copy(preview = repo.previewImport(parsed.take(200)))
        }
    }

    fun setDeck(id: Long?) { _state.value = _state.value.copy(deckId = id) }
    fun setNewTags(v: String) { _state.value = _state.value.copy(newTags = v) }

    fun toggleTag(id: Long) {
        val next = _state.value.selectedTagIds.toMutableSet()
        if (!next.add(id)) next.remove(id)
        _state.value = _state.value.copy(selectedTagIds = next)
    }

    fun runImport(onDone: (ImportResult?) -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            val parsed = ImportParser.parse(s.text)
            if (parsed.isEmpty()) {
                _state.value = s.copy(
                    message = "Couldn't find any front/back pairs in that. Try 'front | back' per line."
                )
                onDone(null)
                return@launch
            }
            val result = repo.import(parsed, s.deckId, s.selectedTagIds.toList(), s.newTags)
            _state.value = if (result.created > 0) {
                ImportState(deckId = s.deckId, result = result)
            } else {
                s.copy(
                    result = result,
                    message = "Nothing new — all ${result.skipped} card" +
                        (if (result.skipped == 1) " was" else "s were") +
                        " already in your collection.",
                )
            }
            onDone(result)
        }
    }
}

// ---------------------------------------------------------------------- study

enum class StudyMode { NORMAL, EXAM }

data class StudySession(
    val mode: StudyMode,
    val label: String,
    val queue: List<Long>,
    val total: Int,
    val correct: Int = 0,
    val again: Int = 0,
) {
    val answered: Int get() = total - queue.size
    val position: Int get() = answered + 1
    val progress: Float get() = if (total == 0) 0f else answered.toFloat() / total
    val graded: Int get() = correct + again
    val accuracy: Int get() = StatsCalc.percent(correct, graded)
}

data class StudyUiState(
    val session: StudySession? = null,
    val card: CardWithRelations? = null,
    val revealed: Boolean = false,
    val previews: Map<String, String> = emptyMap(),
    val finished: Boolean = false,
    val message: String? = null,
    val loading: Boolean = false,
)

class StudyViewModel(private val repo: Repository) : ViewModel() {

    private val _ui = MutableStateFlow(StudyUiState())
    val ui: StateFlow<StudyUiState> = _ui.asStateFlow()

    val dueCount: StateFlow<Int> =
        repo.cards.dueCount(nowMillis())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val totalCount: StateFlow<Int> =
        repo.cards.totalCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val newCount: StateFlow<Int> =
        repo.cards.newCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val decks: StateFlow<List<DeckWithCounts>> =
        repo.decks.withCounts(nowMillis())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tags: StateFlow<List<TagWithCounts>> =
        repo.tags.withCounts(nowMillis())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---- starting a session ----

    fun startNormal(deckId: Long? = null, tagId: Long? = null, label: String? = null) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            val now = nowMillis()
            val ids = when {
                deckId != null -> repo.cards.dueCardIdsInDeck(now, deckId, 200)
                tagId != null -> repo.cards.dueCardIdsWithTag(now, tagId, 200)
                else -> repo.cards.dueCardIds(now, 200)
            }
            if (ids.isEmpty()) {
                _ui.value = StudyUiState(message = "Nothing due in that scope right now.")
                return@launch
            }
            beginSession(StudySession(StudyMode.NORMAL, label ?: "Due queue", ids, ids.size))
        }
    }

    /** order: 0 = shuffled, 1 = oldest first, 2 = newest first. */
    fun startExam(
        deckId: Long?,
        tagId: Long?,
        dueOnly: Boolean,
        limit: Int,
        order: Int,
        label: String,
    ) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            val now = nowMillis()
            var ids = repo.cards.examCardIds(
                deckId = deckId,
                tagId = tagId,
                dueOnly = if (dueOnly) 1 else 0,
                order = order,
                now = now,
            )
            if (order == 0) ids = ids.shuffled()
            ids = ids.take(limit.coerceAtLeast(1))

            if (ids.isEmpty()) {
                _ui.value = StudyUiState(message = "No cards matched that setup.")
                return@launch
            }
            beginSession(StudySession(StudyMode.EXAM, label, ids, ids.size))
        }
    }

    private suspend fun beginSession(session: StudySession) {
        _ui.value = StudyUiState(session = session, loading = true)
        loadCurrent(session)
    }

    private suspend fun loadCurrent(session: StudySession) {
        if (session.queue.isEmpty()) {
            _ui.value = _ui.value.copy(session = session, card = null, finished = true, loading = false)
            return
        }
        val id = session.queue.first()
        val card = repo.cards.byId(id)
        if (card == null) {
            // Deleted mid-session — drop it and move on rather than crashing.
            val trimmed = session.copy(
                queue = session.queue.drop(1),
                total = (session.total - 1).coerceAtLeast(0),
            )
            loadCurrent(trimmed)
            return
        }
        _ui.value = _ui.value.copy(
            session = session,
            card = card,
            revealed = false,
            previews = Scheduler.previewIntervals(card.card.mastery.toScheduling()),
            finished = false,
            loading = false,
        )
    }

    fun reveal() { _ui.value = _ui.value.copy(revealed = true) }

    fun grade(gradeKey: String) {
        val ui = _ui.value
        val session = ui.session ?: return
        val card = ui.card ?: return
        if (!ui.revealed) return

        viewModelScope.launch {
            val grade = Scheduler.gradeFor(gradeKey)
            repo.grade(card.card, grade.quality, session.mode.name.lowercase())

            var next = session.copy(queue = session.queue.drop(1))
            next = if (grade.quality >= 3) {
                next.copy(correct = next.correct + 1)
            } else {
                // A miss comes back at the end of the same run — but only in
                // normal mode; exam mode runs straight through.
                if (session.mode == StudyMode.NORMAL) {
                    next.copy(
                        again = next.again + 1,
                        queue = next.queue + card.card.id,
                        total = next.total + 1,
                    )
                } else {
                    next.copy(again = next.again + 1)
                }
            }
            loadCurrent(next)
        }
    }

    fun endSession() {
        val s = _ui.value.session
        _ui.value = _ui.value.copy(
            session = s?.copy(queue = emptyList()),
            card = null,
            finished = true,
        )
    }

    fun reset() { _ui.value = StudyUiState() }
    fun clearMessage() { _ui.value = _ui.value.copy(message = null) }
}
