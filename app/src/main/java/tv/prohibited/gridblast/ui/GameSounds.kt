package tv.prohibited.gridblast.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import tv.prohibited.gridblast.R
import java.util.concurrent.ConcurrentHashMap

/**
 * Short original synthesizer-generated WAV effects. Uses local Android SoundPool
 * and the system's game/media volume. No streaming, external SDKs, or permissions.
 */
internal class GameSounds(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ready = ConcurrentHashMap.newKeySet<Int>()
    private val moveId: Int
    private val clearId: Int

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) ready.add(sampleId)
        }
        moveId = pool.load(context, R.raw.place, 1)
        clearId = pool.load(context, R.raw.clear, 1)
    }

    fun play(linesCleared: Boolean) {
        val id = if (linesCleared) clearId else moveId
        if (ready.contains(id)) {
            pool.play(id, if (linesCleared) 0.45f else 0.32f,
                if (linesCleared) 0.45f else 0.32f, 1, 0, 1f)
        }
    }

    fun release() {
        pool.release()
        ready.clear()
    }
}
