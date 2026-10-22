package com.zerocode.game2048.ai

import com.zerocode.game2048.core.Board
import com.zerocode.game2048.core.Direction
import com.zerocode.game2048.core.GameState
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class SearchLimits(
    val maxMillis: Long = 200,
    val maxDepth: Int = 8,
    val isCancelled: () -> Boolean = { false },
)

data class MoveEvaluation(val direction: Direction, val evaluation: Double?)
data class AiDecision(
    val direction: Direction,
    val alternatives: List<MoveEvaluation>,
    val completedDepth: Int,
    val searchedNodes: Long,
    val elapsedMillis: Long,
)

class ExpectimaxAi {
    private class SearchStopped : RuntimeException() {
        override fun fillInStackTrace(): Throwable = this
    }

    private data class CacheKey(val board: Board, val depth: Int, val chance: Boolean)
    private val cache = HashMap<CacheKey, Double>(100_000)
    private var nodes = 0L
    private var deadline = 0L
    private var cancelled: () -> Boolean = { false }

    fun chooseMove(game: GameState, limits: SearchLimits = SearchLimits()): AiDecision? {
        val start = System.nanoTime()
        val legal = Direction.entries.mapNotNull { direction ->
            game.board.move(direction).takeIf { it.moved }?.let { direction to it.board }
        }
        if (legal.isEmpty()) return null
        nodes = 0
        cache.clear()
        deadline = start + limits.maxMillis.coerceAtLeast(1) * 1_000_000
        cancelled = limits.isCancelled
        var best = legal.first().first
        var alternatives = Direction.entries.map { direction ->
            MoveEvaluation(direction, legal.firstOrNull { it.first == direction }?.second?.let(::evaluate))
        }
        var completedDepth = 0
        for (depth in 1..limits.maxDepth.coerceAtLeast(1)) {
            try {
                val scores = legal.associate { (direction, board) ->
                    checkDeadline()
                    direction to previewNode(board, game.nextRank, depth - 1)
                }
                best = scores.maxBy { it.value }.key
                alternatives = Direction.entries.map { MoveEvaluation(it, scores[it]) }
                completedDepth = depth
            } catch (_: SearchStopped) {
                break
            }
            if (System.nanoTime() >= deadline) break
        }
        return AiDecision(
            best, alternatives, completedDepth, nodes,
            (System.nanoTime() - start) / 1_000_000,
        )
    }

    // The next tile's value is known; only its empty-cell position is random.
    // Later turns retain the normal 90% / 10% chance branches.
    private fun previewNode(board: Board, rank: Int, depth: Int): Double {
        tick()
        val empty = board.emptyIndices()
        if (empty.isEmpty()) return evaluate(board)
        return empty.sumOf { index -> playerNode(board.withTile(index, rank), depth) } / empty.size
    }

    private fun playerNode(board: Board, depth: Int): Double {
        tick()
        if (depth <= 0) return evaluate(board)
        val key = CacheKey(board, depth, false)
        cache[key]?.let { return it }
        var best = Double.NEGATIVE_INFINITY
        for (direction in Direction.entries) {
            val move = board.move(direction)
            if (move.moved) best = max(best, chanceNode(move.board, depth - 1))
        }
        if (!best.isFinite()) best = evaluate(board) - 100_000.0
        if (cache.size < 200_000) cache[key] = best
        return best
    }

    private fun chanceNode(board: Board, depth: Int): Double {
        tick()
        val empty = board.emptyIndices()
        if (depth <= 0 || empty.isEmpty()) return evaluate(board)
        val key = CacheKey(board, depth, true)
        cache[key]?.let { return it }
        var total = 0.0
        for (index in empty) {
            total += 0.9 * playerNode(board.withTile(index, 1), depth)
            total += 0.1 * playerNode(board.withTile(index, 2), depth)
        }
        val result = total / empty.size
        if (cache.size < 200_000) cache[key] = result
        return result
    }

    private fun tick() {
        nodes++
        if (nodes and 127L == 0L) checkDeadline()
    }

    private fun checkDeadline() {
        if (cancelled() || System.nanoTime() >= deadline) throw SearchStopped()
    }

    private fun evaluate(board: Board): Double {
        val cells = board.toIntArray()
        var result = 0.0
        for (row in 0..3) {
            result += evaluateLine(IntArray(4) { cells[row * 4 + it] })
            result += evaluateLine(IntArray(4) { cells[it * 4 + row] })
        }
        return result
    }

    // 2048-specific row heuristic: empty space, merge runs and monotone large tiles.
    private fun evaluateLine(line: IntArray): Double {
        var empty = 0
        var merges = 0
        var sum = 0.0
        var previous = 0
        var run = 0
        var increasing = 0.0
        var decreasing = 0.0
        for (index in 0..3) {
            val rank = line[index]
            sum += rank.toDouble().pow(3.5)
            if (rank == 0) empty++
            else {
                if (rank == previous) run++
                else if (run > 0) {
                    merges += 1 + run
                    run = 0
                }
                previous = rank
            }
            if (index > 0) {
                val last = line[index - 1].toDouble().pow(4.0)
                val current = rank.toDouble().pow(4.0)
                if (last > current) decreasing += last - current
                else increasing += current - last
            }
        }
        if (run > 0) merges += 1 + run
        return 200_000.0 + 270.0 * empty + 700.0 * merges -
            47.0 * min(increasing, decreasing) - 11.0 * sum
    }
}
