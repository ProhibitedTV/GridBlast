package tv.prohibited.gridblast.ui

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import tv.prohibited.gridblast.game.Cell
import tv.prohibited.gridblast.game.DragPlacement
import tv.prohibited.gridblast.game.GameEngine
import tv.prohibited.gridblast.game.GameHistory
import tv.prohibited.gridblast.game.GameHistoryCodec
import tv.prohibited.gridblast.game.GameState
import tv.prohibited.gridblast.game.GameStateCodec
import tv.prohibited.gridblast.game.MoveHint
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

// The 88dp floating ghost is drawn 100dp above the finger, so its center
// (the placement reference) sits 56dp above the touch point.
private val liftOffset = 56.dp
private val boardPadding = 4.dp

@Composable
fun GridBlastApp() {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("gridblast", Context.MODE_PRIVATE)
    }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val liftPx = with(density) { liftOffset.toPx() }
    val insetPx = with(density) { boardPadding.toPx() }
    val sounds = remember(context) { GameSounds(context.applicationContext) }
    DisposableEffect(sounds) {
        onDispose { sounds.release() }
    }
    // History and the current board are saved as one atomic preference value.
    // Legacy alpha installs continue to load their existing session_v1 snapshot.
    var history by remember(preferences) {
        mutableStateOf(
            GameHistoryCodec.decode(preferences.getString("history_v1", null))
                ?: GameHistory(
                    GameStateCodec.decode(preferences.getString("session_v1", null))
                        ?: GameEngine.newGame(),
                ),
        )
    }
    val game = history.current
    var bestScore by remember(preferences) {
        mutableIntStateOf(maxOf(game.score, preferences.getInt("best_score", 0)))
    }
    var selectedSlot by remember { mutableIntStateOf(-1) }
    var suggestedMove by remember { mutableStateOf<MoveHint?>(null) }
    var activeDrag by remember { mutableStateOf<ActiveDrag?>(null) }
    var boardBounds by remember { mutableStateOf(Rect.Zero) }
    var rootPosition by remember { mutableStateOf(Offset.Zero) }
    var confirmRestart by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var hapticsEnabled by remember(preferences) {
        mutableStateOf(preferences.getBoolean("haptics_enabled", true))
    }
    var soundEnabled by remember(preferences) {
        mutableStateOf(preferences.getBoolean("sound_enabled", false))
    }
    var motionEnabled by remember(preferences) {
        mutableStateOf(preferences.getBoolean("motion_enabled", true))
    }
    var flashCells by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var clearMessage by remember { mutableStateOf("") }
    var clearSequence by remember { mutableIntStateOf(0) }
    val clearFlash = remember { Animatable(0f) }

    LaunchedEffect(clearSequence, motionEnabled) {
        if (clearSequence > 0 && motionEnabled && flashCells.isNotEmpty()) {
            clearFlash.snapTo(0.90f)
            clearFlash.animateTo(0f, animationSpec = tween(durationMillis = 380))
            flashCells = emptySet()
        } else {
            clearFlash.snapTo(0f)
            flashCells = emptySet()
        }
    }
    LaunchedEffect(clearSequence) {
        if (clearSequence > 0) {
            delay(1000)
            clearMessage = ""
        }
    }

    fun persist(updated: GameHistory) {
        val updatedBest = maxOf(bestScore, updated.current.score)
        bestScore = updatedBest
        preferences.edit()
            .putString("history_v1", GameHistoryCodec.encode(updated))
            // Mirror the current game for compatibility with earlier alphas.
            .putString("session_v1", GameStateCodec.encode(updated.current))
            .putInt("best_score", updatedBest)
            .apply()
    }

    fun resetGame() {
        history = GameHistory.newGame()
        selectedSlot = -1
        suggestedMove = null
        activeDrag = null
        confirmRestart = false
        flashCells = emptySet()
        clearMessage = ""
        persist(history)
    }

    fun undoLast() {
        if (history.previous == null) return
        history = history.undoLast()
        selectedSlot = -1
        suggestedMove = null
        activeDrag = null
        flashCells = emptySet()
        clearMessage = ""
        persist(history)
    }

    fun place(slot: Int, row: Int, col: Int) {
        val analysis = GameEngine.analyze(game, slot, row, col) ?: return
        val updated = history.place(slot, row, col)
        if (updated != history) {
            history = updated
            selectedSlot = -1
            suggestedMove = null
            activeDrag = null
            persist(updated)
            if (analysis.linesCleared > 0) {
                flashCells = if (motionEnabled) analysis.clearedIndices else emptySet()
                clearMessage = "${analysis.linesCleared} ${if (analysis.linesCleared == 1) "LINE" else "LINES"} CLEARED  •  +${analysis.points}"
                clearSequence++
            }
            if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (soundEnabled) sounds.play(analysis.linesCleared > 0)
        }
    }

    fun dragTarget(slot: Int, point: Offset): Cell? {
        val piece = game.tray.getOrNull(slot) ?: return null
        return DragPlacement.target(
            fingerX = point.x, fingerY = point.y,
            boardLeft = boardBounds.left, boardTop = boardBounds.top,
            boardWidth = boardBounds.width, boardHeight = boardBounds.height,
            pieceRows = piece.cells.maxOf { it.row } + 1,
            pieceCols = piece.cells.maxOf { it.col } + 1,
            liftPx = liftPx, insetPx = insetPx,
        )
    }

    val draggedPiece = activeDrag?.slot?.let { game.tray.getOrNull(it) }
    val dragAnchor = activeDrag?.let { dragTarget(it.slot, it.point) }
    val previewPiece = draggedPiece ?: suggestedMove?.let { game.tray.getOrNull(it.slot) }
    val previewAnchor = dragAnchor ?: suggestedMove?.let { Cell(it.row, it.col) }
    val validPreview = previewAnchor != null && previewPiece != null &&
        GameEngine.canPlace(game.board, previewPiece, previewAnchor.row, previewAnchor.col)

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
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val compact = maxWidth < 340.dp
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "GRIDBLAST",
                                modifier = Modifier.weight(1f).testTag("game-title"),
                                style = MaterialTheme.typography.headlineMedium,
                                fontSize = if (compact) 20.sp else 26.sp,
                                fontWeight = FontWeight.Black,
                                color = highlightColor,
                                maxLines = 1,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { showSettings = true },
                                    modifier = Modifier.testTag("options-button"),
                                ) {
                                    Text(if (compact) "SET" else "OPTIONS", fontSize = 12.sp)
                                }
                                TextButton(
                                    onClick = {
                                        if (game.score > 0) confirmRestart = true else resetGame()
                                    },
                                    modifier = Modifier.testTag("new-game-button"),
                                ) { Text("NEW", fontSize = 12.sp) }
                            }
                        }
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
                            previewPiece = previewPiece,
                            selectedPiece = game.tray.getOrNull(selectedSlot),
                            previewCell = previewAnchor,
                            validPreview = validPreview,
                            flashCells = flashCells,
                            flashAlpha = clearFlash.value,
                            modifier = Modifier
                                .size(boardSize)
                                .testTag("game-board")
                                .onGloballyPositioned { boardBounds = it.boundsInWindow() },
                            onCellTapped = { row, col ->
                                if (selectedSlot >= 0 && !game.gameOver) place(selectedSlot, row, col)
                            },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = when {
                                suggestedMove != null ->
                                    "HINT: row ${suggestedMove!!.row + 1}, column ${suggestedMove!!.col + 1} (+${suggestedMove!!.points})"
                                clearMessage.isNotEmpty() -> clearMessage
                                activeDrag != null -> "Drop your shape onto the board"
                                selectedSlot >= 0 -> "Tap a cell to place, or drag your shape"
                                else -> "Drag a shape to the board, or tap to select"
                            },
                            color = Color(0xFFCAD5E8),
                            textAlign = TextAlign.Start,
                            fontSize = 13.sp,
                            maxLines = 2,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("status-message")
                                .semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        TextButton(
                            onClick = ::undoLast,
                            enabled = history.previous != null,
                            modifier = Modifier.testTag("undo-button"),
                        ) { Text("UNDO", fontSize = 12.sp) }
                        TextButton(
                            onClick = {
                                val next = GameEngine.suggestMove(game)
                                if (next != null) {
                                    selectedSlot = next.slot
                                    suggestedMove = next
                                    activeDrag = null
                                    clearMessage = ""
                                }
                            },
                            enabled = !game.gameOver,
                            modifier = Modifier.testTag("hint-button"),
                        ) { Text("HINT", fontSize = 12.sp) }
                    }

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
                                onSelect = {
                                    suggestedMove = null
                                    selectedSlot = if (selectedSlot == slot) -1 else slot
                                },
                                onDragPoint = { point ->
                                    suggestedMove = null
                                    selectedSlot = slot
                                    activeDrag = ActiveDrag(slot, point)
                                },
                                onDrop = { point ->
                                    val target = dragTarget(slot, point)
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

        if (showSettings) {
            GridBlastSettingsDialog(
                hapticsEnabled = hapticsEnabled,
                motionEnabled = motionEnabled,
                soundEnabled = soundEnabled,
                onSoundChanged = { enabled ->
                    soundEnabled = enabled
                    preferences.edit().putBoolean("sound_enabled", enabled).apply()
                },
                onHapticsChanged = { enabled ->
                    hapticsEnabled = enabled
                    preferences.edit().putBoolean("haptics_enabled", enabled).apply()
                },
                onMotionChanged = { enabled ->
                    motionEnabled = enabled
                    preferences.edit().putBoolean("motion_enabled", enabled).apply()
                },
                onDismiss = { showSettings = false },
            )
        }

        if (confirmRestart) {
            AlertDialog(
                onDismissRequest = { confirmRestart = false },
                title = { Text("Start a new game?") },
                text = { Text("Your current score will be reset. Your best score will be kept.") },
                confirmButton = { TextButton(onClick = ::resetGame, modifier = Modifier.testTag("confirm-restart")) { Text("RESTART") } },
                dismissButton = { TextButton(onClick = { confirmRestart = false }, modifier = Modifier.testTag("cancel-restart")) { Text("CANCEL") } },
            )
        }
        if (game.gameOver && !confirmRestart) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("No more moves") },
                text = { Text("Final score: ${game.score}. Ready for another round?") },
                confirmButton = { TextButton(onClick = ::resetGame) { Text("PLAY AGAIN") } },
                dismissButton = {
                    if (history.previous != null) {
                        TextButton(
                            onClick = ::undoLast,
                            modifier = Modifier.testTag("undo-gameover-button"),
                        ) { Text("UNDO MOVE") }
                    }
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
        Text(value.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White,
            modifier = Modifier.testTag(if (label == "SCORE") "score-value" else "best-score-value"))
    }
}

