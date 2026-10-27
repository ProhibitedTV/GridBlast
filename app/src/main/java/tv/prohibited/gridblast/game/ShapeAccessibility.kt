package tv.prohibited.gridblast.game

/**
 * A precise, orientation-sensitive description for screen-reader users.
 *
 * A piece's block count alone cannot distinguish e.g. horizontal and vertical
 * bars, mirrored elbows or a square. Describe its bounding dimensions and
 * occupied columns in every row; this works for every original shape and
 * does not depend on color or a mutable list of human-written shape names.
 */
fun Piece.spokenGeometry(): String {
    val height = cells.maxOf { it.row } + 1
    val width = cells.maxOf { it.col } + 1
    val dimensions = "$height ${if (height == 1) "row" else "rows"} by " +
        "$width ${if (width == 1) "column" else "columns"}"
    val rowPattern = (0 until height).joinToString("; ") { row ->
        val columns = cells.filter { it.row == row }.map { it.col + 1 }.distinct().sorted()
        val cellsInRow = when (columns.size) {
            0 -> "empty"
            1 -> "column ${columns.single()}"
            else -> "columns ${columns.joinToString(", ")}"
        }
        "row ${row + 1}: $cellsInRow"
    }
    return "$dimensions; $rowPattern"
}
