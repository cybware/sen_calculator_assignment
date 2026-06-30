package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CalculatorViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            // Map theme colors programmatically to allow dynamic instant toggles
            val themeColors = if (isDarkMode) DarkCalcColors else LightCalcColors

            // Apply dynamic Edge-to-Edge System Bar coloring
            val view = LocalView.current
            val window = (view.context as? Activity)?.window
            if (!view.isInEditMode && window != null) {
                SideEffect {
                    val windowInsetsController = WindowCompat.getInsetsController(window, view)
                    window.statusBarColor = Color.Transparent.toArgb()
                    window.navigationBarColor = Color.Transparent.toArgb()
                    windowInsetsController.isAppearanceLightStatusBars = !isDarkMode
                    windowInsetsController.isAppearanceLightNavigationBars = !isDarkMode
                }
            }

            CalculatorTheme(themeColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = themeColors.background
                ) {
                    CalculatorScreen(viewModel = viewModel, colors = themeColors)
                }
            }
        }
    }
}

/**
 * Clean container to apply localized styles or settings for the custom calculator theme.
 */
@Composable
fun CalculatorTheme(
    colors: CalcThemeColors,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = colors.background,
            surface = colors.displayBackground
        ),
        content = content
    )
}

/**
 * Master layout representing the precision calculator application screen.
 */
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    colors: CalcThemeColors
) {
    val expression by viewModel.expression.collectAsState()
    val realtimeResult by viewModel.realtimeResult.collectAsState()
    val history by viewModel.history.collectAsState()
    val isDegreeMode by viewModel.isDegreeMode.collectAsState()
    val isInverseMode by viewModel.isInverseMode.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val historyListState = rememberLazyListState()

    // Automatically scroll historical tape down when new entries are added
    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            historyListState.animateScrollToItem(history.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing // Prevent camera notch or bottom navigation bar clipping
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CALCULATOR",
                    color = colors.headerLogoColor,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // Theme Switcher Button with spring-backed interactive tactile feedback
                val themeInteractionSource = remember { MutableInteractionSource() }
                val themePressed by themeInteractionSource.collectIsPressedAsState()
                val themeScale by animateFloatAsState(
                    targetValue = if (themePressed) 0.85f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "ThemeToggleScale"
                )

                IconButton(
                    onClick = { viewModel.toggleTheme() },
                    interactionSource = themeInteractionSource,
                    modifier = Modifier.scale(themeScale)
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = colors.textSecondary
                    )
                }
            }

            // Inset Recessed Display Card (Occupies ~35% space)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.displayBackground)
                    .border(BorderStroke(1.dp, colors.numericKeyBorder), RoundedCornerShape(16.dp))
                    .drawBehind {
                        // Drawing professional grid paper matrix pattern
                        val spacing = 24.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            drawLine(
                                color = colors.gridColor,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 0.5.dp.toPx()
                            )
                            x += spacing
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(
                                color = colors.gridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 0.5.dp.toPx()
                            )
                            y += spacing
                        }
                    }
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // History Tape List (Scrolls Vertically, items are clickable to pull back into calculation)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (history.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No history yet",
                                    color = colors.textSecondary.copy(alpha = 0.4f),
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                LazyColumn(
                                    state = historyListState,
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    items(history) { item ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.selectHistoryItem(item) }
                                                .padding(vertical = 4.dp),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = item.expression,
                                                color = colors.textSecondary.copy(alpha = 0.7f),
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Monospace,
                                                textAlign = TextAlign.End
                                            )
                                            Text(
                                                text = item.result,
                                                color = colors.operatorColor,
                                                fontSize = 18.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.End
                                            )
                                        }
                                    }
                                }

                                // Clear History trigger
                                IconButton(
                                    onClick = { viewModel.clearHistory() },
                                    modifier = Modifier.align(Alignment.Top)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clear History",
                                        tint = colors.textSecondary.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Active Working Expression Line (Right-aligned, auto-scales font)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Angle Mode indicator badge
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(colors.functionKeyBg, shape = RoundedCornerShape(4.dp))
                                .border(BorderStroke(0.5.dp, colors.numericKeyBorder), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isDegreeMode) "DEG" else "RAD",
                                color = colors.operatorColor,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val activeFontSize = when {
                            expression.length > 25 -> 18.sp
                            expression.length > 15 -> 24.sp
                            else -> 30.sp
                        }

                        Text(
                            text = expression.ifEmpty { "0" },
                            color = colors.textPrimary,
                            fontSize = activeFontSize,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.End,
                            maxLines = 1
                        )
                    }

                    // Result Display Line (Updates live, auto-scales size)
                    val resultFontSize = when {
                        realtimeResult.length > 18 -> 26.sp
                        realtimeResult.length > 12 -> 36.sp
                        else -> 46.sp
                    }

                    Text(
                        text = realtimeResult.ifEmpty { "0" },
                        color = if (realtimeResult == "Error") Color(0xFFEF4444) else colors.textPrimary,
                        fontSize = resultFontSize,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1
                    )
                }
            }

            // Tactile Scientific & Numeric Keypad (Occupies ~65% space)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: Angle Segmented Toggle & Inv & Basic trig functions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    SegmentedToggleButton(
                        isDegreeMode = isDegreeMode,
                        onToggle = { viewModel.toggleDegreeMode() },
                        colors = colors,
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = "2nd",
                        onClick = { viewModel.toggleInverseMode() },
                        colors = colors,
                        isFunc = true,
                        isSelected = isInverseMode,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = if (isInverseMode) "sin⁻¹" else "sin",
                        onClick = { viewModel.onKeyPress("sin") },
                        colors = colors,
                        isFunc = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = if (isInverseMode) "cos⁻¹" else "cos",
                        onClick = { viewModel.onKeyPress("cos") },
                        colors = colors,
                        isFunc = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = if (isInverseMode) "tan⁻¹" else "tan",
                        onClick = { viewModel.onKeyPress("tan") },
                        colors = colors,
                        isFunc = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // Row 2: Hyperbolic functions & Exponentiation triggers
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CalculatorKey(
                        text = if (isInverseMode) "sinh⁻¹" else "sinh",
                        onClick = { viewModel.onKeyPress("sinh") },
                        colors = colors,
                        isFunc = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = if (isInverseMode) "cosh⁻¹" else "cosh",
                        onClick = { viewModel.onKeyPress("cosh") },
                        colors = colors,
                        isFunc = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = if (isInverseMode) "tanh⁻¹" else "tanh",
                        onClick = { viewModel.onKeyPress("tanh") },
                        colors = colors,
                        isFunc = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = if (isInverseMode) "ʸ√x" else "xʸ",
                        onClick = { viewModel.onKeyPress("xʸ") },
                        colors = colors,
                        isFunc = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = "eˣ",
                        onClick = { viewModel.onKeyPress("eˣ") },
                        colors = colors,
                        isFunc = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // Row 3: Permutation, Combination, Factorial, Logarithms
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CalculatorKey(
                        text = "nPr",
                        onClick = { viewModel.onKeyPress("nPr") },
                        colors = colors,
                        isFunc = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = "nCr",
                        onClick = { viewModel.onKeyPress("nCr") },
                        colors = colors,
                        isFunc = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = "x!",
                        onClick = { viewModel.onKeyPress("x!") },
                        colors = colors,
                        isFunc = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = if (isInverseMode) "10^" else "log",
                        onClick = { viewModel.onKeyPress("log") },
                        colors = colors,
                        isFunc = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = if (isInverseMode) "e^" else "ln",
                        onClick = { viewModel.onKeyPress("ln") },
                        colors = colors,
                        isFunc = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // Row 4: Math delimiters & Clean triggers
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CalculatorKey(
                        text = if (isInverseMode) "π" else "(",
                        onClick = { viewModel.onKeyPress(if (isInverseMode) "π" else "(") },
                        colors = colors,
                        isFunc = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = if (isInverseMode) "e" else ")",
                        onClick = { viewModel.onKeyPress(if (isInverseMode) "e" else ")") },
                        colors = colors,
                        isFunc = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = if (isInverseMode) "x²" else "√",
                        onClick = { viewModel.onKeyPress(if (isInverseMode) "x²" else "√") },
                        colors = colors,
                        isFunc = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = "AC",
                        onClick = { viewModel.onKeyPress("AC") },
                        colors = colors,
                        isClear = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = "DEL",
                        onClick = { viewModel.onKeyPress("DEL") },
                        colors = colors,
                        isFunc = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // Row 5: Numeric 7,8,9 & main operators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CalculatorKey(
                        text = "7",
                        onClick = { viewModel.onKeyPress("7") },
                        colors = colors,
                        isNum = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = "8",
                        onClick = { viewModel.onKeyPress("8") },
                        colors = colors,
                        isNum = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = "9",
                        onClick = { viewModel.onKeyPress("9") },
                        colors = colors,
                        isNum = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = "÷",
                        onClick = { viewModel.onKeyPress("÷") },
                        colors = colors,
                        isOp = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = "×",
                        onClick = { viewModel.onKeyPress("×") },
                        colors = colors,
                        isOp = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // Row 6: Numeric 4,5,6 & addition/subtraction operators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CalculatorKey(
                        text = "4",
                        onClick = { viewModel.onKeyPress("4") },
                        colors = colors,
                        isNum = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = "5",
                        onClick = { viewModel.onKeyPress("5") },
                        colors = colors,
                        isNum = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = "6",
                        onClick = { viewModel.onKeyPress("6") },
                        colors = colors,
                        isNum = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = "-",
                        onClick = { viewModel.onKeyPress("-") },
                        colors = colors,
                        isOp = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CalculatorKey(
                        text = "+",
                        onClick = { viewModel.onKeyPress("+") },
                        colors = colors,
                        isOp = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // Row 7 & 8: Combined layout for merged '=' key
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f) // Maintain proportional double key height safely
                ) {
                    // Left Column housing 1, 2, 3 and 0, .
                    Column(
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            CalculatorKey(
                                text = "1",
                                onClick = { viewModel.onKeyPress("1") },
                                colors = colors,
                                isNum = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            CalculatorKey(
                                text = "2",
                                onClick = { viewModel.onKeyPress("2") },
                                colors = colors,
                                isNum = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            CalculatorKey(
                                text = "3",
                                onClick = { viewModel.onKeyPress("3") },
                                colors = colors,
                                isNum = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            CalculatorKey(
                                text = "0",
                                onClick = { viewModel.onKeyPress("0") },
                                colors = colors,
                                isNum = true,
                                modifier = Modifier
                                    .weight(2f)
                                    .fillMaxHeight()
                            )
                            CalculatorKey(
                                text = ".",
                                onClick = { viewModel.onKeyPress(".") },
                                colors = colors,
                                isNum = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }

                    // Merged Large Equals action key (Occupies 2 columns width and spans both rows)
                    CalculatorKey(
                        text = "=",
                        onClick = { viewModel.onKeyPress("=") },
                        colors = colors,
                        isEquals = true,
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

/**
 * Segmented toggle control for DEG vs RAD selection.
 */
@Composable
fun SegmentedToggleButton(
    isDegreeMode: Boolean,
    onToggle: () -> Unit,
    colors: CalcThemeColors,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by themePressedState(interactionSource)

    Box(
        modifier = modifier
            .testTag("toggle_deg_rad")
            .scale(if (isPressed) 0.96f else 1.0f)
            .background(colors.functionKeyBg, shape = RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, colors.numericKeyBorder), RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // DEG segment
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        color = if (isDegreeMode) colors.background else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        BorderStroke(
                            width = if (isDegreeMode) 1.dp else 0.dp,
                            color = if (isDegreeMode) colors.numericKeyBorder else Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DEG",
                    color = if (isDegreeMode) colors.operatorColor else colors.textSecondary.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // RAD segment
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        color = if (!isDegreeMode) colors.background else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        BorderStroke(
                            width = if (!isDegreeMode) 1.dp else 0.dp,
                            color = if (!isDegreeMode) colors.numericKeyBorder else Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RAD",
                    color = if (!isDegreeMode) colors.operatorColor else colors.textSecondary.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Custom spring-backed scaling key rendering class.
 */
@Composable
fun CalculatorKey(
    text: String,
    onClick: () -> Unit,
    colors: CalcThemeColors,
    modifier: Modifier = Modifier,
    isNum: Boolean = false,
    isOp: Boolean = false,
    isFunc: Boolean = false,
    isEquals: Boolean = false,
    isClear: Boolean = false,
    isSelected: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ButtonPressScale"
    )

    val bg = when {
        isEquals -> colors.equalsBg
        isClear -> colors.clearBg
        isSelected -> colors.operatorColor.copy(alpha = 0.3f)
        isFunc || isOp -> colors.functionKeyBg
        else -> colors.numericKeyBg
    }

    val textColor = when {
        isEquals -> colors.equalsText
        isClear -> colors.clearText
        isOp -> colors.operatorColor
        isFunc -> colors.functionKeyText
        isSelected -> colors.operatorColor
        else -> colors.numericKeyText
    }

    val borderStroke = when {
        isEquals -> BorderStroke(1.dp, colors.equalsBg)
        isClear -> BorderStroke(1.dp, colors.clearBg)
        isSelected -> BorderStroke(1.5.dp, colors.operatorColor)
        else -> BorderStroke(1.dp, colors.numericKeyBorder)
    }

    val fontFamily = if (isNum || text == "." || text == "0") {
        FontFamily.SansSerif
    } else {
        FontFamily.Monospace
    }

    val fontWeight = if (isEquals || isClear || isOp || isFunc) {
        FontWeight.Bold
    } else {
        FontWeight.Medium
    }

    val fontSize = if (isNum) 22.sp else 15.sp

    Box(
        modifier = modifier
            .testTag("key_$text")
            .scale(scale)
            .background(color = bg, shape = RoundedCornerShape(12.dp))
            .border(borderStroke, shape = RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable default grey bounds for clean tactile response
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontSize = fontSize
        )
    }
}

/**
 * Convenient helper to retrieve pressure states of custom buttons.
 */
@Composable
fun themePressedState(interactionSource: MutableInteractionSource): State<Boolean> {
    return interactionSource.collectIsPressedAsState()
}

// Global Theming Configurations (colors matching Design System spec)
data class CalcThemeColors(
    val background: Color,
    val displayBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val operatorColor: Color,
    val functionKeyBg: Color,
    val functionKeyText: Color,
    val numericKeyBg: Color,
    val numericKeyBorder: Color,
    val numericKeyText: Color,
    val equalsBg: Color,
    val equalsText: Color,
    val clearBg: Color,
    val clearText: Color,
    val gridColor: Color,
    val headerLogoColor: Color
)

val DarkCalcColors = CalcThemeColors(
    background = Color(0xFF0F172A),
    displayBackground = Color(0xFF0B1326),
    textPrimary = Color(0xFFDAE2FD),
    textSecondary = Color(0xFF909BB1),
    operatorColor = Color(0xFF38BDF8),
    functionKeyBg = Color(0xFF1E293B),
    functionKeyText = Color(0xFF38BDF8),
    numericKeyBg = Color(0xFF0F172A),
    numericKeyBorder = Color(0xFF1E293B),
    numericKeyText = Color(0xFFFFFFFF),
    equalsBg = Color(0xFFF97316),
    equalsText = Color(0xFFFFFFFF),
    clearBg = Color(0xFF93000A),
    clearText = Color(0xFFFFFFFF),
    gridColor = Color(0xFF222A3D).copy(alpha = 0.35f),
    headerLogoColor = Color(0xFFFFA07A)
)

val LightCalcColors = CalcThemeColors(
    background = Color(0xFFF8FAFC),
    displayBackground = Color(0xFFF1F5F9),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    operatorColor = Color(0xFF0284C7),
    functionKeyBg = Color(0xFFE2E8F0),
    functionKeyText = Color(0xFF0284C7),
    numericKeyBg = Color(0xFFFFFFFF),
    numericKeyBorder = Color(0xFFCBD5E1),
    numericKeyText = Color(0xFF0F172A),
    equalsBg = Color(0xFFEA580C),
    equalsText = Color(0xFFFFFFFF),
    clearBg = Color(0xFFDC2626),
    clearText = Color(0xFFFFFFFF),
    gridColor = Color(0xFFCBD5E1).copy(alpha = 0.5f),
    headerLogoColor = Color(0xFFD84B20)
)
