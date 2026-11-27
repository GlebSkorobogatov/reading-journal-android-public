package ru.gleb.readingjournal.ui.designsystem

/** The first Unicode letter, ignoring punctuation, opening quotes, digits and whitespace. */
fun coverInitial(title: String): String {
    var index = 0
    while (index < title.length) {
        val symbol = title.codePointAt(index)
        if (Character.isLetter(symbol)) return String(Character.toChars(Character.toUpperCase(symbol)))
        index += Character.charCount(symbol)
    }
    return "—"
}
