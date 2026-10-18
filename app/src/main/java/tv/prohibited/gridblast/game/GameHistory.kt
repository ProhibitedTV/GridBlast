package tv.prohibited.gridblast.game

import kotlin.random.Random

/**
 * Immutable game history. Exactly one immediately previous valid turn may be
 * restored for free. An undone move cannot be undone again; the next legal
 * placement opens a fresh one-step undo.
 *
 * Does not hold or modify the all-time best score, which must remain monotonic.
 */
data class GameHistory(
    val current: GameState,
    val previous: GameState? = null,
) {
    fun place(
        slot: Int,
        row: Int,
        col: Int,
        random: Random = Random.Default,
    ): GameHistory {
        val next = GameEngine.place(current, slot, row, col, random)
        return if (next == current) this else GameHistory(next, current)
    }

    fun undoLast(): GameHistory = previous?.let { GameHistory(it) } ?: this

    companion object {
        fun newGame(random: Random = Random.Default): GameHistory =
            GameHistory(GameEngine.newGame(random))
    }
}

/**
 * A bounded, versioned snapshot written to private Android preferences.
 *
 * A tilde is safe as the separator because GameStateCodec's version-1 data
 * contains only digits, dashes, pipes and commas. Both the current game and
 * undo snapshot are saved in one preference value to prevent partial pairs.
 */
object GameHistoryCodec {
    private const val VERSION = "history-1"
    private const val MAX_LENGTH = 2200

    fun encode(history: GameHistory): String {
        val current = GameStateCodec.encode(history.current)
        val previous = history.previous?.let { previous ->
            require(!previous.gameOver && previous.score < history.current.score) {
                "Undo snapshot must precede one legal placement"
            }
            GameStateCodec.encode(previous)
        } ?: "-"
        return "$VERSION~$current~$previous"
    }

    fun decode(raw: String?): GameHistory? {
        if (raw.isNullOrEmpty() || raw.length > MAX_LENGTH) return null
        val fields = raw.split("~")
        if (fields.size != 3 || fields[0] != VERSION) return null
        val current = GameStateCodec.decode(fields[1]) ?: return null
        val previous = if (fields[2] == "-") {
            null
        } else {
            GameStateCodec.decode(fields[2]) ?: return GameHistory(current)
        }
        // Treat invalid/stale undo history as no undo, never discard a valid game.
        if (previous != null &&
            (previous.gameOver || previous.score >= current.score)
        ) return GameHistory(current)
        return GameHistory(current, previous)
    }
}
