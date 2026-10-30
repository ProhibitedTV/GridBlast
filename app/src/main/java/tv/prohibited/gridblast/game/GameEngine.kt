package tv.prohibited.gridblast.game

import kotlin.random.Random

data class Cell(val row: Int, val col: Int)

data class Piece(val shapeId: Int, val colorId: Int, val cells: List<Cell>) {
    init {
        require(cells.isNotEmpty())
        require(cells.all { it.row >= 0 && it.col >= 0 })
    }
}

data class GameState(
    val board: List<Int> = List(GameEngine.SIZE * GameEngine.SIZE) { 0 },
    val tray: List<Piece?> = emptyList(),
    val score: Int = 0,
    val gameOver: Boolean = false,
)

data class PlacementAnalysis(
    val placedIndices: Set<Int>,
    val clearedIndices: Set<Int>,
    val clearedRows: List<Int>,
    val clearedColumns: List<Int>,
) {
    val linesCleared: Int get() = clearedRows.size + clearedColumns.size
    val points: Int get() = placedIndices.size + clearedIndices.size * 10
}

/**
 * A free, optional suggestion. Pure data: requesting a hint never places a
 * piece or changes score, board, tray or saved session.
 */
data class MoveHint(
    val slot: Int,
    val row: Int,
    val col: Int,
    val points: Int,
    val linesCleared: Int,
)

object GameEngine {
    const val SIZE = 8
    const val TRAY_SIZE = 3

    private fun shape(vararg rows: String): List<Cell> = buildList {
        rows.forEachIndexed { row, pattern ->
            pattern.forEachIndexed { col, character ->
                if (character == '#') add(Cell(row, col))
            }
        }
    }

    // Original shapes and palette are deliberately independent of any third-party game assets.
    private val shapes = listOf(
        shape("#"),
        shape("##"), shape("###"), shape("####"), shape("#####"),
        shape("#", "#"), shape("#", "#", "#"), shape("#", "#", "#", "#"), shape("#", "#", "#", "#", "#"),
        shape("##", "##"), shape("###", "###"), shape("###", "###", "###"),
        shape("#.", "##"), shape(".#", "##"), shape("##", "#."), shape("##", ".#"),
        shape("###", ".#."), shape(".#.", "###"),
        shape("##.", ".##"), shape(".##", "##."),
        shape("#..", "###"), shape("..#", "###"),
    )

    fun pieceFromId(shapeId: Int): Piece? =
        shapes.getOrNull(shapeId)?.let { cells -> Piece(shapeId, shapeId % 6 + 1, cells) }

    fun newGame(random: Random = Random.Default): GameState = GameState(tray = drawTray(random))

    private fun drawTray(random: Random): List<Piece?> = List(TRAY_SIZE) {
        pieceFromId(random.nextInt(shapes.size))
    }

    /**
     * A new tray always offers a legal next move when the board has a place
     * for at least one shape. We keep all three random shapes if any fits.
     * Only on an otherwise dead-on-arrival tray do we replace one random slot
     * with a randomly chosen shape that can fit.
     *
     * The player must still choose where to place the rescuing piece. No
     * blocks are cleared, points added, or other pieces changed.
     */
    internal fun refillTray(board: List<Int>, random: Random): List<Piece?> {
        val drawn = drawTray(random)
        if (drawn.filterNotNull().any { piece ->
                (0 until SIZE).any { row ->
                    (0 until SIZE).any { col -> canPlace(board, piece, row, col) }
                }
            }) return drawn

        val compatible = shapes.indices.mapNotNull(::pieceFromId).filter { piece ->
            (0 until SIZE).any { row ->
                (0 until SIZE).any { col -> canPlace(board, piece, row, col) }
            }
        }
        if (compatible.isEmpty()) return drawn // A genuinely exhausted board.

        return drawn.toMutableList().apply {
            val replacementSlot = random.nextInt(TRAY_SIZE)
            this[replacementSlot] = compatible[random.nextInt(compatible.size)]
        }
    }

    fun canPlace(board: List<Int>, piece: Piece, row: Int, col: Int): Boolean {
        if (board.size != SIZE * SIZE) return false
        return piece.cells.all { cell ->
            val r = row + cell.row
            val c = col + cell.col
            r in 0 until SIZE && c in 0 until SIZE && board[r * SIZE + c] == 0
        }
    }

    fun hasAnyMove(state: GameState): Boolean = state.tray.filterNotNull().any { piece ->
        (0 until SIZE).any { row ->
            (0 until SIZE).any { col -> canPlace(state.board, piece, row, col) }
        }
    }

