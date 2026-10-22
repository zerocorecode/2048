package com.zerocode.game2048.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameEngineTest {
    @Test fun fullSlideAndSingleMergePerTile() {
        val board = Board.fromRanks(
            1, 1, 1, 1,
            0, 0, 2, 2,
            1, 0, 1, 0,
            0, 0, 0, 0,
        )
        val result = board.move(Direction.LEFT)
        assertTrue(result.moved)
        assertEquals(listOf(2, 2, 0, 0), (0..3).map(result.board::get))
        assertEquals(listOf(3, 0, 0, 0), (4..7).map(result.board::get))
        assertEquals(20L, result.scoreGain)
    }

    @Test fun playsPast2048() {
        val board = Board.fromRanks(11, 11, 0, 0, *IntArray(12))
        val result = board.move(Direction.LEFT)
        assertEquals(12, result.board[0])
        assertEquals(4096L, result.scoreGain)
    }

    @Test fun invalidMoveDoesNotSpawn() {
        val state = GameState(Board.fromRanks(1, 0, 0, 0, *IntArray(12)))
        assertNull(GameEngine.move(state, Direction.LEFT, Random(1)))
    }

    @Test fun newGameHasTwoTilesAndMovesAreAvailable() {
        val state = GameEngine.newGame(Random(42))
        assertEquals(2, state.board.toIntArray().count { it != 0 })
        assertFalse(state.isGameOver)
        assertTrue(state.nextRank in 1..2)
    }

    @Test fun everySpawnMatchesThePreview() {
        val random = Random(42)
        var state = GameEngine.newGame(random)
        val seen = mutableSetOf<Int>()
        repeat(100) {
            if (state.isGameOver) state = GameEngine.newGame(random)
            val transition = assertNotNull(GameEngine.move(state, state.board.legalDirections().first(), random))
            assertEquals(state.nextRank, transition.spawnedRank)
            assertEquals(state.nextRank, transition.after.board[transition.spawnedIndex])
            assertTrue(transition.after.nextRank in 1..2)
            seen += state.nextRank
            state = transition.after
        }
        assertEquals(setOf(1, 2), seen)
    }

    @Test fun invalidMoveDoesNotConsumeThePreviewOrRandomness() {
        val state = GameState(Board.fromRanks(1, 0, 0, 0, *IntArray(12)), nextRank = 2)
        val random = Random(17)
        assertNull(GameEngine.move(state, Direction.LEFT, random))
        assertEquals(
            GameEngine.move(state, Direction.RIGHT, Random(17)),
            GameEngine.move(state, Direction.RIGHT, random),
        )
    }

    @Test fun restoredStateKeepsItsPreview() {
        val state = GameState(Board.fromRanks(1, 0, 0, 0, *IntArray(12)), nextRank = 2)
        val transition = assertNotNull(GameEngine.move(state, Direction.RIGHT, Random(3)))
        val replay = assertNotNull(GameEngine.move(transition.before, Direction.DOWN, Random(4)))
        assertEquals(2, replay.spawnedRank)
    }

    @Test fun previewDrawKeepsTheClassicTwoAndFourProbabilities() {
        val ranks = (0 until 10).map { bucket ->
            val random = object : Random() {
                override fun nextBits(bitCount: Int): Int = 0
                override fun nextInt(until: Int): Int = if (until == 10) bucket else 0
            }
            GameEngine.newGame(random).nextRank
        }
        assertEquals(9, ranks.count { it == 1 })
        assertEquals(1, ranks.count { it == 2 })
    }
}
