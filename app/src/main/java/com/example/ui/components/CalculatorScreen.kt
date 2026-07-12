package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import com.example.ui.CalculatorViewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val basicMode by viewModel.basicMode.collectAsStateWithLifecycle()
    val expr by viewModel.expr.collectAsStateWithLifecycle()
    val cursorPosition by viewModel.cursorPosition.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val isWelcome by viewModel.isWelcome.collectAsStateWithLifecycle()
    val justCalculated by viewModel.justCalculated.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var dragAccumulator by remember { mutableStateOf(0f) }

    val shift by viewModel.shift.collectAsStateWithLifecycle()
    val alpha by viewModel.alpha.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val calcBase by viewModel.calcBase.collectAsStateWithLifecycle()
    val waitingForSto by viewModel.waitingForSto.collectAsStateWithLifecycle()

    // Dialog state
    val showHistory by viewModel.showHistory.collectAsStateWithLifecycle()
    val showTableMode by viewModel.showTableMode.collectAsStateWithLifecycle()
    val showBaseConverter by viewModel.showBaseConverter.collectAsStateWithLifecycle()
    val showEquationSolver by viewModel.showEquationSolver.collectAsStateWithLifecycle()
    val showDevInfo by viewModel.showDevInfo.collectAsStateWithLifecycle()
    val showConstants by viewModel.showConstants.collectAsStateWithLifecycle()
    val showUnitConverter by viewModel.showUnitConverter.collectAsStateWithLifecycle()

    // Stylings from CSS
    val pageBg = if (isDark) Color(0xFF0A0E1A) else Color(0xFFFFFFFF)
    val panelBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val displayBgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)

    // LCD nostalgia theme
    val lcdBgGradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF3D2F1F), Color(0xFF2E2214)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFD4E4D0), Color(0xFFB8CBB2)))
    }
    val lcdTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val borderStrongColor = if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textDimColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = pageBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(pageBg)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Brand Title, Theme toggler, and Developer Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCIENTIFIC CALCULATOR",
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                            .clickable { viewModel.toggleTheme() }
                    ) {
                        Text(
                            text = if (isDark) "☀️" else "🌙",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2563EB))
                        .border(1.dp, Color(0xFF2563EB), RoundedCornerShape(6.dp))
                        .clickable { viewModel.showDevInfo.value = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Hridoy Hasan Yeasin",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Screen Display Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(displayBgColor)
                    .border(2.dp, borderStrongColor, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // LCD Status indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // DEG/RAD state
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (mode == "RAD") Color(0xFFDC2626) else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                            .border(1.dp, if (mode == "RAD") Color(0xFFDC2626) else borderStrongColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = mode,
                            fontSize = 9.sp,
                            color = if (mode == "RAD") Color.White else textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Base ModeDEC / BIN / OCT / HEX state
                    val baseName = mapOf(10 to "DEC", 2 to "BIN", 8 to "OCT", 16 to "HEX")[calcBase] ?: "DEC"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (calcBase != 10) Color(0xFFDC2626) else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                            .border(1.dp, if (calcBase != 10) Color(0xFFDC2626) else borderStrongColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = baseName,
                            fontSize = 9.sp,
                            color = if (calcBase != 10) Color.White else textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Shift key active indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (shift) Color(0xFFDC2626) else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                            .border(1.dp, if (shift) Color(0xFFDC2626) else borderStrongColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "S",
                            fontSize = 9.sp,
                            color = if (shift) Color.White else textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Alpha key active indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (alpha) Color(0xFF059669) else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                            .border(1.dp, if (alpha) Color(0xFF059669) else borderStrongColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "A",
                            fontSize = 9.sp,
                            color = if (alpha) Color.White else textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // BASIC vs SCIENTIFIC switcher pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                            .border(1.dp, borderStrongColor, RoundedCornerShape(4.dp))
                            .clickable { viewModel.toggleLive() }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (basicMode) "BASIC" else "SCIENTIFIC",
                            fontSize = 9.sp,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // History popup button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2563EB))
                            .clickable { viewModel.showHistory.value = true }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "History",
                            fontSize = 9.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // LCD Screen itself with gradient background & premium glass glare
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(lcdBgGradient)
                        .border(1.5.dp, if (isDark) Color(0xFF2E2B24) else Color(0xFF90A18E), RoundedCornerShape(10.dp))
                ) {
                    // Glass glare diagonal wash
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.05f else 0.20f),
                                    Color.White.copy(alpha = 0.0f),
                                    Color.White.copy(alpha = if (isDark) 0.03f else 0.12f),
                                ),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(w, h)
                            )
                        )
                    }

                    var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

                    val displayAnnotatedExpr = remember(expr, cursorPosition) {
                        val pos = cursorPosition
                        if (pos != null && pos in 0..expr.length) {
                            buildAnnotatedString {
                                append(expr.substring(0, pos))
                                withStyle(SpanStyle(
                                    color = if (isDark) Color(0xFF10B981) else Color(0xFFB91C1C),
                                    fontWeight = FontWeight.Bold
                                )) {
                                    append("|")
                                }
                                append(expr.substring(pos))
                            }
                        } else {
                            buildAnnotatedString {
                                append(expr.ifEmpty { "0" })
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        viewModel.toggleCursor()
                                        val active = (viewModel.cursorPosition.value != null)
                                        android.widget.Toast.makeText(
                                            context,
                                            if (active) "Cursor Active! Drag left/right to move cursor" else "Cursor Deactivated",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onLongPress = { offset ->
                                        menuOffset = offset
                                        showMenu = true
                                    }
                                )
                            }
                            .pointerInput(cursorPosition) {
                                detectHorizontalDragGestures(
                                    onDragStart = { dragAccumulator = 0f },
                                    onHorizontalDrag = { change, dragAmount ->
                                        if (cursorPosition != null) {
                                            dragAccumulator += dragAmount
                                            val threshold = 25f
                                            if (dragAccumulator >= threshold) {
                                                viewModel.moveCursorRight()
                                                dragAccumulator = 0f
                                            } else if (dragAccumulator <= -threshold) {
                                                viewModel.moveCursorLeft()
                                                dragAccumulator = 0f
                                            }
                                        }
                                    }
                                )
                            },
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Copy Expression") },
                                onClick = {
                                    showMenu = false
                                    if (expr.isNotEmpty()) {
                                        clipboardManager.setText(AnnotatedString(expr))
                                        android.widget.Toast.makeText(context, "Input Copied!", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Nothing to copy!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Paste") },
                                onClick = {
                                    showMenu = false
                                    val clipText = clipboardManager.getText()?.text
                                    if (!clipText.isNullOrEmpty()) {
                                        viewModel.insertValueToExpression(clipText)
                                        android.widget.Toast.makeText(context, "Pasted!", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Clipboard is empty!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Cursor") },
                                onClick = {
                                    showMenu = false
                                    if (cursorPosition != null) {
                                        viewModel.toggleCursor()
                                    }
                                }
                            )
                        }

                        // Expression line (scrolling horizontally automatically when cursor is not active)
                        val scrollState = rememberScrollState(Int.MAX_VALUE)
                        LaunchedEffect(scrollState.maxValue) {
                            if (scrollState.maxValue > 0) {
                                scrollState.scrollTo(scrollState.maxValue)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (cursorPosition == null) {
                                        Modifier.horizontalScroll(scrollState)
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = displayAnnotatedExpr,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                color = lcdTextColor.copy(alpha = 0.85f),
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                onTextLayout = { textLayoutResult = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(cursorPosition, displayAnnotatedExpr) {
                                        detectTapGestures(
                                            onDoubleTap = { offset ->
                                                if (cursorPosition == null) {
                                                    textLayoutResult?.let { layoutResult ->
                                                        val tappedIndex = layoutResult.getOffsetForPosition(offset)
                                                        viewModel.cursorPosition.value = tappedIndex.coerceIn(0, expr.length)
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "Cursor Active!",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                } else {
                                                    viewModel.cursorPosition.value = null
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Cursor Deactivated!",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            onTap = { offset ->
                                                if (cursorPosition != null) {
                                                    textLayoutResult?.let { layoutResult ->
                                                        val tappedIndex = layoutResult.getOffsetForPosition(offset)
                                                        val pos = cursorPosition!!
                                                        val originalIndex = if (tappedIndex <= pos) tappedIndex else (tappedIndex - 1).coerceAtLeast(0)
                                                        viewModel.cursorPosition.value = originalIndex.coerceIn(0, expr.length)
                                                    }
                                                }
                                            },
                                            onLongPress = { offset ->
                                                menuOffset = offset
                                                showMenu = true
                                            }
                                        )
                                    }
                                    .pointerInput(cursorPosition) {
                                        detectHorizontalDragGestures(
                                            onDragStart = { dragAccumulator = 0f },
                                            onHorizontalDrag = { change, dragAmount ->
                                                if (cursorPosition != null) {
                                                    dragAccumulator += dragAmount
                                                    val threshold = 15f
                                                    if (dragAccumulator >= threshold) {
                                                        viewModel.moveCursorRight()
                                                        dragAccumulator = 0f
                                                    } else if (dragAccumulator <= -threshold) {
                                                        viewModel.moveCursorLeft()
                                                        dragAccumulator = 0f
                                                    }
                                                }
                                            }
                                        )
                                    }
                            )
                        }

                        // Calculated Result line (larger font)
                        val resultScrollState = rememberScrollState()
                        LaunchedEffect(resultScrollState.maxValue) {
                            if (resultScrollState.maxValue > 0) {
                                resultScrollState.scrollTo(resultScrollState.maxValue)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(result) {
                                    detectTapGestures(
                                        onLongPress = {
                                            if (result.isNotEmpty() && result != "0" && !isWelcome) {
                                                clipboardManager.setText(AnnotatedString(result))
                                                android.widget.Toast.makeText(context, "Result Copied!", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                                .horizontalScroll(resultScrollState),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            if (isWelcome) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Welcome to ",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = lcdTextColor
                                    )
                                    Text(
                                        text = "Hridoy",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF2563EB)
                                    )
                                    Text(
                                        text = "'s World",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = lcdTextColor
                                    )
                                }
                            } else {
                                Text(
                                    text = result,
                                    fontSize = 32.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = lcdTextColor,
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .testTag("result_display")
                                        .then(
                                            if (!justCalculated) {
                                                Modifier.alpha(0.5f)
                                            } else {
                                                Modifier
                                            }
                                        )
                                )
                            }
                        }
                    }
                }

                // Nostalgic casing branding strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (cursorPosition != null) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                                .clickable { viewModel.moveCursorLeft() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowLeft,
                                contentDescription = "Move Cursor Left",
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = "H R I D O Y ' S   C A L C U L A T O R",
                        color = textColor.copy(alpha = 0.35f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    if (cursorPosition != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                                .clickable { viewModel.moveCursorRight() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Move Cursor Right",
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Keyboard grid container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (basicMode) {
                    BasicCalculatorGrid(viewModel, isDark)
                } else {
                    ScientificCalculatorGrid(viewModel, isDark, calcBase)
                }
            }
        }
    }

    // Modal Overlays modeled as custom dialogs
    if (showHistory) {
        HistoryDialog(viewModel, isDark)
    }

    if (showTableMode) {
        TableModeDialog(viewModel, isDark)
    }

    if (showBaseConverter) {
        BaseConverterDialog(viewModel, isDark)
    }

    if (showEquationSolver) {
        EquationSolverDialog(viewModel, isDark)
    }

    if (showDevInfo) {
        InfoDialog(viewModel, isDark)
    }

    if (showConstants) {
        ConstantsDialog(viewModel, isDark)
    }

    if (showUnitConverter) {
        UnitConverterDialog(viewModel, isDark)
    }
}

// Subcomponent grids
@Composable
fun BasicBtnTop(label: String, bg: Color, textColor: Color, isDark: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .padding(3.dp)
            .testTag("btn_basic_$label"),
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = textColor),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp)
    ) {
        Text(text = label, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SciBtnTop(
    label: String,
    bg: Color,
    textColor: Color,
    isDark: Boolean,
    funcBg: Color,
    shiftLabelColor: Color,
    alphaLabelColor: Color,
    bottomLabelColor: Color,
    topLabel: String = "",
    alphaLabel: String = "",
    bottomLabel: String = "",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val buttonOpacity = if (enabled) 1f else 0.40f
    val resolvedTextColor = if (bg == funcBg && textColor == Color.White) {
        if (isDark) Color(0xFFCBD5E1) else Color(0xFF312E81)
    } else {
        textColor
    }

    Button(
        onClick = { if (enabled) onClick() },
        modifier = modifier
            .fillMaxHeight()
            .padding(2.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bg.copy(alpha = buttonOpacity), contentColor = resolvedTextColor.copy(alpha = buttonOpacity)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color(0xFF334155).copy(alpha = 0.3f) else Color(0xFFE2E8F0)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (topLabel.isNotEmpty()) {
                Text(
                    text = topLabel,
                    color = shiftLabelColor.copy(alpha = buttonOpacity),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            if (alphaLabel.isNotEmpty()) {
                Text(
                    text = alphaLabel,
                    color = alphaLabelColor.copy(alpha = buttonOpacity),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
            if (bottomLabel.isNotEmpty()) {
                Text(
                    text = bottomLabel,
                    color = bottomLabelColor.copy(alpha = buttonOpacity),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
fun BasicCalculatorGrid(viewModel: CalculatorViewModel, isDark: Boolean) {
    val buttonBg = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val buttonTextColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val opBg = if (isDark) Color(0xFF4F46E5) else Color(0xFF6366F1)
    val dangerBg = if (isDark) Color(0xFFDC2626) else Color(0xFFEF4444)
    val accentBg = if (isDark) Color(0xFF2563EB) else Color(0xFF3B82F6)

    @Composable
    fun BasicBtn(label: String, bg: Color, textColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
        BasicBtnTop(label, bg, textColor, isDark, modifier, onClick)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val rowWeight = 1f
        Row(modifier = Modifier.weight(rowWeight).fillMaxWidth()) {
            BasicBtn("C", bg = dangerBg, textColor = Color.White, modifier = Modifier.weight(1f)) { viewModel.pressClear() }
            BasicBtn("÷", bg = opBg, textColor = Color.White, modifier = Modifier.weight(1f)) { viewModel.pressOp("÷") }
            BasicBtn("×", bg = opBg, textColor = Color.White, modifier = Modifier.weight(1f)) { viewModel.pressOp("×") }
            BasicBtn("⌫", bg = dangerBg, textColor = Color.White, modifier = Modifier.weight(1f)) { viewModel.pressBack() }
        }
        Row(modifier = Modifier.weight(rowWeight).fillMaxWidth()) {
            BasicBtn("7", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(1f)) { viewModel.pressKey("7") }
            BasicBtn("8", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(1f)) { viewModel.pressKey("8") }
            BasicBtn("9", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(1f)) { viewModel.pressKey("9") }
            BasicBtn("-", bg = opBg, textColor = Color.White, modifier = Modifier.weight(1f)) { viewModel.pressOp("-") }
        }
        Row(modifier = Modifier.weight(2f).fillMaxWidth()) {
            Column(modifier = Modifier.weight(3f).fillMaxHeight()) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    BasicBtn("4", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(1f)) { viewModel.pressKey("4") }
                    BasicBtn("5", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(1f)) { viewModel.pressKey("5") }
                    BasicBtn("6", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(1f)) { viewModel.pressKey("6") }
                }
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    BasicBtn("1", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(1f)) { viewModel.pressKey("1") }
                    BasicBtn("2", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(1f)) { viewModel.pressKey("2") }
                    BasicBtn("3", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(1f)) { viewModel.pressKey("3") }
                }
            }
            BasicBtn("+", bg = opBg, textColor = Color.White, modifier = Modifier.weight(1f).fillMaxHeight()) { viewModel.pressOp("+") }
        }
        Row(modifier = Modifier.weight(rowWeight).fillMaxWidth()) {
            BasicBtn(".", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(1f)) { viewModel.pressKey(".") }
            BasicBtn("0", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(1f)) { viewModel.pressKey("0") }
            BasicBtn("%", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(1f)) { viewModel.pressKey("%") }
            BasicBtn("=", bg = accentBg, textColor = Color.White, modifier = Modifier.weight(1f)) { viewModel.pressEq() }
        }
    }
}

@Composable
fun ScientificCalculatorGrid(viewModel: CalculatorViewModel, isDark: Boolean, calcBase: Int) {
    val buttonBg = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val buttonTextColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val opBg = if (isDark) Color(0xFF4F46E5) else Color(0xFF6366F1)
    val dangerBg = if (isDark) Color(0xFFDC2626) else Color(0xFFEF4444)
    val accentBg = if (isDark) Color(0xFF2563EB) else Color(0xFF3B82F6)
    val funcBg = if (isDark) Color(0xFF2E3B4E) else Color(0xFFE2E8F0)

    val shiftLabelColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    val alphaLabelColor = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val bottomLabelColor = if (isDark) Color(0xFFFBBF24).copy(alpha = 0.85f) else Color(0xFFB45309).copy(alpha = 0.85f)

    @Composable
    fun SciBtn(
        label: String,
        bg: Color,
        textColor: Color,
        topLabel: String = "",
        alphaLabel: String = "",
        bottomLabel: String = "",
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        onClick: () -> Unit
    ) {
        SciBtnTop(
            label = label,
            bg = bg,
            textColor = textColor,
            isDark = isDark,
            funcBg = funcBg,
            shiftLabelColor = shiftLabelColor,
            alphaLabelColor = alphaLabelColor,
            bottomLabelColor = bottomLabelColor,
            topLabel = topLabel,
            alphaLabel = alphaLabel,
            bottomLabel = bottomLabel,
            modifier = modifier,
            enabled = enabled,
            onClick = onClick
        )
    }

    // Determine disabled items in base modes representing identical JS updateBaseButtons rules
    fun isButtonEnabled(text: String): Boolean {
        if (text.lowercase() in listOf("del", "dec", "ans", "m", "bin", "oct", "hex")) return true
        if (calcBase == 2) {
            // Disable digits 2-9, A-F, and advanced math
            if (text.any { it in '2'..'9' || it.uppercaseChar() in 'A'..'F' }) return false
            if (text in listOf("sin", "cos", "tan", "csc", "sec", "cot", "log", "ln", "√", "∛", "!", "×10ˣ", "%", "x²", "xʸ", "π", "fact", "abs", "Rnd", "nPr", "nCr", "Pol(", "Rec(")) return false
        } else if (calcBase == 8) {
            // Disable digits 8-9, A-F and advanced math
            if (text.any { it in '8'..'9' || it.uppercaseChar() in 'A'..'F' }) return false
            if (text in listOf("sin", "cos", "tan", "csc", "sec", "cot", "log", "ln", "√", "∛", "!", "×10ˣ", "%", "x²", "xʸ", "π", "fact", "abs", "Rnd", "nPr", "nCr", "Pol(", "Rec(")) return false
        } else if (calcBase == 16) {
            // Enable HEX digits: 0-9, A-F, as well as buttons representing: x², xʸ, √, π, log, ln (since they are labeled A-F)
            // Disable sin, cos, tan, csc, sec, cot, ∛, !, sto, s, eng, % etc
            if (text.lowercase() in listOf("sin", "cos", "tan", "csc", "sec", "cot", "∛", "!", "sto", "s", "eng", "×10", "%", "fact", "npr", "ncr", "abs", "rnd", "pol", "rec")) return false
        }
        return true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val rowWeight = 1f
        // Row 1
        Row(modifier = Modifier.weight(rowWeight).fillMaxWidth()) {
            SciBtn("⇧", bg = dangerBg, textColor = Color.White, modifier = Modifier.weight(10f)) { viewModel.pressShift() }
            SciBtn("α", bg = Color(0xFF059669), textColor = Color.White, modifier = Modifier.weight(10f)) { viewModel.pressAlpha() }
            SciBtn("M", bg = funcBg, textColor = Color.White, topLabel = "tab", alphaLabel = "bas", bottomLabel = "equ", modifier = Modifier.weight(10f)) { viewModel.pressModeMenu() }
            SciBtn("(", bg = funcBg, textColor = Color.White, modifier = Modifier.weight(10f), enabled = isButtonEnabled("(")) { viewModel.pressParen() }
            SciBtn(")", bg = funcBg, textColor = Color.White, modifier = Modifier.weight(10f), enabled = isButtonEnabled(")")) { viewModel.pressKey(")") }
            SciBtn("CNST", bg = buttonBg, textColor = buttonTextColor, topLabel = "CONV", modifier = Modifier.weight(10f)) {
                if (viewModel.shift.value) {
                    viewModel.shift.value = false
                    viewModel.showUnitConverter.value = true
                } else {
                    viewModel.showConstants.value = true
                }
            }
        }
        // Row 2
        Row(modifier = Modifier.weight(rowWeight).fillMaxWidth()) {
            SciBtn("x²", bg = funcBg, textColor = Color.White, topLabel = "x³", alphaLabel = "A", modifier = Modifier.weight(10f), enabled = isButtonEnabled("A")) { viewModel.pressFunc("pow2") }
            SciBtn("xʸ", bg = funcBg, textColor = Color.White, topLabel = "ˣ√", alphaLabel = "B", modifier = Modifier.weight(10f), enabled = isButtonEnabled("B")) { viewModel.pressFunc("pow") }
            SciBtn("√", bg = funcBg, textColor = Color.White, topLabel = "³√", alphaLabel = "C", modifier = Modifier.weight(10f), enabled = isButtonEnabled("C")) { viewModel.pressFunc("sqrt") }
            SciBtn("π", bg = funcBg, textColor = Color.White, topLabel = "e", alphaLabel = "D", modifier = Modifier.weight(10f), enabled = isButtonEnabled("D")) { viewModel.pressConst("π") }
            SciBtn("log", bg = funcBg, textColor = Color.White, topLabel = "10ˣ", alphaLabel = "E", bottomLabel = "logₓy", modifier = Modifier.weight(10f), enabled = isButtonEnabled("E")) { viewModel.pressFunc("log") }
            SciBtn("ln", bg = funcBg, textColor = Color.White, topLabel = "eˣ", alphaLabel = "F", modifier = Modifier.weight(10f), enabled = isButtonEnabled("F")) { viewModel.pressFunc("ln") }
        }
        // Row 3
        Row(modifier = Modifier.weight(rowWeight).fillMaxWidth()) {
            SciBtn("sin", bg = funcBg, textColor = Color.White, topLabel = "sin⁻¹", alphaLabel = "csc", bottomLabel = "csc⁻¹", modifier = Modifier.weight(10f), enabled = isButtonEnabled("sin")) { viewModel.pressFunc("sin") }
            SciBtn("cos", bg = funcBg, textColor = Color.White, topLabel = "cos⁻¹", alphaLabel = "sec", bottomLabel = "sec⁻¹", modifier = Modifier.weight(10f), enabled = isButtonEnabled("cos")) { viewModel.pressFunc("cos") }
            SciBtn("tan", bg = funcBg, textColor = Color.White, topLabel = "tan⁻¹", alphaLabel = "cot", bottomLabel = "cot⁻¹", modifier = Modifier.weight(10f), enabled = isButtonEnabled("tan")) { viewModel.pressFunc("tan") }
            SciBtn("x!", bg = funcBg, textColor = Color.White, topLabel = "x⁻¹", modifier = Modifier.weight(10f), enabled = isButtonEnabled("fact")) { viewModel.pressFunc("fact") }
            SciBtn("DEL", bg = dangerBg, textColor = Color.White, modifier = Modifier.weight(10f), enabled = isButtonEnabled("del")) { viewModel.pressDel() }
            SciBtn("AC", bg = dangerBg, textColor = Color.White, modifier = Modifier.weight(10f)) { viewModel.pressAC() }
        }
        // Row 4
        Row(modifier = Modifier.weight(rowWeight).fillMaxWidth()) {
            SciBtn("nPr", bg = funcBg, textColor = Color.White, topLabel = "nCr", modifier = Modifier.weight(10f), enabled = isButtonEnabled("nPr")) { viewModel.pressFunc("nPr") }
            SciBtn("Pol", bg = funcBg, textColor = Color.White, topLabel = "Rec(", modifier = Modifier.weight(10f), enabled = isButtonEnabled("pol")) { viewModel.pressFunc("pol") }
            SciBtn("S⇔D", bg = funcBg, textColor = Color.White, modifier = Modifier.weight(10f), enabled = isButtonEnabled("S⇔D")) { viewModel.pressSD() }
            SciBtn("BIN", bg = funcBg, textColor = Color.White, topLabel = "0b", modifier = Modifier.weight(10f), enabled = isButtonEnabled("bin")) { if (viewModel.shift.value) viewModel.pressBasePrefix(2) else viewModel.pressBaseKey(2) }
            SciBtn("OCT", bg = funcBg, textColor = Color.White, topLabel = "0o", modifier = Modifier.weight(10f), enabled = isButtonEnabled("oct")) { if (viewModel.shift.value) viewModel.pressBasePrefix(8) else viewModel.pressBaseKey(8) }
            SciBtn("÷", bg = opBg, textColor = Color.White, modifier = Modifier.weight(10f), enabled = isButtonEnabled("÷")) { viewModel.pressOp("÷") }
        }
        // Row 5
        Row(modifier = Modifier.weight(rowWeight).fillMaxWidth()) {
            SciBtn("7", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(10f), enabled = isButtonEnabled("7")) { viewModel.pressBaseDigit("7") }
            SciBtn("8", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(10f), enabled = isButtonEnabled("8")) { viewModel.pressBaseDigit("8") }
            SciBtn("9", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(10f), enabled = isButtonEnabled("9")) { viewModel.pressBaseDigit("9") }
            SciBtn("DEC", bg = funcBg, textColor = Color.White, modifier = Modifier.weight(10f), enabled = isButtonEnabled("dec")) { viewModel.pressBaseKey(10) }
            SciBtn("HEX", bg = funcBg, textColor = Color.White, topLabel = "0x", modifier = Modifier.weight(10f), enabled = isButtonEnabled("hex")) { if (viewModel.shift.value) viewModel.pressBasePrefix(16) else viewModel.pressBaseKey(16) }
            SciBtn("×", bg = opBg, textColor = Color.White, modifier = Modifier.weight(10f), enabled = isButtonEnabled("×")) { viewModel.pressOp("×") }
        }
        // Row 6
        Row(modifier = Modifier.weight(rowWeight).fillMaxWidth()) {
            SciBtn("4", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(10f), enabled = isButtonEnabled("4")) { viewModel.pressBaseDigit("4") }
            SciBtn("5", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(10f), enabled = isButtonEnabled("5")) { viewModel.pressBaseDigit("5") }
            SciBtn("6", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(10f), enabled = isButtonEnabled("6")) { viewModel.pressBaseDigit("6") }
            SciBtn("Abs", bg = funcBg, textColor = Color.White, topLabel = "Rnd", modifier = Modifier.weight(10f), enabled = isButtonEnabled("abs")) { viewModel.pressFunc("abs") }
            SciBtn("STO", bg = funcBg, textColor = Color.White, alphaLabel = "clear", modifier = Modifier.weight(10f), enabled = isButtonEnabled("sto")) { viewModel.pressSto() }
            SciBtn("-", bg = opBg, textColor = Color.White, modifier = Modifier.weight(10f), enabled = isButtonEnabled("-")) { viewModel.pressOp("-") }
        }
        // Row 7
        Row(modifier = Modifier.weight(rowWeight).fillMaxWidth()) {
            SciBtn("1", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(10f), enabled = isButtonEnabled("1")) { viewModel.pressBaseDigit("1") }
            SciBtn("2", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(10f), enabled = isButtonEnabled("2")) { viewModel.pressBaseDigit("2") }
            SciBtn("3", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(10f), enabled = isButtonEnabled("3")) { viewModel.pressBaseDigit("3") }
            SciBtn("×10ˣ", bg = funcBg, textColor = Color.White, modifier = Modifier.weight(10f), enabled = isButtonEnabled("×10ˣ")) { viewModel.pressExp() }
            SciBtn("ENG", bg = funcBg, textColor = Color.White, modifier = Modifier.weight(10f), enabled = isButtonEnabled("eng")) { viewModel.pressEng() }
            SciBtn("+", bg = opBg, textColor = Color.White, modifier = Modifier.weight(10f), enabled = isButtonEnabled("+")) { viewModel.pressOp("+") }
        }
        // Row 8
        Row(modifier = Modifier.weight(rowWeight).fillMaxWidth()) {
            SciBtn("0", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(10f), enabled = isButtonEnabled("0")) { viewModel.pressBaseDigit("0") }
            SciBtn(".", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(10f), enabled = isButtonEnabled(".")) { viewModel.pressKey(".") }
            SciBtn("±", bg = buttonBg, textColor = buttonTextColor, modifier = Modifier.weight(10f), enabled = isButtonEnabled("±")) { viewModel.pressNeg() }
            SciBtn("%", bg = funcBg, textColor = Color.White, topLabel = ",", modifier = Modifier.weight(10f), enabled = isButtonEnabled("%")) { viewModel.pressKey("%") }
            SciBtn("Ans", bg = funcBg, textColor = Color.White, modifier = Modifier.weight(10f), enabled = isButtonEnabled("ans")) { viewModel.pressAns() }
            SciBtn("=", bg = accentBg, textColor = Color.White, topLabel = "=", modifier = Modifier.weight(10f), enabled = isButtonEnabled("=")) { viewModel.pressEq() }
        }
    }
}

// Dialog Overlays
@Composable
fun HistoryDialog(viewModel: CalculatorViewModel, isDark: Boolean) {
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val bg = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val textCol = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val panelDarkCol = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    Dialog(onDismissRequest = { viewModel.showHistory.value = false }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(12.dp)
                .testTag("dialog_history"),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = bg),
            border = BorderStroke(2.dp, if (isDark) Color(0xFF475569) else Color(0xFF94A3B8))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(panelDarkCol)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Calculation History", color = textCol, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { viewModel.showHistory.value = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textCol)
                    }
                }

                // Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    if (historyList.isEmpty()) {
                        Text(
                            text = "No calculations yet",
                            color = textCol.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 14.sp
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(historyList) { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                                        .clickable { viewModel.loadFromHistory(item) }
                                        .padding(10.dp)
                                ) {
                                    Text(text = item.date, fontSize = 10.sp, color = textCol.copy(alpha = 0.5f))
                                    Text(text = item.expr, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textCol, fontFamily = FontFamily.Monospace)
                                    Text(
                                        text = "= ${item.result}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB),
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(panelDarkCol)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { viewModel.clearHistory() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Clear All", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun TableModeDialog(viewModel: CalculatorViewModel, isDark: Boolean) {
    val bg = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val textCol = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val panelDarkCol = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    val fx by viewModel.tableFx.collectAsStateWithLifecycle()
    val start by viewModel.tableStart.collectAsStateWithLifecycle()
    val end by viewModel.tableEnd.collectAsStateWithLifecycle()
    val step by viewModel.tableStep.collectAsStateWithLifecycle()
    val rows by viewModel.tableRows.collectAsStateWithLifecycle()
    val tableError by viewModel.tableError.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = { viewModel.showTableMode.value = false }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(12.dp)
                .testTag("dialog_table_mode"),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = bg),
            border = BorderStroke(2.dp, if (isDark) Color(0xFF475569) else Color(0xFF94A3B8))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(panelDarkCol)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Table Mode f(x)", color = textCol, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { viewModel.showTableMode.value = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textCol)
                    }
                }

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Formula input
                    OutlinedTextField(
                        value = fx,
                        onValueChange = { viewModel.tableFx.value = it },
                        label = { Text("f(x) = ") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = borderStrongColor(isDark),
                            focusedLabelColor = Color(0xFF2563EB),
                            unfocusedLabelColor = textCol,
                            focusedTextColor = textCol,
                            unfocusedTextColor = textCol
                        )
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = start,
                            onValueChange = { viewModel.tableStart.value = it },
                            label = { Text("Start") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB), focusedLabelColor = Color(0xFF2563EB), focusedTextColor = textCol, unfocusedTextColor = textCol)
                        )
                        OutlinedTextField(
                            value = end,
                            onValueChange = { viewModel.tableEnd.value = it },
                            label = { Text("End") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB), focusedLabelColor = Color(0xFF2563EB), focusedTextColor = textCol, unfocusedTextColor = textCol)
                        )
                        OutlinedTextField(
                            value = step,
                            onValueChange = { viewModel.tableStep.value = it },
                            label = { Text("Step") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB), focusedLabelColor = Color(0xFF2563EB), focusedTextColor = textCol, unfocusedTextColor = textCol)
                        )
                    }

                    Button(
                        onClick = { viewModel.generateTable() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Generate", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (tableError != null) {
                        Text(text = tableError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }

                    // Result Table
                    if (rows.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, borderStrongColor(isDark), RoundedCornerShape(4.dp))
                        ) {
                            // Table Headers
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2563EB))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "x", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text(text = "f(x)", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            }

                            // Dynamic Rows
                            rows.forEachIndexed { index, pair ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (index % 2 == 0) Color(0xFF2563EB).copy(alpha = 0.05f) else Color.Transparent)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = pair.first.toString(), color = textCol, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace)
                                    Text(text = pair.second, color = textCol, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BaseConverterDialog(viewModel: CalculatorViewModel, isDark: Boolean) {
    val bg = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val textCol = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val panelDarkCol = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    val baseFrom by viewModel.baseFrom.collectAsStateWithLifecycle()
    val baseInput by viewModel.baseInput.collectAsStateWithLifecycle()
    val bin by viewModel.baseResultBin.collectAsStateWithLifecycle()
    val oct by viewModel.baseResultOct.collectAsStateWithLifecycle()
    val dec by viewModel.baseResultDec.collectAsStateWithLifecycle()
    val hex by viewModel.baseResultHex.collectAsStateWithLifecycle()
    val baseError by viewModel.baseError.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = { viewModel.showBaseConverter.value = false }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(12.dp)
                .testTag("dialog_base_converter"),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = bg),
            border = BorderStroke(2.dp, if (isDark) Color(0xFF475569) else Color(0xFF94A3B8))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(panelDarkCol)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Base Converter", color = textCol, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { viewModel.showBaseConverter.value = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textCol)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var expandedDropdown by remember { mutableStateOf(false) }
                    val bases = listOf(10 to "DEC (Decimal)", 2 to "BIN (Binary)", 8 to "OCT (Octal)", 16 to "HEX (Hexadecimal)")
                    val selectedBaseName = bases.firstOrNull { it.first == baseFrom }?.second ?: "DEC"

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { expandedDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9), contentColor = textCol),
                            border = BorderStroke(1.dp, borderStrongColor(isDark))
                        ) {
                            Text(selectedBaseName)
                        }
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            bases.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b.second) },
                                    onClick = {
                                        viewModel.baseFrom.value = b.first
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = baseInput,
                        onValueChange = { viewModel.baseInput.value = it },
                        label = { Text("Enter number") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            focusedLabelColor = Color(0xFF2563EB),
                            focusedTextColor = textCol,
                            unfocusedTextColor = textCol
                        )
                    )

                    Button(
                        onClick = { viewModel.convertBase() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Convert", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (baseError != null) {
                        Text(text = baseError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }

                    // Converted rows
                    if (bin.isNotEmpty() || oct.isNotEmpty() || dec.isNotEmpty() || hex.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ConvertedRow("BIN (2)", bin, isDark)
                            ConvertedRow("OCT (8)", oct, isDark)
                            ConvertedRow("DEC (10)", dec, isDark)
                            ConvertedRow("HEX (16)", hex, isDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConvertedRow(label: String, value: String, isDark: Boolean) {
    val textCol = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
            .border(1.dp, borderStrongColor(isDark), RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) {
        Text(text = label, color = Color(0xFF2563EB), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = textCol, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EquationSolverDialog(viewModel: CalculatorViewModel, isDark: Boolean) {
    val bg = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val textCol = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val panelDarkCol = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    val currentType by viewModel.eqnType.collectAsStateWithLifecycle()
    val inputs by viewModel.eqnInputs.collectAsStateWithLifecycle()
    val eqnResult by viewModel.eqnResult.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = { viewModel.showEquationSolver.value = false }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(12.dp)
                .testTag("dialog_eqn_solver"),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = bg),
            border = BorderStroke(2.dp, if (isDark) Color(0xFF475569) else Color(0xFF94A3B8))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(panelDarkCol)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Equation Solver", color = textCol, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { viewModel.showEquationSolver.value = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textCol)
                    }
                }

                // Type Tab Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val eqTypes = listOf(
                        1 to "ax+b=0",
                        2 to "ax²+bx+c=0",
                        3 to "ax³+bx²+cx+d=0",
                        4 to "2x2 Linear",
                        5 to "3x3 Linear"
                    )

                    eqTypes.forEach { type ->
                        val isSelected = currentType == type.first
                        Button(
                            onClick = {
                                viewModel.eqnType.value = type.first
                                viewModel.eqnInputs.value = emptyMap()
                                viewModel.eqnResult.value = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF2563EB) else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                contentColor = if (isSelected) Color.White else textCol
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(type.second, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Title formula
                    Text(
                        text = when (currentType) {
                            1 -> "ax + b = 0"
                            2 -> "ax² + bx + c = 0"
                            3 -> "ax³ + bx² + cx + d = 0"
                            4 -> "a₁x + b₁y = c₁\na₂x + b₂y = c₂"
                            5 -> "a₁x + b₁y + c₁z = d₁\na₂x + b₂y + c₂z = d₂\na₃x + b₃y + c₃z = d₃"
                            else -> ""
                        },
                        color = textCol,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 14.sp
                    )

                    // Dynamic inputs fields
                    val fieldsList = when (currentType) {
                        1 -> listOf("a", "b")
                        2 -> listOf("a", "b", "c")
                        3 -> listOf("a", "b", "c", "d")
                        4 -> listOf("a1", "b1", "c1", "a2", "b2", "c2")
                        5 -> listOf("a1", "b1", "c1", "d1", "a2", "b2", "c2", "d2", "a3", "b3", "c3", "d3")
                        else -> emptyList()
                    }

                    // Dynamically map list into 2 columns for high fields and single row for smaller
                    val columns = if (fieldsList.size > 4) 3 else 1
                    val chunkedFields = fieldsList.chunked(columns)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        chunkedFields.forEach { rowFields ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rowFields.forEach { key ->
                                    OutlinedTextField(
                                        value = inputs[key] ?: "",
                                        onValueChange = {
                                            val m = inputs.toMutableMap()
                                            m[key] = it
                                            viewModel.eqnInputs.value = m
                                        },
                                        label = { Text(key) },
                                        modifier = Modifier
                                            .weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF2563EB),
                                            focusedLabelColor = Color(0xFF2563EB),
                                            focusedTextColor = textCol,
                                            unfocusedTextColor = textCol
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.solveEquation() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("SOLVE", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // Solved Result output rendering
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                            .border(2.dp, borderStrongColor(isDark), RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = eqnResult.ifEmpty { "Enter variables and click SOLVE." },
                            color = textCol,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoDialog(viewModel: CalculatorViewModel, isDark: Boolean) {
    val context = LocalContext.current
    val bg = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val textCol = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val panelDarkCol = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    Dialog(onDismissRequest = { viewModel.showDevInfo.value = false }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(12.dp)
                .testTag("dialog_dev_info"),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = bg),
            border = BorderStroke(2.dp, if (isDark) Color(0xFF475569) else Color(0xFF94A3B8))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(panelDarkCol)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Developer Info", color = textCol, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { viewModel.showDevInfo.value = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textCol)
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Dynamic Profile Image loaded via Coil with custom "H" circle fallback
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("https://pbs.twimg.com/profile_images/1549038045931769862/NJjQA0_i_400x400.jpg")
                            .crossfade(true)
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                            .build(),
                        contentDescription = "Developer Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape),
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1E3A8A)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "H", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFFF00))
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1E3A8A)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "H", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFFF00))
                            }
                        }
                    )

                    Text(text = "Hridoy Hasan Yeasin", color = textCol, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Calculator Developer", color = textCol.copy(alpha = 0.7f), fontSize = 12.sp)

                    // Contact action flows
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Phone Link
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2563EB).copy(alpha = 0.1f))
                                .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:01600180139"))
                                    context.startActivity(intent)
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📞", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
                            Column {
                                Text(text = "PHONE", fontSize = 9.sp, color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
                                Text(text = "01600180139", fontSize = 13.sp, color = textCol, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // Email Link
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2563EB).copy(alpha = 0.1f))
                                .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:hridoyeasin63@gmail.com"))
                                    context.startActivity(intent)
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "✉️", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
                            Column {
                                Text(text = "EMAIL", fontSize = 9.sp, color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
                                Text(text = "hridoyeasin63@gmail.com", fontSize = 13.sp, color = textCol)
                            }
                        }
                    }

                    Divider(color = borderStrongColor(isDark), thickness = 1.dp)

                    // Keyboard Combos reference list
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                            .border(1.dp, borderStrongColor(isDark), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "🔘 BUTTON COMBOS",
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val combos = listOf(
                            "M" to "DEG ↔ RAD toggle",
                            "SHIFT + M" to "f(x) Table Mode",
                            "ALPHA + M" to "Base Converter",
                            "SHIFT + ALPHA + M" to "Equation Solver",
                            "SHIFT" to "sin⁻¹, cos⁻¹, tan⁻¹, x³, ³√, eˣ, 10ˣ",
                            "ALPHA" to "csc, sec, cot, A, B, C, D, E, F",
                            "SHIFT + ALPHA" to "csc⁻¹, sec⁻¹, cot⁻¹",
                            "SHIFT + STO" to "Clear All Variables",
                            "SHIFT + AC" to "Full Hardware Reset",
                            "SHIFT + %" to "Comma (,)"
                        )

                        combos.forEach { combo ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = combo.first, color = Color(0xFFDC2626), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                                Text(text = "= " + combo.second, color = textCol, fontSize = 10.sp, modifier = Modifier.weight(1.8f))
                            }
                        }
                    }

                    // Social links action widgets
                    Text(text = "SOCIAL LINKS", color = Color(0xFF60A5FA), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SocialNetworkCard("Facebook", "https://facebook.com/hridoyeasin63", context, textCol, Modifier.weight(1f))
                        SocialNetworkCard("WhatsApp", "https://wa.me/8801600180139", context, textCol, Modifier.weight(1f))
                        SocialNetworkCard("IMO", "https://s.imoim.net/jyLtH6", context, textCol, Modifier.weight(1f))
                        SocialNetworkCard("X", "https://x.com/hridoyeasin63", context, textCol, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun FacebookLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        drawCircle(
            color = Color(0xFF1877F2),
            radius = w / 2f
        )
        val scale = w / 24f
        val path = Path().apply {
            moveTo(15f * scale, 21f * scale)
            lineTo(15f * scale, 13.5f * scale)
            lineTo(17.5f * scale, 13.5f * scale)
            lineTo(17.8f * scale, 10.5f * scale)
            lineTo(15f * scale, 10.5f * scale)
            lineTo(15f * scale, 8.8f * scale)
            quadraticTo(15f * scale, 8f * scale, 15.8f * scale, 7.5f * scale)
            quadraticTo(16.3f * scale, 7.2f * scale, 17.5f * scale, 7.2f * scale)
            lineTo(17.5f * scale, 4.5f * scale)
            quadraticTo(15.5f * scale, 4.3f * scale, 14.2f * scale, 4.8f * scale)
            quadraticTo(12.5f * scale, 5.5f * scale, 11.8f * scale, 7f * scale)
            quadraticTo(11.2f * scale, 8.2f * scale, 11.2f * scale, 10.5f * scale)
            lineTo(9.5f * scale, 10.5f * scale)
            lineTo(9.5f * scale, 13.5f * scale)
            lineTo(11.2f * scale, 13.5f * scale)
            lineTo(11.2f * scale, 21f * scale)
            close()
        }
        drawPath(path = path, color = Color.White)
    }
}

@Composable
fun WhatsAppLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val scale = w / 24f
        
        val bubblePath = Path().apply {
            addOval(Rect(0f, 0f, w, h))
        }
        drawPath(path = bubblePath, color = Color(0xFF25D366))
        
        val tailPath = Path().apply {
            moveTo(4f * scale, 17f * scale)
            lineTo(2f * scale, 22f * scale)
            lineTo(7.5f * scale, 20f * scale)
            close()
        }
        drawPath(path = tailPath, color = Color(0xFF25D366))

        val phonePath = Path().apply {
            moveTo(8f * scale, 7.5f * scale)
            quadraticTo(9f * scale, 6.5f * scale, 10f * scale, 7.5f * scale)
            lineTo(11f * scale, 8.5f * scale)
            quadraticTo(12f * scale, 9.5f * scale, 11f * scale, 10.5f * scale)
            lineTo(10.2f * scale, 11.3f * scale)
            quadraticTo(10.8f * scale, 12.8f * scale, 12.2f * scale, 14.2f * scale)
            quadraticTo(13.6f * scale, 14.8f * scale, 14.7f * scale, 14f * scale)
            lineTo(15.5f * scale, 13.2f * scale)
            quadraticTo(16.5f * scale, 12.2f * scale, 17.5f * scale, 13.2f * scale)
            lineTo(18.5f * scale, 14.2f * scale)
            quadraticTo(19.5f * scale, 15.2f * scale, 18.5f * scale, 16.2f * scale)
            quadraticTo(17f * scale, 17.7f * scale, 14.5f * scale, 17.5f * scale)
            quadraticTo(11f * scale, 17.2f * scale, 8.5f * scale, 13.5f * scale)
            quadraticTo(6f * scale, 9.8f * scale, 7.5f * scale, 8.5f * scale)
            close()
        }
        drawPath(path = phonePath, color = Color.White)
    }
}

@Composable
fun IMOLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(Color(0xFF1D9BF0)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "imo",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(bottom = 1.dp)
        )
    }
}

@Composable
fun XLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val scale = w / 24f
        
        drawCircle(color = Color.Black, radius = w / 2f)
        
        val thickPath = Path().apply {
            moveTo(4.5f * scale, 4.5f * scale)
            lineTo(9.5f * scale, 4.5f * scale)
            lineTo(19.5f * scale, 19.5f * scale)
            lineTo(14.5f * scale, 19.5f * scale)
            close()
        }
        drawPath(path = thickPath, color = Color.White)
        
        val hollowPath = Path().apply {
            moveTo(6.5f * scale, 4.5f * scale)
            lineTo(8.0f * scale, 4.5f * scale)
            lineTo(17.5f * scale, 19.5f * scale)
            lineTo(16.0f * scale, 19.5f * scale)
            close()
        }
        drawPath(path = hollowPath, color = Color.Black)
        
        val thinPath = Path().apply {
            moveTo(19.5f * scale, 4.5f * scale)
            lineTo(18.0f * scale, 4.5f * scale)
            lineTo(4.5f * scale, 19.5f * scale)
            lineTo(6.0f * scale, 19.5f * scale)
            close()
        }
        drawPath(path = thinPath, color = Color.White)
    }
}

@Composable
fun SocialNetworkCard(name: String, url: String, context: android.content.Context, textColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2563EB).copy(alpha = 0.15f)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                when (name.lowercase()) {
                    "facebook" -> FacebookLogo()
                    "whatsapp" -> WhatsAppLogo()
                    "imo" -> IMOLogo()
                    "x" -> XLogo()
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

// Helpers
fun borderStrongColor(isDark: Boolean) = if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)

data class SciConstant(
    val no: String,
    val name: String,
    val symbol: String,
    val valueText: String
)

val scientificConstants = listOf(
    SciConstant("01", "Speed of Light", "c", "2.99792458×10⁸"),
    SciConstant("02", "Standard Gravity", "g", "9.80665"),
    SciConstant("03", "Gravitational Constant", "G", "6.67430×10⁻¹¹"),
    SciConstant("04", "Magnetic Constant", "μ₀", "1.25663706212×10⁻⁶"),
    SciConstant("05", "Electric Constant", "ε₀", "8.8541878128×10⁻¹²"),
    SciConstant("06", "Planck Constant", "h", "6.62607015×10⁻³⁴"),
    SciConstant("07", "Reduced Planck Constant", "ħ", "1.054571817×10⁻³⁴"),
    SciConstant("08", "Elementary Charge", "e", "1.602176634×10⁻¹⁹"),
    SciConstant("09", "Electron Mass", "mₑ", "9.1093837015×10⁻³¹"),
    SciConstant("10", "Proton Mass", "mₚ", "1.67262192369×10⁻²⁷"),
    SciConstant("11", "Neutron Mass", "mₙ", "1.67492749804×10⁻²⁷"),
    SciConstant("12", "Muon Mass", "mμ", "1.883531627×10⁻²⁸"),
    SciConstant("13", "Bohr Radius", "a₀", "5.29177210903×10⁻¹¹"),
    SciConstant("14", "Fine-Structure Constant", "α", "7.2973525693×10⁻³"),
    SciConstant("15", "Classical Electron Radius", "rₑ", "2.8179403262×10⁻¹⁵"),
    SciConstant("16", "Compton Wavelength", "λc", "2.42631023867×10⁻¹²"),
    SciConstant("17", "Rydberg Constant", "R∞", "1.0973731568×10⁷"),
    SciConstant("18", "Avogadro Constant", "Nₐ", "6.02214076×10²³"),
    SciConstant("19", "Boltzmann Constant", "k", "1.380649×10⁻²³"),
    SciConstant("20", "Gas Constant", "R", "8.314462618"),
    SciConstant("21", "Faraday Constant", "F", "96485.33212"),
    SciConstant("22", "Bohr Magneton", "μB", "9.2740100783×10⁻²⁴"),
    SciConstant("23", "Nuclear Magneton", "μN", "5.0507837461×10⁻²⁷"),
    SciConstant("24", "Electron Magnetic Moment", "μₑ", "−9.2847647043×10⁻²⁴"),
    SciConstant("25", "Proton Magnetic Moment", "μₚ", "1.41060679736×10⁻²⁶"),
    SciConstant("26", "Neutron Magnetic Moment", "μₙ", "−9.6623651×10⁻²⁷"),
    SciConstant("27", "Atomic Mass Constant", "u", "1.66053906660×10⁻²⁷"),
    SciConstant("28", "Electron Volt", "eV", "1.602176634×10⁻¹⁹"),
    SciConstant("29", "Hartree Energy", "Eₕ", "4.359744722×10⁻¹⁸"),
    SciConstant("30", "Stefan–Boltzmann Constant", "σ", "5.670374419×10⁻⁸"),
    SciConstant("31", "Wien Constant", "b", "2.897771955×10⁻³"),
    SciConstant("32", "Standard Atmosphere", "atm", "101325"),
    SciConstant("33", "Molar Volume", "Vₘ", "22.413962×10⁻³"),
    SciConstant("34", "Astronomical Unit", "AU", "1.495978707×10¹¹"),
    SciConstant("35", "Light Year", "ly", "9.460730472×10¹⁵"),
    SciConstant("36", "Parsec", "pc", "3.085677581×10¹⁶"),
    SciConstant("37", "Solar Mass", "M☉", "1.98847×10³⁰"),
    SciConstant("38", "Earth Mass", "M⊕", "5.9722×10²⁴"),
    SciConstant("39", "Earth Radius", "R⊕", "6.371×10⁶"),
    SciConstant("40", "Solar Radius", "R☉", "6.957×10⁸"),
    SciConstant("41", "Solar Luminosity", "L☉", "3.828×10²⁶"),
    SciConstant("42", "Standard Pressure", "p₀", "100000"),
    SciConstant("43", "Vacuum Impedance", "Z₀", "376.730313668"),
    SciConstant("44", "Josephson Constant", "KJ", "4.835978484×10¹⁴"),
    SciConstant("45", "von Klitzing Constant", "RK", "25812.80745"),
    SciConstant("46", "Coulomb Constant", "kₑ", "8.9875517923×10⁹"),
    SciConstant("47", "Loschmidt Constant", "n₀", "2.686780111×10²⁵")
)

@Composable
fun ConstantsDialog(viewModel: CalculatorViewModel, isDark: Boolean) {
    val bg = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val textCol = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBorder = if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)
    val headerBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val rowAltBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF8FAFC)
    val accentBlue = Color(0xFF2563EB)

    var searchQuery by remember { mutableStateOf("") }

    val filteredConstants = remember(searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            scientificConstants
        } else {
            val query = searchQuery.lowercase()
            scientificConstants.filter {
                it.name.lowercase().contains(query) || it.symbol.lowercase().contains(query)
            }
        }
    }

    Dialog(onDismissRequest = { viewModel.showConstants.value = false }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(4.dp)
                .testTag("dialog_constants"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = bg),
            border = BorderStroke(2.dp, cardBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBg)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Scientific Constants",
                        color = textCol,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { viewModel.showConstants.value = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dialog",
                            tint = textCol.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or symbol...", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentBlue,
                        unfocusedBorderColor = cardBorder.copy(alpha = 0.5f)
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = textCol),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = textCol.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBg.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Constant", color = textCol.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.38f))
                    Text(text = "Symbol", color = textCol.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.14f), textAlign = TextAlign.Center)
                    Text(text = "Value", color = textCol.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.48f))
                }

                Divider(color = cardBorder.copy(alpha = 0.3f), thickness = 1.dp)

                // Constants List Table Body
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (filteredConstants.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No constants match search query.",
                                    color = textCol.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        items(filteredConstants) { constant ->
                            val isEven = constant.no.toIntOrNull()?.let { it % 2 == 0 } ?: false
                            val rowBg = if (isEven) rowAltBg else Color.Transparent

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg)
                                    .clickable {
                                        viewModel.pressScientificConstant(constant.symbol)
                                        viewModel.showConstants.value = false
                                    }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = constant.name,
                                    color = textCol,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(0.38f)
                                )
                                Text(
                                    text = constant.symbol,
                                    color = accentBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif,
                                    modifier = Modifier.weight(0.14f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = constant.valueText,
                                    color = textCol.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(0.48f)
                                )
                            }
                            Divider(color = cardBorder.copy(alpha = 0.15f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

enum class ConversionCategory(val displayName: String, val emoji: String) {
    LENGTH("Length", "📏"),
    MASS("Mass", "⚖️"),
    VOLUME("Volume", "🧪"),
    AREA("Area", "🗺️"),
    TEMPERATURE("Temperature", "🌡️"),
    PRESSURE("Pressure", "🎈"),
    ENERGY("Energy", "⚡"),
    POWER("Power", "🔌"),
    TIME("Time", "⏱️"),
    SPEED("Speed", "🚀"),
    DATA("Data Storage", "💾")
}

data class ConversionUnit(val name: String, val symbol: String, val factorToBase: Double)

val lengthUnits = listOf(
    ConversionUnit("Meter", "m", 1.0),
    ConversionUnit("Kilometer", "km", 1000.0),
    ConversionUnit("Centimeter", "cm", 0.01),
    ConversionUnit("Millimeter", "mm", 0.001),
    ConversionUnit("Mile", "mi", 1609.344),
    ConversionUnit("Yard", "yd", 0.9144),
    ConversionUnit("Foot", "ft", 0.3048),
    ConversionUnit("Inch", "in", 0.0254),
    ConversionUnit("Nautical Mile", "n mile", 1852.0)
)

val massUnits = listOf(
    ConversionUnit("Gram", "g", 1.0),
    ConversionUnit("Kilogram", "kg", 1000.0),
    ConversionUnit("Milligram", "mg", 0.001),
    ConversionUnit("Pound", "lb", 453.59237),
    ConversionUnit("Ounce", "oz", 28.349523125)
)

val volumeUnits = listOf(
    ConversionUnit("Liter", "L", 1.0),
    ConversionUnit("Milliliter", "mL", 0.001),
    ConversionUnit("Cubic Meter", "m³", 1000.0),
    ConversionUnit("Gallon (US)", "gal (US)", 3.785411784),
    ConversionUnit("Gallon (UK)", "gal (UK)", 4.54609),
    ConversionUnit("Quart (US)", "qt", 0.946352946),
    ConversionUnit("Pint (US)", "pt (US)", 0.473176473),
    ConversionUnit("Pint (UK)", "pt (UK)", 0.56826125),
    ConversionUnit("Cup (US)", "cup", 0.236588236)
)

val areaUnits = listOf(
    ConversionUnit("Square Meter", "m²", 1.0),
    ConversionUnit("Square Kilometer", "km²", 1000000.0),
    ConversionUnit("Square Centimeter", "cm²", 0.0001),
    ConversionUnit("Square Mile", "mi²", 2589988.110336),
    ConversionUnit("Acre", "acre", 4046.8564224),
    ConversionUnit("Hectare", "ha", 10000.0)
)

val tempUnits = listOf(
    ConversionUnit("Celsius", "°C", 1.0),
    ConversionUnit("Fahrenheit", "°F", 1.0),
    ConversionUnit("Kelvin", "K", 1.0)
)

val pressureUnits = listOf(
    ConversionUnit("Pascal", "Pa", 1.0),
    ConversionUnit("Kilopascal", "kPa", 1000.0),
    ConversionUnit("Atmosphere", "atm", 101325.0),
    ConversionUnit("mmHg", "mmHg", 133.322387415),
    ConversionUnit("psi (lbf/in²)", "psi", 6894.757293),
    ConversionUnit("kgf/cm²", "kgf/cm²", 98066.5)
)

val powerUnits = listOf(
    ConversionUnit("Watt", "W", 1.0),
    ConversionUnit("Kilowatt", "kW", 1000.0),
    ConversionUnit("Horsepower", "hp", 745.699872)
)

val energyUnits = listOf(
    ConversionUnit("Joule", "J", 1.0),
    ConversionUnit("Calorie", "cal", 4.184),
    ConversionUnit("kgf·m", "kgf·m", 9.80665)
)

val timeUnits = listOf(
    ConversionUnit("Second", "s", 1.0),
    ConversionUnit("Minute", "min", 60.0),
    ConversionUnit("Hour", "h", 3600.0),
    ConversionUnit("Day", "d", 86400.0),
    ConversionUnit("Week", "wk", 604800.0),
    ConversionUnit("Year", "yr", 31536000.0)
)

val speedUnits = listOf(
    ConversionUnit("Meter/second", "m/s", 1.0),
    ConversionUnit("Kilometer/hour", "km/h", 0.2777777777777778),
    ConversionUnit("Mile/hour", "mph", 0.44704),
    ConversionUnit("Knot", "kt", 0.514444)
)

val dataUnits = listOf(
    ConversionUnit("Byte", "B", 1.0),
    ConversionUnit("Kilobyte", "KB", 1024.0),
    ConversionUnit("Megabyte", "MB", 1048576.0),
    ConversionUnit("Gigabyte", "GB", 1073741824.0),
    ConversionUnit("Terabyte", "TB", 1099511627776.0)
)

fun getUnitsForCategory(category: ConversionCategory): List<ConversionUnit> {
    return when (category) {
        ConversionCategory.LENGTH -> lengthUnits
        ConversionCategory.MASS -> massUnits
        ConversionCategory.VOLUME -> volumeUnits
        ConversionCategory.AREA -> areaUnits
        ConversionCategory.TEMPERATURE -> tempUnits
        ConversionCategory.PRESSURE -> pressureUnits
        ConversionCategory.ENERGY -> energyUnits
        ConversionCategory.POWER -> powerUnits
        ConversionCategory.TIME -> timeUnits
        ConversionCategory.SPEED -> speedUnits
        ConversionCategory.DATA -> dataUnits
    }
}

fun formatDouble(value: Double): String {
    if (!value.isFinite()) return "Error"
    if (value == 0.0) return "0"
    if (kotlin.math.abs(value) >= 1e7 || kotlin.math.abs(value) <= 1e-4) {
        val rawScientific = String.format(java.util.Locale.US, "%.6e", value)
        val parts = rawScientific.split('e', 'E')
        if (parts.size == 2) {
            val mantissa = parts[0].trimEnd('0').trimEnd('.')
            val exponent = parts[1].toIntOrNull() ?: 0
            return if (exponent != 0) "$mantissa×10^$exponent" else mantissa
        }
        return rawScientific
    }
    return String.format(java.util.Locale.US, "%.10f", value).trimEnd('0').trimEnd('.')
}

@Composable
fun UnitConverterDialog(viewModel: CalculatorViewModel, isDark: Boolean) {
    val bg = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val textCol = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBorder = if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)
    val headerBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val accentBlue = Color(0xFF2563EB)

    // Category states
    var selectedCategory by remember { mutableStateOf(ConversionCategory.LENGTH) }
    val currentUnits = remember(selectedCategory) { getUnitsForCategory(selectedCategory) }

    var fromUnit by remember { mutableStateOf(lengthUnits[0]) }
    var toUnit by remember { mutableStateOf(lengthUnits[1]) }

    // Reset units when category changes
    LaunchedEffect(selectedCategory) {
        val units = getUnitsForCategory(selectedCategory)
        fromUnit = units.firstOrNull() ?: lengthUnits[0]
        toUnit = units.getOrNull(1) ?: fromUnit
    }

    var inputValueStr by remember { mutableStateOf("1") }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedFrom by remember { mutableStateOf(false) }
    var expandedTo by remember { mutableStateOf(false) }

    val convertedValue = remember(inputValueStr, fromUnit, toUnit, selectedCategory) {
        val doubleVal = inputValueStr.toDoubleOrNull() ?: 0.0
        if (selectedCategory == ConversionCategory.TEMPERATURE) {
            val celsius = when (fromUnit.name) {
                "Celsius" -> doubleVal
                "Fahrenheit" -> (doubleVal - 32.0) * 5.0 / 9.0
                "Kelvin" -> doubleVal - 273.15
                else -> doubleVal
            }
            when (toUnit.name) {
                "Celsius" -> celsius
                "Fahrenheit" -> celsius * 9.0 / 5.0 + 32.0
                "Kelvin" -> celsius + 273.15
                else -> celsius
            }
        } else {
            val valueInBase = doubleVal * fromUnit.factorToBase
            valueInBase / toUnit.factorToBase
        }
    }

    val resultValueStr = remember(convertedValue) {
        formatDouble(convertedValue)
    }

    Dialog(onDismissRequest = { viewModel.showUnitConverter.value = false }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(4.dp)
                .testTag("dialog_unit_converter"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = bg),
            border = BorderStroke(2.dp, cardBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                // Header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBg)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Unit Converter (CONV)",
                        color = textCol,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { viewModel.showUnitConverter.value = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dialog",
                            tint = textCol.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // CONTENT COLUMN
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Category Selection (Dropdown)
                    Text(
                        text = "Category",
                        color = textCol.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { expandedCategory = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                contentColor = textCol
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(selectedCategory.emoji + " ", fontSize = 16.sp)
                                    Text(
                                        text = selectedCategory.displayName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textCol
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select category dropdown",
                                    modifier = Modifier.size(20.dp),
                                    tint = textCol
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = expandedCategory,
                            onDismissRequest = { expandedCategory = false },
                            modifier = Modifier
                                .width(220.dp)
                                .background(bg)
                        ) {
                            ConversionCategory.values().forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(cat.emoji + " ", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(cat.displayName, color = textCol, fontSize = 14.sp)
                                        }
                                    },
                                    onClick = {
                                        selectedCategory = cat
                                        expandedCategory = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                        // Input Value Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "From (${fromUnit.symbol})",
                                        color = textCol.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    // Paste Ans helper
                                    Text(
                                        text = "Paste Ans",
                                        color = accentBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable {
                                                val ansVal = viewModel.getAnsValue()
                                                inputValueStr = if (ansVal.isFinite()) {
                                                    if (ansVal % 1.0 == 0.0) ansVal.toLong().toString() else ansVal.toString()
                                                } else {
                                                    "1"
                                                }
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                OutlinedTextField(
                                    value = inputValueStr,
                                    onValueChange = { inputValueStr = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("unit_conv_input"),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 16.sp,
                                        color = textCol
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentBlue,
                                        unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Unit Selection (From & To dropdown selectors)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // From unit box
                            Box(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = { expandedFrom = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                        contentColor = textCol
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${fromUnit.name} (${fromUnit.symbol})",
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select from unit",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = expandedFrom,
                                    onDismissRequest = { expandedFrom = false },
                                    modifier = Modifier.background(bg)
                                ) {
                                    currentUnits.forEach { u ->
                                        DropdownMenuItem(
                                            text = { Text("${u.name} (${u.symbol})", color = textCol, fontSize = 13.sp) },
                                            onClick = {
                                                fromUnit = u
                                                expandedFrom = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Swap Button
                            IconButton(
                                onClick = {
                                    val temp = fromUnit
                                    fromUnit = toUnit
                                    toUnit = temp
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9), CircleShape)
                                    .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), CircleShape)
                            ) {
                                Text(
                                    text = "⇄",
                                    color = accentBlue,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // To unit box
                            Box(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = { expandedTo = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                        contentColor = textCol
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${toUnit.name} (${toUnit.symbol})",
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select to unit",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = expandedTo,
                                    onDismissRequest = { expandedTo = false },
                                    modifier = Modifier.background(bg)
                                ) {
                                    currentUnits.forEach { u ->
                                        DropdownMenuItem(
                                            text = { Text("${u.name} (${u.symbol})", color = textCol, fontSize = 13.sp) },
                                            onClick = {
                                                toUnit = u
                                                expandedTo = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Converted Result Display Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF070B14) else Color(0xFFF8FAFC)),
                            border = BorderStroke(2.dp, accentBlue.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Converted Value",
                                    color = textCol.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                SelectionContainer {
                                    Text(
                                        text = resultValueStr,
                                        color = textCol,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "${toUnit.name} (${toUnit.symbol})",
                                    color = accentBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Button
                        Button(
                            onClick = {
                                val mathExprCompatible = resultValueStr.replace("×10^", "*10^")
                                viewModel.insertValueToExpression(mathExprCompatible)
                                viewModel.showUnitConverter.value = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_unit_conv_insert"),
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Insert",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Insert into Calculator",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                }
            }
        }
    }
}
