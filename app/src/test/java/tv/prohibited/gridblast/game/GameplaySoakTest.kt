package tv.prohibited.gridblast.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Deterministic, offline soak tests across varied complete game sessions.
 *
 * This complements (but does not replace) physical playtesting: it covers
 * thousands of successful and unsuccessful game transitions rather than
 * isolated fixtures. The seed and round/turn indices make failures repeatable.
 */
class GameplaySoakTest {
    private data class Move(val slot: Int, val row: Int, val col: Int)

    private fun legalMoves(state: GameState): List<Move> = buildList {
        if (state.gameOver) return@buildList
        state.tray.forEachIndexed { slot, piece ->
            if (piece != null) {
                for (row in 0 until GameEngine.SIZE) {
                    for (col in 0 until GameEngine.SIZE) {
                        if (GameEngine.canPlace(state.board, piece, row, col)) {
                            add(Move(slot, row, col))
                        }
                    }
                }
            }
        }
    }

    @Test fun seededLongSessionsPreserveScoringRefillsUndoAndSaves() {
        var playedMoves = 0
        var refills = 0
        var completedRounds = 0
        // Mix random legal placements with occasional survival-aware hints.
        // Bounded turns prevent a regression from making CI hang forever.
        repeat(96) { round ->
            val random = Random(20261103 + round * 101)
            var history = GameHistory.newGame(random)
            var turn = 0
            while (!history.current.gameOver && turn < 120) {
                val before = history.current
                val legal = legalMoves(before)
                val context = "round=$round turn=$turn"
                assertTrue("$context: playable round must have moves", legal.isNotEmpty())
                assertFalse("$context: gameOver must match legal moves", before.gameOver)

                val chosen = if (turn % 7 == 0) {
                    val stateBeforeHint = history.current
                    val hint = GameEngine.suggestMove(stateBeforeHint)
                    assertNotNull("$context: playable round must have a hint", hint)
                    assertEquals("$context: hint must not mutate game", stateBeforeHint, history.current)
                    Move(hint!!.slot, hint.row, hint.col)
                } else {
                    legal[random.nextInt(legal.size)]
                }
                assertTrue("$context: chosen placement must be legal", chosen in legal)
                val analysis = GameEngine.analyze(
                    before, chosen.slot, chosen.row, chosen.col,
                )!!
                val placedPiece = before.tray[chosen.slot]!!
                val next = history.place(chosen.slot, chosen.row, chosen.col, random)
                assertEquals("$context: previous checkpoint", before, next.previous)
                assertEquals(
                    "$context: awarded points",
                    GameEngine.cappedScore(before.score, analysis.points),
                    next.current.score,
                )

                val expectedBoard = before.board.toMutableList()
                analysis.placedIndices.forEach { expectedBoard[it] = placedPiece.colorId }
                analysis.clearedIndices.forEach { expectedBoard[it] = 0 }
                assertEquals("$context: placed and cleared cells", expectedBoard, next.current.board)

                assertEquals(
                    "$context: accurate game-over detection",
                    !GameEngine.hasAnyMove(next.current),
                    next.current.gameOver,
                )
                assertTrue("$context: tray must have shapes", next.current.tray.any { it != null })
                assertTrue(
                    "$context: board colors stay canonical",
                    next.current.board.all { it in 0..6 },
                )
                val fullRefill = before.tray.count { it != null } == 1
                if (fullRefill) {
                    refills++
                    assertEquals(
                        "$context: last piece triggers three-piece refill",
                        GameEngine.TRAY_SIZE,
                        next.current.tray.count { it != null },
                    )
                    if (next.current.board.any { it == 0 }) {
                        assertFalse(
                            "$context: refill must not cause random no-move loss",
                            next.current.gameOver,
                        )
                    }
                }

                // Bad coordinates/slots cannot consume a turn or erase Undo.
                assertSame("$context: reject invalid slot", next, next.place(-1, 0, 0, random))
                assertSame("$context: reject off-board", next, next.place(0, -1, -1, random))

                // Encode/decode simulates process death after a completed turn.
                // Every restored checkpoint must undo to the exact prior state,
                // including game over and random-tray refill boundaries.
                val reloaded = GameHistoryCodec.decode(GameHistoryCodec.encode(next))
                assertEquals("$context: saved game round-trip", next, reloaded)
                val undo = reloaded!!.undoLast()
                assertEquals("$context: one-turn Undo", before, undo.current)
                assertNull("$context: cannot undo twice", undo.previous)
                assertEquals(
                    "$context: standalone session save",
                    next.current,
                    GameStateCodec.decode(GameStateCodec.encode(next.current)),
                )

                history = reloaded
                turn++
                playedMoves++
            }
            if (history.current.gameOver) completedRounds++
        }
        assertTrue("Soak must exercise many placements: $playedMoves", playedMoves >= 250)
        assertTrue("Soak must cross repeated tray refills: $refills", refills >= 50)
        assertTrue("Soak must reach some real game overs: $completedRounds", completedRounds >= 1)
    }
}
