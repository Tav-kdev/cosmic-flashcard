package com.cosmic.flashcards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmic.flashcards.data.entity.DeckEntity
import com.cosmic.flashcards.data.entity.TagEntity
import com.cosmic.flashcards.ui.components.Caption
import com.cosmic.flashcards.ui.components.Chip
import com.cosmic.flashcards.ui.components.CosmicDropdown
import com.cosmic.flashcards.ui.components.CosmicField
import com.cosmic.flashcards.ui.components.DangerButton
import com.cosmic.flashcards.ui.components.DeckTile
import com.cosmic.flashcards.ui.components.EmptyState
import com.cosmic.flashcards.ui.components.GhostButton
import com.cosmic.flashcards.ui.components.GlassCard
import com.cosmic.flashcards.ui.components.PrimaryButton
import com.cosmic.flashcards.ui.components.ProgressBar
import com.cosmic.flashcards.ui.components.SectionLabel
import com.cosmic.flashcards.ui.components.SegmentedRow
import com.cosmic.flashcards.ui.nav.BottomSpacer
import com.cosmic.flashcards.ui.theme.Accent
import com.cosmic.flashcards.ui.theme.Accents
import com.cosmic.flashcards.ui.theme.DECK_EMOJI
import com.cosmic.flashcards.ui.theme.NeonCyan
import com.cosmic.flashcards.ui.theme.NeonPurple
import com.cosmic.flashcards.ui.theme.Rose
import com.cosmic.flashcards.ui.theme.TextMuted
import com.cosmic.flashcards.ui.theme.TextSecondary
import com.cosmic.flashcards.vm.DecksViewModel
import com.cosmic.flashcards.vm.StudyViewModel
import com.cosmic.flashcards.vm.TagsViewModel

// ----------------------------------------------------------------- decks

@Composable
fun DecksScreen(
    vm: DecksViewModel,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenCards: (Long) -> Unit,
    onOpenUnsorted: () -> Unit,
    onStudyDeck: (Long, String) -> Unit,
) {
    val decks by vm.decks.collectAsStateWithLifecycle()
    val unsorted by vm.unsortedCount.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Decks", style = MaterialTheme.typography.titleLarge)
                    Caption("${decks.size} deck${plural(decks.size)} in orbit", color = TextSecondary)
                }
                GhostButton("+ Deck", onCreate, tint = NeonCyan)
            }
        }

        if (decks.isEmpty()) {
            item {
                EmptyState("🗂️", "No decks yet",
                    "Decks group your cards — start with one per subject.") {
                    PrimaryButton("+ Create your first deck", onCreate, Modifier.weight(1f))
                }
            }
        }

        items(decks, key = { it.deck.id }) { d ->
            val accent = Accents.of(d.deck.accent)
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        DeckTile(d.deck.emoji, accent)
                        Column(Modifier.weight(1f)) {
                            Text(d.deck.name, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Caption(
                                "${d.cardCount} card${plural(d.cardCount)}" +
                                    if (d.dueCount > 0) " · ${d.dueCount} due" else "",
                                Modifier.padding(top = 2.dp), TextSecondary,
                            )
                        }
                        Text("${d.masteredPct}%", color = accent.solid,
                            fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }

                    ProgressBar(
                        d.masteredPct / 100f,
                        Modifier.padding(top = 12.dp),
                        height = 6,
                    )

                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Chip("Cards", TextSecondary, onClick = { onOpenCards(d.deck.id) })
                        if (d.dueCount > 0) {
                            Chip("Study", NeonCyan,
                                onClick = { onStudyDeck(d.deck.id, d.deck.name) })
                        }
                        Box(Modifier.weight(1f))
                        Text("Edit", color = TextSecondary, fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onEdit(d.deck.id) }
                                .padding(horizontal = 8.dp, vertical = 4.dp))
                        Text("Delete", color = Rose, fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { vm.delete(d.deck.id) }
                                .padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }

        if (unsorted > 0) {
            item {
                GlassCard(Modifier.fillMaxWidth(), onClick = onOpenUnsorted) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.10f),
                                    RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Text("📭", fontSize = 20.sp) }
                        Column(Modifier.weight(1f)) {
                            Text("Unsorted", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Caption("$unsorted card${plural(unsorted)} with no deck",
                                Modifier.padding(top = 2.dp), TextSecondary)
                        }
                    }
                }
            }
        }

        item { BottomSpacer() }
    }
}

