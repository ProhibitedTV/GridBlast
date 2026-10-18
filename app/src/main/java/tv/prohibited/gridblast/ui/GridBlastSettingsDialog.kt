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
import androidx.compose.ui.unit.dp

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
    var showPrivacy by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = {
            if (showPrivacy) showPrivacy = false else onDismiss()
        },
        title = { Text(if (showPrivacy) "Privacy policy" else "Options") },
        text = {
            if (showPrivacy) {
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
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        onClick = { showPrivacy = true },
                        modifier = Modifier.testTag("privacy-button"),
                    ) { Text("PRIVACY POLICY") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (showPrivacy) showPrivacy = false else onDismiss()
                },
                modifier = Modifier.testTag(if (showPrivacy) "privacy-back-button" else "options-done-button"),
            ) { Text(if (showPrivacy) "BACK" else "DONE") }
        },
    )
}
