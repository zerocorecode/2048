package com.zerocode.game2048

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerocode.game2048.ui.GameScreen
import com.zerocode.game2048.ui.theme.GameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                val model: GameViewModel = viewModel()
                GameScreen(
                    state = model.uiState.collectAsStateWithLifecycle().value,
                    onMove = model::move,
                    onNewGame = model::newGame,
                    onUndo = model::undo,
                    onToggleAi = model::toggleAi,
                    onToggleAuto = model::toggleAuto,
                )
            }
        }
    }
}
