package tv.prohibited.gridblast.game

import java.security.MessageDigest
import java.util.Base64

/**
 * A portable, user-initiated offline save. Only game state and three local
 * preferences are included. No IDs, device data or network access.
 *
 * SHA-256 detects accidental corruption; it is NOT authentication or encryption.
 * Treat every imported file as untrusted even when its checksum matches.
 */
data class GameBackup(
    val history: GameHistory,
    val bestScore: Int,
    val soundEnabled: Boolean,
    val hapticsEnabled: Boolean,
    val motionEnabled: Boolean,
)

object GameBackupCodec {
    const val MAX_BYTES = 8192
    private const val HEADER = "GRIDBLAST-BACKUP-1"
    private val SHA = Regex("[0-9a-f]{64}")
    private val B64 = Regex("[A-Za-z0-9_-]+")

    private fun checksum(body: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(body.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    fun encode(backup: GameBackup): String {
        require(backup.bestScore >= backup.history.current.score && backup.bestScore >= 0) {
            "Best score cannot be below the current round score"
        }
        val history = GameHistoryCodec.encode(backup.history)
        val encodedHistory = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(history.toByteArray(Charsets.UTF_8))
        val body = listOf(
            HEADER,
            "history=$encodedHistory",
            "best=${backup.bestScore}",
            "sound=${if (backup.soundEnabled) 1 else 0}",
            "haptics=${if (backup.hapticsEnabled) 1 else 0}",
            "motion=${if (backup.motionEnabled) 1 else 0}",
        ).joinToString("\n", postfix = "\n")
        val result = body + "sha256=${checksum(body)}\n"
        require(result.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        return result
    }

    /** Reject unknown versions, noncanonical fields, tampering and invalid history. */
    fun decode(raw: String?): GameBackup? {
        if (raw.isNullOrEmpty() || raw.length > MAX_BYTES || !raw.endsWith("\n") ||
            raw.any { it.code > 127 || it == '\r' }
        ) return null
        val parts = raw.dropLast(1).split("\n")
        if (parts.size != 7 || parts[0] != HEADER) return null
        if (!parts[1].startsWith("history=") || !parts[2].startsWith("best=") ||
            !parts[3].startsWith("sound=") || !parts[4].startsWith("haptics=") ||
            !parts[5].startsWith("motion=") || !parts[6].startsWith("sha256=")
        ) return null

        val expected = parts[6].removePrefix("sha256=")
        if (!SHA.matches(expected)) return null
        val body = parts.take(6).joinToString("\n", postfix = "\n")
        if (checksum(body) != expected) return null

        val encoded = parts[1].removePrefix("history=")
        if (!B64.matches(encoded)) return null
        val historyString = try {
            String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            return null
        }
        val history = GameHistoryCodec.decode(historyString) ?: return null
        // History decode intentionally drops invalid previous turns for normal
        // private storage recovery, but an external backup must be fully valid.
        if (GameHistoryCodec.encode(history) != historyString) return null

        val best = parts[2].removePrefix("best=").toIntOrNull()
            ?.takeIf { it >= history.current.score } ?: return null
        fun flag(index: Int, prefix: String): Boolean? = when (parts[index]) {
            "${prefix}1" -> true
            "${prefix}0" -> false
            else -> null
        }
        val sound = flag(3, "sound=") ?: return null
        val haptics = flag(4, "haptics=") ?: return null
        val motion = flag(5, "motion=") ?: return null
        val backup = GameBackup(history, best, sound, haptics, motion)
        // Also disallow noncanonical Base64, integers, extra delimiters or data.
        return backup.takeIf { encode(it) == raw }
    }
}
