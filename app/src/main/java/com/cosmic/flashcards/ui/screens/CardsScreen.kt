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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmic.flashcards.data.dao.CardSort
import com.cosmic.flashcards.data.dao.CardState
import com.cosmic.flashcards.domain.Scheduler
import com.cosmic.flashcards.ui.components.Caption
import com.cosmic.flashcards.ui.components.Chip
import com.cosmic.flashcards.ui.components.CosmicField
import com.cosmic.flashcards.ui.components.EmptyState
import com.cosmic.flashcards.ui.components.GhostButton
import com.cosmic.flashcards.ui.components.GlassCard
import com.cosmic.flashcards.ui.components.PrimaryButton
import com.cosmic.flashcards.ui.components.SectionLabel
import com.cosmic.flashcards.ui.components.SegmentedRow
import com.cosmic.flashcards.ui.nav.BottomSpacer
import com.cosmic.flashcards.ui.theme.Accents
import com.cosmic.flashcards.ui.theme.NeonCyan
import com.cosmic.flashcards.ui.theme.NeonPurple
import com.cosmic.flashcards.ui.theme.Rose
import com.cosmic.flashcards.ui.theme.Space950
import com.cosmic.flashcards.ui.theme.TextMuted
import com.cosmic.flashcards.ui.theme.TextPrimary
import com.cosmic.flashcards.ui.theme.TextSecondary
import com.cosmic.flashcards.vm.CardsViewModel

@Composable
fun CardsScreen(
    vm: CardsViewModel,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
    onImport: () -> Unit,
) {
    val cards by vm.cards.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val revealed by vm.revealed.collectAsStateWithLifecycle()
    val decks by vm.decks.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Card Vault", style = MaterialTheme.typography.titleLarge)
                    Caption("${cards.size} card${plural(cards.size)} matching", color = TextSecondary)
                }
                GhostButton("+ Card", onCreate, tint = NeonCyan)
            }
        }

        // ---------- filters ----------
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    CosmicField(
                        value = filters.query,
                        onValueChange = vm::setQuery,
                        placeholder = "Search fronts and backs…",
                    )

                    SectionLabel("Deck")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip("All", filters.deckId == null && !filters.unsortedOnly) {
                            vm.setDeck(null)
                        }
                        FilterChip("Unsorted", filters.unsortedOnly) { vm.setDeck(null, true) }
                        decks.forEach { d ->
                            FilterChip("${d.emoji} ${d.name}", filters.deckId == d.id) {
                                vm.setDeck(d.id)
                            }
                        }
                    }

                    if (tags.isNotEmpty()) {
                        SectionLabel("Tag")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip("All", filters.tagId == null) { vm.setTag(null) }
                            tags.forEach { t ->
                                FilterChip("#${t.name}", filters.tagId == t.id) { vm.setTag(t.id) }
                            }
                        }
                    }

                    SectionLabel("State")
                    SegmentedRow(
                        options = listOf(
                            CardState.ANY.toString() to "Any",
                            CardState.DUE.toString() to "Due now",
                            CardState.NEW.toString() to "New",
                            CardState.LEARNING.toString() to "Learning",
                            CardState.MASTERED.toString() to "Mastered",
                        ),
                        selectedKey = filters.state.toString(),
                        onSelect = { vm.setState(it.toInt()) },
                        accent = NeonCyan,
                    )

                    SectionLabel("Sort")
                    SegmentedRow(
                        options = listOf(
                            CardSort.RECENT.toString() to "Newest",
                            CardSort.DUE.toString() to "Due soonest",
                            CardSort.ALPHA.toString() to "A–Z",
                        ),
                        selectedKey = filters.sort.toString(),
                        onSelect = { vm.setSort(it.toInt()) },
                        accent = NeonPurple,
                    )

                    if (filters.isActive) {
                        Text(
                            "Clear all filters",
                            color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { vm.clearFilters() }
                                .padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }

        // ---------- results ----------
        if (cards.isEmpty()) {
            item {
                EmptyState(
                    "🌑",
                    "Nothing out here",
                    if (filters.isActive) "No cards match those filters."
                    else "Your vault is empty — create or import a few cards.",
                ) {
                    PrimaryButton("+ Create card", onCreate, Modifier.weight(1f))
                    GhostButton("Import", onImport)
                }
            }
        } else {
            items(cards, key = { it.card.id }) { item ->
                val isRevealed = item.card.id in revealed
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                item.deck?.let { "${it.emoji} ${it.name}" } ?: "Unsorted",
                                color = if (item.deck != null) TextSecondary else TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            val stage = item.card.mastery.stage
                            Chip(
                                stage,
                                when (stage) {
                                    "mastered" -> NeonPurple
                                    "learning" -> NeonCyan
                                    else -> TextSecondary
                                },
                            )
                        }

                        Text(
                            item.card.front,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            lineHeight = 21.sp,
                            modifier = Modifier.padding(top = 10.dp),
                        )

                        if (isRevealed) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Space950.copy(alpha = 0.5f))
                                    .padding(12.dp)
                            ) {
                                Text(item.card.back, fontSize = 14.sp,
                                    color = Color(0xFFD1D5DB), lineHeight = 20.sp)
                            }
                        }

                        Text(
                            if (isRevealed) "Hide answer ↑" else "Reveal answer ↓",
                            color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { vm.toggleReveal(item.card.id) }
                                .padding(vertical = 4.dp),
                        )

                        if (item.tags.isNotEmpty()) {
                            FlowRow(
                                Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                item.tags.forEach { t ->
                                    Chip("#${t.name}", Accents.of(t.accent).solid)
                                }
                            }
                        }

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.05f))
                        )

                        Row(
                            Modifier.fillMaxWidth().padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Caption(
                                if (item.card.mastery.lastReviewedAt != null)
                                    "Due ${Scheduler.naturalDue(item.card.mastery.dueAt, now)}"
                                else "Never reviewed",
                                Modifier.weight(1f),
                            )
                            Text(
                                "Edit",
                                color = TextSecondary, fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onEdit(item.card.id) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                            Text(
                                "Delete",
                                color = Rose, fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { vm.delete(item.card.id) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        item { BottomSpacer() }
    }
}

@Composable
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    Chip(
        text = label,
        color = if (active) NeonCyan else TextSecondary,
        fill = if (active) NeonCyan.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f),
        border = if (active) NeonCyan.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.10f),
        onClick = onClick,
    )
}
