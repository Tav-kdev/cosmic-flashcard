package com.cosmic.flashcards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmic.flashcards.ui.components.Caption
import com.cosmic.flashcards.ui.components.Chip
import com.cosmic.flashcards.ui.components.CosmicDropdown
import com.cosmic.flashcards.ui.components.CosmicField
import com.cosmic.flashcards.ui.components.DangerButton
import com.cosmic.flashcards.ui.components.GhostButton
import com.cosmic.flashcards.ui.components.GlassCard
import com.cosmic.flashcards.ui.components.PrimaryButton
import com.cosmic.flashcards.ui.components.SectionLabel
import com.cosmic.flashcards.ui.theme.Accents
import com.cosmic.flashcards.ui.theme.Amber
import com.cosmic.flashcards.ui.theme.NeonCyan
import com.cosmic.flashcards.ui.theme.Rose
import com.cosmic.flashcards.ui.theme.TextMuted
import com.cosmic.flashcards.ui.theme.TextSecondary
import com.cosmic.flashcards.vm.CardEditViewModel
import com.cosmic.flashcards.vm.ImportViewModel

// ----------------------------------------------------------- card editor

@Composable
fun CardEditScreen(
    vm: CardEditViewModel,
    cardId: Long,
    onDone: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val decks by vm.decks.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()

    LaunchedEffect(cardId) { vm.load(cardId) }

    val isEdit = state.existing != null

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text(
                    if (isEdit) "Edit Card" else "New Card",
                    style = MaterialTheme.typography.titleLarge,
                )
                Caption(
                    if (isEdit) "Editing the text doesn't reset its schedule."
                    else "It enters the queue due immediately.",
                    color = TextSecondary,
                )
            }
        }

        if (state.error != null) {
            item {
                GlassCard(Modifier.fillMaxWidth(), borderColor = Rose.copy(alpha = 0.35f)) {
                    Text(
                        state.error.orEmpty(),
                        color = Rose,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CosmicField(
                        value = state.front,
                        onValueChange = vm::setFront,
                        label = "Front",
                        placeholder = "What's on the front?",
                        singleLine = false,
                        minLines = 3,
                    )
                    CosmicField(
                        value = state.back,
                        onValueChange = vm::setBack,
                        label = "Back",
                        placeholder = "And the answer…",
                        singleLine = false,
                        minLines = 4,
                    )
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CosmicDropdown(
                        label = "Deck",
                        options = decks,
                        selected = decks.firstOrNull { it.id == state.deckId },
                        optionLabel = { it?.let { d -> "${d.emoji}  ${d.name}" } ?: "— Unsorted —" },
                        onSelect = { vm.setDeck(it?.id) },
                        noneLabel = "— Unsorted —",
                    )

                    if (tags.isNotEmpty()) {
                        Column {
                            SectionLabel("Tags", Modifier.padding(bottom = 8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                tags.forEach { t ->
                                    val on = t.id in state.selectedTagIds
                                    val accent = Accents.of(t.accent)
                                    Chip(
                                        text = "#${t.name}",
                                        color = if (on) accent.solid else TextSecondary,
                                        fill = if (on) accent.chipFill else Color.White.copy(alpha = 0.05f),
                                        border = if (on) accent.chipBorder else Color.White.copy(alpha = 0.10f),
                                        onClick = { vm.toggleTag(t.id) },
                                    )
                                }
                            }
                        }
                    }

                    CosmicField(
                        value = state.newTags,
                        onValueChange = vm::setNewTags,
                        label = "Add new tags",
                        placeholder = "comma, separated, tags",
                        supportingText = "Existing tags are reused.",
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    if (isEdit) "Save changes" else "Launch card",
                    onClick = { vm.save { ok -> if (ok) onDone() } },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!isEdit) {
                    GhostButton(
                        "Save & add another",
                        onClick = { vm.saveAndAddAnother { } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    DangerButton(
                        "Delete card",
                        onClick = { vm.delete(onDone) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// --------------------------------------------------------------- import

@Composable
fun ImportScreen(
    vm: ImportViewModel,
    onImported: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val decks by vm.decks.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    var showFormats by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Bulk Import", style = MaterialTheme.typography.titleLarge)
                Caption("Paste anything — the parser works out the format.", color = TextSecondary)
            }
        }

        if (state.message != null) {
            item {
                GlassCard(Modifier.fillMaxWidth(), borderColor = Amber.copy(alpha = 0.35f)) {
                    Text(
                        state.message.orEmpty(),
                        color = Amber, fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    CosmicField(
                        value = state.text,
                        onValueChange = vm::setText,
                        label = "Paste your cards",
                        placeholder = "Mitochondria | Powerhouse of the cell\nDendrite | Receives signals",
                        singleLine = false,
                        minLines = 8,
                    )
                    Text(
                        if (showFormats) "Hide supported formats" else "Supported formats",
                        color = TextMuted, fontSize = 12.sp,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showFormats = !showFormats }
                            .padding(vertical = 4.dp),
                    )
                    if (showFormats) {
                        Column(Modifier.padding(top = 6.dp)) {
                            listOf(
                                "front | back  — also tab, ;; :: or  -  ",
                                "Q: … / A: …  blocks",
                                "CSV with two or more columns",
                                "Blank-line blocks: line 1 front, rest back",
                            ).forEach {
                                Caption("•  $it", Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }

        // ---------- live preview ----------
        if (state.preview.isNotEmpty()) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel("Preview", Modifier.weight(1f))
                            Chip("${state.newCount} new", NeonCyan)
                            if (state.dupeCount > 0) {
                                Box(Modifier.padding(start = 6.dp)) {
                                    Chip("${state.dupeCount} duplicate", Amber)
                                }
                            }
                        }
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            state.preview.take(25).forEach { (parsed, dupe) ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (dupe) Amber.copy(alpha = 0.06f)
                                            else Color.White.copy(alpha = 0.04f)
                                        )
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        if (dupe) "⊘" else "✦",
                                        color = if (dupe) Amber else NeonCyan,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(end = 8.dp),
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(parsed.front, fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium, maxLines = 1)
                                        Caption(parsed.back, Modifier.padding(top = 2.dp))
                                    }
                                }
                            }
                            if (state.preview.size > 25) {
                                Caption("…and ${state.preview.size - 25} more")
                            }
                        }
                    }
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CosmicDropdown(
                        label = "Into deck",
                        options = decks,
                        selected = decks.firstOrNull { it.id == state.deckId },
                        optionLabel = { it?.let { d -> "${d.emoji}  ${d.name}" } ?: "— Unsorted —" },
                        onSelect = { vm.setDeck(it?.id) },
                        noneLabel = "— Unsorted —",
                    )

                    if (tags.isNotEmpty()) {
                        Column {
                            SectionLabel("Apply tags", Modifier.padding(bottom = 8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                tags.forEach { t ->
                                    val on = t.id in state.selectedTagIds
                                    val accent = Accents.of(t.accent)
                                    Chip(
                                        text = "#${t.name}",
                                        color = if (on) accent.solid else TextSecondary,
                                        fill = if (on) accent.chipFill else Color.White.copy(alpha = 0.05f),
                                        border = if (on) accent.chipBorder else Color.White.copy(alpha = 0.10f),
                                        onClick = { vm.toggleTag(t.id) },
                                    )
                                }
                            }
                        }
                    }

                    CosmicField(
                        value = state.newTags,
                        onValueChange = vm::setNewTags,
                        label = "Add new tags",
                        placeholder = "comma, separated, tags",
                    )
                }
            }
        }

        item {
            Column {
                PrimaryButton(
                    "Import cards",
                    onClick = { vm.runImport { result -> if (result != null && result.created > 0) onImported() } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.preview.isNotEmpty(),
                )
                Caption(
                    "Duplicates are skipped automatically — importing the same paste twice is safe.",
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}
