package com.cosmic.flashcards.domain

/** One front/back pair recovered from pasted text. */
data class ParsedCard(val front: String, val back: String)

/**
 * Turns pasted text into front/back pairs.
 *
 * Auto-detects, in order:
 *  1. An explicit separator per line: `front | back` (also tab, `;;`, `::`, ` - `)
 *  2. `Q:` / `A:` blocks
 *  3. CSV with two or more columns
 *  4. Blank-line-separated blocks: line 1 is the front, the rest the back
 *
 * Ported from the Django app's `cards/services/import_parser.py` and kept
 * behaviourally identical so the same paste produces the same cards on both.
 */
object ImportParser {

    private val SEPARATORS = listOf("\t", "|", ";;", " :: ", " — ", " - ")
    private val INLINE_WS = Regex("[ \\t]+")
    private val BLANK_LINE = Regex("\\n\\s*\\n")
    private val HEADER_WORDS = setOf("front", "question", "term")

    private val QA = Regex(
        """^\s*(?:Q|Question)\s*[:.)]\s*(.+?)\n\s*(?:A|Answer)\s*[:.)]\s*(.+?)(?=\n\s*(?:Q|Question)\s*[:.)]|$)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
    )

    private fun clean(s: String): String = INLINE_WS.replace(s, " ").trim()

    fun parse(raw: String?): List<ParsedCard> {
        val text = raw.orEmpty().replace("\r\n", "\n").trim()
        if (text.isEmpty()) return emptyList()

        for (strategy in listOf(::bySeparator, ::byQA, ::byCsv, ::byBlocks)) {
            val result = strategy(text)
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    private fun bySeparator(text: String): List<ParsedCard> {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        for (sep in SEPARATORS) {
            val hits = lines.count { it.contains(sep) }
            // Require the separator to carry most lines, so a stray hyphen in
            // one answer doesn't hijack the whole paste.
            val threshold = maxOf(1, (lines.size * 0.6).toInt())
            if (hits < threshold) continue

            val out = mutableListOf<ParsedCard>()
            for (line in lines) {
                val idx = line.indexOf(sep)
                if (idx < 0) continue
                val front = clean(line.substring(0, idx))
                val back = clean(line.substring(idx + sep.length))
                if (front.isNotEmpty() && back.isNotEmpty()) out.add(ParsedCard(front, back))
            }
            if (out.isNotEmpty()) return out
        }
        return emptyList()
    }

    private fun byQA(text: String): List<ParsedCard> =
        QA.findAll(text).mapNotNull { m ->
            val front = clean(m.groupValues[1])
            val back = clean(m.groupValues[2])
            if (front.isNotEmpty() && back.isNotEmpty()) ParsedCard(front, back) else null
        }.toList()

    private fun byCsv(text: String): List<ParsedCard> {
        val delimiter = listOf(',', ';', '\t').maxByOrNull { d -> text.count { it == d } } ?: return emptyList()
        if (text.count { it == delimiter } == 0) return emptyList()

        val out = mutableListOf<ParsedCard>()
        for (row in splitCsv(text, delimiter)) {
            if (row.size < 2) continue
            val front = clean(row[0])
            val back = clean(row[1])
            if (front.isEmpty() || back.isEmpty()) continue
            if (front.lowercase() in HEADER_WORDS) continue  // header row
            out.add(ParsedCard(front, back))
        }
        return out
    }

    /** Minimal RFC-4180-ish reader: handles quoted fields and escaped quotes. */
    private fun splitCsv(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                    field.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == delimiter -> { row.add(field.toString()); field.setLength(0) }
                !inQuotes && c == '\n' -> {
                    row.add(field.toString()); field.setLength(0)
                    rows.add(row); row = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        row.add(field.toString())
        if (row.any { it.isNotBlank() }) rows.add(row)
        return rows
    }

    private fun byBlocks(text: String): List<ParsedCard> {
        val out = mutableListOf<ParsedCard>()
        for (block in BLANK_LINE.split(text)) {
            if (block.isBlank()) continue
            val lines = block.lines().filter { it.isNotBlank() }
            if (lines.size < 2) continue
            val front = clean(lines.first())
            val back = clean(lines.drop(1).joinToString(" "))
            if (front.isNotEmpty() && back.isNotEmpty()) out.add(ParsedCard(front, back))
        }
        return out
    }
}
