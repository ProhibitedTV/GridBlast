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

    /**
     * A saved Undo must represent a real, single successful placement.
     * Merely requiring a lower previous score allows stale/mixed sessions to
     * appear as valid checkpoints and roll the player back to another board.
     *
     * The reducer is deterministic except for the contents of a freshly drawn
     * tray. For refills, validate the score, board, and full tray size; the
     * exact randomly chosen shape IDs cannot be reconstructed from a save.
     */
    private fun isOneMoveSuccessor(previous: GameState, current: GameState): Boolean {
        for (slot in previous.tray.indices) {
            val piece = previous.tray[slot] ?: continue
            for (row in 0 until GameEngine.SIZE) {
                for (col in 0 until GameEngine.SIZE) {
                    val analysis = GameEngine.analyze(previous, slot, row, col) ?: continue
                    if (previous.score.toLong() + analysis.points != current.score.toLong()) continue

                    val board = previous.board.toMutableList()
                    analysis.placedIndices.forEach { board[it] = piece.colorId }
                    analysis.clearedIndices.forEach { board[it] = 0 }
                    if (board != current.board) continue

                    val remaining = previous.tray.toMutableList()
                    remaining[slot] = null
                    if (remaining.all { it == null }) {
                        // The third placement draws a new full tray; its random
                        // contents cannot be reproduced without the RNG state.
                        if (current.tray.size != GameEngine.TRAY_SIZE ||
                            current.tray.any { it == null }
                        ) continue
                    } else if (current.tray != remaining) {
                        continue
                    }
                    return true
                }
            }
        }
        return false
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
            (previous.gameOver || previous.score >= current.score ||
                !isOneMoveSuccessor(previous, current))
        ) return GameHistory(current)
        return GameHistory(current, previous)
    }
}
