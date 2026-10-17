package tv.prohibited.gridblast.game

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {
    private fun single() = Piece(0, 1, listOf(Cell(0, 0)))
    private fun square() = Piece(9, 2, listOf(Cell(0, 0), Cell(0, 1), Cell(1, 0), Cell(1, 1)))

    @Test fun newGameHasEmptyBoardAndThreeShapes() {
        val state = GameEngine.newGame(Random(123))
        assertEquals(64, state.board.size)
        assertTrue(state.board.all { it == 0 })
        assertEquals(3, state.tray.filterNotNull().size)
        assertEquals(0, state.score)
    }

    @Test fun invalidPlacementDoesNotConsumeShapeOrChangeScore() {
        val initial = GameState(tray = listOf(square(), null, null))
        assertEquals(initial, GameEngine.place(initial, 0, 7, 7, Random(1)))
        assertEquals(initial, GameEngine.place(initial, 0, -1, 0, Random(1)))
        assertEquals(initial, GameEngine.place(initial, 5, 0, 0, Random(1)))
    }

    @Test fun validPlacementConsumesOneShape() {
        val initial = GameState(tray = listOf(single(), square(), null))
        val updated = GameEngine.place(initial, 0, 3, 4, Random(1))
        assertEquals(1, updated.board[3 * 8 + 4])
        assertEquals(1, updated.score)
        assertNull(updated.tray[0])
        assertNotNull(updated.tray[1])
    }

    @Test fun completingCrossingRowAndColumnClearsIntersectionOnlyOnce() {
        val board = MutableList(64) { 0 }
        for (i in 0..6) {
            board[7 * 8 + i] = 1
            board[i * 8 + 7] = 1
        }
        val initial = GameState(board = board, tray = listOf(single(), null, null))
        val result = GameEngine.place(initial, 0, 7, 7, Random(3))
        assertTrue(result.board.all { it == 0 })
        assertEquals(151, result.score) // 1 placed + 15 unique squares cleared * 10
    }

    @Test fun trayRefillsAfterThirdPlacement() {
        val initial = GameState(tray = listOf(single(), single(), single()))
        val a = GameEngine.place(initial, 0, 0, 0, Random(3))
        val b = GameEngine.place(a, 1, 0, 1, Random(3))
        val c = GameEngine.place(b, 2, 0, 2, Random(3))
        assertEquals(3, c.tray.filterNotNull().size)
        assertEquals(3, c.score)
    }

    @Test fun detectsNoRemainingMovesWithoutClearingLines() {
        val board = List(64) { index ->
            val row = index / 8
            val col = index % 8
            if ((row + col) % 2 == 0) 0 else 3
        }
        val initial = GameState(board = board, tray = listOf(single(), square(), null))
        val result = GameEngine.place(initial, 0, 0, 0, Random(2))
        assertTrue(result.gameOver)
        assertFalse(GameEngine.hasAnyMove(result))
    }

    @Test fun occupiedCellsRejectPlacement() {
        val board = MutableList(64) { 0 }
        board[1 * 8 + 1] = 1
        assertFalse(GameEngine.canPlace(board, square(), 0, 0))
        assertTrue(GameEngine.canPlace(board, square(), 2, 2))
    }
}
