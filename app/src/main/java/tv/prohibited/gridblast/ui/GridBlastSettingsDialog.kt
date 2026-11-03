package tv.prohibited.gridblast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class OptionsPage { SETTINGS, HOW_TO_PLAY, PRIVACY }

@Composable
fun GridBlastSettingsDialog(
    hapticsEnabled: Boolean,
    motionEnabled: Boolean,
    soundEnabled: Boolean,
    onHapticsChanged: (Boolean) -> Unit,
    onMotionChanged: (Boolean) -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var page by remember { mutableStateOf(OptionsPage.SETTINGS) }
    fun navigateBack() {
        if (page == OptionsPage.SETTINGS) onDismiss() else page = OptionsPage.SETTINGS
    }

    AlertDialog(
        onDismissRequest = ::navigateBack,
        title = {
            Text(when (page) {
                OptionsPage.SETTINGS -> "Options"
                OptionsPage.HOW_TO_PLAY -> "How to play"
                OptionsPage.PRIVACY -> "Privacy policy"
            })
        },
        text = {
            when (page) {
                OptionsPage.HOW_TO_PLAY -> {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Fill rows or columns", fontWeight = FontWeight.Bold)
                        Text("Place the three shapes from your tray on the 8 by 8 board. Fill an entire row or column to clear it and score points. Rows and columns that cross can clear together.")
                        Text("Drag or tap", fontWeight = FontWeight.Bold)
                        Text("Drag a shape onto the board, or tap a shape to select it, then tap an outlined top-left placement cell. Red means a drag will not fit. You cannot rotate shapes.")
                        Text("Refill and game over", fontWeight = FontWeight.Bold)
                        Text("After you place all three tray shapes, you receive three new ones. Each new tray offers at least one legal move if any shape could still fit. The round ends when none of your available shapes can be placed.")
                        Text("Hint and Undo", fontWeight = FontWeight.Bold)
                        Text("HINT previews a free suggested move. Tap the highlighted cell or press PLACE to confirm; requesting a hint never spends points or a piece. UNDO restores your last successful placement once, even after game over.")
                        Text("Your progress", fontWeight = FontWeight.Bold)
                        Text("Your current round and best score save on this device. NEW starts another round; your best score stays.")
                    }
                }
                OptionsPage.PRIVACY -> {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("GridBlast privacy policy — ProhibitedTV")
                        Text("GridBlast does not collect, transmit, share or sell personal information. There are no ads, analytics, trackers, accounts or network access.")
                        Text("Your puzzle board, one-move undo, score, best score and options stay in the app's private storage on this device. This data is not sent to us or third parties.")
                        Text("You can delete local game data by clearing GridBlast storage in Android Settings or uninstalling the app. We do not keep a server copy.")
                        Text("For privacy inquiries, contact ProhibitedTV through GitHub issues at github.com/ProhibitedTV/GridBlast/issues.")
                        Text("This describes GridBlast itself; the Android operating system and app store may process their own service data under separate policies.")
                    }
                }
                OptionsPage.SETTINGS -> {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Sound effects")
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = onSoundChanged,
                                modifier = Modifier.testTag("sound-switch"),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Vibration")
                            Switch(
                                checked = hapticsEnabled,
                                onCheckedChange = onHapticsChanged,
                                modifier = Modifier.testTag("haptics-switch"),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Clear animations")
                            Switch(
                                checked = motionEnabled,
                                onCheckedChange = onMotionChanged,
                                modifier = Modifier.testTag("motion-switch"),
                            )
                        }
                        Text("Settings and progress stay on this device. No account or internet required.")
                        TextButton(
                            onClick = { page = OptionsPage.HOW_TO_PLAY },
                            modifier = Modifier.testTag("how-to-play-button"),
                        ) { Text("HOW TO PLAY") }
                        TextButton(
                            onClick = { page = OptionsPage.PRIVACY },
                            modifier = Modifier.testTag("privacy-button"),
                        ) { Text("PRIVACY POLICY") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = ::navigateBack,
                modifier = Modifier.testTag(
                    if (page == OptionsPage.SETTINGS) "options-done-button"
                    else if (page == OptionsPage.HOW_TO_PLAY) "how-to-play-back-button"
                    else "privacy-back-button",
                ),
            ) { Text(if (page == OptionsPage.SETTINGS) "DONE" else "BACK") }
        },
    )
}
