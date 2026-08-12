package com.pokeapp.util

import com.pokeapp.domain.model.Card

data class ScanMatch(val card: Card, val score: Double)

private const val CONFIDENCE_THRESHOLD = 0.4

object OcrTextMatcher {

    /** Ranks candidate cards by similarity of their name to the raw OCR text. */
    fun rank(ocrText: String, candidates: List<Card>): List<ScanMatch> {
        val normalizedQuery = normalize(ocrText)
        return candidates
            .map { card -> ScanMatch(card, similarity(normalizedQuery, normalize(card.cardName))) }
            .sortedByDescending { it.score }
    }

    fun hasConfidentMatch(matches: List<ScanMatch>): Boolean =
        matches.firstOrNull()?.let { it.score >= CONFIDENCE_THRESHOLD } ?: false

    private fun normalize(text: String) = text.lowercase().trim()

    /** 1.0 = identical, 0.0 = completely different, based on normalized edit distance. */
    private fun similarity(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val distance = levenshtein(a, b)
        val maxLen = maxOf(a.length, b.length)
        return 1.0 - (distance.toDouble() / maxLen)
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost,
                )
            }
        }
        return dp[a.length][b.length]
    }
}
