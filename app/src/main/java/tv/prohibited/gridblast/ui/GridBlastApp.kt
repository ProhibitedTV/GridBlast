package tv.prohibited.gridblast.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import kotlin.math.roundToInt
import tv.prohibited.gridblast.game.Cell
import tv.prohibited.gridblast.game.GameEngine
import tv.prohibited.gridblast.game.GameState
import tv.prohibited.gridblast.game.GameStateCodec
import tv.prohibited.gridblast.game.Piece

private val backgroundColor = Color(0xFF10172A)
private val panelColor = Color(0xFF202A44)
private val emptyCellColor = Color(0xFF303D5A)
private val highlightColor = Color(0xFF7CE0E2)
private val invalidColor = Color(0xFFED687A)
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
private data class ActiveDrag(val slot: Int, val point: Offset)

/** Convert absolute window pointer coordinates to the game's grid. */
private fun gridCell(point: Offset, bounds: Rect): Cell? {
    if (bounds.width <= 0f || bounds.height <= 0f || !bounds.contains(point)) return null
    val row = floor((point.y - bounds.top) * GameEngine.SIZE / bounds.height).toInt()
    val col = floor((point.x - bounds.left) * GameEngine.SIZE / bounds.width).toInt()
    if (row !in 0 until GameEngine.SIZE || col !in 0 until GameEngine.SIZE) return null
    return Cell(row, col)
}