@Composable
fun DeckEditScreen(
    vm: DecksViewModel,
    deckId: Long,
    onDone: () -> Unit,
) {
    var existing by remember { mutableStateOf<DeckEntity?>(null) }
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🗂️") }
    var accentKey by remember { mutableStateOf("cyan") }
    var loaded by remember { mutableStateOf(false) }
    val error by vm.error.collectAsStateWithLifecycle()

    LaunchedEffect(deckId) {
        if (deckId > 0) {
            val d = vm.deckById(deckId)
            if (d != null) {
                existing = d; name = d.name; emoji = d.emoji; accentKey = d.accent
            }
        }
        loaded = true
    }

    if (!loaded) return
    val accent = Accents.of(accentKey)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                if (existing == null) "New Deck" else "Edit Deck",
                style = MaterialTheme.typography.titleLarge,
            )
        }

        // live preview
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    DeckTile(emoji, accent)
                    Column(Modifier.weight(1f)) {
                        Text(
                            name.ifBlank { "Your deck name" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (name.isBlank()) TextMuted else Color.White,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Caption("preview", Modifier.padding(top = 2.dp))
                    }
                }
            }
        }

        if (error != null) {
            item {
                GlassCard(Modifier.fillMaxWidth(), borderColor = Rose.copy(alpha = 0.35f)) {
                    Text(error.orEmpty(), color = Rose, fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp))
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CosmicField(
                        value = name,
                        onValueChange = { name = it; vm.clearError() },
                        label = "Deck name",
                        placeholder = "Neuroscience Foundations",
                    )

                    Column {
                        SectionLabel("Icon", Modifier.padding(bottom = 8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            DECK_EMOJI.forEach { e ->
                                val on = e == emoji
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (on) NeonCyan.copy(alpha = 0.18f)
                                            else Color.White.copy(alpha = 0.05f)
                                        )
                                        .border(
                                            width = if (on) 1.dp else 0.dp,
                                            color = if (on) NeonCyan.copy(alpha = 0.5f) else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp),
                                        )
                                        .clickable { emoji = e },
                                    contentAlignment = Alignment.Center,
                                ) { Text(e, fontSize = 18.sp) }
                            }
                        }
                    }

                    Column {
                        SectionLabel("Accent", Modifier.padding(bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Accents.ALL.forEach { a ->
                                AccentSwatch(a, a.key == accentKey, Modifier.weight(1f)) {
                                    accentKey = a.key
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    if (existing == null) "Create deck" else "Save changes",
                    onClick = { vm.save(existing, name, emoji, accentKey, onDone) },
                    modifier = Modifier.fillMaxWidth(),
                )
                val deck = existing
                if (deck != null) {
                    DangerButton(
                        "Delete deck",
                        onClick = { vm.delete(deck.id); onDone() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Caption("Its cards move to Unsorted — they aren't deleted.",
                        Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun AccentSwatch(
    accent: Accent,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.tile)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
    )
}

// ------------------------------------------------------------------ tags

@Composable
fun TagsScreen(
    vm: TagsViewModel,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenCards: (Long) -> Unit,
    onStudyTag: (Long, String) -> Unit,
) {
    val tags by vm.tags.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tags", style = MaterialTheme.typography.titleLarge)
                    Caption("Cross-cutting labels, independent of decks", color = TextSecondary)
                }
                GhostButton("+ Tag", onCreate, tint = NeonCyan)
            }
        }

        if (tags.isEmpty()) {
            item {
                EmptyState("🏷️", "No tags yet",
                    "Tags cut across decks — one card can carry several.") {
                    PrimaryButton("+ Create a tag", onCreate, Modifier.weight(1f))
                }
            }
        }

        items(tags, key = { it.tag.id }) { t ->
            val accent = Accents.of(t.tag.accent)
            GlassCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Chip("#${t.tag.name}", accent.solid, onClick = { onOpenCards(t.tag.id) })
                    Column(Modifier.weight(1f)) {
                        Caption(
                            "${t.cardCount} card${plural(t.cardCount)}" +
                                if (t.dueCount > 0) " · ${t.dueCount} due" else "",
                            color = TextSecondary,
                        )
                    }
                    if (t.dueCount > 0) {
                        Chip("Study", NeonCyan, onClick = { onStudyTag(t.tag.id, "#${t.tag.name}") })
                    }
                    Text("Edit", color = TextSecondary, fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEdit(t.tag.id) }
                            .padding(horizontal = 6.dp, vertical = 4.dp))
                    Text("✕", color = Rose, fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { vm.delete(t.tag.id) }
                            .padding(horizontal = 6.dp, vertical = 4.dp))
                }
            }
        }

        item { BottomSpacer() }
    }
}

