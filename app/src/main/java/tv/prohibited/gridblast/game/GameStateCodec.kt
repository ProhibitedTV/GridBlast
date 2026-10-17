package tv.prohibited.gridblast.game

/**
 * Versioned, dependency-free, locally stored game snapshot.
 * A snapshot never contains personal information or network identifiers.
 * The format stores only the board, score and original shape IDs. Anything
 * malformed is rejected rather than exposing the UI to a partial game.
 */
object GameStateCodec {
    private const val VERSION = "1"
    private const val MAX_LENGTH = 1024

    fun encode(game: GameState): String {
        require(game.board.size == GameEngine.SIZE * GameEngine.SIZE)
        require(game.tray.size == GameEngine.TRAY_SIZE)
        require(game.board.all { it in 0..6 })
        require(game.score >= 0)
        require(game.tray.any { it != null })
        require(game.tray.filterNotNull().all { GameEngine.pieceFromId(it.shapeId) == it })
        return listOf(
            VERSION,
            game.score.toString(),
            game.board.joinToString(","),
            game.tray.joinToString(",") { it?.shapeId?.toString() ?: "-" },
        ).joinToString("|")
    }

    fun decode(raw: String?): GameState? {
        if (raw.isNullOrEmpty() || raw.length > MAX_LENGTH) return null
        val fields = raw.split("|")
        if (fields.size != 4 || fields[0] != VERSION) return null
        val score = fields[1].toIntOrNull()?.takeIf { it >= 0 } ?: return null

        val boardStrings = fields[2].split(",")
        if (boardStrings.size != GameEngine.SIZE * GameEngine.SIZE) return null
        val board = boardStrings.map { it.toIntOrNull()?.takeIf { id -> id in 0..6 } ?: return null }

        val shapeIds = fields[3].split(",")
        if (shapeIds.size != GameEngine.TRAY_SIZE) return null
        val tray: List<Piece?> = shapeIds.map {
            if (it == "-") {
                null
            } else {
                val id = it.toIntOrNull() ?: return null
                GameEngine.pieceFromId(id) ?: return null
            }
        }
        if (tray.all { it == null }) return null
        val restored = GameState(board = board, tray = tray, score = score)
        return restored.copy(gameOver = !GameEngine.hasAnyMove(restored))
    }
}
