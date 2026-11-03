package tv.prohibited.gridblast.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import tv.prohibited.gridblast.R
import java.util.concurrent.ConcurrentHashMap

/**
 * Short original synthesizer-generated WAV effects. Uses local Android SoundPool
 * and the system's game/media volume. No streaming, external SDKs, or permissions.
 */
internal class GameSounds(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
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
        // An enabled toggle is not permission to play through silent/vibrate
        // mode or a muted media stream. Recheck for every effect so changing
        // the device volume takes effect immediately without reopening Options.
        val manager = audioManager ?: return
        if (!SoundPlaybackPolicy.allowsEffects(
                normalRingerMode = manager.ringerMode == AudioManager.RINGER_MODE_NORMAL,
                mediaVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC),
                mediaMuted = manager.isStreamMute(AudioManager.STREAM_MUSIC),
            )
        ) return

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

/** Pure, locally testable policy; never changes the device's audio settings. */
internal object SoundPlaybackPolicy {
    fun allowsEffects(normalRingerMode: Boolean, mediaVolume: Int, mediaMuted: Boolean): Boolean =
        normalRingerMode && mediaVolume > 0 && !mediaMuted
}
