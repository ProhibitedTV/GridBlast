package tv.prohibited.gridblast.game

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class PlacementAnalysisTest {
    private val single = GameEngine.pieceFromId(0)!!

    @Test fun invalidMovesProduceNoAnalysis() {
        val state = GameState(tray = listOf(single, null, null))
        assertNull(GameEngine.analyze(state, 0, -1, 0))
        assertNull(GameEngine.analyze(state, 2, 0, 0))
        assertNull(GameEngine.analyze(state, 0, 8, 0))
        val over = state.copy(gameOver = true)
        assertNull(GameEngine.analyze(over, 0, 0, 0))
    }

    @Test fun analysisDoesNotMutateInputBoard() {
        val state = GameState(tray = listOf(single, null, null))
        val snapshot = state.board.toList()
        val analysis = GameEngine.analyze(state, 0, 4, 5)!!
        assertEquals(setOf(4 * 8 + 5), analysis.placedIndices)
        assertTrue(analysis.clearedIndices.isEmpty())
        assertEquals(1, analysis.points)
        assertEquals(snapshot, state.board)
    }

    @Test fun clearingTwoCrossingLinesHasUniqueCellCount() {
        val board = MutableList(64) { 0 }
        for (i in 0..6) {
            board[7 * 8 + i] = 2
            board[i * 8 + 7] = 4
        }
        val state = GameState(board = board, tray = listOf(single, null, null))
        val analysis = GameEngine.analyze(state, 0, 7, 7)!!
        assertEquals(listOf(7), analysis.clearedRows)
        assertEquals(listOf(7), analysis.clearedColumns)
        assertEquals(15, analysis.clearedIndices.size)
        assertEquals(151, analysis.points)
        val next = GameEngine.place(state, 0, 7, 7, Random(2))
        assertTrue(next.board.all { it == 0 })
        assertEquals(analysis.points, next.score)
    }

    @Test fun previewAndActualPlacementStayInSync() {
        val original = GameEngine.newGame(Random(13))
        val piece = original.tray[0]!!
        val analysis = GameEngine.analyze(original, 0, 0, 0)!!
        val moved = GameEngine.place(original, 0, 0, 0, Random(15))
        assertEquals(piece.cells.size, analysis.placedIndices.size)
        assertEquals(analysis.points, moved.score)
        analysis.placedIndices.subtract(analysis.clearedIndices).forEach { index ->
            assertEquals(piece.colorId, moved.board[index])
        }
    }
}
