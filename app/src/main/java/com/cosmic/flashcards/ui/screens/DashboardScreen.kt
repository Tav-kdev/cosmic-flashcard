package com.cosmic.flashcards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmic.flashcards.data.dao.CardState
import com.cosmic.flashcards.ui.components.Caption
import com.cosmic.flashcards.ui.components.Chip
import com.cosmic.flashcards.ui.components.DeckTile
import com.cosmic.flashcards.ui.components.GlassCard
import com.cosmic.flashcards.ui.components.ProgressBar
import com.cosmic.flashcards.ui.components.SectionLabel
import com.cosmic.flashcards.ui.nav.BottomSpacer
import com.cosmic.flashcards.ui.theme.Accents
import com.cosmic.flashcards.ui.theme.Amber
import com.cosmic.flashcards.ui.theme.Display
import com.cosmic.flashcards.ui.theme.NeonCyan
import com.cosmic.flashcards.ui.theme.NeonPink
import com.cosmic.flashcards.ui.theme.NeonPurple
import com.cosmic.flashcards.ui.theme.TextMuted
import com.cosmic.flashcards.ui.theme.TextSecondary
import com.cosmic.flashcards.vm.DashboardViewModel

@Composable
fun DashboardScreen(
    vm: DashboardViewModel,
    onOpenCards: (Int) -> Unit,
    onOpenDeck: (Long) -> Unit,
    onOpenTag: (Long) -> Unit,
    onAllDecks: () -> Unit,
    onAllTags: () -> Unit,
    onStudyNormal: () -> Unit,
    onExam: () -> Unit,
    onCreateCard: () -> Unit,
    onImport: () -> Unit,
    onCreateDeck: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column {
                Text("Command Center", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (state.due > 0) "${state.due} card${plural(state.due)} waiting on you"
                    else "Ready for your next orbit",
                    color = TextSecondary,
                    fontSize = 14.sp,
                )
            }
        }

        // ---------- stats, 2x2 ----------
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        "Due Today", state.dueDisplay, "card${plural(state.due)} waiting",
                        "◈", NeonCyan, Modifier.weight(1f),
                    ) { onOpenCards(CardState.DUE) }
                    StatTile(
                        "Mastered", state.masteredDisplay, "long-term",
                        "✦", NeonPurple, Modifier.weight(1f),
                    ) { onOpenCards(CardState.MASTERED) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        "Total", state.totalDisplay,
                        "${state.deckCount} deck${plural(state.deckCount)}",
                        "◉", NeonPink, Modifier.weight(1f),
                    ) { onOpenCards(CardState.ANY) }
                    StatTile(
                        "Streak", state.streak.toString(), "day${plural(state.streak)}",
                        "🔥", Amber, Modifier.weight(1f), onClick = null,
                    )
                }
            }
        }

        // ---------- study CTAs ----------
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeCard(
                    title = "Normal Mode",
                    subtitle = "Spaced repetition · SM-2",
                    emoji = "🚀",
                    accent = NeonCyan,
                    chips = buildList {
                        add("${state.due} due" to NeonCyan)
                        if (state.newCards > 0) add("${state.newCards} new" to TextSecondary)
                    },
                    onClick = onStudyNormal,
                )
                ModeCard(
                    title = "Exam Mode",
                    subtitle = "Fixed set · your pick",
                    emoji = "🪐",
                    accent = NeonPurple,
                    chips = listOf("custom" to NeonPurple),
                    onClick = onExam,
                )
            }
        }

        // ---------- recent decks ----------
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("Recent Decks")
                Text(
                    "See all",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onAllDecks)
                        .padding(4.dp),
                )
            }
        }

        if (state.decks.isEmpty()) {
            item {
                GlassCard(Modifier.fillMaxWidth(), onClick = onCreateDeck) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("🗂️", fontSize = 32.sp)
                        Text("No decks yet", fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 8.dp))
                        Caption("Create your first deck to organise cards",
                            Modifier.padding(top = 4.dp))
                    }
                }
            }
        } else {
            items(state.decks, key = { it.deck.id }) { deck ->
                val accent = Accents.of(deck.deck.accent)
                GlassCard(Modifier.fillMaxWidth(), onClick = { onOpenDeck(deck.deck.id) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        DeckTile(deck.deck.emoji, accent)
                        Column(Modifier.weight(1f)) {
                            Text(
                                deck.deck.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Caption(
                                "${deck.cardCount} card${plural(deck.cardCount)}" +
                                    if (deck.dueCount > 0) " · ${deck.dueCount} due" else "",
                                Modifier.padding(top = 2.dp),
                                TextSecondary,
                            )
                        }
                        Text(
                            "${deck.masteredPct}%",
                            color = accent.solid,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        // ---------- quick launch ----------
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    SectionLabel("Quick Launch", Modifier.padding(bottom = 12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickAction("📝", "Create", NeonCyan, Modifier.weight(1f), onCreateCard)
                        QuickAction("📥", "Import", NeonPurple, Modifier.weight(1f), onImport)
                    }
                    Row(
                        Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        QuickAction("🏷️", "Tags", NeonPink, Modifier.weight(1f), onAllTags)
                        QuickAction("📁", "Deck", Amber, Modifier.weight(1f), onCreateDeck)
                    }
                }
            }
        }

        // ---------- popular tags ----------
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    SectionLabel("Popular Tags", Modifier.padding(bottom = 12.dp))
                    if (state.tags.isEmpty()) {
                        Caption("No tags yet — add some when you create a card.")
                    } else {
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.tags.forEach { t ->
                                val accent = Accents.of(t.tag.accent)
                                Chip(
                                    "#${t.tag.name}",
                                    accent.solid,
                                    onClick = { onOpenTag(t.tag.id) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---------- orbit status ----------
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    SectionLabel("Orbit Status", Modifier.padding(bottom = 8.dp))
                    Text(
                        state.orbitStatus,
                        color = Color(0xFFD1D5DB),
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    )
                    if (state.total > 0) {
                        Row(Modifier.padding(top = 6.dp)) {
                            Text("${state.streak}-day streak",
                                color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(" · ", color = TextMuted, fontSize = 13.sp)
                            Text("${state.masteredPct}%",
                                color = NeonPurple, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(" past the long-interval mark",
                                color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    ProgressBar(
                        state.progressPct / 100f,
                        Modifier.padding(top = 12.dp),
                    )

                    // 14-day activity strip
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        state.activity.forEach { day ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight(
                                        if (day.count > 0)
                                            (day.pct / 100f).coerceAtLeast(0.08f)
                                        else 0.06f
                                    )
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (day.count > 0)
                                            Brush.verticalGradient(
                                                listOf(NeonPurple.copy(alpha = 0.75f),
                                                    NeonCyan.copy(alpha = 0.45f))
                                            )
                                        else
                                            Brush.verticalGradient(
                                                listOf(Color.White.copy(alpha = 0.06f),
                                                    Color.White.copy(alpha = 0.06f))
                                            )
                                    )
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Caption("14 days ago")
                        Caption("${state.reviewedToday} reviewed today")
                    }
                }
            }
        }

        item { BottomSpacer() }
    }
}

// ---------------------------------------------------------------- pieces

@Composable
private fun StatTile(
    label: String,
    value: String,
    sub: String,
    glyph: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    GlassCard(modifier, onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel(label)
                Text(glyph, color = accent, fontSize = 15.sp)
            }
            Text(
                value,
                fontFamily = Display,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Caption(sub, Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    emoji: String,
    accent: Color,
    chips: List<Pair<String, Color>>,
    onClick: () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = accent)
                    Caption(subtitle, Modifier.padding(top = 2.dp), TextSecondary)
                }
                Text(emoji, fontSize = 26.sp)
            }
            Row(
                Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chips.forEach { (text, color) -> Chip(text, color) }
            }
        }
    }
}

@Composable
private fun QuickAction(
    emoji: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 20.sp)
        Text(
            label,
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

internal fun plural(n: Int) = if (n == 1) "" else "s"