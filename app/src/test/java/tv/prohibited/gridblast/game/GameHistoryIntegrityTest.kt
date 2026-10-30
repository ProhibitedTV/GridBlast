package tv.prohibited.gridblast.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameHistoryIntegrityTest {
    private val single = GameEngine.pieceFromId(0)!!
    private val square = GameEngine.pieceFromId(9)!!

    private fun initial() = GameState(tray = listOf(single, square, null))

    private fun forgedHistory(current: GameState, previous: GameState): String =
        "history-1~" + GameStateCodec.encode(current) + "~" + GameStateCodec.encode(previous)

    @Test fun unrelatedPreviousBoardCannotBeUndone() {
        val moved = GameHistory(initial()).place(0, 0, 0, Random(1))
        val unrelated = initial().copy(
            board = MutableList(64) { 0 }.apply { this[63] = 3 },
        )
        val restored = GameHistoryCodec.decode(forgedHistory(moved.current, unrelated))
        assertNotNull(restored)
        assertEquals(moved.current, restored!!.current)
        assertNull(restored.previous)
    }

    @Test fun unrelatedTrayCannotBeUndoneEvenWithMatchingScoreAndBoard() {
        val moved = GameHistory(initial()).place(0, 0, 0, Random(1))
        val unrelated = initial().copy(tray = listOf(single, single, null))
        val restored = GameHistoryCodec.decode(forgedHistory(moved.current, unrelated))
        assertNotNull(restored)
        assertEquals(moved.current, restored!!.current)
        assertNull(restored.previous)
    }

    @Test fun impossibleScoreDeltaCannotBeUndone() {
        val moved = GameHistory(initial()).place(0, 0, 0, Random(1))
        val forgedCurrent = moved.current.copy(score = moved.current.score + 1)
        val restored = GameHistoryCodec.decode(forgedHistory(forgedCurrent, initial()))
        assertNotNull(restored)
        assertEquals(forgedCurrent, restored!!.current)
        assertNull(restored.previous)
    }

    @Test fun rowColumnClearAndFullRefillStillRestoreUndo() {
        val board = MutableList(64) { 0 }
        for (i in 0..6) {
            board[7 * 8 + i] = 1
            board[i * 8 + 7] = 1
        }
        val previous = GameState(board = board, tray = listOf(null, null, single))
        val moved = GameHistory(previous).place(2, 7, 7, Random(3))
        assertEquals(151, moved.current.score)
        assertTrue(moved.current.board.all { it == 0 })
        assertEquals(3, moved.current.tray.count { it != null })

        val restored = GameHistoryCodec.decode(GameHistoryCodec.encode(moved))
        assertEquals(moved, restored)
        assertEquals(previous, restored!!.undoLast().current)
    }

    @Test fun seededGameplayCheckpointsRoundTripIncludingGameOver() {
        var history = GameHistory.newGame(Random(2026))
        repeat(100) { turn ->
            val hint = GameEngine.suggestMove(history.current)
            if (hint == null) {
                history = GameHistory.newGame(Random(turn + 1000))
            } else {
                history = history.place(hint.slot, hint.row, hint.col, Random(turn + 10))
                val decoded = GameHistoryCodec.decode(GameHistoryCodec.encode(history))
                assertEquals("Undo mismatch at turn $turn", history, decoded)
                assertFalse(decoded!!.previous?.gameOver ?: false)
                assertEquals(history.previous, decoded.previous)
            }
        }
    }
}
