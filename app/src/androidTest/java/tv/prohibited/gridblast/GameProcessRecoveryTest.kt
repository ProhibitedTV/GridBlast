package tv.prohibited.gridblast

import android.content.Context
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
 * A two-invocation, actual-process test. CI runs prepare(), force-stops the
 * application package with Android ActivityManager, then runs verify() in a
 * separate instrumentation invocation. No mocking and no debug-only app code.
 *
 * Both tests are skipped in the ordinary connectedDebugAndroidTest suite;
 * running just one of them without the CI orchestration proves nothing.
 */
@RunWith(AndroidJUnit4::class)
class GameProcessRecoveryTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun fixture(): GameState = GameState(
        board = List(64) { 0 },
        tray = listOf(
            GameEngine.pieceFromId(0),
            GameEngine.pieceFromId(9),
            GameEngine.pieceFromId(1),
        ),
        score = 0,
    )

    private fun expectedHistory(): GameHistory {
        val previous = fixture()
        val current = GameEngine.place(previous, 0, 0, 0, Random(1234))
        return GameHistory(current, previous)
    }

    private fun phaseOnly(phase: String) {
        assumeTrue(
            "Only executed by the emulator's two-process save/recovery script",
            InstrumentationRegistry.getArguments().getString("recovery_phase") == phase,
        )
    }

    @Test
    fun prepareSavedTurnForForceStop() {
        phaseOnly("prepare")
        val previous = fixture()
        compose.activityRule.scenario.onActivity {
            val prefs = it.getSharedPreferences("gridblast", Context.MODE_PRIVATE)
            assertTrue(
                "Fixture must be committed to private storage",
                prefs.edit().clear().putString("session_v1", GameStateCodec.encode(previous)).commit(),
            )
        }
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("score-value").assertTextEquals("0")
        compose.onNodeWithTag("tray-slot-0").performClick()
        compose.onNodeWithTag("board-cell-0-0").performClick()
        compose.onNodeWithTag("score-value").assertTextEquals("1")
        compose.onNodeWithTag("undo-button").assertIsEnabled()

        // Read the real on-device SharedPreferences that the UI wrote.
        val prefs = compose.activity.getSharedPreferences("gridblast", Context.MODE_PRIVATE)
        assertEquals(expectedHistory(), GameHistoryCodec.decode(prefs.getString("history_v1", null)))
        assertEquals(expectedHistory().current, GameStateCodec.decode(prefs.getString("session_v1", null)))
        assertEquals(1, prefs.getInt("best_score", -1))
    }

    @Test
    fun verifySavedTurnAfterForceStopAndUndo() {
        phaseOnly("verify")
        // This activity is created by an entirely separate instrumentation
        // invocation, AFTER the shell's "am force-stop" process kill.
        val prefs = compose.activity.getSharedPreferences("gridblast", Context.MODE_PRIVATE)
        assertNotNull("Force-stopped session must still exist", prefs.getString("history_v1", null))
        assertEquals(expectedHistory(), GameHistoryCodec.decode(prefs.getString("history_v1", null)))
        assertEquals(expectedHistory().current, GameStateCodec.decode(prefs.getString("session_v1", null)))
        assertEquals(1, prefs.getInt("best_score", -1))

        compose.onNodeWithTag("score-value").assertTextEquals("1")
        compose.onNodeWithTag("best-score-value").assertTextEquals("1")
        compose.onNodeWithTag("tray-slot-0")
            .assertContentDescriptionEquals("Empty shape slot")
        compose.onNodeWithTag("undo-button").assertIsEnabled().performClick()

        compose.onNodeWithTag("score-value").assertTextEquals("0")
        compose.onNodeWithTag("best-score-value").assertTextEquals("1")
        compose.onNodeWithTag("undo-button").assertIsNotEnabled()
        assertEquals(GameHistory(fixture()), GameHistoryCodec.decode(prefs.getString("history_v1", null)))
        assertEquals(1, prefs.getInt("best_score", -1))
    }
}
