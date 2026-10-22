package com.zerocode.game2048.core

import kotlin.random.Random

data class GameState(
    val board: Board,
    val score: Long = 0,
    val moveCount: Int = 0,
    val nextRank: Int = 1,
) {
    init { require(nextRank in 1..2) }
    val isGameOver: Boolean get() = board.legalDirections().isEmpty()
}

data class GameTransition(
    val before: GameState,
    val after: GameState,
    val direction: Direction,
    val spawnedIndex: Int,
    val spawnedRank: Int,
    val scoreGain: Long,
    val motions: List<TileMotion>,
)

object GameEngine {
    fun newGame(random: Random = Random.Default): GameState {
        var board = Board.empty()
        repeat(2) { board = spawn(board, random).first }
        return GameState(board, nextRank = drawRank(random))
    }

    fun move(state: GameState, direction: Direction, random: Random = Random.Default): GameTransition? {
        val slide = state.board.move(direction)
        if (!slide.moved) return null
        val (board, index, rank) = spawn(slide.board, random, state.nextRank)
        return GameTransition(
            state,
            state.copy(
                board = board,
                score = Board.saturatingAdd(state.score, slide.scoreGain),
                moveCount = state.moveCount + 1,
                nextRank = drawRank(random),
            ),
            direction, index, rank, slide.scoreGain, slide.motions,
        )
    }

    private fun drawRank(random: Random): Int = if (random.nextInt(10) == 0) 2 else 1

    private fun spawn(board: Board, random: Random, rank: Int = drawRank(random)): Triple<Board, Int, Int> {
        val empty = board.emptyIndices()
        val index = empty[random.nextInt(empty.size)]
        return Triple(board.withTile(index, rank), index, rank)
    }
}
