package tv.prohibited.gridblast.game

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class FairRefillTest {
    private val isolatedSpaces = List(64) { index ->
        if (((index / 8) + (index % 8)) % 2 == 0) 0 else 3
    }

    @Test fun aFreshTrayAlwaysHasAtLeastOneMoveWhenAnyShapeFits() {
        // The checkerboard has 32 playable single cells but no adjacent pairs.
        // Most all-random three-piece trays cannot fit any shape here.
        val playable = List(3) { GameEngine.pieceFromId(0) }
        assertTrue(GameEngine.hasAnyMove(GameState(board = isolatedSpaces, tray = playable)))
        for (seed in 0..511) {
            val tray = GameEngine.refillTray(isolatedSpaces, Random(seed))
            assertEquals(3, tray.filterNotNull().size)
            assertTrue("No playable shape for seed $seed",
                GameEngine.hasAnyMove(GameState(board = isolatedSpaces, tray = tray)))
        }
    }

    @Test fun playableRandomDrawIsKeptUnmodified() {
        // Same RNG makes newGame's random draw a reproducible reference.
        for (seed in 0..200) {
            val randomDraw = GameEngine.newGame(Random(seed)).tray
            val canPlayOriginal = GameEngine.hasAnyMove(
                GameState(board = isolatedSpaces, tray = randomDraw),
            )
            if (canPlayOriginal) {
                assertEquals(
                    "An already-playable tray must not be modified: seed $seed",
                    randomDraw,
                    GameEngine.refillTray(isolatedSpaces, Random(seed)),
                )
            }
        }
    }

    @Test fun trulyBlockedBoardCannotBeRescued() {
        val full = List(64) { 1 }
        for (seed in 0..32) {
            val tray = GameEngine.refillTray(full, Random(seed))
            assertEquals(GameEngine.newGame(Random(seed)).tray, tray)
            assertFalse(GameEngine.hasAnyMove(GameState(board = full, tray = tray)))
        }
    }

    @Test fun actualThirdPlacementRefillsIntoAPossibleNextTurn() {
        val single = GameEngine.pieceFromId(0)!!
        val before = GameState(
            board = isolatedSpaces,
            tray = listOf(null, null, single),
            score = 12,
        )
        for (seed in 0..150) {
            val after = GameEngine.place(before, 2, 0, 0, Random(seed))
            assertEquals(13, after.score)
            assertEquals(3, after.tray.filterNotNull().size)
            assertFalse("Premature game over for seed $seed", after.gameOver)
            assertTrue(GameEngine.hasAnyMove(after))
        }
    }

    @Test fun undoRestoresOriginalTrayAfterAssistedRefill() {
        val before = GameHistory(
            GameState(
                board = isolatedSpaces,
                tray = listOf(null, null, GameEngine.pieceFromId(0)),
            ),
        )
        val after = before.place(2, 0, 0, Random(150))
        assertTrue(GameEngine.hasAnyMove(after.current))
        assertEquals(before.current, after.undoLast().current)
        assertEquals(
            after,
            GameHistoryCodec.decode(GameHistoryCodec.encode(after)),
        )
    }

    @Test fun sameSeedAndBoardProduceIdenticalRefills() {
        for (seed in 0..200) {
            val a = GameEngine.refillTray(isolatedSpaces, Random(seed))
            val b = GameEngine.refillTray(isolatedSpaces.toList(), Random(seed))
            assertEquals(a, b)
        }
    }
}
