package com.health.knowledge.util

object ArticleTextFormatter {

    private val REGEX_PUNCT_SPACE = Regex("([।?!])([^\\s\\d\\)\"’”\\]>])")
    private val REGEX_PUNCT_DOUBLE_SPACE = Regex("([।?!:\"’”\\]])[ \\t]{2,}")
    private val REGEX_TRIPLE_SPACE = Regex("[ \\t]{3,}")
    private val SECTION_ICON_REGEXES = listOf("✅", "📌", "🔥", "⚠️", "🚨", "💡", "🌟", "❓", "ℹ️")
        .map { icon -> icon to Regex("(?<!\n)[ \\t]*${Regex.escape(icon)}") }
    private val BULLET_ICON_REGEXES = listOf("🔹", "👉", "➡️", "▶️", "▫️", "▪️", "★", "✔", "•")
        .map { icon -> icon to Regex("(?<!\n)[ \\t]*${Regex.escape(icon)}") }
    private val REGEX_SPECIAL_LABELS = Regex("(?<!\n)[ \\t]*(বি:দ্র:?|করনীয়[ঃ:]|করণীয়[ঃ:]|উপকারিতা[ঃ:]|অপকারিতা[ঃ:]|নিয়ম[ঃ:]|নিয়মাবলি[ঃ:]|সতর্কতা[ঃ:])")
    private val REGEX_NUMBERED_LIST = Regex("(?<=[।?!])\\s*([০-৯\\d]+[\\.\\)])\\s+")
    private val REGEX_DOT_BULLET = Regex("(?<=[।?!])\\s*\\.\\s+([^\\s])")
    private val REGEX_ARROW_BLOCK = Regex("(?<!\n)\\s+=>\\s*")
    private val REGEX_QUOTE_BLOCK = Regex("(?<!\n)\\s+>\\s*([\"'“‘«\\w])")
    private val REGEX_SIGNATURE = Regex("(?<!\n)\\s*(~[^\\n]+)$")
    private val REGEX_EMOJI_JOIN_1 = Regex("(📌|✅|🔥|⚠️)\\s*\\n+\\s*(🔹|👉)")
    private val REGEX_EMOJI_JOIN_2 = Regex("(📌|✅|🔥|⚠️)\\s*\\n+\\s*([^\\n]+)")
    private val REGEX_EXCESS_NEWLINES = Regex("\\n{3,}")

    /**
     * Formats raw Bengali article text into clean paragraphs, bullet points,
     * and readable line breaks.
     */
    fun formatBengaliParagraphs(text: String): String {
        if (text.isBlank()) return text

        var t = text.replace("\r\n", "\n").replace("\r", "\n")

        // 1. Add space after dandi/punctuation if followed immediately by letter
        t = REGEX_PUNCT_SPACE.replace(t, "$1 $2")

        // 2. Punctuation (dandi, ?, !, colon, quote, brackets) followed by 2 or more spaces -> paragraph break
        t = REGEX_PUNCT_DOUBLE_SPACE.replace(t, "$1\n\n")

        // 3. 3 or more spaces anywhere -> paragraph break
        t = REGEX_TRIPLE_SPACE.replace(t, "\n\n")

        // 4. Before section headers / major icons -> \n\n
        for ((icon, regex) in SECTION_ICON_REGEXES) {
            t = regex.replace(t, "\n\n$icon")
        }

        // 5. Before list item bullets -> \n
        for ((icon, regex) in BULLET_ICON_REGEXES) {
            t = regex.replace(t, "\n$icon")
        }

        // 6. Before special labels with colon
        t = REGEX_SPECIAL_LABELS.replace(t, "\n\n$1")

        // 7. Before numbered lists or bullets
        t = REGEX_NUMBERED_LIST.replace(t, "\n\n$1 ")
        t = REGEX_DOT_BULLET.replace(t, "\n• $1")
        t = REGEX_ARROW_BLOCK.replace(t, "\n\n=> ")
        t = REGEX_QUOTE_BLOCK.replace(t, "\n\n> $1")

        // Author signature
        t = REGEX_SIGNATURE.replace(t, "\n\n$1")

        // Clean up standalone emoji lines
        t = REGEX_EMOJI_JOIN_1.replace(t, "$1 $2")
        t = REGEX_EMOJI_JOIN_2.replace(t, "$1 $2")

        // Clean up excessive newlines
        val lines = t.split("\n").map { it.trim() }
        t = lines.joinToString("\n")
        t = REGEX_EXCESS_NEWLINES.replace(t, "\n\n")

        return t.trim()
    }
}
