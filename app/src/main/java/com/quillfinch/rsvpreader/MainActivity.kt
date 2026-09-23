package com.quillfinch.rsvpreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.quillfinch.rsvpreader.ui.HomeScreen
import com.quillfinch.rsvpreader.ui.ReaderScreen
import com.quillfinch.rsvpreader.ui.theme.RsvpTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RsvpTheme {
                val viewModel: ReaderViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { ReaderViewModel(application) }
                    }
                )
                val settings by viewModel.settings.collectAsState()
                val text by viewModel.text.collectAsState()
                val fileName by viewModel.fileName.collectAsState()
                val busy by viewModel.busy.collectAsState()
                val error by viewModel.error.collectAsState()
                val reader by viewModel.reader.collectAsState()

                var reading by rememberSaveable { mutableStateOf(false) }

                val filePicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) viewModel.loadFile(uri)
                }

                if (reading) {
                    ReaderScreen(
                        state = reader,
                        settings = settings,
                        onTap = viewModel::playPause,
                        onSkip = viewModel::skipSentences,
                        onRestart = viewModel::restart,
                        onScrub = viewModel::setIndexFromFraction,
                        onWpmChange = viewModel::setWpm,
                        onExit = {
                            viewModel.pause()
                            reading = false
                        },
                    )
                } else {
                    HomeScreen(
                        text = text,
                        fileName = fileName,
                        busy = busy,
                        error = error,
                        settings = settings,
                        onTextChange = viewModel::onTextChange,
                        onPickFile = { filePicker.launch(FILE_MIME_TYPES) },
                        onStart = {
                            viewModel.beginSession()
                            reading = true
                        },
                        onWpmChange = viewModel::setWpm,
                        onTextColorChange = viewModel::setTextColor,
                        onBackgroundColorChange = viewModel::setBackgroundColor,
                        onFocusColorChange = viewModel::setFocusColor,
                        onResetColors = viewModel::resetColors,
                        onErrorShown = viewModel::clearError,
                    )
                }
            }
        }
    }

    companion object {
        private val FILE_MIME_TYPES = arrayOf(
            "text/plain",
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        )
    }
}
