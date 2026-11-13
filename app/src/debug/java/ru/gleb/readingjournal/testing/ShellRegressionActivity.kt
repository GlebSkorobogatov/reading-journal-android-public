package ru.gleb.readingjournal.testing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.gleb.readingjournal.app.JournalApp

/** Retains the accepted UI-01 shell fixture for its unchanged placeholder assertions. */
class ShellRegressionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { JournalApp(showHomeDemo = false) }
    }
}
