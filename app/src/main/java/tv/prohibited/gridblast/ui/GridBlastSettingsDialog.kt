package tv.prohibited.gridblast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Options") },
        text = {
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
                    )
                }
                Text("Settings and progress stay on this device. No account or internet required.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("DONE") } },
    )
}
