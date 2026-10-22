package com.zerocode.game2048

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerocode.game2048.ai.AiDecision
import com.zerocode.game2048.ai.ExpectimaxAi
import com.zerocode.game2048.ai.SearchLimits
import com.zerocode.game2048.core.Direction
import com.zerocode.game2048.core.GameEngine
import com.zerocode.game2048.core.GameState
import com.zerocode.game2048.core.GameTransition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlin.random.Random

data class GameUiState(
    val game: GameState,
    val bestScore: Long = game.score,
    val aiEnabled: Boolean = false,
    val decision: AiDecision? = null,
    val isThinking: Boolean = false,
    val isAutoRunning: Boolean = false,
    val history: List<GameState> = emptyList(),
    val lastTransition: GameTransition? = null,
    val lastMoveWasAuto: Boolean = false,
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val random = Random.Default
    private val scorePreferences = application.getSharedPreferences("game_stats", Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(
        GameUiState(GameEngine.newGame(random), bestScore = scorePreferences.getLong("best_score", 0L)),
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null
    private var generation = 0L

    fun newGame() {
        if (_uiState.value.isAutoRunning) return
        stopSearch()
        val previous = _uiState.value
        _uiState.value = GameUiState(
            game = GameEngine.newGame(random),
            bestScore = previous.bestScore,
            aiEnabled = previous.aiEnabled,
        )
        refreshDecision()
    }

    fun toggleAi() {
        if (_uiState.value.isAutoRunning) return
        stopSearch()
        _uiState.update { it.copy(aiEnabled = !it.aiEnabled, decision = null) }
        refreshDecision()
    }

    fun move(direction: Direction) {
        if (_uiState.value.isAutoRunning || _uiState.value.game.isGameOver) return
        stopSearch()
        applyMove(direction)
        refreshDecision()
    }

    fun undo() {
        if (_uiState.value.isAutoRunning) return
        stopSearch()
        val current = _uiState.value
        val previous = current.history.lastOrNull() ?: return
        _uiState.value = current.copy(
            game = previous,
            history = current.history.dropLast(1),
            lastTransition = null,
            decision = null,
        )
        refreshDecision()
    }

    fun toggleAuto() {
        if (_uiState.value.isAutoRunning) {
            stopSearch()
            refreshDecision()
        } else if (_uiState.value.aiEnabled && !_uiState.value.game.isGameOver) {
            startSearch(auto = true)
        }
    }

    private fun refreshDecision() = startSearch(auto = false)

    private fun startSearch(auto: Boolean) {
        stopSearch()
        val current = _uiState.value
        if (!current.aiEnabled || current.game.isGameOver) return
        val ownGeneration = generation
        _uiState.update { it.copy(isThinking = true, isAutoRunning = auto, decision = null) }
        searchJob = viewModelScope.launch {
            try {
                while (isActive && !_uiState.value.game.isGameOver) {
                    val snapshot = _uiState.value
                    val context = currentCoroutineContext()
                    val decision = runInterruptible(Dispatchers.Default) {
                        ExpectimaxAi().chooseMove(
                            snapshot.game,
                            SearchLimits(isCancelled = { !context.isActive }),
                        )
                    }
                    context.ensureActive()
                    if (ownGeneration != generation || snapshot.game != _uiState.value.game) return@launch
                    _uiState.update { it.copy(decision = decision, isThinking = false) }
                    if (!auto || decision == null) break
                    delay(130)
                    context.ensureActive()
                    if (!applyMove(decision.direction)) break
                    delay(350)
                    _uiState.update { it.copy(isThinking = true, decision = null) }
                }
            } finally {
                if (ownGeneration == generation) {
                    _uiState.update { it.copy(isAutoRunning = false, isThinking = false) }
                }
            }
        }
    }

    private fun stopSearch() {
        generation++
        searchJob?.cancel()
        searchJob = null
        _uiState.update { it.copy(isAutoRunning = false, isThinking = false) }
    }

    private fun applyMove(direction: Direction): Boolean {
        val current = _uiState.value
        val transition = GameEngine.move(current.game, direction, random) ?: return false
        val bestScore = maxOf(current.bestScore, transition.after.score)
        if (bestScore > current.bestScore) {
            scorePreferences.edit().putLong("best_score", bestScore).apply()
        }
        _uiState.value = current.copy(
            game = transition.after,
            bestScore = bestScore,
            history = (current.history + current.game).takeLast(40),
            lastTransition = transition,
            lastMoveWasAuto = current.isAutoRunning,
            decision = null,
            isAutoRunning = current.isAutoRunning && !transition.after.isGameOver,
        )
        return true
    }
}
