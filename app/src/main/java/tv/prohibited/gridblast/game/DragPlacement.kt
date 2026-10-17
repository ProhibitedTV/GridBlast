package tv.prohibited.gridblast.game

import kotlin.math.roundToInt

/**
 * Converts a lifted drag preview into its actual top-left grid placement.
 *
 * [fingerX] and [fingerY] are in the same coordinate space as the outer board
 * bounds. The piece preview is centered horizontally and drawn [liftPx] above
 * the finger, so the game must use that same center when calculating the drop.
 *
 * An out-of-bounds top-left placement is returned (rather than clamped) when
 * part of the piece falls outside the board; GameEngine.canPlace rejects it.
 */
object DragPlacement {
    fun target(
        fingerX: Float,
        fingerY: Float,
        boardLeft: Float,
        boardTop: Float,
        boardWidth: Float,
        boardHeight: Float,
        pieceRows: Int,
        pieceCols: Int,
        liftPx: Float,
        insetPx: Float = 0f,
    ): Cell? {
        if (pieceRows !in 1..GameEngine.SIZE || pieceCols !in 1..GameEngine.SIZE) return null
        if (!fingerX.isFinite() || !fingerY.isFinite() || !liftPx.isFinite() ||
            !boardLeft.isFinite() || !boardTop.isFinite() ||
            !boardWidth.isFinite() || !boardHeight.isFinite() ||
            !insetPx.isFinite() || insetPx < 0f) return null

        val innerWidth = boardWidth - 2f * insetPx
        val innerHeight = boardHeight - 2f * insetPx
        if (innerWidth <= 0f || innerHeight <= 0f) return null

        val x = fingerX - boardLeft - insetPx
        val y = fingerY - liftPx - boardTop - insetPx
        if (x < 0f || x >= innerWidth || y < 0f || y >= innerHeight) return null

        val col = (x * GameEngine.SIZE / innerWidth - pieceCols / 2f).roundToInt()
        val row = (y * GameEngine.SIZE / innerHeight - pieceRows / 2f).roundToInt()
        return Cell(row, col)
    }
}
