package tv.prohibited.gridblast.game

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

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

    private fun board(vararg rows: String): List<Int> {
        require(rows.size == GameEngine.SIZE && rows.all { it.length == GameEngine.SIZE })
        return rows.flatMap { row -> row.map { if (it == '#') 3 else 0 } }
    }

    @Test fun prefersSurvivalEvenWhenItScoresFewerPoints() {
        // Placing the two-cell piece at row 1, column 5 scores 162 but
        // strands the vertical five-cell piece. Another location scores 152
        // and leaves a legal placement for the remaining piece.
        val state = GameState(
            board = board(
                "####..##",
                ".####..#",
                "#####.##",
                ".#.#####",
                "########",
                "..#.####",
                "#.#...##",
                "####....",
            ),
            tray = listOf(GameEngine.pieceFromId(8), GameEngine.pieceFromId(1), null),
        )
        val originalBoard = state.board.toList()
        val originalTray = state.tray.toList()
        assertTrue(GameEngine.place(state, 1, 0, 4, Random(5)).gameOver)

        val hint = GameEngine.suggestMove(state)!!
        assertEquals(MoveHint(1, 7, 6, 152, 2), hint)
        assertFalse(GameEngine.place(state, hint.slot, hint.row, hint.col, Random(5)).gameOver)
        assertEquals(originalBoard, state.board)
        assertEquals(originalTray, state.tray)
        assertEquals(0, state.score)
    }

    @Test fun survivalBreaksEqualScoreTies() {
        val state = GameState(
            board = board(
                "####.###",
                "##.#....",
                "#####.##",
                "#####.##",
                "####...#",
                ".#######",
                ".######.",
                ".###.#.#",
            ),
            tray = listOf(GameEngine.pieceFromId(1), GameEngine.pieceFromId(4), null),
        )
        assertTrue(GameEngine.place(state, 0, 1, 4, Random(5)).gameOver)
        val hint = GameEngine.suggestMove(state)!!
        assertEquals(MoveHint(0, 1, 6, 162, 2), hint)
        assertFalse(GameEngine.place(state, hint.slot, hint.row, hint.col, Random(5)).gameOver)
    }

    @Test fun forcedLossStillReturnsBestAvailableMove() {
        val occupied = MutableList(64) { index ->
            if ((index / 8 + index % 8) % 2 == 0) 3 else 0
        }
        for (row in 0..2) for (col in 0..2) occupied[row * 8 + col] = 0
        val state = GameState(
            board = occupied,
            tray = listOf(GameEngine.pieceFromId(11), GameEngine.pieceFromId(11), null),
        )
        val hint = GameEngine.suggestMove(state)!!
        assertEquals(MoveHint(0, 0, 0, 9, 0), hint)
        assertTrue(GameEngine.place(state, hint.slot, hint.row, hint.col, Random(5)).gameOver)
    }

    /**
     * A slow independent oracle: evaluate every legal move through a real
     * engine turn. The optimized hint must choose exactly the same move
     * (including survival, scoring and stable tie-breaking).
     */
    private fun referenceHint(state: GameState): MoveHint? {
        var best: MoveHint? = null
        var bestSurvives = false
        for (slot in state.tray.indices) {
            if (state.tray[slot] == null) continue
            for (row in 0 until GameEngine.SIZE) {
                for (col in 0 until GameEngine.SIZE) {
                    val analysis = GameEngine.analyze(state, slot, row, col) ?: continue
                    val survives = !GameEngine.place(state, slot, row, col, Random(11)).gameOver
                    val candidate = MoveHint(slot, row, col, analysis.points, analysis.linesCleared)
                    if (best == null || (survives && !bestSurvives) ||
                        (survives == bestSurvives &&
                            (candidate.points > best.points ||
                                (candidate.points == best.points &&
                                    candidate.linesCleared > best.linesCleared)))
                    ) {
                        best = candidate
                        bestSurvives = survives
                    }
                }
            }
        }
        return best
    }

    @Test fun optimizedHintsMatchRealTurnsOnVariedSeededBoards() {
        val random = Random(20261022)
        val densities = listOf(0, 12, 25, 42, 60, 78, 95)
        repeat(84) { iteration ->
            val density = densities[iteration % densities.size]
            val board = List(GameEngine.SIZE * GameEngine.SIZE) {
                if (random.nextInt(100) < density) random.nextInt(1, 7) else 0
            }
            val tray = List(GameEngine.TRAY_SIZE) { slot ->
                if (iteration % 3 == 0 && slot != 0) null
                else if (iteration % 3 == 1 && slot == 2) null
                else GameEngine.pieceFromId(random.nextInt(22))
            }
            val state = GameState(board = board, tray = tray, score = iteration * 100)
            val boardBefore = state.board.toList()
            val trayBefore = state.tray.toList()

            assertEquals(
                "Hint differs from full engine simulation at board $iteration",
                referenceHint(state),
                GameEngine.suggestMove(state),
            )
            assertEquals(boardBefore, state.board)
            assertEquals(trayBefore, state.tray)
            assertEquals(iteration * 100, state.score)
        }
    }
}
