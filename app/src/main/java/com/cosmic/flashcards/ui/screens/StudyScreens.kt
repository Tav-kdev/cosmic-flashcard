package com.cosmic.flashcards.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmic.flashcards.domain.Scheduler
import com.cosmic.flashcards.ui.components.Caption
import com.cosmic.flashcards.ui.components.Chip
import com.cosmic.flashcards.ui.components.DeckTile
import com.cosmic.flashcards.ui.components.EmptyState
import com.cosmic.flashcards.ui.components.GhostButton
import com.cosmic.flashcards.ui.components.GlassCard
import com.cosmic.flashcards.ui.components.PrimaryButton
import com.cosmic.flashcards.ui.components.ProgressBar
import com.cosmic.flashcards.ui.components.SectionLabel
import com.cosmic.flashcards.ui.nav.BottomSpacer
import com.cosmic.flashcards.ui.theme.Accents
import com.cosmic.flashcards.ui.theme.Amber
import com.cosmic.flashcards.ui.theme.Display
import com.cosmic.flashcards.ui.theme.NeonCyan
import com.cosmic.flashcards.ui.theme.NeonPurple
import com.cosmic.flashcards.ui.theme.Rose
import com.cosmic.flashcards.ui.theme.TextMuted
import com.cosmic.flashcards.ui.theme.TextPrimary
import com.cosmic.flashcards.ui.theme.TextSecondary
import com.cosmic.flashcards.vm.StudyMode
import com.cosmic.flashcards.vm.StudyViewModel

// ------------------------------------------------------------- launchpad

@Composable
fun StudyHomeScreen(
    vm: StudyViewModel,
    onStartNormal: () -> Unit,
    onExam: () -> Unit,
    onStartDeck: (Long, String) -> Unit,
    onCreateCard: () -> Unit,
    onImport: () -> Unit,
) {
    val due by vm.dueCount.collectAsStateWithLifecycle()
    val total by vm.totalCount.collectAsStateWithLifecycle()
    val decks by vm.decks.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text("Launchpad", style = MaterialTheme.typography.titleLarge)
                Caption("Pick your trajectory", color = TextSecondary)
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth(), onClick = if (due > 0) onStartNormal else null) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Normal Mode", fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold, color = NeonCyan)
                            Caption("Spaced repetition · SM-2", color = TextSecondary)
                        }
                        Text("🚀", fontSize = 26.sp)
                    }
                    Text(
                        "Works the due queue. Each answer reschedules the card; misses come back before you finish.",
                        color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Box(Modifier.padding(top = 12.dp)) {
                        Chip(
                            if (due > 0) "$due due now" else "nothing due",
                            if (due > 0) NeonCyan else TextMuted,
                        )
                    }
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth(), onClick = onExam) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Exam Mode", fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold, color = NeonPurple)
                            Caption("Fixed set · your pick", color = TextSecondary)
                        }
                        Text("🪐", fontSize = 26.sp)
                    }
                    Text(
                        "A set number of cards from any deck or tag, due or not. Runs straight through — no repeats.",
                        color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Box(Modifier.padding(top = 12.dp)) { Chip("configure", NeonPurple) }
                }
            }
        }

        val withDue = decks.filter { it.dueCount > 0 }
        if (withDue.isNotEmpty()) {
            item { SectionLabel("Study one deck") }
            items(withDue, key = { it.deck.id }) { deck ->
                val accent = Accents.of(deck.deck.accent)
                GlassCard(
                    Modifier.fillMaxWidth(),
                    onClick = { onStartDeck(deck.deck.id, deck.deck.name) },
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        DeckTile(deck.deck.emoji, accent, size = 40)
                        Column(Modifier.weight(1f)) {
                            Text(deck.deck.name, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Caption("${deck.dueCount} due", color = NeonCyan)
                        }
                    }
                }
            }
        }

        if (total == 0) {
            item {
                EmptyState("🌑", "No cards to study yet",
                    "Add a few and they'll be due immediately.") {
                    PrimaryButton("+ Create card", onCreateCard, Modifier.weight(1f))
                    GhostButton("Import", onImport)
                }
            }
        } else if (due == 0) {
            item {
                EmptyState("🌠", "Queue clear",
                    "Nothing is due right now. Exam mode works on any card, due or not.")
            }
        }

        item { BottomSpacer() }
    }
}

// --------------------------------------------------------------- session

