package ru.gleb.readingjournal

import java.io.File
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.gleb.readingjournal.ui.designsystem.libraryBooks

class LibraryAndLauncherTest {
    private val bookPath =
        "M7,3.5 L17.5,3.5 Q19,3.5 19,5 L19,19 Q19,20.5 17.5,20.5 L7,20.5 Q5.5,20.5 5.5,19 L5.5,5 Q5.5,3.5 7,3.5 Z M8.5,3.5 L8.5,20.5"

    @Test fun libraryIsThreeOffsetBooksWithPageEdgesTowardTheViewer() {
        val books = libraryBooks()
        assertEquals(3, books.size)
        val lefts = books.map { it.left }.toSet()
        assertEquals("books should not share one left edge", 3, lefts.size)
        books.zipWithNext().forEach { (above, below) ->
            assertTrue("covers need a visible gap", below.top - above.bottom >= 2f)
        }
        books.forEach { book ->
            assertTrue("a lying book is wider than it is tall", book.width > book.height)
            val lowerHalf = book.top + book.height / 2f
            assertTrue("page edge sits in the lower half, not on a spine", book.pageY > lowerHalf)
            assertTrue("page edge stays clear of the cover stroke", book.bottom - book.pageY >= 2f)
        }
    }

    @Test fun readingAndStatisticsIconsStayTheOriginalDrawings() {
        val icon = source("src/main/java/ru/gleb/readingjournal/ui/designsystem/LineIcon.kt")
        val book = source("src/main/res/drawable/ic_closed_book.xml")
        assertTrue(book.contains(bookPath))
        assertTrue(icon.contains("ic_closed_book"))
        assertTrue(icon.contains("moveTo(4f, 4f); lineTo(4f, 20f); lineTo(21f, 20f)"))
        assertTrue(icon.contains("moveTo(8f, 16f); lineTo(8f, 12f)"))
        assertTrue(icon.contains("moveTo(13f, 16f); lineTo(13f, 8f)"))
        assertTrue(icon.contains("moveTo(18f, 16f); lineTo(18f, 5f)"))
    }

    @Test fun launcherReusesTheReadingIconInsideTheSafeCircle() {
        val foreground = source("src/main/res/drawable/ic_launcher_foreground.xml")
        val monochrome = source("src/main/res/drawable/ic_launcher_monochrome.xml")
        val adaptive = source("src/main/res/mipmap-anydpi/ic_launcher.xml")
        assertTrue(foreground.contains(bookPath))
        assertTrue(monochrome.contains(bookPath))
        assertTrue(foreground.contains("@color/launcher_ink"))
        assertTrue(foreground.contains("@color/launcher_page"))
        assertTrue(adaptive.contains("@drawable/ic_launcher_foreground"))
        assertTrue(adaptive.contains("@drawable/ic_launcher_monochrome"))
        assertTrue(adaptive.contains("@color/launcher_background"))
        val scale = 2.76f
        val pivotX = 12.25f
        val pivotY = 12f
        val translateX = 41.75f
        val translateY = 42f
        listOf(foreground, monochrome).forEach { xml ->
            assertTrue(xml.contains("android:scaleX=\"2.76\""))
            assertTrue(xml.contains("android:scaleY=\"2.76\""))
            assertTrue(xml.contains("android:pivotX=\"12.25\""))
            assertTrue(xml.contains("android:translateX=\"41.75\""))
            assertTrue(xml.contains("android:translateY=\"42\""))
        }
        val corners = listOf(5.5f to 3.5f, 19f to 3.5f, 19f to 20.5f, 5.5f to 20.5f)
        val halfStroke = 1.5f * scale / 2f
        corners.forEach { (x, y) ->
            val mappedX = pivotX + (x - pivotX) * scale + translateX
            val mappedY = pivotY + (y - pivotY) * scale + translateY
            val reach = hypot(mappedX - 54f, mappedY - 54f) + halfStroke
            assertTrue("stroke leaves the 66dp safe circle: $reach", reach < 33f)
        }
    }

    private fun source(relative: String): String {
        val file = listOf(File(relative), File("app/$relative")).first { it.isFile }
        return file.readText()
    }
}
