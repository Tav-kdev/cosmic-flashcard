package com.cosmic.flashcards.domain

import java.security.MessageDigest
import java.util.Locale

/**
 * Stable fingerprint of a card, used to block duplicates.
 *
 * Normalised so whitespace and casing differences don't create near-identical
 * cards. Must stay byte-compatible with the Django app's `content_hash_for`,
 * so decks exported from there keep the same identity here.
 */
object ContentHash {

    private val WHITESPACE = Regex("\\s+")

    private fun norm(s: String?): String =
        WHITESPACE.replace(s.orEmpty(), " ").trim().lowercase(Locale.ROOT)

    fun of(front: String?, back: String?): String {
        val payload = "${norm(front)}\u001f${norm(back)}"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(64)
        for (b in digest) {
            val v = b.toInt() and 0xFF
            sb.append(HEX[v ushr 4]).append(HEX[v and 0x0F])
        }
        return sb.toString()
    }

    private val HEX = "0123456789abcdef".toCharArray()
}