@Composable
private fun Board(
    board: List<Int>,
    previewPiece: Piece?,
    selectedPiece: Piece?,
    previewCell: Cell?,
    validPreview: Boolean,
    flashCells: Set<Int>,
    flashAlpha: Float,
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
                        row * GameEngine.SIZE + col in flashCells ->
                            lerp(emptyCellColor, highlightColor, flashAlpha)
                        id != 0 -> pieceColor(id)
                        else -> emptyCellColor
                    }
                    val placeable = selectedPiece != null &&
                        GameEngine.canPlace(board, selectedPiece, row, col)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(2.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(fill)
                            .testTag("board-cell-$row-$col")
                            .clickable(enabled = placeable) { onCellTapped(row, col) }
                            .semantics {
                                contentDescription = when {
                                    placeable ->
                                        "Row ${row + 1}, column ${col + 1}: place selected shape"
                                    selectedPiece != null ->
                                        "Row ${row + 1}, column ${col + 1}: shape does not fit"
                                    id == 0 -> "Row ${row + 1}, column ${col + 1}: empty"
                                    else -> "Row ${row + 1}, column ${col + 1}: occupied"
                                }
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
            .testTag("tray-slot-$slot")
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
                    selected -> "Selected shape ${slot + 1}, ${piece.cells.size} blocks. Tap an available board cell to place."
                    else -> "Select shape ${slot + 1}, ${piece.cells.size} blocks, or drag onto the board"
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
