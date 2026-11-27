package ru.gleb.readingjournal

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.gleb.readingjournal.ui.overview.formatOverviewPages
import java.util.Locale

class OverviewPagesFormatTest {
    @Test fun formatsApprovedPageCountsWithoutRoundingUp() {
        val cases = listOf(
            0 to "0",
            999 to "999",
            1_000 to "1K",
            1_050 to "1K",
            1_100 to "1,1K",
            1_250 to "1,2K",
            1_500 to "1,5K",
            1_999 to "1,9K",
            9_999 to "9,9K",
            12_345 to "12,3K",
            100_000 to "100K",
            128_540 to "128,5K",
            2_000 to "2K",
        )
        for ((count, expected) in cases) {
            assertEquals("Page count $count", expected, formatOverviewPages(count))
        }
    }

    @Test fun keepsExactFormattingFromOneMillion() {
        assertEquals("999,9K", formatOverviewPages(999_999))
        assertEquals("1 000 000", formatOverviewPages(1_000_000))
        assertEquals("2 147 483 647", formatOverviewPages(Int.MAX_VALUE))
    }

    @Test fun usesTheSameDigitsAndCommaAcrossSystemLocales() {
        val previous = Locale.getDefault()
        val previousDisplay = Locale.getDefault(Locale.Category.DISPLAY)
        val previousFormat = Locale.getDefault(Locale.Category.FORMAT)
        try {
            for (locale in listOf(Locale.US, Locale.forLanguageTag("ru-RU"), Locale.forLanguageTag("ar-EG"))) {
                Locale.setDefault(locale)
                assertEquals("128,5K", formatOverviewPages(128_540))
                assertEquals("1 000 000", formatOverviewPages(1_000_000))
            }
        } finally {
            Locale.setDefault(previous)
            Locale.setDefault(Locale.Category.DISPLAY, previousDisplay)
            Locale.setDefault(Locale.Category.FORMAT, previousFormat)
        }
    }
}
