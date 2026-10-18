package tv.prohibited.gridblast

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
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
import tv.prohibited.gridblast.game.GameEngine
import tv.prohibited.gridblast.game.GameState
import tv.prohibited.gridblast.game.GameStateCodec

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

    /** Load a known legal board to test edges without relying on random pieces. */
    private fun loadFixture(shapeIds: List<Int>, occupied: Set<Int> = emptySet()) {
        val state = GameState(
            board = List(64) { if (it in occupied) 3 else 0 },
            tray = shapeIds.map { GameEngine.pieceFromId(it) },
        )
        compose.activityRule.scenario.onActivity {
            it.getSharedPreferences("gridblast", Context.MODE_PRIVATE)
                .edit().putString("session_v1", GameStateCodec.encode(state)).commit()
        }
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
    }

    @Test
    fun undoRestoresBoardAndTrayWithoutErasingBestScore() {
        loadFixture(listOf(0, 9, 1))
        compose.onNodeWithTag("undo-button").assertIsNotEnabled()
        compose.onNodeWithTag("tray-slot-0").performClick()
        compose.onNodeWithTag("board-cell-0-0").performClick()
        compose.onNodeWithTag("score-value").assertTextEquals("1")
        compose.onNodeWithTag("undo-button").assertIsEnabled().performClick()
        compose.onNodeWithTag("score-value").assertTextEquals("0")
        compose.onNodeWithTag("best-score-value").assertTextEquals("1")
        compose.onNodeWithTag("undo-button").assertIsNotEnabled()
        compose.onNodeWithTag("tray-slot-0")
            .assertContentDescriptionEquals("Select shape 1, 1 blocks, or drag onto the board")
        compose.onNodeWithTag("tray-slot-0").performClick()
        compose.onNodeWithTag("board-cell-1-1").assertIsEnabled().performClick()
        compose.onNodeWithTag("score-value").assertTextEquals("1")
        compose.onNodeWithTag("undo-button").assertIsEnabled()
    }

    @Test
    fun savedUndoSurvivesActivityRecreation() {
        loadFixture(listOf(0, 9, 1))
        compose.onNodeWithTag("tray-slot-0").performClick()
        compose.onNodeWithTag("board-cell-0-0").performClick()
        compose.onNodeWithTag("undo-button").assertIsEnabled()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("score-value").assertTextEquals("1")
        compose.onNodeWithTag("undo-button").assertIsEnabled().performClick()
        compose.onNodeWithTag("score-value").assertTextEquals("0")
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("undo-button").assertIsNotEnabled()
        compose.onNodeWithTag("score-value").assertTextEquals("0")
    }

    @Test
    fun undoIsAvailableFromGameOverDialog() {
        val blocked = (0 until 64).filter { (it / 8 + it % 8) % 2 != 0 }.toSet()
        // Square 2x2 cannot fit; after the final single-cell move, game ends.
        val state = GameState(
            board = List(64) { if (it in blocked) 3 else 0 },
            tray = listOf(
                GameEngine.pieceFromId(0), GameEngine.pieceFromId(9), null,
            ),
        )
        compose.activityRule.scenario.onActivity {
            it.getSharedPreferences("gridblast", Context.MODE_PRIVATE)
                .edit().putString("session_v1", GameStateCodec.encode(state))
                .remove("history_v1").commit()
        }
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("tray-slot-0").performClick()
        compose.onNodeWithTag("board-cell-0-0").performClick()
        compose.onNodeWithText("No more moves").assertExists()
        compose.onNodeWithTag("undo-gameover-button").performClick()
        compose.onNodeWithTag("score-value").assertTextEquals("0")
        compose.onNodeWithTag("undo-button").assertIsNotEnabled()
        compose.onNodeWithTag("tray-slot-0")
            .assertContentDescriptionEquals("Select shape 1, 1 blocks, or drag onto the board")
    }

    @Test
    fun headerAndBoardControlsStayOnScreen() {
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        val title = compose.onNodeWithTag("game-title").getUnclippedBoundsInRoot()
        val options = compose.onNodeWithTag("options-button").getUnclippedBoundsInRoot()
        val newGame = compose.onNodeWithTag("new-game-button").getUnclippedBoundsInRoot()
        val hint = compose.onNodeWithTag("hint-button").getUnclippedBoundsInRoot()
        val board = compose.onNodeWithTag("game-board").getUnclippedBoundsInRoot()
        val tray = compose.onNodeWithTag("tray-slot-2").getUnclippedBoundsInRoot()
        assertTrue("title and options cannot overlap", title.right <= options.left)
        assertTrue("options and new game cannot overlap", options.right <= newGame.left)
        assertTrue("toolbar must fit on screen", newGame.right <= root.right)
        assertTrue("hint must fit on screen", hint.right <= root.right)
        assertTrue("board must fit in viewport", board.left >= root.left && board.right <= root.right)
        assertTrue("tray must fit in viewport", tray.right <= root.right && tray.bottom <= root.bottom)
    }

    @Test
    fun suggestedMoveRequiresSeparatePlaceConfirmation() {
        loadFixture(listOf(0, 11, 1), occupied = (0..6).toSet())
        compose.onNodeWithTag("hint-button").performClick()
        compose.onNodeWithTag("place-hint-button")
            .assertContentDescriptionEquals(
                "Place suggested shape 1 at row 1, column 8 for 81 points",
            )
        compose.onNodeWithTag("score-value").assertTextEquals("0")
        compose.onNodeWithTag("undo-button").assertIsNotEnabled()
        compose.onNodeWithTag("place-hint-button").performClick()
        compose.onNodeWithTag("score-value").assertTextEquals("81")
        compose.onNodeWithTag("undo-button").assertIsEnabled()
        compose.onNodeWithTag("hint-button").assertExists()
        compose.onNodeWithTag("tray-slot-0")
            .assertContentDescriptionEquals("Empty shape slot")
    }

    @Test
    fun manuallySelectingPieceCancelsSuggestedPlaceAction() {
        loadFixture(listOf(0, 11, 1), occupied = (0..6).toSet())
        compose.onNodeWithTag("hint-button").performClick()
        compose.onNodeWithTag("place-hint-button").assertExists()
        compose.onNodeWithTag("tray-slot-1").performClick()
        compose.onNodeWithTag("hint-button").assertExists()
        compose.onNodeWithTag("place-hint-button").assertDoesNotExist()
        compose.onNodeWithTag("score-value").assertTextEquals("0")
    }

    @Test
    fun suggestedPlacementDoesNotPersistAcrossRestart() {
        loadFixture(listOf(0, 11, 1), occupied = (0..6).toSet())
        compose.onNodeWithTag("hint-button").performClick()
        compose.onNodeWithTag("place-hint-button").assertExists()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("place-hint-button").assertDoesNotExist()
        compose.onNodeWithTag("hint-button").assertContentDescriptionEquals(
            "Suggest a free legal move",
        )
        compose.onNodeWithTag("score-value").assertTextEquals("0")
    }

    @Test
    fun hintSuggestsRowClearWithoutSpendingPointsOrMovingPiece() {
        loadFixture(listOf(0, 11, 1), occupied = (0..6).toSet())
        compose.onNodeWithTag("hint-button").performClick()
        compose.onNodeWithTag("status-message")
            .assertTextEquals("HINT: row 1, column 8 (+81)")
        compose.onNodeWithTag("score-value").assertTextEquals("0")
        compose.onNodeWithTag("board-cell-0-7").assertIsEnabled().performClick()
        compose.onNodeWithTag("score-value").assertTextEquals("81")
        compose.onNodeWithTag("tray-slot-0")
            .assertContentDescriptionEquals("Empty shape slot")
    }

    @Test
    fun hintDoesNotPersistOrAlterBoardWhenActivityRestarts() {
        loadFixture(listOf(0, 11, 1), occupied = (0..6).toSet())
        compose.onNodeWithTag("hint-button").performClick()
        compose.onNodeWithTag("status-message")
            .assertTextEquals("HINT: row 1, column 8 (+81)")
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("score-value").assertTextEquals("0")
        compose.onNodeWithTag("board-cell-0-7").assertIsNotEnabled()
        compose.onNodeWithTag("tray-slot-0").performClick()
        compose.onNodeWithTag("board-cell-0-7").assertIsEnabled().performClick()
        compose.onNodeWithTag("score-value").assertTextEquals("81")
    }

    @Test
    fun oversizedPieceRejectsBottomRightButFitsAtFiveFive() {
        loadFixture(listOf(11, 0, 1)) // Shape 11 is a full 3x3 square.
        compose.onNodeWithTag("tray-slot-0").performClick()
        compose.onNodeWithTag("board-cell-7-7").assertIsNotEnabled()
        compose.onNodeWithTag("score-value").assertTextEquals("0")
        compose.onNodeWithTag("board-cell-5-5").assertIsEnabled().performClick()
        compose.onNodeWithTag("tray-slot-0")
            .assertContentDescriptionEquals("Empty shape slot")
        compose.onNodeWithTag("score-value").assertTextEquals("9")
    }

    @Test
    fun occupiedCellRejectsPlacementWithoutConsumingThePiece() {
        loadFixture(listOf(0, 1, 11), occupied = setOf(0))
        compose.onNodeWithTag("tray-slot-0").performClick()
        compose.onNodeWithTag("board-cell-0-0").assertIsNotEnabled()
        compose.onNodeWithTag("score-value").assertTextEquals("0")
        compose.onNodeWithTag("board-cell-0-1").assertIsEnabled().performClick()
        compose.onNodeWithTag("tray-slot-0")
            .assertContentDescriptionEquals("Empty shape slot")
        compose.onNodeWithTag("score-value").assertTextEquals("1")
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
    fun privacyPolicyIsReadableOfflineFromOptions() {
        compose.onNodeWithTag("options-button").performClick()
        compose.onNodeWithTag("privacy-button").performClick()
        compose.onNodeWithText("GridBlast privacy policy — ProhibitedTV").assertExists()
        compose.onNodeWithText(
            "GridBlast does not collect, transmit, share or sell personal information. There are no ads, analytics, trackers, accounts or network access.",
        ).assertExists()
        compose.onNodeWithTag("privacy-back-button").performClick()
        compose.onNodeWithTag("sound-switch").assertExists()
        compose.onNodeWithTag("options-done-button").performClick()
        compose.onNodeWithTag("options-button").assertExists()
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
