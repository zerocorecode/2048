package com.zerocode.game2048.ai

import com.zerocode.game2048.core.Board
import com.zerocode.game2048.core.GameState
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExpectimaxAiTest {
    @Test fun choosesLegalDirectionAbove2048() {
        val state = GameState(Board.fromRanks(11, 11, 0, 0, *IntArray(12)))
        val decision = assertNotNull(ExpectimaxAi().chooseMove(state, SearchLimits(maxMillis = 50)))
        assertTrue(state.board.move(decision.direction).moved)
        assertTrue(decision.alternatives.any { it.evaluation != null })
    }

    @Test fun cancellationReturnsFallbackDirection() {
        val state = GameState(Board.fromRanks(1, 1, 0, 0, *IntArray(12)))
        val decision = assertNotNull(ExpectimaxAi().chooseMove(
            state, SearchLimits(isCancelled = { true })
        ))
        assertTrue(state.board.move(decision.direction).moved)
    }

    @Test fun searchUsesTheKnownNextTile() {
        val state = GameState(Board.fromRanks(1, 1, 0, 0, *IntArray(12)))
        val limits = SearchLimits(maxMillis = 1_000, maxDepth = 1)
        val two = assertNotNull(ExpectimaxAi().chooseMove(state.copy(nextRank = 1), limits))
        val four = assertNotNull(ExpectimaxAi().chooseMove(state.copy(nextRank = 2), limits))
        assertTrue(two.completedDepth == 1 && four.completedDepth == 1)
        assertTrue(two.alternatives.zip(four.alternatives).any { (a, b) ->
            a.evaluation != null && b.evaluation != null && a.evaluation != b.evaluation
        })
    }
}
