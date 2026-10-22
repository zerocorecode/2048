package com.zerocode.game2048.core

class Board private constructor(private val tiles: IntArray) {
    init {
        require(tiles.size == CELL_COUNT)
        require(tiles.all { it in 0..MAX_RANK })
    }

    operator fun get(index: Int): Int = tiles[index]
    fun toIntArray(): IntArray = tiles.copyOf()
    fun emptyIndices(): List<Int> = tiles.indices.filter { tiles[it] == 0 }
    fun highestRank(): Int = tiles.max()
    fun legalDirections(): List<Direction> = Direction.entries.filter { move(it).moved }

    fun withTile(index: Int, rank: Int): Board {
        require(index in tiles.indices && tiles[index] == 0 && rank in 1..MAX_RANK)
        return Board(tiles.copyOf().also { it[index] = rank })
    }

    fun move(direction: Direction): BoardMove {
        val result = IntArray(CELL_COUNT)
        var scoreGain = 0L
        val motions = mutableListOf<TileMotion>()
        var moved = false
        for (line in 0 until SIZE) {
            val occupied = (0 until SIZE).mapNotNull { offset ->
                val index = cell(direction, line, offset)
                tiles[index].takeIf { it != 0 }?.let { rank -> index to rank }
            }
            var output = 0
            var cursor = 0
            while (cursor < occupied.size) {
                val (source, rank) = occupied[cursor]
                val next = occupied.getOrNull(cursor + 1)
                val merge = next != null && next.second == rank && rank < MAX_RANK
                val target = cell(direction, line, output++)
                result[target] = if (merge) rank + 1 else rank
                motions += TileMotion(source, target, merge)
                if (source != target) moved = true
                if (merge) {
                    motions += TileMotion(next.first, target, true)
                    scoreGain = saturatingAdd(scoreGain, valueOf(rank + 1))
                    moved = true
                    cursor += 2
                } else cursor++
            }
        }
        return BoardMove(if (moved) Board(result) else this, moved, scoreGain, motions)
    }

    override fun equals(other: Any?): Boolean = other is Board && tiles.contentEquals(other.tiles)
    override fun hashCode(): Int = tiles.contentHashCode()

    companion object {
        const val SIZE = 4
        const val CELL_COUNT = 16
        const val MAX_RANK = 62

        fun empty(): Board = Board(IntArray(CELL_COUNT))
        fun fromRanks(vararg ranks: Int): Board = Board(ranks.copyOf())
        fun valueOf(rank: Int): Long = if (rank == 0) 0 else 1L shl rank
        fun saturatingAdd(left: Long, right: Long): Long =
            if (Long.MAX_VALUE - left < right) Long.MAX_VALUE else left + right

        private fun cell(direction: Direction, line: Int, offset: Int): Int = when (direction) {
            Direction.LEFT -> line * SIZE + offset
            Direction.RIGHT -> line * SIZE + SIZE - 1 - offset
            Direction.UP -> offset * SIZE + line
            Direction.DOWN -> (SIZE - 1 - offset) * SIZE + line
        }
    }
}

data class TileMotion(val fromIndex: Int, val toIndex: Int, val merges: Boolean)
data class BoardMove(
    val board: Board,
    val moved: Boolean,
    val scoreGain: Long,
    val motions: List<TileMotion>,
)
