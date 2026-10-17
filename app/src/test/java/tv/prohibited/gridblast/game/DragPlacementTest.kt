package tv.prohibited.gridblast.game

import org.junit.Assert.*
import org.junit.Test

class DragPlacementTest {
    private fun at(x: Float, y: Float, rows: Int = 1, cols: Int = 1, inset: Float = 0f): Cell? =
        DragPlacement.target(
            fingerX = x,
            fingerY = y + 56f,
            boardLeft = 100f,
            boardTop = 200f,
            boardWidth = 320f,
            boardHeight = 320f,
            pieceRows = rows,
            pieceCols = cols,
            liftPx = 56f,
            insetPx = inset,
        )

    @Test fun previewMatchesSingleTileCenter() {
        assertEquals(Cell(0, 0), at(120f, 220f))
        assertEquals(Cell(7, 7), at(400f, 500f))
    }

    @Test fun previewCentersEvenSizedPieces() {
        assertEquals(Cell(1, 1), at(180f, 280f, rows = 2, cols = 2))
        assertEquals(Cell(3, 2), at(240f, 360f, rows = 2, cols = 4))
    }

    @Test fun previewCentersOddSizedPieces() {
        assertEquals(Cell(0, 0), at(160f, 260f, rows = 3, cols = 3))
        assertEquals(Cell(3, 3), at(280f, 380f, rows = 3, cols = 3))
    }

    @Test fun rejectsDropsOutsideBoardRatherThanSnapping() {
        assertNull(at(99f, 260f))
        assertNull(at(420f, 260f))
        assertNull(at(160f, 199f))
        assertNull(at(160f, 520f))
    }

    @Test fun partlyOffBoardPieceIsNotClamped() {
        val cell = at(120f, 220f, rows = 3, cols = 3)!!
        assertEquals(Cell(-1, -1), cell)
        val piece = GameEngine.pieceFromId(11)!!
        assertFalse(GameEngine.canPlace(List(64) { 0 }, piece, cell.row, cell.col))
    }

    @Test fun respectsBoardPadding() {
        assertEquals(Cell(0, 0), at(124.5f, 224.5f, inset = 4f))
        assertEquals(Cell(7, 7), at(395.5f, 495.5f, inset = 4f))
    }

    @Test fun invalidGeometryNeverProducesCell() {
        assertNull(DragPlacement.target(Float.NaN, 0f, 0f, 0f, 320f, 320f, 1, 1, 0f))
        assertNull(DragPlacement.target(0f, 0f, 0f, 0f, 0f, 320f, 1, 1, 0f))
        assertNull(DragPlacement.target(0f, 0f, 0f, 0f, 320f, 320f, 0, 1, 0f))
        assertNull(DragPlacement.target(0f, 0f, 0f, 0f, 320f, 320f, 1, 1, 0f, 200f))
    }
}
