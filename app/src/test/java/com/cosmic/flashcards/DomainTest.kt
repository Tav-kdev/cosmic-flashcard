package com.cosmic.flashcards

import com.cosmic.flashcards.domain.*
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pure-logic tests for the scheduler, parser, hashing and stats.
 *
 * These are plain JVM tests (no emulator needed):
 *     ./gradlew test
 *
 * The content-hash cases are pinned to values produced by the Django app's
 * `content_hash_for`, so a card exported from the web app keeps its identity
 * here and imports as a duplicate rather than a second copy.
 */
class DomainTest {

    private val failures = mutableListOf<String>()

    private fun check(label: String, cond: Boolean, detail: String = "") {
        if (!cond) failures.add(label + if (detail.isNotEmpty()) "  — $detail" else "")
    }

    @Test
    fun domainLogicHolds() {
    var s = SchedulingState()
    s = Scheduler.apply(s, 5)
    check("First good answer -> 1 day", s.intervalDays == 1, "got ${s.intervalDays}")
    s = Scheduler.apply(s, 5)
    check("Second good answer -> 6 days", s.intervalDays == 6, "got ${s.intervalDays}")
    val before = s.intervalDays
    s = Scheduler.apply(s, 5)
    check("Third answer multiplies by ease", s.intervalDays > before, "got ${s.intervalDays}")
    check("Ease rises on an easy answer", s.easeFactor > 2.5, "got ${s.easeFactor}")

    s = Scheduler.apply(s, 1)
    check("A miss resets reps and interval", s.repetitions == 0 && s.intervalDays == 1,
        "reps=${s.repetitions} interval=${s.intervalDays}")

    var s2 = SchedulingState(easeFactor = 1.3, intervalDays = 10, repetitions = 5)
    repeat(10) { s2 = Scheduler.apply(s2, 3) }
    check("Ease factor never drops below 1.3", s2.easeFactor >= 1.3, "got ${s2.easeFactor}")

    // Quality clamping
    val clampHigh = Scheduler.apply(SchedulingState(), 99)
    val clampLow = Scheduler.apply(SchedulingState(), -5)
    check("Quality above 5 is clamped", clampHigh.repetitions == 1)
    check("Quality below 0 is clamped to a miss", clampLow.intervalDays == 1 && clampLow.repetitions == 0)

    // Purity: apply() must not mutate its input
    val orig = SchedulingState(2.5, 10, 3)
    Scheduler.apply(orig, 5)
    check("apply() does not mutate its input", orig.intervalDays == 10 && orig.repetitions == 3)

    val prev = Scheduler.previewIntervals(SchedulingState())
    check("Preview returns all four grades",
        prev.keys == setOf("again", "hard", "good", "easy"), "got ${prev.keys}")

    check("humanizeDays: 1 -> 1d", Scheduler.humanizeDays(1) == "1d")
    check("humanizeDays: 45 -> 2mo", Scheduler.humanizeDays(45) == "2mo", Scheduler.humanizeDays(45))
    check("humanizeDays: 400 -> 1.1y", Scheduler.humanizeDays(400) == "1.1y", Scheduler.humanizeDays(400))
    check("humanizeDays: 0 -> now", Scheduler.humanizeDays(0) == "now")

    val now = 1_700_000_000_000L
    val day = 86_400_000L
    check("naturalDue: past -> overdue", Scheduler.naturalDue(now - 3 * day, now) == "3d overdue",
        Scheduler.naturalDue(now - 3 * day, now))
    check("naturalDue: tomorrow", Scheduler.naturalDue(now + (day + 1000), now) == "tomorrow",
        Scheduler.naturalDue(now + day + 1000, now))
    check("naturalDue: hours", Scheduler.naturalDue(now + 5 * 3_600_000L, now) == "in 5h",
        Scheduler.naturalDue(now + 5 * 3_600_000L, now))
    check("naturalDue: exactly now", Scheduler.naturalDue(now, now) == "now")
    val h1 = ContentHash.of("Mitochondria", "Powerhouse")
    val h2 = ContentHash.of("  mitochondria  ", "POWERHOUSE")
    val h3 = ContentHash.of("Mitochondria", "Something else")
    check("Hash is 64 hex chars", h1.length == 64 && h1.all { it in "0123456789abcdef" }, h1)
    check("Case and whitespace normalise to the same hash", h1 == h2)
    check("Different backs hash differently", h1 != h3)
    check("Newlines collapse like the web app", ContentHash.of("a\n\nb", "x") == ContentHash.of("a b", "x"))
    // Known value: must match Django's content_hash_for (sha256 of "a\u001fb")
    // Verified byte-identical against Django's content_hash_for.
    check("Matches Django's hash exactly (interop)",
        ContentHash.of("a", "b") == "f04cdced9736a69da6103f08a4daaf8c485dd481217d218a1b4993c8c3968e13",
        ContentHash.of("a", "b"))
    check("Matches Django on a unicode pair",
        ContentHash.of("\u00e9moji \u2726 front", "back \u2726") ==
            "72d0d6f610a0a32c2607022174669ad992f15056140d2774adf13c44d49be376")
    data class Case(val name: String, val text: String, val expect: Int)
    val cases = listOf(
        Case("pipe", "A | 1\nB | 2\nC | 3", 3),
        Case("tab", "A\t1\nB\t2", 2),
        Case("qa", "Q: What is X?\nA: It is Y.\n\nQ: And Z?\nA: It is W.", 2),
        Case("blocks", "Front one\nBack one here\n\nFront two\nBack two here", 2),
        Case("dash", "Term one - Def one\nTerm two - Def two\nTerm three - Def three", 3),
        Case("csv", "front,back\nAlpha,First\nBeta,Second", 2),
        Case("semicolons", "A ;; 1\nB ;; 2", 2),
        Case("double-colon", "A :: 1\nB :: 2", 2),
        Case("empty", "", 0),
        Case("garbage", "just one line with nothing", 0),
        Case("crlf", "A | 1\r\nB | 2\r\n", 2),
    )
    for (c in cases) {
        val got = ImportParser.parse(c.text).size
        check("Parser · ${c.name} -> ${c.expect} cards", got == c.expect, "got $got")
    }

    val piped = ImportParser.parse("Mitochondria | Powerhouse of the cell")
    check("Parser splits front and back correctly",
        piped[0].front == "Mitochondria" && piped[0].back == "Powerhouse of the cell",
        "${piped[0]}")

    val qa = ImportParser.parse("Q: What is X?\nA: It is Y.")
    check("Q/A strips the prefixes", qa[0].front == "What is X?" && qa[0].back == "It is Y.", "${qa[0]}")

    val csvHeader = ImportParser.parse("front,back\nAlpha,First")
    check("CSV header row is skipped", csvHeader.size == 1 && csvHeader[0].front == "Alpha", "${csvHeader}")

    val quoted = ImportParser.parse("term,definition\n\"Smith, John\",\"A person, notably\"")
    check("CSV respects quoted commas",
        quoted.size == 1 && quoted[0].front == "Smith, John", "${quoted}")

    // A stray hyphen inside one answer must not hijack a pipe-delimited paste
    val mixed = ImportParser.parse("A | first - thing\nB | second\nC | third")
    check("Pipe wins over a stray hyphen", mixed.size == 3 && mixed[0].back == "first - thing", "${mixed}")

    val multiline = ImportParser.parse("Front\nline one\nline two\n\nOther\nback")
    check("Block mode joins multi-line backs",
        multiline[0].back == "line one line two", "${multiline[0]}")
    check("compact: 47", StatsCalc.compact(47) == "47")
    check("compact: 3900 -> 3.9k", StatsCalc.compact(3900) == "3.9k", StatsCalc.compact(3900))
    check("compact: 1000 -> 1k", StatsCalc.compact(1000) == "1k", StatsCalc.compact(1000))
    check("compact: 1200000 -> 1.2m", StatsCalc.compact(1_200_000) == "1.2m", StatsCalc.compact(1_200_000))
    check("percent guards divide-by-zero", StatsCalc.percent(5, 0) == 0)
    check("percent rounds", StatsCalc.percent(1, 3) == 33, "${StatsCalc.percent(1, 3)}")
    check("percent clamps at 100", StatsCalc.percent(10, 5) == 100)

    val zone = ZoneId.of("Asia/Bangkok")
    val today = LocalDate.of(2026, 9, 20)
    fun ms(d: LocalDate) = d.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    check("Streak of zero with no reviews", StatsCalc.streak(emptyList(), zone, today) == 0)
    check("Streak counts consecutive days",
        StatsCalc.streak(listOf(ms(today), ms(today.minusDays(1)), ms(today.minusDays(2))), zone, today) == 3,
        "${StatsCalc.streak(listOf(ms(today), ms(today.minusDays(1)), ms(today.minusDays(2))), zone, today)}")
    check("Streak breaks on a gap",
        StatsCalc.streak(listOf(ms(today), ms(today.minusDays(2))), zone, today) == 1)
    check("Streak survives 'nothing yet today'",
        StatsCalc.streak(listOf(ms(today.minusDays(1)), ms(today.minusDays(2))), zone, today) == 2)
    check("Streak is zero if the last review is old",
        StatsCalc.streak(listOf(ms(today.minusDays(5))), zone, today) == 0)
    check("Duplicate reviews on one day count once",
        StatsCalc.streak(listOf(ms(today), ms(today), ms(today)), zone, today) == 1)

    val act = StatsCalc.activity(listOf(ms(today), ms(today), ms(today.minusDays(3))), zone, today)
    check("Activity has 14 days", act.size == 14, "${act.size}")
    check("Activity marks today", act.last().isToday && act.last().count == 2, "${act.last()}")
    check("Activity peak is 100%", act.maxOf { it.pct } == 100)
    check("Activity is chronological", act.first().date == today.minusDays(13))
    check("Empty activity doesn't divide by zero",
        StatsCalc.activity(emptyList(), zone, today).all { it.pct == 0 })

    check("Orbit status: empty", StatsCalc.orbitStatus(0, 0, 0, 0).startsWith("Empty orbit"))
    check("Orbit status: no streak", StatsCalc.orbitStatus(10, 5, 0, 50).startsWith("Orbit decaying"))
    check("Orbit status: clear queue", StatsCalc.orbitStatus(10, 0, 3, 50).startsWith("All clear"))
    check("Orbit status: strong", StatsCalc.orbitStatus(10, 2, 3, 80).startsWith("Strong trajectory"))

        assertTrue(
            "\n" + failures.size + " domain check(s) failed:\n" +
                failures.joinToString("\n") { "  ✗ $it" },
            failures.isEmpty(),
        )
    }
}
