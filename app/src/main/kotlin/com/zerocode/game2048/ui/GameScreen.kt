package com.zerocode.game2048.ui

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import com.zerocode.game2048.GameUiState
import com.zerocode.game2048.R
import com.zerocode.game2048.audio.GameAudioController
import com.zerocode.game2048.core.Board
import com.zerocode.game2048.core.Direction
import com.zerocode.game2048.core.GameTransition
import com.zerocode.game2048.core.TileMotion
import com.zerocode.game2048.ui.theme.Accent
import com.zerocode.game2048.ui.theme.BoardColor
import com.zerocode.game2048.ui.theme.Coral
import com.zerocode.game2048.ui.theme.EmptyColor
import com.zerocode.game2048.ui.theme.Ink
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TileFont = FontFamily(Font(R.font.numbers_premium, weight = FontWeight.Normal))

@Composable
fun GameScreen(
    state: GameUiState,
    onMove: (Direction) -> Unit,
    onNewGame: () -> Unit,
    onUndo: () -> Unit,
    onToggleAi: () -> Unit,
    onToggleAuto: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferences = remember(context) {
        context.getSharedPreferences("game_audio", Context.MODE_PRIVATE)
    }
    val audioController = remember(context) {
        GameAudioController(context.applicationContext)
    }
    var audioEnabled by rememberSaveable { mutableStateOf(preferences.getBoolean("enabled", true)) }
    var autoAudioEnabled by rememberSaveable { mutableStateOf(false) }
    val effectiveAudio = if (state.isAutoRunning) autoAudioEnabled else audioEnabled
    var previousMoveCount by remember { mutableStateOf(state.game.moveCount) }
    var confirmNewGame by rememberSaveable { mutableStateOf(false) }
    var hideGameOver by rememberSaveable(state.game.isGameOver) { mutableStateOf(false) }
    var gameOverReady by remember(state.game.isGameOver) { mutableStateOf(false) }
    var boardAnimating by remember { mutableStateOf(false) }
    LaunchedEffect(state.game.isGameOver) {
        if (state.game.isGameOver) {
            delay(450)
            gameOverReady = true
        }
    }
    DisposableEffect(lifecycleOwner, audioController) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> audioController.setForeground(true)
                Lifecycle.Event.ON_STOP -> audioController.setForeground(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        audioController.setForeground(
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
        )
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(audioController) { onDispose(audioController::release) }
    LaunchedEffect(effectiveAudio) { audioController.setEnabled(effectiveAudio) }
    LaunchedEffect(state.game.moveCount) {
        if (state.game.moveCount > previousMoveCount &&
            (if (state.lastMoveWasAuto) autoAudioEnabled else audioEnabled)
        ) {
            audioController.playMove(
                merged = (state.lastTransition?.scoreGain ?: 0) > 0,
            )
        }
        previousMoveCount = state.game.moveCount
    }
    fun submitMove(direction: Direction) {
        if (state.isAutoRunning || state.game.isGameOver || boardAnimating) return
        if (!state.game.board.move(direction).moved) {
            audioController.playInvalidMove()
            return
        }
        onMove(direction)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding()
                .navigationBarsPadding().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(18.dp))
            Header(state)
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                val boardWidth = minOf(maxWidth, maxHeight, 500.dp)
                Box(modifier = Modifier.width(boardWidth)) {
                    GameBoard(
                        board = state.game.board,
                        lastTransition = state.lastTransition,
                        enabled = !state.isAutoRunning && !state.game.isGameOver,
                        onAnimationStateChange = { boardAnimating = it },
                        onInvalidMove = audioController::playInvalidMove,
                        onMove = onMove,
                    )
                    if (state.game.isGameOver && gameOverReady && !hideGameOver) {
                        GameOverOverlay(
                            state = state,
                            onReview = { audioController.playClick(); hideGameOver = true },
                            onNewGame = { audioController.playClick(); onNewGame() },
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                }
            }
            AiStatus(state, ::submitMove, audioController::playClick)
            ActionBar(
                state = state,
                audioEnabled = effectiveAudio,
                onToggleAi = { audioController.playClick(); onToggleAi() },
                onToggleAuto = {
                    audioController.playDeepClick(allowWhenDisabled = audioEnabled)
                    if (!state.isAutoRunning) autoAudioEnabled = false
                    onToggleAuto()
                },
                onUndo = { audioController.playClick(); onUndo() },
                onToggleAudio = {
                    val wasEnabled = effectiveAudio
                    if (wasEnabled) audioController.playClick()
                    if (state.isAutoRunning) {
                        autoAudioEnabled = !autoAudioEnabled
                        audioController.setEnabled(autoAudioEnabled)
                    } else {
                        audioEnabled = !audioEnabled
                        preferences.edit().putBoolean("enabled", audioEnabled).apply()
                        audioController.setEnabled(audioEnabled)
                    }
                    if (!wasEnabled) audioController.playClick()
                },
                onNewGame = {
                    audioController.playClick()
                    if (state.game.isGameOver) onNewGame() else confirmNewGame = true
                },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
    if (confirmNewGame) {
        Dialog(onDismissRequest = { confirmNewGame = false }) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("开始新游戏？", color = Ink, fontWeight = FontWeight.Bold,
                        fontSize = 20.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("当前进度会被清除。", color = Ink.copy(alpha = 0.78f))
                    Spacer(Modifier.height(22.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            audioController.playClick()
                            confirmNewGame = false
                        }) { Text("取消", color = Ink) }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                audioController.playClick()
                                confirmNewGame = false
                                onNewGame()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Coral),
                            shape = RoundedCornerShape(7.dp),
                        ) { Text("开始") }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: GameUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScoreBlock("分数", state.game.score)
            ScoreBlock("最佳", state.bestScore, color = Color(0xFFC69A55))
        }
        Surface(shape = RoundedCornerShape(6.dp), color = BoardColor) {
            Column(
                modifier = Modifier.width(84.dp).height(65.dp).padding(6.dp)
                    .semantics { contentDescription = "下一张 ${Board.valueOf(state.game.nextRank)}" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Tile(state.game.nextRank, 44.dp, compact = true)
            }
        }
    }
}

@Composable
private fun ScoreBlock(label: String, score: Long, color: Color = BoardColor) {
    val formatted = formatCompactScore(score)
    Surface(shape = RoundedCornerShape(6.dp), color = color) {
        Column(
            modifier = Modifier.width(84.dp).height(65.dp).padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
            Text(
                formatted, fontSize = if (formatted.length >= 8) 15.sp else 19.sp,
                fontFamily = TileFont, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1,
            )
        }
    }
}

private data class PendingSwipe(
    val board: Board,
    val motions: List<TileMotion>,
    val progress: Float,
)

@Composable
private fun GameBoard(
    board: Board,
    lastTransition: GameTransition?,
    enabled: Boolean,
    onAnimationStateChange: (Boolean) -> Unit,
    onInvalidMove: () -> Unit,
    onMove: (Direction) -> Unit,
) {
    var drag by remember { mutableStateOf(Offset.Zero) }
    var activeTransition by remember { mutableStateOf<GameTransition?>(null) }
    var popTransition by remember { mutableStateOf<GameTransition?>(null) }
    var completedTransition by remember { mutableStateOf<GameTransition?>(null) }
    var pendingSwipe by remember { mutableStateOf<PendingSwipe?>(null) }
    var reboundDirection by remember { mutableStateOf<Direction?>(null) }
    var reboundJob by remember { mutableStateOf<Job?>(null) }
    val slideProgress = remember { Animatable(0f) }
    val popProgress = remember { Animatable(0f) }
    val reboundDistance = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    LaunchedEffect(board, lastTransition) {
        if (lastTransition?.after?.board != board) {
            activeTransition = null
            popTransition = null
            pendingSwipe = null
            completedTransition = null
            onAnimationStateChange(false)
            return@LaunchedEffect
        }
        onAnimationStateChange(true)
        try {
            val start = pendingSwipe?.takeIf { it.board == lastTransition.before.board }?.progress ?: 0f
            slideProgress.snapTo(start)
            activeTransition = lastTransition
            pendingSwipe = null
            slideProgress.animateTo(1f, tween(170, easing = FastOutSlowInEasing))
            activeTransition = null
            popProgress.snapTo(0f)
            popTransition = lastTransition
            popProgress.animateTo(1f, tween(140, easing = FastOutSlowInEasing))
            completedTransition = lastTransition
        } finally {
            activeTransition = null
            popTransition = null
            pendingSwipe = null
            onAnimationStateChange(false)
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(BoardColor, RoundedCornerShape(8.dp))
            .padding(7.dp),
    ) {
        val gap = 7.dp
        val cell = (maxWidth - gap * 3) / 4
        val stride = cell + gap
        val stridePx = with(density) { stride.toPx() }
        val reboundLimit = with(density) { 15.dp.toPx() }
        val incomingTransition = lastTransition?.takeIf {
            it.after.board == board && it != completedTransition && it != popTransition
        }
        val slidingTransition = activeTransition ?: incomingTransition
        val swipe = pendingSwipe?.takeIf {
            it.board == board || it.board == slidingTransition?.before?.board
        }
        val renderedBoard = slidingTransition?.before?.board ?: swipe?.board ?: board
        val previewDirection = if (abs(drag.x) > 6f || abs(drag.y) > 6f) {
            if (abs(drag.x) > abs(drag.y)) {
                if (drag.x < 0) Direction.LEFT else Direction.RIGHT
            } else {
                if (drag.y < 0) Direction.UP else Direction.DOWN
            }
        } else null
        val previewMove = previewDirection?.let(renderedBoard::move)

        Box(
            Modifier.fillMaxSize().pointerInput(enabled, board, slidingTransition, popTransition) {
                if (!enabled || slidingTransition != null || popTransition != null ||
                    pendingSwipe != null) return@pointerInput
                detectDragGestures(
                    onDragStart = {
                        reboundJob?.cancel()
                        reboundDirection = null
                        drag = Offset.Zero
                    },
                    onDragEnd = {
                        val released = drag
                        val direction = directionForDrag(released)
                        if (direction == null) {
                            drag = Offset.Zero
                            return@detectDragGestures
                        }
                        val move = board.move(direction)
                        if (!move.moved) {
                            drag = Offset.Zero
                            onInvalidMove()
                            reboundDirection = direction
                            reboundJob = scope.launch {
                                reboundDistance.snapTo(
                                    min(directionDistance(direction, released), reboundLimit),
                                )
                                reboundDistance.animateTo(
                                    0f,
                                    spring(
                                        dampingRatio = 0.42f,
                                        stiffness = Spring.StiffnessMedium,
                                    ),
                                )
                                reboundDirection = null
                            }
                        } else {
                            pendingSwipe = PendingSwipe(
                                board = board,
                                motions = move.motions,
                                progress = (directionDistance(direction, released) / stridePx)
                                    .coerceIn(0f, 0.72f),
                            )
                            drag = Offset.Zero
                            onAnimationStateChange(true)
                            onMove(direction)
                        }
                    },
                    onDragCancel = { drag = Offset.Zero },
                    onDrag = { change, amount ->
                        change.consume()
                        drag += amount
                    },
                )
            },
        )

        repeat(Board.CELL_COUNT) { index ->
            Box(
                Modifier.offset(x = stride * (index % 4), y = stride * (index / 4))
                    .size(cell).background(EmptyColor, RoundedCornerShape(6.dp)),
            )
        }
        repeat(Board.CELL_COUNT) { index ->
            val rank = renderedBoard[index]
            if (rank == 0) return@repeat
            val motion = when {
                slidingTransition != null -> slidingTransition.motions.firstOrNull {
                    it.fromIndex == index
                }
                swipe != null -> swipe.motions.firstOrNull { it.fromIndex == index }
                previewMove?.moved == true -> previewMove.motions.firstOrNull {
                    it.fromIndex == index
                }
                else -> null
            }
            val progress = when {
                activeTransition != null -> slideProgress.value
                swipe != null -> swipe.progress
                slidingTransition != null -> 0f
                motion != null && previewDirection != null ->
                    (directionDistance(previewDirection, drag) / stridePx).coerceIn(0f, 0.72f)
                else -> 0f
            }
            val travel = motion?.let { motionOffset(it, stridePx) * progress } ?: Offset.Zero
            val invalid = if (slidingTransition == null && swipe == null &&
                previewDirection != null &&
                previewMove?.moved == false
            ) {
                directionalOffset(
                    previewDirection,
                    min(directionDistance(previewDirection, drag), reboundLimit),
                )
            } else Offset.Zero
            val rebound = reboundDirection?.let {
                directionalOffset(it, reboundDistance.value)
            } ?: Offset.Zero
            val pop = popTransition?.takeIf { it.after.board == board }
            val scale = when {
                pop?.spawnedIndex == index -> 0.58f + 0.42f * popProgress.value
                pop?.motions?.any { it.merges && it.toIndex == index } == true ->
                    0.82f + 0.18f * popProgress.value
                else -> 1f
            }
            Box(
                Modifier.offset(x = stride * (index % 4), y = stride * (index / 4))
                    .size(cell)
                    .zIndex(if (motion != null) 1f else 0f)
                    .graphicsLayer {
                        translationX = travel.x + invalid.x + rebound.x
                        translationY = travel.y + invalid.y + rebound.y
                        scaleX = scale
                        scaleY = scale
                    },
            ) { Tile(rank, cell) }
        }
    }
}

private fun directionDistance(direction: Direction, offset: Offset): Float = when (direction) {
    Direction.LEFT, Direction.RIGHT -> abs(offset.x)
    Direction.UP, Direction.DOWN -> abs(offset.y)
}

private fun directionalOffset(direction: Direction, distance: Float): Offset = when (direction) {
    Direction.LEFT -> Offset(-distance, 0f)
    Direction.RIGHT -> Offset(distance, 0f)
    Direction.UP -> Offset(0f, -distance)
    Direction.DOWN -> Offset(0f, distance)
}

private fun motionOffset(motion: TileMotion, stridePx: Float): Offset = Offset(
    x = ((motion.toIndex % 4) - (motion.fromIndex % 4)) * stridePx,
    y = ((motion.toIndex / 4) - (motion.fromIndex / 4)) * stridePx,
)

@Composable
private fun Tile(rank: Int, size: Dp, compact: Boolean = false) {
    val value = if (rank == 0) "" else Board.valueOf(rank).toString()
    val shape = RoundedCornerShape(6.dp)
    if (rank == 0) {
        Box(Modifier.size(size).background(EmptyColor, shape))
    } else {
        val face = tileColor(rank)
        Box(
            modifier = Modifier.size(size)
                .shadow(3.dp, shape)
                .background(darken(face), shape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(bottom = 5.dp)
                    .background(
                        Brush.verticalGradient(listOf(lighten(face), face, face)),
                        shape,
                    ),
            )
            val scaled = when {
                value.length <= 2 -> 0.39f
                value.length <= 3 -> 0.34f
                value.length <= 4 -> 0.29f
                value.length <= 5 -> 0.24f
                else -> 1.5f / value.length
            }
            Text(
                value,
                color = if (rank <= 2) Ink else Color.White,
                fontFamily = TileFont,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 23.sp else (size.value * scaled).sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().offset(y = (-2).dp),
            )
        }
    }
}

@Composable
private fun AiStatus(
    state: GameUiState,
    onMove: (Direction) -> Unit,
    onClickSound: () -> Unit,
) {
    val result = state.decision
    val pendingText = if (state.isThinking) "…" else "—"
    val depthText = result?.completedDepth?.toString() ?: pendingText
    val nodesText = result?.searchedNodes?.let { nodes ->
        when {
            nodes >= 1_000_000 -> "%.1fM".format(java.util.Locale.ROOT, nodes / 1_000_000.0)
            nodes >= 10_000 -> "%.1fk".format(java.util.Locale.ROOT, nodes / 1_000.0)
            else -> formatNumber(nodes)
        }
    } ?: pendingText
    val elapsedText = result?.elapsedMillis?.let { millis ->
        if (millis >= 1_000) "%.1fs".format(java.util.Locale.ROOT, millis / 1_000.0) else "${millis}ms"
    } ?: pendingText
    Row(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                if (state.aiEnabled) "AI 模式" else "手动模式",
                color = if (state.aiEnabled) Accent else Ink,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "深度 $depthText · 节点 $nodesText · 耗时 $elapsedText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.48f),
                maxLines = 1,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Direction.entries.forEach { direction ->
                DirectionControl(
                    direction = direction,
                    selected = state.decision?.direction == direction,
                    enabled = !state.isAutoRunning && !state.game.isGameOver,
                    aiEnabled = state.aiEnabled,
                    evaluation = state.decision?.alternatives
                        ?.firstOrNull { it.direction == direction }?.evaluation,
                    isThinking = state.isThinking,
                    onMove = { onMove(direction) },
                    onClickSound = onClickSound,
                )
            }
        }
    }
}

@Composable
private fun DirectionControl(
    direction: Direction,
    selected: Boolean,
    enabled: Boolean,
    aiEnabled: Boolean,
    evaluation: Double?,
    isThinking: Boolean,
    onMove: () -> Unit,
    onClickSound: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val icon = when (direction) {
        Direction.LEFT -> Icons.AutoMirrored.Rounded.ArrowBack
        Direction.UP -> Icons.Rounded.ArrowUpward
        Direction.RIGHT -> Icons.AutoMirrored.Rounded.ArrowForward
        Direction.DOWN -> Icons.Rounded.ArrowDownward
    }
    val label = when (direction) {
        Direction.LEFT -> "向左"
        Direction.UP -> "向上"
        Direction.RIGHT -> "向右"
        Direction.DOWN -> "向下"
    }
    Box {
        Surface(
            onClick = {
                onClickSound()
                if (aiEnabled) menuOpen = true else onMove()
            },
            enabled = enabled,
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = when {
                selected -> Accent
                evaluation != null -> Accent.copy(alpha = 0.14f)
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.055f)
            },
            border = BorderStroke(
                if (selected) 2.dp else 1.dp,
                if (selected) Accent else MaterialTheme.colorScheme.outline,
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon, contentDescription = label,
                    tint = when {
                        selected -> Color.White
                        evaluation != null -> Accent
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
                    },
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            DropdownMenuItem(
                text = { Text(
                    when {
                        evaluation != null -> "$label  ${"%.1f".format(evaluation)}"
                        isThinking -> "$label  计算中"
                        else -> "$label  不可移动"
                    }
                ) },
                onClick = { onClickSound(); menuOpen = false },
            )
        }
    }
}

@Composable
private fun ActionBar(
    state: GameUiState,
    audioEnabled: Boolean,
    onToggleAi: () -> Unit,
    onToggleAuto: () -> Unit,
    onUndo: () -> Unit,
    onToggleAudio: () -> Unit,
    onNewGame: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionIcon(onToggleAi, !state.isAutoRunning && !state.game.isGameOver, "切换AI模式") {
            Icon(Icons.Rounded.Lightbulb, null, tint = if (state.aiEnabled) Accent else Ink)
        }
        Button(
            onClick = onToggleAuto,
            enabled = state.aiEnabled && !state.game.isGameOver,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ink),
        ) {
            Icon(if (state.isAutoRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
            Spacer(Modifier.width(3.dp))
            Text(if (state.isAutoRunning) "暂停" else "AI 自动", maxLines = 1)
        }
        ActionIcon(onUndo, !state.isAutoRunning && state.history.isNotEmpty(), "撤销") {
            Icon(Icons.AutoMirrored.Rounded.Undo, null)
        }
        ActionIcon(onToggleAudio, true, if (audioEnabled) "关闭声音" else "开启声音") {
            Icon(
                if (audioEnabled) Icons.AutoMirrored.Rounded.VolumeUp
                else Icons.AutoMirrored.Rounded.VolumeOff,
                null,
            )
        }
        ActionIcon(onNewGame, !state.isAutoRunning, "新游戏") {
            Icon(Icons.Rounded.RestartAlt, null)
        }
    }
}

@Composable
private fun ActionIcon(
    onClick: () -> Unit,
    enabled: Boolean,
    description: String,
    icon: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(48.dp).semantics { contentDescription = description },
        ) {
            Box(contentAlignment = Alignment.Center) {
                icon()
            }
        }
    }
}