@Composable
fun StudySessionScreen(
    vm: StudyViewModel,
    onFinished: () -> Unit,
    onExit: () -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val session = ui.session
    val card = ui.card

    // No session at all (deep link, or state was reset) — go back rather than
    // showing an empty summary.
    if (session == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onExit() }
        return
    }
    if (ui.finished) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onFinished() }
        return
    }
    if (card == null) return

    val rotation by animateFloatAsState(
        targetValue = if (ui.revealed) 180f else 0f,
        animationSpec = tween(durationMillis = 480),
        label = "flip",
    )
    val screenHeight = LocalConfiguration.current.screenHeightDp

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ---- progress header ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable(onClick = onExit),
                contentAlignment = Alignment.Center,
            ) { Text("✕", color = TextSecondary, fontSize = 15.sp) }

            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${if (session.mode == StudyMode.EXAM) "🪐" else "🚀"} ${session.label}",
                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Caption("${session.position} / ${session.total}")
                }
                ProgressBar(session.progress, Modifier.padding(top = 6.dp), height = 6)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- the card ----
        val cardHeight = (screenHeight * 0.46f).coerceIn(260f, 460f)
        Box(
            Modifier
                .fillMaxWidth()
                .height(cardHeight.dp)
                .graphicsLayer {
                    rotationX = rotation
                    cameraDistance = 14f * density
                }
                .clip(RoundedCornerShape(24.dp))
                .background(com.cosmic.flashcards.ui.theme.GlassFill)
                .border(1.dp, com.cosmic.flashcards.ui.theme.GlassBorder, RoundedCornerShape(24.dp))
                .clickable(enabled = !ui.revealed) { vm.reveal() }
        ) {
            if (rotation <= 90f) {
                CardFront(
                    front = card.card.front,
                    deckEmoji = card.deck?.emoji,
                    deckName = card.deck?.name,
                    stage = card.card.mastery.stage,
                    onReveal = vm::reveal,
                )
            } else {
                // Counter-rotate so the back reads the right way up.
                Box(Modifier.fillMaxSize().graphicsLayer { rotationX = 180f }) {
                    CardBack(
                        front = card.card.front,
                        back = card.card.back,
                        tags = card.tags.map { it.name to Accents.of(it.accent).solid },
                        intervalDays = card.card.mastery.intervalDays,
                        ease = card.card.mastery.easeFactor,
                        reps = card.card.mastery.repetitions,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- grade buttons ----
        AnimatedVisibility(
            visible = ui.revealed,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Scheduler.GRADES.forEach { grade ->
                    val color = when (grade.key) {
                        "again" -> Rose
                        "hard" -> Amber
                        "good" -> NeonCyan
                        else -> NeonPurple
                    }
                    GradeButton(
                        label = grade.label,
                        preview = ui.previews[grade.key].orEmpty(),
                        color = color,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.grade(grade.key) },
                    )
                }
            }
        }

        if (!ui.revealed) {
            Text(
                "Answer honestly — the schedule only works if the grade is real.",
                color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Caption("${session.correct} recalled", color = NeonCyan)
            Caption("   ·   ")
            Caption("${session.again} missed", color = Rose)
        }
    }
}

@Composable
private fun CardFront(
    front: String,
    deckEmoji: String?,
    deckName: String?,
    stage: String,
    onReveal: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (deckName != null) {
                Text("${deckEmoji.orEmpty()} $deckName",
                    color = TextSecondary, fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
            } else {
                Text("Unsorted", color = TextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
            }
            StageChip(stage)
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                front,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp,
                color = TextPrimary,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }

        PrimaryButton("Reveal answer", onReveal, Modifier.fillMaxWidth())
    }
}

@Composable
private fun CardBack(
    front: String,
    back: String,
    tags: List<Pair<String, Color>>,
    intervalDays: Int,
    ease: Double,
    reps: Int,
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Answer", Modifier.weight(1f))
            if (tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.take(3).forEach { (name, color) -> Chip("#$name", color) }
                }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                front,
                color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.06f))
            )
            Text(
                back,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                lineHeight = 27.sp,
                color = TextPrimary,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        Caption(
            "Interval ${intervalDays}d · ease ${String.format("%.2f", ease)} · seen ${reps}×",
            Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StageChip(stage: String) {
    val color = when (stage) {
        "mastered" -> NeonPurple
        "learning" -> NeonCyan
        else -> TextSecondary
    }
    Chip(stage, color)
}

@Composable
private fun GradeButton(
    label: String,
    preview: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.38f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(preview, color = color.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}

// ------------------------------------------------------------ session done

@Composable
fun SessionDoneScreen(
    vm: StudyViewModel,
    onKeepGoing: () -> Unit,
    onHome: () -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val due by vm.dueCount.collectAsStateWithLifecycle()
    val session = ui.session

    val graded = session?.graded ?: 0
    val correct = session?.correct ?: 0
    val accuracy = session?.accuracy ?: 0

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    when {
                        accuracy >= 80 -> "🌟"
                        accuracy >= 50 -> "🛰️"
                        else -> "🌑"
                    },
                    fontSize = 44.sp,
                )
                Text(
                    "SESSION COMPLETE",
                    fontFamily = Display,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Caption(session?.label ?: "", Modifier.padding(top = 4.dp), TextSecondary)

                Row(
                    Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SummaryTile("Graded", graded.toString(), TextPrimary,
                        Color.White.copy(alpha = 0.05f), Modifier.weight(1f))
                    SummaryTile("Recalled", correct.toString(), NeonCyan,
                        NeonCyan.copy(alpha = 0.10f), Modifier.weight(1f))
                    SummaryTile("Accuracy", "$accuracy%", NeonPurple,
                        NeonPurple.copy(alpha = 0.10f), Modifier.weight(1f))
                }

                ProgressBar(accuracy / 100f, Modifier.padding(top = 20.dp))
            }
        }

        if (due > 0) {
            PrimaryButton("Keep going · $due due", onKeepGoing, Modifier.fillMaxWidth())
        }
        GhostButton("Command Center", onHome, Modifier.fillMaxWidth())
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: String,
    color: Color,
    fill: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(fill)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontFamily = Display, fontWeight = FontWeight.Bold,
            fontSize = 19.sp, color = color)
        Text(label.uppercase(), fontSize = 9.sp, color = TextMuted,
            modifier = Modifier.padding(top = 2.dp))
    }
}
