package ru.gleb.readingjournal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

/** The root pager must not take a gesture a nested horizontal child can still use. */
class PagerNestingTest {
    @get:Rule val rule = createComposeRule()

    @Test fun aChildThatCanScrollHorizontallyKeepsTheGesture() {
        rule.setContent {
            val pager = rememberPagerState { 2 }
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().testTag("pager")) { page ->
                if (page == 0) {
                    Row(Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).testTag("strip")) {
                        Box(Modifier.size(2400.dp, 48.dp).testTag("wide"))
                        Text("Конец ленты", Modifier.testTag("strip-end"))
                    }
                } else {
                    Text("Вторая страница", Modifier.testTag("second"))
                }
            }
        }
        rule.onNodeWithTag("strip").performTouchInput { swipeLeft() }
        rule.onAllNodesWithTag("second").assertCountEquals(0)
        rule.onNodeWithTag("strip").performSemanticsAction(SemanticsActions.ScrollBy) { it(20_000f, 0f) }
        rule.waitForIdle()
        rule.onNodeWithTag("strip").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithText("Вторая страница").assertIsDisplayed()
    }
}