@Composable
private fun GameOverOverlay(
    state: GameUiState,
    onReview: () -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Ink.copy(alpha = 0.26f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 10.dp,
        ) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("游戏结束", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(14.dp))
                Text("得分 ${formatNumber(state.game.score)}", color = Ink, fontSize = 18.sp)
                Text(
                    "最大方块 ${Board.valueOf(state.game.board.highestRank())}",
                    color = Ink.copy(alpha = 0.7f), fontSize = 14.sp,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onNewGame,
                    colors = ButtonDefaults.buttonColors(containerColor = Coral),
                    shape = RoundedCornerShape(7.dp),
                ) { Text("再来一局") }
                TextButton(onClick = onReview) { Text("查看棋盘", color = Ink) }
            }
        }
    }
}

private fun tileColor(rank: Int): Color = when (rank) {
    0 -> EmptyColor
    1 -> Color(0xFFEEE4DA)
    2 -> Color(0xFFEDE0C8)
    3 -> Color(0xFFF2B179)
    4 -> Color(0xFFF59563)
    5 -> Color(0xFFF67C5F)
    6 -> Color(0xFFF65E3B)
    7 -> Color(0xFFEDCF72)
    8 -> Color(0xFFEDCC61)
    9 -> Color(0xFFEDC850)
    10 -> Color(0xFFEDC53F)
    11 -> Color(0xFFEDC22E)
    else -> Color(0xFF3C3A32)
}

private fun darken(color: Color): Color = Color(color.red * 0.82f, color.green * 0.82f,
    color.blue * 0.82f)

private fun lighten(color: Color): Color = Color(
    color.red + (1f - color.red) * 0.10f,
    color.green + (1f - color.green) * 0.10f,
    color.blue + (1f - color.blue) * 0.10f,
)

private fun directionForDrag(offset: Offset): Direction? {
    if (abs(offset.x) < 40f && abs(offset.y) < 40f) return null
    return if (abs(offset.x) > abs(offset.y)) {
        if (offset.x < 0) Direction.LEFT else Direction.RIGHT
    } else if (offset.y < 0) Direction.UP else Direction.DOWN
}

private fun formatNumber(value: Long): String = "%,d".format(value)

private fun formatCompactScore(value: Long): String = when {
    value >= 1_000_000_000_000 -> "%.1fT".format(value / 1_000_000_000_000.0)
    value >= 1_000_000_000 -> "%.1fB".format(value / 1_000_000_000.0)
    value >= 1_000_000 -> "%.1fM".format(value / 1_000_000.0)
    value >= 100_000 -> "%.0fK".format(value / 1_000.0)
    else -> formatNumber(value)
}
