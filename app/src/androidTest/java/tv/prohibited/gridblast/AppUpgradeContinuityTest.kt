package tv.prohibited.gridblast

import android.content.Context
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tv.prohibited.gridblast.game.GameEngine
import tv.prohibited.gridblast.game.GameHistory
import tv.prohibited.gridblast.game.GameHistoryCodec
import tv.prohibited.gridblast.game.GameState
import tv.prohibited.gridblast.game.GameStateCodec

/**
 * Run by the host installer in TWO independent instrumentation invocations:
 *
 * A signed debug APK at versionCode A is installed, `prepare` plays through
 * the real UI and checks persisted state. The host installs a distinct APK
 * with versionCode B > A and the SAME signing certificate using adb install -r,
 * without pm clear/uninstall. Then `verify` checks the saved state and Undo.
 *
 * These tests skip when run in ordinary connectedDebugAndroidTest so that a
 * single-process test can never appear to have proved install-over-install.
 */
@RunWith(AndroidJUnit4::class)
class AppUpgradeContinuityTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun onlyPhase(name: String) {
        assumeTrue(
            "Requires two separately built and installed APK versions",
            InstrumentationRegistry.getArguments().getString("upgrade_phase") == name,
        )
    }

    private fun fixture() = GameState(
        board = List(64) { 0 },
        tray = listOf(
            GameEngine.pieceFromId(0),
            GameEngine.pieceFromId(9),
            GameEngine.pieceFromId(1),
        ),
        score = 0,
    )

    private fun expectedHistory(): GameHistory {
        val initial = fixture()
        return GameHistory(GameEngine.place(initial, 0, 0, 0, Random(123)), initial)
    }

    @Test
    fun prepareGameAndPreferencesOnVersionA() {
        onlyPhase("prepare")
        val initial = fixture()
        compose.activityRule.scenario.onActivity {
            assertTrue(
                it.getSharedPreferences("gridblast", Context.MODE_PRIVATE).edit()
                    .clear().putString("session_v1", GameStateCodec.encode(initial)).commit()
            )
        }
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()

        compose.onNodeWithTag("tray-slot-0").performClick()
        compose.onNodeWithTag("board-cell-0-0").performClick()
        compose.onNodeWithTag("score-value").assertTextEquals("1")
        compose.onNodeWithTag("undo-button").assertIsEnabled()
        compose.onNodeWithTag("options-button").performClick()
        compose.onNodeWithTag("sound-switch").assertIsOff().performClick()
        compose.onNodeWithTag("sound-switch").assertIsOn()
        compose.onNodeWithTag("motion-switch").assertIsOn().performClick()
        compose.onNodeWithTag("motion-switch").assertIsOff()
        compose.onNodeWithTag("haptics-switch").assertIsOn().performClick()
        compose.onNodeWithTag("haptics-switch").assertIsOff()
        compose.onNodeWithTag("options-done-button").performClick()
        compose.waitForIdle()

        val prefs = compose.activity.getSharedPreferences("gridblast", Context.MODE_PRIVATE)
        assertEquals(expectedHistory(), GameHistoryCodec.decode(prefs.getString("history_v1", null)))
        assertEquals(expectedHistory().current, GameStateCodec.decode(prefs.getString("session_v1", null)))
        assertEquals(1, prefs.getInt("best_score", -1))
        assertEquals(true, prefs.getBoolean("sound_enabled", false))
        assertEquals(false, prefs.getBoolean("motion_enabled", true))
        assertEquals(false, prefs.getBoolean("haptics_enabled", true))
    }

    @Test
    fun verifyProgressAndUndoOnVersionB() {
        onlyPhase("verify")
        val prefs = compose.activity.getSharedPreferences("gridblast", Context.MODE_PRIVATE)
        assertEquals(expectedHistory(), GameHistoryCodec.decode(prefs.getString("history_v1", null)))
        assertEquals(expectedHistory().current, GameStateCodec.decode(prefs.getString("session_v1", null)))
        assertEquals(1, prefs.getInt("best_score", -1))
        assertEquals(true, prefs.getBoolean("sound_enabled", false))
        assertEquals(false, prefs.getBoolean("motion_enabled", true))
        assertEquals(false, prefs.getBoolean("haptics_enabled", true))

        compose.onNodeWithTag("score-value").assertTextEquals("1")
        compose.onNodeWithTag("best-score-value").assertTextEquals("1")
        compose.onNodeWithTag("tray-slot-0").assertContentDescriptionEquals("Empty shape slot")
        compose.onNodeWithTag("undo-button").assertIsEnabled()
        compose.onNodeWithTag("options-button").performClick()
        compose.onNodeWithTag("sound-switch").assertIsOn()
        compose.onNodeWithTag("motion-switch").assertIsOff()
        compose.onNodeWithTag("haptics-switch").assertIsOff()
        compose.onNodeWithTag("options-done-button").performClick()

        compose.onNodeWithTag("undo-button").assertIsEnabled().performClick()
        compose.onNodeWithTag("score-value").assertTextEquals("0")
        compose.onNodeWithTag("best-score-value").assertTextEquals("1")
        compose.onNodeWithTag("undo-button").assertIsNotEnabled()
        assertEquals(GameHistory(fixture()), GameHistoryCodec.decode(prefs.getString("history_v1", null)))
        assertEquals(1, prefs.getInt("best_score", -1))
    }
}
