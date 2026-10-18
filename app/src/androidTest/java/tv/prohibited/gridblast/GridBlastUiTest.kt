package tv.prohibited.gridblast

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Android UI smoke tests. The app is exercised through its actual
 * Compose semantics and touches: no mocked engine and no network calls.
 */
@RunWith(AndroidJUnit4::class)
class GridBlastUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun freshGame() {
        compose.activityRule.scenario.onActivity {
            it.getSharedPreferences("gridblast", Context.MODE_PRIVATE).edit().clear().commit()
        }
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
    }

    @Test
    fun tappingPieceAndBoardPersistsAfterActivityRecreation() {
        compose.onNodeWithTag("score-value").assertTextEquals("0")
        compose.onNodeWithTag("tray-slot-0").performClick()
        compose.onNodeWithTag("board-cell-0-0").performClick()
        compose.onNodeWithTag("tray-slot-0")
            .assertContentDescriptionEquals("Empty shape slot")

        val score = compose.activity.getSharedPreferences("gridblast", Context.MODE_PRIVATE)
            .getInt("best_score", 0)
        assertTrue(score > 0)
        compose.onNodeWithTag("score-value").assertTextEquals(score.toString())

        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("score-value").assertTextEquals(score.toString())
        compose.onNodeWithTag("tray-slot-0")
            .assertContentDescriptionEquals("Empty shape slot")
    }

    @Test
    fun soundPreferenceDefaultsOffAndSurvivesRecreation() {
        compose.onNodeWithTag("options-button").performClick()
        compose.onNodeWithTag("sound-switch").assertIsOff().performClick()
        compose.onNodeWithTag("sound-switch").assertIsOn()
        compose.onNodeWithTag("motion-switch").assertIsOn().performClick()
        compose.onNodeWithTag("motion-switch").assertIsOff()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("options-button").performClick()
        compose.onNodeWithTag("sound-switch").assertIsOn()
        compose.onNodeWithTag("motion-switch").assertIsOff()
    }

    @Test
    fun restartRequiresConfirmationAfterScoring() {
        compose.onNodeWithTag("tray-slot-0").performClick()
        compose.onNodeWithTag("board-cell-0-0").performClick()
        compose.onNodeWithTag("new-game-button").performClick()
        compose.onNodeWithTag("cancel-restart").performClick()
        val score = compose.activity.getSharedPreferences("gridblast", Context.MODE_PRIVATE)
            .getInt("best_score", 0)
        compose.onNodeWithTag("score-value").assertTextEquals(score.toString())
        compose.onNodeWithTag("new-game-button").performClick()
        compose.onNodeWithTag("confirm-restart").performClick()
        compose.onNodeWithTag("score-value").assertTextEquals("0")
        compose.onNodeWithTag("best-score-value").assertTextEquals(score.toString())
    }

    @Test
    fun draggingCenteredPiecePlacesItOnBoard() {
        val trayBounds = compose.onNodeWithTag("tray-slot-0").getUnclippedBoundsInRoot()
        val boardBounds = compose.onNodeWithTag("game-board").getUnclippedBoundsInRoot()
        val targetX = with(compose.density) {
            ((boardBounds.left + boardBounds.right) / 2 - trayBounds.left).toPx()
        }
        val targetY = with(compose.density) {
            ((boardBounds.top + boardBounds.bottom) / 2 + 56.dp - trayBounds.top).toPx()
        }
        compose.onNodeWithTag("tray-slot-0").performTouchInput {
            swipe(start = center, end = Offset(targetX, targetY), durationMillis = 900)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("tray-slot-0")
            .assertContentDescriptionEquals("Empty shape slot")
        val score = compose.activity.getSharedPreferences("gridblast", Context.MODE_PRIVATE)
            .getInt("best_score", 0)
        assertTrue(score > 0)
    }
}
