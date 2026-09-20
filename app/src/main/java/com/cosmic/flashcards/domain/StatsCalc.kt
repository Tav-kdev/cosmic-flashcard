package com.cosmic.flashcards.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** One bar in the dashboard's activity strip. */
data class ActivityDay(
    val date: LocalDate,
    val count: Int,
    val pct: Int,
    val label: String,
    val isToday: Boolean,
)

/**
 * Pure dashboard arithmetic. The repository supplies raw values; everything
 * derived is computed here so it can be tested without a database.
 */
object StatsCalc {

    /** The interval, in days, at which a card counts as "mastered". */
    const val MASTERED_INTERVAL_DAYS = 21

    /** 3900 -> "3.9k", matching the web app's number style. */
    fun compact(n: Int): String = when {
        n < 1_000 -> n.toString()
        n < 1_000_000 -> trimZero(n / 1_000.0) + "k"
        else -> trimZero(n / 1_000_000.0) + "m"
    }

    private fun trimZero(v: Double): String {
        val s = String.format("%.1f", v)
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }

    fun percent(part: Int, whole: Int): Int =
        if (whole <= 0) 0 else (part.toDouble() / whole * 100).roundToInt().coerceIn(0, 100)

    /**
     * Consecutive days ending today (or yesterday) that have at least one
     * review. A gap of one day is tolerated only if today has no reviews yet.
     */
    fun streak(reviewMillis: List<Long>, zone: ZoneId, today: LocalDate): Int {
        if (reviewMillis.isEmpty()) return 0
        val days = reviewMillis.mapTo(HashSet()) { millisToDate(it, zone) }

        var cursor = if (today in days) today else today.minusDays(1)
        if (cursor !in days) return 0

        var streak = 0
        while (cursor in days) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /** Per-day review counts for the sparkline strip. */
    fun activity(
        reviewMillis: List<Long>,
        zone: ZoneId,
        today: LocalDate,
        days: Int = 14,
    ): List<ActivityDay> {
        val start = today.minusDays((days - 1).toLong())
        val buckets = LinkedHashMap<LocalDate, Int>(days)
        for (i in 0 until days) buckets[start.plusDays(i.toLong())] = 0

        for (ms in reviewMillis) {
            val d = millisToDate(ms, zone)
            if (buckets.containsKey(d)) buckets[d] = buckets.getValue(d) + 1
        }

        val peak = (buckets.values.maxOrNull() ?: 0).coerceAtLeast(1)
        return buckets.map { (date, count) ->
            ActivityDay(
                date = date,
                count = count,
                pct = (count.toDouble() / peak * 100).roundToInt(),
                label = date.dayOfWeek.name.take(1),
                isToday = date == today,
            )
        }
    }

    /** The one-line narrative in the Orbit Status panel. */
    fun orbitStatus(total: Int, due: Int, streak: Int, masteredPct: Int): String = when {
        total == 0 -> "Empty orbit. Add or import a few cards to start your first trajectory."
        streak == 0 -> "Orbit decaying. A single session today restarts your streak."
        due == 0 -> "All clear. Nothing due right now — the next batch is already scheduled."
        masteredPct >= 60 -> "Strong trajectory. Most of your deck has cleared the long-interval threshold."
        else -> "Climbing steadily. Keep clearing the due queue to push cards into long-term orbit."
    }

    fun millisToDate(millis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