@Composable
fun GridBlastApp() {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("gridblast", Context.MODE_PRIVATE)
    }
    val haptics = LocalHapticFeedback.current
    var game by remember(preferences) {
        mutableStateOf(
            GameStateCodec.decode(preferences.getString("session_v1", null))
                ?: GameEngine.newGame(),
        )
    }
    var bestScore by remember(preferences) {
        mutableIntStateOf(maxOf(game.score, preferences.getInt("best_score", 0)))
    }
    var selectedSlot by remember { mutableIntStateOf(-1) }
    var activeDrag by remember { mutableStateOf<ActiveDrag?>(null) }
    var boardBounds by remember { mutableStateOf(Rect.Zero) }
    var rootPosition by remember { mutableStateOf(Offset.Zero) }
    var confirmRestart by remember { mutableStateOf(false) }

    fun persist(updated: GameState) {
        val updatedBest = maxOf(bestScore, updated.score)
        bestScore = updatedBest
        preferences.edit()
            .putString("session_v1", GameStateCodec.encode(updated))
            .putInt("best_score", updatedBest)
            .apply()
    }

    fun resetGame() {
        game = GameEngine.newGame()
        selectedSlot = -1
        activeDrag = null
        confirmRestart = false
        persist(game)
    }

    fun place(slot: Int, row: Int, col: Int) {
        val updated = GameEngine.place(game, slot, row, col)
        if (updated != game) {
            game = updated
            selectedSlot = -1
            activeDrag = null
            persist(updated)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val draggedPiece = activeDrag?.slot?.let { game.tray.getOrNull(it) }
    val dragAnchor = activeDrag?.let { gridCell(it.point, boardBounds) }
    val validPreview = dragAnchor != null && draggedPiece != null &&
        GameEngine.canPlace(game.board, draggedPiece, dragAnchor.row, dragAnchor.col)

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = highlightColor,
            background = backgroundColor,
            surface = panelColor,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = backgroundColor) {
            Box(
                modifier = Modifier.fillMaxSize().onGloballyPositioned {
                    rootPosition = it.positionInWindow()
                },
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
                        TextButton(onClick = {
                            if (game.score > 0) confirmRestart = true else resetGame()
                        }) { Text("NEW GAME") }
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
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        val boardSize = minOf(maxWidth, maxHeight)
                        Board(
                            board = game.board,
                            previewPiece = draggedPiece,
                            previewCell = dragAnchor,
                            validPreview = validPreview,
                            modifier = Modifier
                                .size(boardSize)
                                .onGloballyPositioned { boardBounds = it.boundsInWindow() },
                            onCellTapped = { row, col ->
                                if (selectedSlot >= 0 && !game.gameOver) place(selectedSlot, row, col)
                            },
                        )
                    }

                    Text(
                        text = when {
                            activeDrag != null -> "Drop your shape onto the board"
                            selectedSlot >= 0 -> "Tap a cell to place, or drag your shape"
                            else -> "Drag a shape to the board, or tap to select"
                        },
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
                            TrayPiece(
                                piece = piece,
                                slot = slot,
                                selected = selectedSlot == slot,
                                enabled = !game.gameOver,
                                modifier = Modifier.weight(1f),
                                onSelect = { selectedSlot = if (selectedSlot == slot) -1 else slot },
                                onDragPoint = { point ->
                                    selectedSlot = slot
                                    activeDrag = ActiveDrag(slot, point)
                                },
                                onDrop = { point ->
                                    val target = gridCell(point, boardBounds)
                                    if (target != null) place(slot, target.row, target.col)
                                    activeDrag = null
                                },
                                onCancel = { activeDrag = null },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // A lifted visual follows the finger; the board also previews occupied cells.
                val drag = activeDrag
                if (drag != null && draggedPiece != null) {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (drag.point.x - rootPosition.x - 44.dp.toPx()).roundToInt(),
                                    (drag.point.y - rootPosition.y - 100.dp.toPx()).roundToInt(),
                                )
                            }
                            .size(88.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(panelColor.copy(alpha = 0.94f))
                            .border(2.dp, if (validPreview) highlightColor else invalidColor, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        PiecePreview(draggedPiece, 19.dp)
                    }
                }
            }
        }

        if (confirmRestart) {
            AlertDialog(
                onDismissRequest = { confirmRestart = false },
                title = { Text("Start a new game?") },
                text = { Text("Your current score will be reset. Your best score will be kept.") },
                confirmButton = { TextButton(onClick = ::resetGame) { Text("RESTART") } },
                dismissButton = { TextButton(onClick = { confirmRestart = false }) { Text("CANCEL") } },
            )
        }
        if (game.gameOver && !confirmRestart) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("No more moves") },
                text = { Text("Final score: ${game.score}. Ready for another round?") },
                confirmButton = { TextButton(onClick = ::resetGame) { Text("PLAY AGAIN") } },
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
    previewPiece: Piece?,
    previewCell: Cell?,
    validPreview: Boolean,
    modifier: Modifier,
    onCellTapped: (Int, Int) -> Unit,
) {
    val previewCells = if (previewPiece != null && previewCell != null) {
        previewPiece.cells.map { Cell(it.row + previewCell.row, it.col + previewCell.col) }.toSet()
    } else emptySet()

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
                    val isPreview = Cell(row, col) in previewCells
                    val fill = when {
                        isPreview && !validPreview -> invalidColor.copy(alpha = 0.78f)
                        isPreview && validPreview -> pieceColor(previewPiece!!.colorId).copy(alpha = 0.72f)
                        id != 0 -> pieceColor(id)
                        else -> emptyCellColor
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(2.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(fill)
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
private fun TrayPiece(
    piece: Piece?,
    slot: Int,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onSelect: () -> Unit,
    onDragPoint: (Offset) -> Unit,
    onDrop: (Offset) -> Unit,
    onCancel: () -> Unit,
) {
    var windowOrigin by remember { mutableStateOf(Offset.Zero) }
    val currentDrag by rememberUpdatedState(onDragPoint)
    val currentDrop by rememberUpdatedState(onDrop)
    val currentCancel by rememberUpdatedState(onCancel)
    Box(
        modifier = modifier
            .height(104.dp)
            .onGloballyPositioned { windowOrigin = it.positionInWindow() }
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFF345C69) else panelColor)
            .border(
                2.dp,
                if (selected) highlightColor else Color.Transparent,
                RoundedCornerShape(14.dp),
            )
            .clickable(enabled = enabled && piece != null, onClick = onSelect)
            .pointerInput(piece, enabled) {
                if (enabled && piece != null) {
                    var finger = Offset.Zero
                    detectDragGestures(
                        onDragStart = { start ->
                            finger = windowOrigin + start
                            currentDrag(finger)
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            finger += amount
                            currentDrag(finger)
                        },
                        onDragEnd = { currentDrop(finger) },
                        onDragCancel = { currentCancel() },
                    )
                }
            }
            .semantics {
                contentDescription = when {
                    piece == null -> "Empty shape slot"
                    selected -> "Selected shape ${slot + 1}. Drag or tap to place."
                    else -> "Select shape ${slot + 1}, or drag it onto the board"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        PiecePreview(piece)
    }
}

@Composable
private fun PiecePreview(piece: Piece?, blockSize: Dp = 17.dp) {
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
                            .size(blockSize)
                            .padding(1.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (filled) pieceColor(piece.colorId) else Color.Transparent),
                    )
                }
            }
        }
    }
}
