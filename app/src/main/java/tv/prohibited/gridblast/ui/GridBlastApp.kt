package tv.prohibited.gridblast.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.prohibited.gridblast.game.GameEngine
import tv.prohibited.gridblast.game.Piece

private val backgroundColor = Color(0xFF10172A)
private val panelColor = Color(0xFF202A44)
private val emptyCellColor = Color(0xFF303D5A)
private val highlightColor = Color(0xFF7CE0E2)
private val shapeColors = listOf(
    Color(0xFF000000),
    Color(0xFF64B5F6),
    Color(0xFFFFBE6B),
    Color(0xFFAFA1FF),
    Color(0xFFFF7F9D),
    Color(0xFF74E0B9),
    Color(0xFFFFDA74),
)

private fun pieceColor(id: Int): Color = shapeColors.getOrElse(id) { highlightColor }

@Composable
fun GridBlastApp() {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("gridblast", Context.MODE_PRIVATE)
    }
    var game by remember { mutableStateOf(GameEngine.newGame()) }
    var selectedSlot by remember { mutableIntStateOf(-1) }
    var bestScore by remember { mutableIntStateOf(preferences.getInt("best_score", 0)) }

    val resetGame = {
        game = GameEngine.newGame()
        selectedSlot = -1
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = highlightColor,
            background = backgroundColor,
            surface = panelColor,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = backgroundColor,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "GRIDBLAST",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = highlightColor,
                    )
                    TextButton(onClick = resetGame) { Text("NEW GAME") }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ScorePanel(label = "SCORE", value = game.score, modifier = Modifier.weight(1f))
                    ScorePanel(label = "BEST", value = bestScore, modifier = Modifier.weight(1f))
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    val boardSize = minOf(maxWidth, maxHeight)
                    Board(
                        board = game.board,
                        modifier = Modifier.size(boardSize),
                        onCellTapped = { row, col ->
                            if (selectedSlot >= 0 && !game.gameOver) {
                                val next = GameEngine.place(game, selectedSlot, row, col)
                                if (next != game) {
                                    game = next
                                    selectedSlot = -1
                                    if (next.score > bestScore) {
                                        bestScore = next.score
                                        preferences.edit().putInt("best_score", bestScore).apply()
                                    }
                                }
                            }
                        },
                    )
                }

                Text(
                    text = if (selectedSlot >= 0) "Tap a grid cell to place your shape" else "Choose a shape to place",
                    color = Color(0xFFCAD5E8),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(GameEngine.TRAY_SIZE) { slot ->
                        val piece = game.tray.getOrNull(slot)
                        val selected = slot == selectedSlot
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(104.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) Color(0xFF345C69) else panelColor)
                                .border(
                                    2.dp,
                                    if (selected) highlightColor else Color.Transparent,
                                    RoundedCornerShape(14.dp),
                                )
                                .clickable(enabled = piece != null && !game.gameOver) {
                                    selectedSlot = if (selected) -1 else slot
                                }
                                .semantics {
                                    contentDescription = when {
                                        piece == null -> "Empty shape slot"
                                        selected -> "Selected shape ${slot + 1}"
                                        else -> "Select shape ${slot + 1}"
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            PiecePreview(piece)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        if (game.gameOver) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("No more moves") },
                text = { Text("Final score: ${game.score}. Ready for another round?") },
                confirmButton = {
                    TextButton(onClick = resetGame) { Text("PLAY AGAIN") }
                },
            )
        }
    }
}

@Composable
private fun ScorePanel(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(panelColor)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 12.sp, color = Color(0xFFA9B9D4), fontWeight = FontWeight.SemiBold)
        Text(value.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun Board(
    board: List<Int>,
    modifier: Modifier,
    onCellTapped: (Int, Int) -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(panelColor)
            .padding(4.dp),
    ) {
        repeat(GameEngine.SIZE) { row ->
            Row(Modifier.weight(1f)) {
                repeat(GameEngine.SIZE) { col ->
                    val id = board[row * GameEngine.SIZE + col]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(2.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (id == 0) emptyCellColor else pieceColor(id))
                            .clickable { onCellTapped(row, col) }
                            .semantics {
                                contentDescription = "Row ${row + 1}, column ${col + 1}, " +
                                    if (id == 0) "empty" else "filled"
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun PiecePreview(piece: Piece?) {
    if (piece == null) {
        Text("USED", fontSize = 12.sp, color = Color(0xFF8A99B0))
        return
    }
    val rows = piece.cells.maxOf { it.row } + 1
    val cols = piece.cells.maxOf { it.col } + 1
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        repeat(rows) { row ->
            Row {
                repeat(cols) { col ->
                    val filled = piece.cells.any { it.row == row && it.col == col }
                    Box(
                        modifier = Modifier
                            .size(17.dp)
                            .padding(1.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (filled) pieceColor(piece.colorId) else Color.Transparent),
                    )
                }
            }
        }
    }
}
