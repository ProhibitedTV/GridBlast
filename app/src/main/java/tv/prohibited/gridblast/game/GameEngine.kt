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

    /** Invalid moves leave the board and tray untouched. */
    fun place(
        state: GameState,
        slot: Int,
        row: Int,
        col: Int,
        random: Random = Random.Default,
    ): GameState {
        if (state.gameOver || slot !in state.tray.indices) return state
        val piece = state.tray[slot] ?: return state
        if (!canPlace(state.board, piece, row, col)) return state

        val board = state.board.toMutableList()
        piece.cells.forEach { cell -> board[(row + cell.row) * SIZE + col + cell.col] = piece.colorId }

        // Collect completed rows and columns together so intersections only clear once.
        val rows = (0 until SIZE).filter { r -> (0 until SIZE).all { c -> board[r * SIZE + c] != 0 } }
        val cols = (0 until SIZE).filter { c -> (0 until SIZE).all { r -> board[r * SIZE + c] != 0 } }
        val removed = mutableSetOf<Int>()
        rows.forEach { r -> (0 until SIZE).forEach { c -> removed += r * SIZE + c } }
        cols.forEach { c -> (0 until SIZE).forEach { r -> removed += r * SIZE + c } }
        removed.forEach { board[it] = 0 }

        val remaining = state.tray.toMutableList()
        remaining[slot] = null
        val nextTray = if (remaining.all { it == null }) drawTray(random) else remaining
        val next = GameState(
            board = board,
            tray = nextTray,
            score = state.score + piece.cells.size + removed.size * 10,
        )
        return next.copy(gameOver = !hasAnyMove(next))
    }
}
