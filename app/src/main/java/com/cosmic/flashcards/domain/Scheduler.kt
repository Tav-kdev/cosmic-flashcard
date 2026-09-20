package com.cosmic.flashcards.domain

import kotlin.math.max
import kotlin.math.roundToInt

/** A grade button, as shown in the study screen. */
data class Grade(
    val key: String,
    val quality: Int,
    val label: String,
    val hint: String,
)

/** The scheduling numbers for one card, independent of how they're stored. */
data class SchedulingState(
    val easeFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
)

/**
 * SM-2 spaced repetition.
 *
 * Deliberately pure: no Android, no database, no clock. The caller supplies
 * "now" and persists the result, which is what makes this testable on the JVM.
 */
object Scheduler {

    val GRADES = listOf(
        Grade("again", 1, "Again", "Blanked"),
        Grade("hard", 3, "Hard", "Struggled"),
        Grade("good", 4, "Good", "Recalled"),
        Grade("easy", 5, "Easy", "Instant"),
    )

    fun gradeFor(key: String): Grade = GRADES.firstOrNull { it.key == key } ?: GRADES[2]

    /** Pure SM-2 arithmetic. Returns the next state; mutates nothing. */
    fun apply(state: SchedulingState, quality: Int): SchedulingState {
        val q = quality.coerceIn(0, 5)

        if (q < 3) {
            return state.copy(repetitions = 0, intervalDays = 1)
        }

        val reps = state.repetitions + 1
        val ease = max(1.3, state.easeFactor + (0.1 - (5 - q) * 0.08))
        val interval = when (reps) {
            1 -> 1
            2 -> 6
            else -> (state.intervalDays * ease).roundToInt().coerceAtLeast(1)
        }
        return SchedulingState(easeFactor = ease, intervalDays = interval, repetitions = reps)
    }

    /** What each button would schedule, without committing anything. */
    fun previewIntervals(state: SchedulingState): Map<String, String> =
        GRADES.associate { it.key to humanizeDays(apply(state, it.quality).intervalDays) }

    fun humanizeDays(days: Int): String = when {
        days <= 0 -> "now"
        days == 1 -> "1d"
        days < 30 -> "${days}d"
        days < 365 -> "${(days / 30.0).roundToInt()}mo"
        else -> String.format("%.1fy", days / 365.0)
    }

    /** Compact "in 3d" / "2d overdue" wording for a due timestamp. */
    fun naturalDue(dueAtMillis: Long, nowMillis: Long): String {
        val deltaMs = dueAtMillis - nowMillis
        val dayMs = 86_400_000L

        if (deltaMs <= 0) {
            val overdue = (-deltaMs / dayMs).toInt()
            return when {
                overdue <= 0 -> "now"
                overdue == 1 -> "1d overdue"
                else -> "${overdue}d overdue"
            }
        }
        val days = (deltaMs / dayMs).toInt()
        return when {
            days == 0 -> {
                val hours = (deltaMs / 3_600_000L).toInt()
                if (hours > 0) "in ${hours}h" else "in <1h"
            }
            days == 1 -> "tomorrow"
            days < 30 -> "in ${days}d"
            days < 365 -> "in ${(days / 30.0).roundToInt()}mo"
            else -> String.format("in %.1fy", days / 365.0)
        }
    }
}
