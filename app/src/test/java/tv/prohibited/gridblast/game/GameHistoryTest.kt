package tv.prohibited.gridblast.game

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameHistoryTest {
    private val one = GameEngine.pieceFromId(0)!!
    private val square = GameEngine.pieceFromId(9)!!

    private fun start(): GameHistory =
        GameHistory(GameState(tray = listOf(one, square, null)))

    @Test fun legalMoveCanBeUndoneExactlyOnce() {
        val fresh = start()
        val moved = fresh.place(0, 0, 0, Random(1))
        assertEquals(1, moved.current.score)
        assertNull(moved.current.tray[0])
        assertEquals(fresh.current, moved.previous)
        val undone = moved.undoLast()
        assertEquals(fresh.current, undone.current)
        assertNull(undone.previous)
        assertSame(undone, undone.undoLast())
    }

    @Test fun invalidMoveDoesNotOverwriteValidUndo() {
        val moved = start().place(0, 0, 0)
        assertSame(moved, moved.place(0, 0, 0))
        assertSame(moved, moved.place(2, 0, 0))
        assertEquals(start().current, moved.undoLast().current)
    }

    @Test fun eachNewPlacementReplacesOneStepHistory() {
        val first = start().place(0, 0, 0)
        val second = first.place(1, 2, 2)
        assertTrue(second.current.score > first.current.score)
        assertEquals(first.current, second.undoLast().current)
        assertEquals(first.current, second.previous)
        val replay = second.undoLast().place(1, 4, 4)
        assertEquals(first.current, replay.undoLast().current)
    }

    @Test fun undoRemainsAvailableAfterGameOver() {
        val board = List(64) { index ->
            if ((index / 8 + index % 8) % 2 == 0) 0 else 3
        }
        val start = GameHistory(
            GameState(board = board, tray = listOf(one, square, null)),
        )
        val lost = start.place(0, 0, 0, Random(12))
        assertTrue(lost.current.gameOver)
        val undone = lost.undoLast()
        assertFalse(undone.current.gameOver)
        assertEquals(start.current, undone.current)
    }

    @Test fun historyRoundTripPreservesUndoAndBoard() {
        val history = start().place(0, 1, 1)
        assertEquals(history, GameHistoryCodec.decode(GameHistoryCodec.encode(history)))
        assertEquals(
            start().current,
            GameHistoryCodec.decode(GameHistoryCodec.encode(history))!!.undoLast().current,
        )
    }

    @Test fun newGameAndUndoResultHaveNoUndoSnapshot() {
        assertNull(GameHistory.newGame(Random(42)).previous)
        val undone = start().place(0, 0, 0).undoLast()
        val reloaded = GameHistoryCodec.decode(GameHistoryCodec.encode(undone))!!
        assertNull(reloaded.previous)
        assertEquals(undone.current, reloaded.current)
    }

    @Test fun rejectsUnknownVersionAndBrokenCurrentSession() {
        val encoded = GameHistoryCodec.encode(start())
        assertNull(GameHistoryCodec.decode("history-2" + encoded.drop("history-1".length)))
        assertNull(GameHistoryCodec.decode("garbage"))
        assertNull(GameHistoryCodec.decode("x".repeat(2300)))
        assertNull(GameHistoryCodec.decode("history-1~bad~-"))
    }

    @Test fun damagedUndoDoesNotDestroyValidCurrentSession() {
        val moved = start().place(0, 0, 0)
        val raw = GameHistoryCodec.encode(moved)
        val fields = raw.split("~")
        val corrupted = "${fields[0]}~${fields[1]}~not-valid"
        val result = GameHistoryCodec.decode(corrupted)!!
        assertEquals(moved.current, result.current)
        assertNull(result.previous)
    }

    @Test fun malformedUndoWithGreaterScoreIsDropped() {
        val moved = start().place(0, 0, 0)
        val impossiblyHigh = start().current.copy(score = 500)
        val currentEncoded = GameStateCodec.encode(moved.current)
        val badUndoEncoded = GameStateCodec.encode(impossiblyHigh)
        val restored = GameHistoryCodec.decode(
            "history-1~$currentEncoded~$badUndoEncoded",
        )!!
        assertEquals(moved.current, restored.current)
        assertNull(restored.previous)
    }
}
