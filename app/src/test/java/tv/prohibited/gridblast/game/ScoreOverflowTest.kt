package tv.prohibited.gridblast.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class ScoreOverflowTest {
    private val one = GameEngine.pieceFromId(0)!!
    private val square = GameEngine.pieceFromId(9)!!

    @Test fun scoringNearMaxValueSaturatesWithoutCorruptingSave() {
        val initial = GameHistory(
            GameState(score = Int.MAX_VALUE - 1, tray = listOf(one, square, null)),
        )
        val moved = initial.place(0, 0, 0, Random(11))
        assertEquals(Int.MAX_VALUE, moved.current.score)
        assertEquals(1, moved.current.board[0])
        assertEquals(initial.current, moved.previous)
        assertEquals(moved, GameHistoryCodec.decode(GameHistoryCodec.encode(moved)))
    }

    @Test fun movesAtMaxScoreRemainPlayableAndUndoable() {
        val initial = GameHistory(
            GameState(score = Int.MAX_VALUE, tray = listOf(one, square, null)),
        )
        val moved = initial.place(0, 0, 0, Random(12))
        assertEquals(Int.MAX_VALUE, moved.current.score)
        assertEquals(1, moved.current.board[0])
        assertNull(moved.current.tray[0])
        assertNotNull(moved.current.tray[1])

        val restored = GameHistoryCodec.decode(GameHistoryCodec.encode(moved))
        assertEquals(moved, restored)
        assertEquals(initial.current, restored!!.undoLast().current)
    }

    @Test fun maxScoreStillSupportsThirdPieceRefills() {
        val initial = GameHistory(
            GameState(score = Int.MAX_VALUE, tray = listOf(null, null, one)),
        )
        val moved = initial.place(2, 0, 0, Random(13))
        assertEquals(Int.MAX_VALUE, moved.current.score)
        assertEquals(3, moved.current.tray.count { it != null })
        assertEquals(moved, GameHistoryCodec.decode(GameHistoryCodec.encode(moved)))
    }

    @Test fun equalNonMaxScoreNeverMakesForgedUndoValid() {
        val previous = GameState(score = 300, tray = listOf(one, square, null))
        val successor = GameEngine.place(previous, 0, 0, 0, Random(14))
        val forgedCurrent = successor.copy(score = 300)
        val raw = "history-1~" + GameStateCodec.encode(forgedCurrent) +
            "~" + GameStateCodec.encode(previous)
        val restored = GameHistoryCodec.decode(raw)
        assertNotNull(restored)
        assertEquals(forgedCurrent, restored!!.current)
        assertNull(restored.previous)
    }
}
