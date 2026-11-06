package com.health.knowledge.util

object ArticleTextFormatter {

    /**
     * Formats raw Bengali article text into clean paragraphs, bullet points,
     * and readable line breaks.
     */
    fun formatBengaliParagraphs(text: String): String {
        if (text.isBlank()) return text

        var t = text.replace("\r\n", "\n").replace("\r", "\n")

        // 1. Add space after dandi/punctuation if followed immediately by letter
        t = t.replace(Regex("([।?!])([^\\s\\d\\)\"’”\\]>])"), "$1 $2")

        // 2. Punctuation (dandi, ?, !, colon, quote, brackets) followed by 2 or more spaces -> paragraph break
        t = t.replace(Regex("([।?!:\"’”\\]])[ \\t]{2,}"), "$1\n\n")

        // 3. 3 or more spaces anywhere -> paragraph break
        t = t.replace(Regex("[ \\t]{3,}"), "\n\n")

        // 4. Before section headers / major icons -> \n\n
        val sectionIcons = listOf("✅", "📌", "🔥", "⚠️", "🚨", "💡", "🌟", "❓", "ℹ️")
        for (icon in sectionIcons) {
            t = t.replace(Regex("(?<!\n)[ \\t]*${Regex.escape(icon)}"), "\n\n$icon")
        }

        // 5. Before list item bullets -> \n
        val bulletIcons = listOf("🔹", "👉", "➡️", "▶️", "▫️", "▪️", "★", "✔", "•")
        for (icon in bulletIcons) {
            t = t.replace(Regex("(?<!\n)[ \\t]*${Regex.escape(icon)}"), "\n$icon")
        }

        // 6. Before special labels with colon
        t = t.replace(
            Regex("(?<!\n)[ \\t]*(বি:দ্র:?|করনীয়[ঃ:]|করণীয়[ঃ:]|উপকারিতা[ঃ:]|অপকারিতা[ঃ:]|নিয়ম[ঃ:]|নিয়মাবলি[ঃ:]|সতর্কতা[ঃ:])"),
            "\n\n$1"
        )

        // 7. Before numbered lists or bullets
        t = t.replace(Regex("(?<=[।?!])\\s*([০-৯\\d]+[\\.\\)])\\s+"), "\n\n$1 ")
        t = t.replace(Regex("(?<=[।?!])\\s*\\.\\s+([^\\s])"), "\n• $1")
        t = t.replace(Regex("(?<!\n)\\s+=>\\s*"), "\n\n=> ")
        t = t.replace(Regex("(?<!\n)\\s+>\\s*([\"'“‘«\\w])"), "\n\n> $1")

        // Author signature
        t = t.replace(Regex("(?<!\n)\\s*(~[^\\n]+)$"), "\n\n$1")

        // Clean up standalone emoji lines
        t = t.replace(Regex("(📌|✅|🔥|⚠️)\\s*\\n+\\s*(🔹|👉)"), "$1 $2")
        t = t.replace(Regex("(📌|✅|🔥|⚠️)\\s*\\n+\\s*([^\\n]+)"), "$1 $2")

        // Clean up excessive newlines
        val lines = t.split("\n").map { it.trim() }
        t = lines.joinToString("\n")
        t = t.replace(Regex("\\n{3,}"), "\n\n")

        return t.trim()
    }
}
