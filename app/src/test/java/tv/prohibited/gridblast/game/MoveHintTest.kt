package tv.prohibited.gridblast.game

import org.junit.Assert.*
import org.junit.Test

class MoveHintTest {
    private val single = GameEngine.pieceFromId(0)!!
    private val square = GameEngine.pieceFromId(11)!!

    @Test fun freshGameReturnsLegalHintWithoutMutatingSession() {
        val state = GameState(tray = listOf(single, square, null))
        val before = state.copy(board = state.board.toList(), tray = state.tray.toList())
        val hint = GameEngine.suggestMove(state)!!
        val piece = state.tray[hint.slot]!!
        assertTrue(GameEngine.canPlace(state.board, piece, hint.row, hint.col))
        assertEquals(9, hint.points)
        assertEquals(1, hint.slot)
        assertEquals(state, before)
    }

    @Test fun choosesFullRowInsteadOfLargerNonClearingPiece() {
        val board = MutableList(64) { 0 }
        for (col in 0 until 7) board[col] = 3
        val state = GameState(board = board, tray = listOf(single, square, null))
        val hint = GameEngine.suggestMove(state)!!
        assertEquals(0, hint.slot)
        assertEquals(0, hint.row)
        assertEquals(7, hint.col)
        assertEquals(1, hint.linesCleared)
        assertEquals(81, hint.points)
    }

    @Test fun crossedRowAndColumnHasMaximumImmediatePoints() {
        val board = MutableList(64) { 0 }
        for (i in 0 until 7) {
            board[7 * 8 + i] = 1
            board[i * 8 + 7] = 1
        }
        val state = GameState(board = board, tray = listOf(single, square, null))
        val hint = GameEngine.suggestMove(state)!!
        assertEquals(0, hint.slot)
        assertEquals(7, hint.row)
        assertEquals(7, hint.col)
        assertEquals(2, hint.linesCleared)
        assertEquals(151, hint.points)
    }

    @Test fun givesNoHintIfThereAreNoMoves() {
        val blocked = GameState(
            board = List(64) { 5 },
            tray = listOf(single, null, null),
        )
        assertNull(GameEngine.suggestMove(blocked))
        assertNull(GameEngine.suggestMove(blocked.copy(gameOver = true)))
        assertNull(GameEngine.suggestMove(GameState(tray = listOf(null, null, null))))
    }

    @Test fun tiesUseStableTopLeftLocation() {
        val state = GameState(tray = listOf(single, single, null))
        assertEquals(MoveHint(0, 0, 0, 1, 0), GameEngine.suggestMove(state))
        assertEquals(GameEngine.suggestMove(state), GameEngine.suggestMove(state))
    }

    @Test fun hintMatchesActualGameEnginePlacementAnalysis() {
        val board = MutableList(64) { 0 }
        for (i in 1..6) board[i] = 2
        val state = GameState(board = board, tray = listOf(single, square, null))
        val hint = GameEngine.suggestMove(state)!!
        val analysis = GameEngine.analyze(state, hint.slot, hint.row, hint.col)!!
        assertEquals(analysis.points, hint.points)
        assertEquals(analysis.linesCleared, hint.linesCleared)
    }
}
