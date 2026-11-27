package ru.gleb.readingjournal

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.gleb.readingjournal.ui.designsystem.JournalShape

class IslandShapeTest {
    @Test fun navigationIslandsUseProportionalShapesAndCardsStayUnchanged() {
        assertEquals(RoundedCornerShape(50), JournalShape.islandCapsule)
        assertEquals(CircleShape, JournalShape.islandAction)
        assertEquals(RoundedCornerShape(16.dp), JournalShape.card)
        assertEquals(RoundedCornerShape(20.dp), JournalShape.largeCard)
    }
}
