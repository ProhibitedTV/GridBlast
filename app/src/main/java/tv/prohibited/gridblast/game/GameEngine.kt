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
     * Suggest the highest immediate-value legal placement.
     *
     * Favors clearing lines, then placing larger pieces, then deterministic
     * top-to-bottom/left-to-right order. No randomness or game state mutation.
     * This is a one-move heuristic, not a promise of a winnable round.
     */
    fun suggestMove(state: GameState): MoveHint? {
        if (state.gameOver) return null
        var best: MoveHint? = null
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
                    // Equal scores retain the first (stable) slot/row/column.
                    if (best == null || candidate.points > best.points ||
                        (candidate.points == best.points &&
                            candidate.linesCleared > best.linesCleared)) {
                        best = candidate
                    }
                }
            }
        }
        return best
    }

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
        val nextTray = if (remaining.all { it == null }) drawTray(random) else remaining
        val next = GameState(
            board = board,
            tray = nextTray,
            score = state.score + analysis.points,
        )
        return next.copy(gameOver = !hasAnyMove(next))
    }
}
