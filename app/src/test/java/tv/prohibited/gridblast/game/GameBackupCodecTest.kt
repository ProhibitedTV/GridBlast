package tv.prohibited.gridblast.game

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameBackupCodecTest {
    private fun sample() = GameBackup(
        history = GameHistory(GameEngine.newGame(Random(94))),
        bestScore = 157,
        soundEnabled = true,
        hapticsEnabled = false,
        motionEnabled = true,
    )

    @Test fun freshGameAndSettingsRoundTrip() {
        val backup = sample()
        assertEquals(backup, GameBackupCodec.decode(GameBackupCodec.encode(backup)))
    }

    @Test fun preservesActualUndoCheckpointAndBest() {
        val previous = GameHistory(GameEngine.newGame(Random(2)))
        val played = previous.place(0, 0, 0, Random(3))
        assertNotNull(played.previous)
        val backup = sample().copy(history = played)
        val restored = GameBackupCodec.decode(GameBackupCodec.encode(backup))!!
        assertEquals(played, restored.history)
        assertEquals(previous.current, restored.history.undoLast().current)
        assertEquals(157, restored.bestScore)
    }

    @Test fun rejectsOversizedAndEmptyBackups() {
        assertNull(GameBackupCodec.decode(null))
        assertNull(GameBackupCodec.decode(""))
        assertNull(GameBackupCodec.decode("x".repeat(GameBackupCodec.MAX_BYTES + 1)))
    }

    @Test fun rejectsUnknownVersion() {
        val raw = GameBackupCodec.encode(sample())
        assertNull(GameBackupCodec.decode(raw.replaceFirst("GRIDBLAST-BACKUP-1", "GRIDBLAST-BACKUP-2")))
    }

    @Test fun rejectsChecksumMismatchAndTruncation() {
        val raw = GameBackupCodec.encode(sample())
        assertNull(GameBackupCodec.decode(raw.replace("best=157", "best=158")))
        assertNull(GameBackupCodec.decode(raw.dropLast(1)))
        assertNull(GameBackupCodec.decode(raw.dropLast(20)))
    }

    @Test fun rejectsReorderedAndExtraFields() {
        val raw = GameBackupCodec.encode(sample())
        assertNull(GameBackupCodec.decode(raw.replace("sound=1\nhaptics=0", "haptics=0\nsound=1")))
        assertNull(GameBackupCodec.decode(raw + "extra=sneaky\n"))
    }

    @Test fun rejectsNoncanonicalBooleans() {
        val raw = GameBackupCodec.encode(sample())
        assertNull(GameBackupCodec.decode(raw.replace("sound=1", "sound=true")))
    }

    @Test fun refusesBestScoreBelowCurrentRound() {
        val game = GameEngine.newGame(Random(7))
        val moved = GameHistory(game).place(0, 0, 0, Random(8))
        val backup = sample().copy(history = moved, bestScore = 0)
        assertThrows(IllegalArgumentException::class.java) {
            GameBackupCodec.encode(backup)
        }
    }

    @Test fun rejectsBadEncodingAndArbitraryText() {
        val raw = GameBackupCodec.encode(sample())
        assertNull(GameBackupCodec.decode(raw.replace("history=", "history=@")))
        assertNull(GameBackupCodec.decode(raw.replace("GRIDBLAST", "GRIDBLÅST")))
        assertNull(GameBackupCodec.decode("hello, world!\n"))
    }

    @Test fun acceptsScoreAtIntegerMaximum() {
        val backup = sample().copy(bestScore = Int.MAX_VALUE)
        assertEquals(Int.MAX_VALUE,
            GameBackupCodec.decode(GameBackupCodec.encode(backup))!!.bestScore)
    }
}
