package ru.gleb.readingjournal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Rotation restores context; a genuinely new process starts at Home (NAV-12).
        super.onCreate(savedInstanceState?.takeIf { it.getString(PROCESS_KEY) == processId })
        enableEdgeToEdge()
        setContent { JournalApp() }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(PROCESS_KEY, processId)
    }
    companion object {
        private const val PROCESS_KEY = "journal.ui01.process"
        private val processId = UUID.randomUUID().toString()
    }
}