    /**
     * Pure move analysis used both for scoring and for gameplay feedback.
     * A cell crossing a completed row and column is cleared only once.
     */
    fun analyze(state: GameState, slot: Int, row: Int, col: Int): PlacementAnalysis? {
        if (state.gameOver || slot !in state.tray.indices) return null
        val piece = state.tray[slot] ?: return null
        if (!canPlace(state.board, piece, row, col)) return null

        val placed = piece.cells.map { cell ->
            (row + cell.row) * SIZE + col + cell.col
        }.toSet()
        val filled = state.board.toMutableList()
        placed.forEach { filled[it] = piece.colorId }

        val rows = (0 until SIZE).filter { r ->
            (0 until SIZE).all { c -> filled[r * SIZE + c] != 0 }
        }
        val cols = (0 until SIZE).filter { c ->
            (0 until SIZE).all { r -> filled[r * SIZE + c] != 0 }
        }
        val cleared = mutableSetOf<Int>()
        rows.forEach { r -> (0 until SIZE).forEach { c -> cleared += r * SIZE + c } }
        cols.forEach { c -> (0 until SIZE).forEach { r -> cleared += r * SIZE + c } }
        return PlacementAnalysis(placed, cleared, rows, cols)
    }

    /**
     * Suggest the highest-scoring move that keeps the round alive, if possible.
     *
     * Some legal placements strand the other tray pieces and end the round.
     * Prefer a non-terminal move to a higher-scoring terminal move; if every
     * legal move loses, still offer the best available placement. Within each
     * category, prefer points, then lines cleared, then stable slot/row/column.
     *
     * Reuse the placement analysis to check next-turn legality without
     * placing the piece twice, drawing a tray or consuming gameplay RNG.
     * This is only a one-move lookahead.
     */
    fun suggestMove(state: GameState): MoveHint? {
        if (state.gameOver) return null
        var best: MoveHint? = null
        var bestSurvives = false
        for (slot in state.tray.indices) {
            if (state.tray[slot] == null) continue
            for (row in 0 until SIZE) {
                for (col in 0 until SIZE) {
                    val analysis = analyze(state, slot, row, col) ?: continue
                    val candidate = MoveHint(
                        slot = slot,
                        row = row,
                        col = col,
                        points = analysis.points,
                        linesCleared = analysis.linesCleared,
                    )
                    // Once a safe hint is found, lower-scoring or equal
                    // candidates cannot improve it regardless of survival.
                    if (bestSurvives && best != null &&
                        (candidate.points < best.points ||
                            (candidate.points == best.points &&
                                candidate.linesCleared <= best.linesCleared))
                    ) continue

                    val survives = survivesAfterPlacement(state, slot, analysis)
                    // If the only way to stay alive scores fewer points, favor
                    // survival. Forced-loss hints remain available.
                    if (best == null || (survives && !bestSurvives) ||
                        (survives == bestSurvives &&
                            (candidate.points > best.points ||
                                (candidate.points == best.points &&
                                    candidate.linesCleared > best.linesCleared)))) {
                        best = candidate
                        bestSurvives = survives
                    }
                }
            }
        }
        return best
    }

    /**
     * Check whether any piece will be playable after this placement. Uses the
     * already calculated placement/clearing result; never modifies game state.
     * If the tray is exhausted, refillTray guarantees a playable shape when
     * any empty cell exists, because the one-cell shape is always available.
     */
    private fun survivesAfterPlacement(
        state: GameState,
        slot: Int,
        analysis: PlacementAnalysis,
    ): Boolean {
        val piece = state.tray[slot] ?: return false
        val board = state.board.toMutableList()
        analysis.placedIndices.forEach { board[it] = piece.colorId }
        analysis.clearedIndices.forEach { board[it] = 0 }

        val remaining = state.tray.toMutableList()
        remaining[slot] = null
        if (remaining.none { it != null }) return board.any { it == 0 }
        return hasAnyMove(GameState(board = board, tray = remaining))
    }

    /**
     * Scores are stored as non-negative Ints in the existing local save format.
     * Saturation keeps marathon sessions valid instead of wrapping negative
     * and crashing when the save codec rejects the overflowed score.
     */
    internal fun cappedScore(score: Int, earned: Int): Int =
        (score.toLong() + earned.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    /** Invalid moves leave the board and tray untouched. */
    fun place(
        state: GameState,
        slot: Int,
        row: Int,
        col: Int,
        random: Random = Random.Default,
    ): GameState {
        val analysis = analyze(state, slot, row, col) ?: return state
        val piece = state.tray[slot] ?: return state
        val board = state.board.toMutableList()
        analysis.placedIndices.forEach { board[it] = piece.colorId }
        analysis.clearedIndices.forEach { board[it] = 0 }

        val remaining = state.tray.toMutableList()
        remaining[slot] = null
        val nextTray = if (remaining.all { it == null }) refillTray(board, random) else remaining
        val next = GameState(
            board = board,
            tray = nextTray,
            score = cappedScore(state.score, analysis.points),
        )
        return next.copy(gameOver = !hasAnyMove(next))
    }
}
