package tv.prohibited.gridblast.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeAccessibilityTest {
    @Test fun horizontalAndVerticalBarsDescribeDifferentDirections() {
        assertEquals(
            "1 row by 3 columns; row 1: columns 1, 2, 3",
            GameEngine.pieceFromId(2)!!.spokenGeometry(),
        )
        assertEquals(
            "3 rows by 1 column; row 1: column 1; row 2: column 1; row 3: column 1",
            GameEngine.pieceFromId(6)!!.spokenGeometry(),
        )
    }

    @Test fun mirroredElbowsHaveDistinctGeometry() {
        val first = GameEngine.pieceFromId(12)!!.spokenGeometry()
        val second = GameEngine.pieceFromId(13)!!.spokenGeometry()
        assertEquals(
            "2 rows by 2 columns; row 1: column 1; row 2: columns 1, 2",
            first,
        )
        assertEquals(
            "2 rows by 2 columns; row 1: column 2; row 2: columns 1, 2",
            second,
        )
        assertNotEquals(first, second)
    }

    @Test fun squareDescribesAllNineBlocks() {
        assertEquals(
            "3 rows by 3 columns; row 1: columns 1, 2, 3; " +
                "row 2: columns 1, 2, 3; row 3: columns 1, 2, 3",
            GameEngine.pieceFromId(11)!!.spokenGeometry(),
        )
    }

    @Test fun allGamePiecesGetNonEmptyRowByRowDescriptions() {
        for (shapeId in 0..21) {
            val piece = GameEngine.pieceFromId(shapeId)!!
            val spoken = piece.spokenGeometry()
            assertTrue("missing shape bounds for $shapeId", spoken.contains(" by "))
            assertTrue("missing row 1 for $shapeId", spoken.contains("row 1:"))
            assertTrue("missing last row for $shapeId",
                spoken.contains("row ${piece.cells.maxOf { it.row } + 1}:"))
        }
    }
}