@Composable
fun TagEditScreen(
    vm: TagsViewModel,
    tagId: Long,
    onDone: () -> Unit,
) {
    val tags by vm.tags.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    val existing: TagEntity? = remember(tags, tagId) {
        if (tagId > 0) tags.firstOrNull { it.tag.id == tagId }?.tag else null
    }

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var accentKey by remember(existing) { mutableStateOf(existing?.accent ?: "purple") }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                if (existing == null) "New Tag" else "Edit Tag",
                style = MaterialTheme.typography.titleLarge,
            )
        }

        if (error != null) {
            item {
                GlassCard(Modifier.fillMaxWidth(), borderColor = Rose.copy(alpha = 0.35f)) {
                    Text(error.orEmpty(), color = Rose, fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp))
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CosmicField(
                        value = name,
                        onValueChange = { name = it; vm.clearError() },
                        label = "Tag name",
                        placeholder = "django",
                        supportingText = "Lower-cased automatically. No need for the #.",
                    )
                    Column {
                        SectionLabel("Accent", Modifier.padding(bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Accents.ALL.forEach { a ->
                                AccentSwatch(a, a.key == accentKey, Modifier.weight(1f)) {
                                    accentKey = a.key
                                }
                            }
                        }
                    }
                    Box {
                        Chip("#${name.ifBlank { "preview" }}", Accents.of(accentKey).solid)
                    }
                }
            }
        }

        item {
            PrimaryButton(
                if (existing == null) "Create tag" else "Save changes",
                onClick = { vm.save(existing, name, accentKey, onDone) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ------------------------------------------------------------ exam setup

@Composable
fun ExamSetupScreen(
    vm: StudyViewModel,
    onStarted: () -> Unit,
) {
    val decks by vm.decks.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()

    var scope by remember { mutableStateOf("all") }
    var deckId by remember { mutableStateOf<Long?>(null) }
    var tagId by remember { mutableStateOf<Long?>(null) }
    var limitText by remember { mutableStateOf("20") }
    var order by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Exam Mode", style = MaterialTheme.typography.titleLarge)
                Caption("A fixed set, run straight through", color = TextSecondary)
            }
        }

        if (error != null) {
            item {
                GlassCard(Modifier.fillMaxWidth(), borderColor = Rose.copy(alpha = 0.35f)) {
                    Text(error.orEmpty(), color = Rose, fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp))
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column {
                        SectionLabel("Draw from", Modifier.padding(bottom = 8.dp))
                        SegmentedRow(
                            options = listOf(
                                "all" to "Everything",
                                "deck" to "One deck",
                                "tag" to "One tag",
                                "due" to "Only what's due",
                            ),
                            selectedKey = scope,
                            onSelect = { scope = it; error = null },
                        )
                    }

                    if (scope == "deck") {
                        CosmicDropdown(
                            label = "Deck",
                            options = decks.map { it.deck },
                            selected = decks.map { it.deck }.firstOrNull { it.id == deckId },
                            optionLabel = { it?.let { d -> "${d.emoji}  ${d.name}" } ?: "— Pick a deck —" },
                            onSelect = { deckId = it?.id },
                            noneLabel = "— Pick a deck —",
                        )
                    }

                    if (scope == "tag") {
                        CosmicDropdown(
                            label = "Tag",
                            options = tags.map { it.tag },
                            selected = tags.map { it.tag }.firstOrNull { it.id == tagId },
                            optionLabel = { it?.let { t -> "#${t.name}" } ?: "— Pick a tag —" },
                            onSelect = { tagId = it?.id },
                            noneLabel = "— Pick a tag —",
                        )
                    }

                    CosmicField(
                        value = limitText,
                        onValueChange = { v -> limitText = v.filter { it.isDigit() }.take(3) },
                        label = "How many cards",
                        placeholder = "20",
                    )

                    Column {
                        SectionLabel("Order", Modifier.padding(bottom = 8.dp))
                        SegmentedRow(
                            options = listOf(
                                "0" to "Shuffled",
                                "1" to "Oldest first",
                                "2" to "Newest first",
                            ),
                            selectedKey = order.toString(),
                            onSelect = { order = it.toInt() },
                            accent = NeonCyan,
                        )
                    }
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    "Exam answers still update each card's SM-2 schedule, so a strong run pushes " +
                        "cards further out. Cards you miss don't repeat within the session — they " +
                        "just come back sooner in normal mode.",
                    color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }

        item {
            PrimaryButton(
                "Start exam",
                onClick = {
                    when {
                        scope == "deck" && deckId == null ->
                            error = "Pick a deck, or change the scope."
                        scope == "tag" && tagId == null ->
                            error = "Pick a tag, or change the scope."
                        else -> {
                            error = null
                            val label = when (scope) {
                                "deck" -> "Exam · " + (decks.firstOrNull { it.deck.id == deckId }?.deck?.name ?: "deck")
                                "tag" -> "Exam · #" + (tags.firstOrNull { it.tag.id == tagId }?.tag?.name ?: "tag")
                                "due" -> "Exam · due only"
                                else -> "Exam · everything"
                            }
                            vm.startExam(
                                deckId = if (scope == "deck") deckId else null,
                                tagId = if (scope == "tag") tagId else null,
                                dueOnly = scope == "due",
                                limit = limitText.toIntOrNull() ?: 20,
                                order = order,
                                label = label,
                            )
                            onStarted()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
