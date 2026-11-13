package ru.gleb.readingjournal.testing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.gleb.readingjournal.demo.CatalogDarkPreview
import ru.gleb.readingjournal.demo.CatalogLightPreview

/** Debug-only test fixture, no launcher route or release manifest entry. */
class PreviewHostActivity : ComponentActivity() {
    val previewState: PreviewHostState
        get() = ViewModelProvider(this)[PreviewHostState::class.java]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Reinstall the composition on every configuration recreation, unlike a bare
        // ComponentActivity whose content was supplied only by ComposeTestRule.setContent.
        setContent {
            if (previewState.dark) CatalogDarkPreview() else CatalogLightPreview()
        }
    }
}

class PreviewHostState : ViewModel() {
    var dark by mutableStateOf(false)
}
