package com.quillfinch.rsvpreader.rsvp

/** A single word shown for one frame of the presentation. */
data class Word(
    val text: String,
    /** Index of the focus (ORP) letter held at the fixed center position. */
    val orpIndex: Int,
    /** True when this word ends a paragraph, warranting a longer pause. */
    val paragraphEnd: Boolean = false,
)

object Tokenizer {
    private const val MAX_LEN = 13
    private val whitespace = Regex("\\s+")

    fun tokenize(text: String): List<Word> {
        val words = mutableListOf<Word>()
        for (paragraph in text.split('\n')) {
            val tokens = paragraph.trim().split(whitespace).filter { it.isNotBlank() }
            tokens.forEachIndexed { tokenIndex, token ->
                val chunks = chunkLong(token)
                chunks.forEachIndexed { chunkIndex, chunk ->
                    words += Word(
                        text = chunk,
                        orpIndex = orpIndex(chunk),
                        paragraphEnd = tokenIndex == tokens.lastIndex && chunkIndex == chunks.lastIndex,
                    )
                }
            }
        }
        return words
    }

    /** Spritz-style optimal recognition point for a word of a given length. */
    fun orpIndex(word: String): Int = when (word.length) {
        0, 1 -> 0
        in 2..5 -> 1
        in 6..9 -> 2
        in 10..13 -> 3
        else -> 4
    }

    /** Extra pause factor after words that end in punctuation. */
    fun delayFactor(word: String): Float = when (word.lastOrNull()) {
        ',', ';', ':' -> 1.4f
        '.', '!', '?', '…' -> 2.0f
        else -> 1f
    }

    fun endsSentence(word: Word): Boolean {
        val last = word.text.lastOrNull()
        return last != null && last in ".!?…"
    }

    /** Splits words longer than [MAX_LEN] into dash-continued chunks. */
    private fun chunkLong(word: String): List<String> {
        if (word.length <= MAX_LEN) return listOf(word)
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < word.length) {
            val end = minOf(start + MAX_LEN - 1, word.length)
            val part = word.substring(start, end)
            chunks += if (end < word.length) "$part-" else part
            start = end
        }
        return chunks
    }
}
