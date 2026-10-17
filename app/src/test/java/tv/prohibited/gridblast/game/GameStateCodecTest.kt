package tv.prohibited.gridblast.game

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameStateCodecTest {
    @Test fun roundTripsFreshGame() {
        val game = GameEngine.newGame(Random(51))
        assertEquals(game, GameStateCodec.decode(GameStateCodec.encode(game)))
    }

    @Test fun roundTripsPartlyUsedTrayAndPlacedCells() {
        val original = GameEngine.newGame(Random(3))
        val piece = original.tray[0]!!
        val moved = GameEngine.place(original, 0, 0, 0, Random(1))
        assertNull(moved.tray[0])
        assertEquals(moved, GameStateCodec.decode(GameStateCodec.encode(moved)))
        assertTrue(piece.cells.isNotEmpty())
    }

    @Test fun rejectsUnknownVersion() {
        val encoded = GameStateCodec.encode(GameEngine.newGame(Random(1)))
        assertNull(GameStateCodec.decode(encoded.replaceFirst("1|", "2|")))
    }

    @Test fun rejectsCorruptAndOversizedPayloads() {
        assertNull(GameStateCodec.decode(null))
        assertNull(GameStateCodec.decode(""))
        assertNull(GameStateCodec.decode("garbage"))
        assertNull(GameStateCodec.decode("x".repeat(1100)))
    }

    @Test fun rejectsInvalidBoardCell() {
        val game = GameEngine.newGame(Random(1))
        val raw = GameStateCodec.encode(game)
        assertNull(GameStateCodec.decode(raw.replaceFirst("|0,", "|99,")))
    }

    @Test fun rejectsUnknownPiece() {
        val raw = GameStateCodec.encode(GameEngine.newGame(Random(1)))
        val parts = raw.split("|").toMutableList()
        parts[3] = "999,-,-"
        assertNull(GameStateCodec.decode(parts.joinToString("|")))
    }

    @Test fun recomputesGameOverOnRestore() {
        // Checkerboard with one remaining single square and one 2x2 square.
        val board = List(64) { index -> if ((index / 8 + index % 8) % 2 == 0) 0 else 3 }
        val state = GameState(
            board = board,
            tray = listOf(GameEngine.pieceFromId(0), GameEngine.pieceFromId(9), null),
        )
        val placed = GameEngine.place(state, 0, 0, 0, Random(8))
        assertTrue(placed.gameOver)
        assertTrue(GameStateCodec.decode(GameStateCodec.encode(placed))!!.gameOver)
    }
}
