package tv.prohibited.gridblast.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundPlaybackPolicyTest {
    @Test fun enabledEffectsPlayInNormalModeWithAudibleMediaVolume() {
        assertTrue(SoundPlaybackPolicy.allowsEffects(
            normalRingerMode = true, mediaVolume = 1, mediaMuted = false,
        ))
    }

    @Test fun silentOrVibrateModeSuppressesEffectsDespiteNonzeroMediaVolume() {
        assertFalse(SoundPlaybackPolicy.allowsEffects(
            normalRingerMode = false, mediaVolume = 7, mediaMuted = false,
        ))
    }

    @Test fun zeroMediaVolumeSuppressesEffectsInNormalMode() {
        assertFalse(SoundPlaybackPolicy.allowsEffects(
            normalRingerMode = true, mediaVolume = 0, mediaMuted = false,
        ))
    }

    @Test fun explicitMediaMuteSuppressesEffectsEvenIfSavedVolumeIsNonzero() {
        assertFalse(SoundPlaybackPolicy.allowsEffects(
            normalRingerMode = true, mediaVolume = 7, mediaMuted = true,
        ))
    }

    @Test fun returningToNormalAudibleModeAllowsEffectsWithoutChangingPreference() {
        assertFalse(SoundPlaybackPolicy.allowsEffects(false, 5, false))
        assertFalse(SoundPlaybackPolicy.allowsEffects(true, 0, false))
        assertTrue(SoundPlaybackPolicy.allowsEffects(true, 5, false))
    }
}
