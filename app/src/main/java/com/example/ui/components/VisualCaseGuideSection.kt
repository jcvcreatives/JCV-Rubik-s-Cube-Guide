package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.PresetScramble

enum class CaseCategory(val label: String) {
    ALL("All Cases"),
    OLL("OLL (Orientation)"),
    TWO_LOOK("2-Look Beginner"),
    PLL("PLL (Permutation)")
}

data class VisualCubeCase(
    val id: String,
    val badgeTitle: String,
    val subtitle: String,
    val category: CaseCategory,
    val algorithm: String,
    val practiceScrambleSeq: String,
    val uGrid: List<Int>,      // 9 stickers: 0=Gray, 1=Yellow, 2=Green, 3=Red, 4=Blue, 5=Orange
    val topBars: List<Int>,    // 3 side stickers along Back (top)
    val bottomBars: List<Int>, // 3 side stickers along Front (bottom)
    val leftBars: List<Int>,   // 3 side stickers along Left
    val rightBars: List<Int>   // 3 side stickers along Right
)

data class NotationGuideEntry(
    val token: String,
    val name: String,
    val beginnerMeaning: String,
    val highlightCols: List<Int> = emptyList(),
    val highlightRows: List<Int> = emptyList(),
    val arrowDirection: String = "UP" // UP, DOWN, LEFT, RIGHT, CW, CCW
)

val BEGINNER_NOTATION_GUIDE = listOf(
    NotationGuideEntry(
        token = "R",
        name = "Right (1 Layer)",
        beginnerMeaning = "Push the Right outer column UP (away from you) 1 quarter-turn.",
        highlightCols = listOf(2),
        arrowDirection = "UP"
    ),
    NotationGuideEntry(
        token = "R'",
        name = "Right Prime",
        beginnerMeaning = "Pull the Right outer column DOWN (toward you) 1 quarter-turn.",
        highlightCols = listOf(2),
        arrowDirection = "DOWN"
    ),
    NotationGuideEntry(
        token = "r",
        name = "Wide Right (2 Layers)",
        beginnerMeaning = "Grab the Right TWO columns together (Right + Middle) and push UP.",
        highlightCols = listOf(1, 2),
        arrowDirection = "UP"
    ),
    NotationGuideEntry(
        token = "r'",
        name = "Wide Right Prime (2 Layers)",
        beginnerMeaning = "Grab the Right TWO columns together (Right + Middle) and pull DOWN.",
        highlightCols = listOf(1, 2),
        arrowDirection = "DOWN"
    ),
    NotationGuideEntry(
        token = "U",
        name = "Up / Top Clockwise",
        beginnerMeaning = "Push the Top row to the LEFT (clockwise looking from above).",
        highlightRows = listOf(0),
        arrowDirection = "LEFT"
    ),
    NotationGuideEntry(
        token = "U'",
        name = "Up / Top Prime",
        beginnerMeaning = "Push the Top row to the RIGHT (counter-clockwise from above).",
        highlightRows = listOf(0),
        arrowDirection = "RIGHT"
    ),
    NotationGuideEntry(
        token = "U2 / U2'",
        name = "Top Double Turn (180°)",
        beginnerMeaning = "Turn the Top layer TWICE (halfway around). U2 and U2' end in the exact same position.",
        highlightRows = listOf(0),
        arrowDirection = "LEFT"
    ),
    NotationGuideEntry(
        token = "F / F'",
        name = "Front Face",
        beginnerMeaning = "F = tilt the Front face Clockwise (right). F' = tilt Front Counter-Clockwise (left).",
        highlightRows = listOf(0, 1, 2),
        arrowDirection = "CW"
    ),
    NotationGuideEntry(
        token = "f / f'",
        name = "Wide Front (2 Layers)",
        beginnerMeaning = "Turn the Front TWO layers together Clockwise (f) or Counter-Clockwise (f').",
        highlightRows = listOf(0, 1, 2),
        arrowDirection = "CW"
    ),
    NotationGuideEntry(
        token = "L / L'",
        name = "Left (1 Layer)",
        beginnerMeaning = "L = pull Left column DOWN toward you. L' = push Left column UP away from you.",
        highlightCols = listOf(0),
        arrowDirection = "DOWN"
    ),
    NotationGuideEntry(
        token = "D / D'",
        name = "Down / Bottom Layer",
        beginnerMeaning = "D = push Bottom row to the RIGHT. D' = push Bottom row to the LEFT.",
        highlightRows = listOf(2),
        arrowDirection = "RIGHT"
    )
)

fun explainMoveTokenForBeginner(token: String): String = when (token) {
    "R" -> "Right layer UP ↑"
    "R'" -> "Right layer DOWN ↓"
    "R2", "R2'" -> "Right layer TWICE (180°)"
    "r" -> "Right 2 layers UP ↑↑"
    "r'" -> "Right 2 layers DOWN ↓↓"
    "r2", "r2'" -> "Right 2 layers TWICE (180°)"
    "L" -> "Left layer DOWN ↓"
    "L'" -> "Left layer UP ↑"
    "L2", "L2'" -> "Left layer TWICE (180°)"
    "l" -> "Left 2 layers DOWN ↓↓"
    "l'" -> "Left 2 layers UP ↑↑"
    "U" -> "Top layer LEFT ←"
    "U'" -> "Top layer RIGHT →"
    "U2", "U2'" -> "Top layer TWICE (180°)"
    "u" -> "Top 2 layers LEFT ←←"
    "u'" -> "Top 2 layers RIGHT →→"
    "D" -> "Bottom layer RIGHT →"
    "D'" -> "Bottom layer LEFT ←"
    "D2", "D2'" -> "Bottom layer TWICE (180°)"
    "F" -> "Front face Clockwise ↻"
    "F'" -> "Front face Counter-CW ↺"
    "F2", "F2'" -> "Front face TWICE (180°)"
    "f" -> "Front 2 layers Clockwise ↻"
    "f'" -> "Front 2 layers Counter-CW ↺"
    "B" -> "Back face Clockwise"
    "B'" -> "Back face Counter-CW"
    "B2" -> "Back face TWICE (180°)"
    else -> token
}

val VISUAL_CUBE_CASES = listOf(
    VisualCubeCase(
        id = "oll_8",
        badgeTitle = "OLL 8",
        subtitle = "Small Lightning Bolt · Wide Sune",
        category = CaseCategory.OLL,
        algorithm = "r' U' R U' R' U2' r",
        practiceScrambleSeq = "L' U2 R U R' U L",
        uGrid = listOf(
            1, 0, 0,
            1, 1, 0,
            0, 1, 0
        ),
        topBars = listOf(0, 1, 1),
        bottomBars = listOf(1, 0, 0),
        leftBars = listOf(0, 0, 0),
        rightBars = listOf(0, 1, 1)
    ),
    VisualCubeCase(
        id = "oll_25",
        badgeTitle = "OLL 25",
        subtitle = "Bowtie · Diagonal Corner Twist",
        category = CaseCategory.OLL,
        algorithm = "F' r U R' U' r' F R",
        practiceScrambleSeq = "R' F' L F R F' L' F",
        uGrid = listOf(
            0, 1, 1,
            1, 1, 1,
            1, 1, 0
        ),
        topBars = listOf(0, 0, 0),
        bottomBars = listOf(0, 0, 1),
        leftBars = listOf(1, 0, 0),
        rightBars = listOf(0, 0, 0)
    ),
    VisualCubeCase(
        id = "oll_27",
        badgeTitle = "OLL 27",
        subtitle = "Sune · 3-Corner Cycle",
        category = CaseCategory.TWO_LOOK,
        algorithm = "R U R' U R U2' R'",
        practiceScrambleSeq = "R U2 R' U' R U' R'",
        uGrid = listOf(
            0, 1, 0,
            1, 1, 1,
            1, 1, 0
        ),
        topBars = listOf(0, 0, 1),
        bottomBars = listOf(0, 0, 1),
        leftBars = listOf(1, 0, 0),
        rightBars = listOf(0, 0, 0)
    ),
    VisualCubeCase(
        id = "oll_26",
        badgeTitle = "OLL 26",
        subtitle = "Anti-Sune · Reverse 3-Corner",
        category = CaseCategory.TWO_LOOK,
        algorithm = "R U2 R' U' R U' R'",
        practiceScrambleSeq = "R U R' U R U2 R'",
        uGrid = listOf(
            0, 1, 1,
            1, 1, 1,
            0, 1, 0
        ),
        topBars = listOf(1, 0, 0),
        bottomBars = listOf(1, 0, 0),
        leftBars = listOf(0, 0, 0),
        rightBars = listOf(0, 0, 1)
    ),
    VisualCubeCase(
        id = "oll_21",
        badgeTitle = "OLL 21",
        subtitle = "H-Case · Double Headlights",
        category = CaseCategory.TWO_LOOK,
        algorithm = "F R U R' U' R U R' U' F'",
        practiceScrambleSeq = "F U R U' R' U R U' R' F'",
        uGrid = listOf(
            0, 1, 0,
            1, 1, 1,
            0, 1, 0
        ),
        topBars = listOf(1, 0, 1),
        bottomBars = listOf(1, 0, 1),
        leftBars = listOf(0, 0, 0),
        rightBars = listOf(0, 0, 0)
    ),
    VisualCubeCase(
        id = "oll_24",
        badgeTitle = "OLL 24",
        subtitle = "Chameleon · Sledgehammer",
        category = CaseCategory.OLL,
        algorithm = "r U R' U' r' F R F'",
        practiceScrambleSeq = "F R' F' L F R F' L'",
        uGrid = listOf(
            0, 1, 1,
            1, 1, 1,
            0, 1, 1
        ),
        topBars = listOf(1, 0, 0),
        bottomBars = listOf(1, 0, 0),
        leftBars = listOf(0, 0, 0),
        rightBars = listOf(0, 0, 0)
    ),
    VisualCubeCase(
        id = "oll_45",
        badgeTitle = "OLL 45",
        subtitle = "T-Shape · Line to Cross",
        category = CaseCategory.TWO_LOOK,
        algorithm = "F R U R' U' F'",
        practiceScrambleSeq = "F U R U' R' F'",
        uGrid = listOf(
            0, 0, 1,
            1, 1, 1,
            0, 0, 1
        ),
        topBars = listOf(0, 1, 0),
        bottomBars = listOf(0, 1, 0),
        leftBars = listOf(1, 0, 1),
        rightBars = listOf(0, 0, 0)
    ),
    VisualCubeCase(
        id = "pll_t",
        badgeTitle = "T Perm",
        subtitle = "PLL · Swap 2 Corners & 2 Edges",
        category = CaseCategory.PLL,
        algorithm = "R U R' U' R' F R2 U' R' U' R U R' F'",
        practiceScrambleSeq = "F R U' R' U R U R2 F' R U R U' R'",
        uGrid = listOf(
            1, 1, 1,
            1, 1, 1,
            1, 1, 1
        ),
        topBars = listOf(4, 4, 3),
        bottomBars = listOf(2, 2, 5),
        leftBars = listOf(5, 3, 5),
        rightBars = listOf(4, 5, 2)
    ),
    VisualCubeCase(
        id = "pll_ua",
        badgeTitle = "Ua Perm",
        subtitle = "PLL · 3-Edge Cycle",
        category = CaseCategory.PLL,
        algorithm = "R U' R U R U R U' R' U' R2",
        practiceScrambleSeq = "R2 U R U R' U' R' U' R' U R'",
        uGrid = listOf(
            1, 1, 1,
            1, 1, 1,
            1, 1, 1
        ),
        topBars = listOf(4, 4, 4),
        bottomBars = listOf(2, 5, 2),
        leftBars = listOf(5, 3, 5),
        rightBars = listOf(3, 2, 3)
    )
)

@Composable
fun VisualCaseGuideSection(
    onLoadCaseOnCube: (PresetScramble) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var selectedCategory by remember { mutableStateOf(CaseCategory.ALL) }
    var isBeginnerDictionaryOpen by remember { mutableStateOf(true) }

    val filteredCases = remember(selectedCategory) {
        if (selectedCategory == CaseCategory.ALL) {
            VISUAL_CUBE_CASES
        } else {
            VISUAL_CUBE_CASES.filter { it.category == selectedCategory }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("visual_case_guide_section"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Visual Algorithm & Case Guide (OLL / PLL)",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface
                )
                Text(
                    text = "Match your top layer pattern to the 2D diagram below (yellow = oriented sticker, gray = unoriented, outer bars = side stickers). Each card translates every letter into plain English for beginners.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }

            // Beginner Letter & Symbol Key (What are r, R, U', U2', F', etc.?)
            BeginnerLetterAndSymbolKey(
                isExpanded = isBeginnerDictionaryOpen,
                onToggleExpand = { isBeginnerDictionaryOpen = !isBeginnerDictionaryOpen }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (cat in CaseCategory.entries) {
                    val isSelected = selectedCategory == cat
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { selectedCategory = cat }
                            .border(
                                width = 1.dp,
                                color = if (isSelected) colors.primary else colors.outline,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .testTag("case_category_chip_${cat.name.lowercase()}"),
                        color = if (isSelected) colors.primary else colors.surface
                    ) {
                        Text(
                            text = cat.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) colors.onPrimary else colors.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                for ((idx, cubeCase) in filteredCases.withIndex()) {
                    if (idx > 0) {
                        HorizontalDivider(color = colors.outline)
                    }
                    VisualCaseFlashcard(
                        cubeCase = cubeCase,
                        onPracticeClick = {
                            onLoadCaseOnCube(
                                PresetScramble(
                                    id = cubeCase.id,
                                    name = cubeCase.badgeTitle,
                                    subtitle = cubeCase.subtitle,
                                    movesNotation = cubeCase.practiceScrambleSeq
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BeginnerLetterAndSymbolKey(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceVariant)
            .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Beginner Key: What do r, R, U', U2' mean?",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface
                )
                Text(
                    text = "Capital = 1 outer layer · Lowercase (r, f) = 2 layers together · ' = reverse · 2 = turn twice",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onToggleExpand,
                modifier = Modifier.testTag("toggle_beginner_symbol_key_button")
            ) {
                Text(
                    text = if (isExpanded) "Hide Key" else "Show Key",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Quick 3-rule summary pill strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RuleSummaryBadge(
                        symbol = "R  U  F",
                        title = "Capital Letter",
                        desc = "Turn 1 outer layer (90°)"
                    )
                    RuleSummaryBadge(
                        symbol = "r  f  l  u",
                        title = "Lowercase Letter",
                        desc = "Turn 2 layers together (Wide)"
                    )
                    RuleSummaryBadge(
                        symbol = "' (Prime)",
                        title = "Apostrophe '",
                        desc = "Reverse / Counter-Clockwise"
                    )
                    RuleSummaryBadge(
                        symbol = "2  or  2'",
                        title = "Number 2",
                        desc = "Turn twice (180° half-turn)"
                    )
                }

                HorizontalDivider(color = colors.outline)

                // Detailed visual row for each common letter/symbol
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (entry in BEGINNER_NOTATION_GUIDE) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.outline, RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MiniLayerHighlightDiagram(
                                highlightCols = entry.highlightCols,
                                highlightRows = entry.highlightRows,
                                arrowDirection = entry.arrowDirection
                            )

                            Box(
                                modifier = Modifier
                                    .width(64.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF6D31B))
                                    .border(1.dp, Color(0xFF141413).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = entry.token,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFF141413)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.onSurface
                                )
                                Text(
                                    text = entry.beginnerMeaning,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleSummaryBadge(
    symbol: String,
    title: String,
    desc: String
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.onSurface
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurface
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun MiniLayerHighlightDiagram(
    highlightCols: List<Int>,
    highlightRows: List<Int>,
    arrowDirection: String,
    sizeDp: Dp = 38.dp
) {
    Canvas(modifier = Modifier.size(sizeDp)) {
        val w = size.width
        val gap = w * 0.06f
        val cell = (w - gap * 2f) / 3f
        val activeColor = Color(0xFFF6D31B)
        val idleColor = Color(0xFF707070)
        val strokeColor = Color(0xFF141413)

        for (r in 0..2) {
            for (c in 0..2) {
                val isHighlighted = c in highlightCols || r in highlightRows
                val left = c * (cell + gap)
                val top = r * (cell + gap)
                drawRoundRect(
                    color = if (isHighlighted) activeColor else idleColor,
                    topLeft = Offset(left, top),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(left, top),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(2f, 2f),
                    style = Stroke(width = 1.5f)
                )
            }
        }

        // Draw simple directional indicator line
        val cx = w * 0.5f
        val cy = w * 0.5f
        if (arrowDirection == "UP" || arrowDirection == "DOWN") {
            val targetCol = highlightCols.average().toFloat()
            val x = targetCol * (cell + gap) + cell * 0.5f
            drawLine(
                color = strokeColor,
                start = Offset(x, w * 0.82f),
                end = Offset(x, w * 0.18f),
                strokeWidth = 2.5f
            )
        } else if (arrowDirection == "LEFT" || arrowDirection == "RIGHT") {
            val targetRow = highlightRows.average().toFloat()
            val y = targetRow * (cell + gap) + cell * 0.5f
            drawLine(
                color = strokeColor,
                start = Offset(w * 0.18f, y),
                end = Offset(w * 0.82f, y),
                strokeWidth = 2.5f
            )
        } else {
            drawCircle(
                color = strokeColor,
                radius = w * 0.24f,
                center = Offset(cx, cy),
                style = Stroke(width = 2.2f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisualCaseFlashcard(
    cubeCase: VisualCubeCase,
    onPracticeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val highlightYellow = Color(0xFFF6D31B)
    val badgeInk = Color(0xFF141413)
    val moveTokens = remember(cubeCase.algorithm) {
        cubeCase.algorithm.split(" ").filter { it.isNotBlank() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceVariant)
            .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
            .clickable { onPracticeClick() }
            .padding(14.dp)
            .testTag("visual_case_card_${cubeCase.id}"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TopDownCubeDiagram(
                uGrid = cubeCase.uGrid,
                topBars = cubeCase.topBars,
                bottomBars = cubeCase.bottomBars,
                leftBars = cubeCase.leftBars,
                rightBars = cubeCase.rightBars,
                diagramSize = 128.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(highlightYellow)
                        .border(1.dp, badgeInk.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cubeCase.badgeTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = badgeInk,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = cubeCase.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Tap to load on 3D Cube →",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurface
                )
            }
        }

        // Yellow Algorithm Formula Bar (matches user screenshot)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(highlightYellow)
                .border(1.dp, badgeInk.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                .padding(vertical = 13.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cubeCase.algorithm,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = 0.5.sp
                ),
                color = badgeInk,
                textAlign = TextAlign.Center
            )
        }

        // Step-by-Step Beginner Translation of each move in the algorithm
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
                .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Beginner Step-by-Step Meaning:",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for ((idx, tok) in moveTokens.withIndex()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceVariant)
                            .border(1.dp, colors.outline, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "${idx + 1}. $tok",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.onSurface
                        )
                        Text(
                            text = "= ${explainMoveTokenForBeginner(tok)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun stickerCodeColor(code: Int): Color = when (code) {
    1 -> Color(0xFFFCEC1A) // Speedcubing diagram bright yellow
    2 -> Color(0xFF268E57) // Green
    3 -> Color(0xFFD63828) // Red
    4 -> Color(0xFF245EE0) // Blue
    5 -> Color(0xFFE67822) // Orange
    else -> Color(0xFF707070) // Unoriented neutral gray
}

@Composable
fun TopDownCubeDiagram(
    uGrid: List<Int>,
    topBars: List<Int>,
    bottomBars: List<Int>,
    leftBars: List<Int>,
    rightBars: List<Int>,
    diagramSize: Dp = 128.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(diagramSize)) {
        val w = size.width
        val strokePx = (w * 0.018f).coerceAtLeast(2f)
        val gap = w * 0.018f
        val barThick = w * 0.088f
        val coreMargin = barThick + gap * 1.4f
        val coreSize = w - coreMargin * 2f
        val cell = (coreSize - gap * 2f) / 3f

        val coreBlack = Color(0xFF0E0E0D)
        val innerRadius = CornerRadius(cell * 0.30f, cell * 0.30f)
        val outerRadius = CornerRadius(cell * 0.11f, cell * 0.11f)
        val barRadius = CornerRadius(barThick * 0.36f, barThick * 0.36f)

        // Draw black internal core under the 3x3 grid so curved inner cubie corners show black core
        drawRoundRect(
            color = coreBlack,
            topLeft = Offset(coreMargin - strokePx * 0.5f, coreMargin - strokePx * 0.5f),
            size = Size(coreSize + strokePx, coreSize + strokePx),
            cornerRadius = outerRadius
        )

        // Draw 3x3 U-face cubie stickers with authentic speedcube inner-corner rounding
        for (r in 0..2) {
            for (c in 0..2) {
                val idx = r * 3 + c
                val fill = stickerCodeColor(uGrid.getOrElse(idx) { 0 })
                val left = coreMargin + c * (cell + gap)
                val top = coreMargin + r * (cell + gap)
                val rect = Rect(left, top, left + cell, top + cell)

                // Corners facing center (1,1) get strong inner rounding
                val tl = if (r >= 1 && c >= 1) innerRadius else outerRadius
                val tr = if (r >= 1 && c <= 1) innerRadius else outerRadius
                val br = if (r <= 1 && c <= 1) innerRadius else outerRadius
                val bl = if (r <= 1 && c >= 1) innerRadius else outerRadius

                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = rect,
                            topLeft = tl,
                            topRight = tr,
                            bottomRight = br,
                            bottomLeft = bl
                        )
                    )
                }
                drawPath(path = path, color = fill)
                drawPath(path = path, color = coreBlack, style = Stroke(width = strokePx))
            }
        }

        // Draw 3 top (Back) side bars
        for (c in 0..2) {
            val fill = stickerCodeColor(topBars.getOrElse(c) { 0 })
            val left = coreMargin + c * (cell + gap)
            val top = coreMargin - gap - barThick
            drawRoundRect(
                color = fill,
                topLeft = Offset(left, top),
                size = Size(cell, barThick),
                cornerRadius = barRadius
            )
            drawRoundRect(
                color = coreBlack,
                topLeft = Offset(left, top),
                size = Size(cell, barThick),
                cornerRadius = barRadius,
                style = Stroke(width = strokePx)
            )
        }

        // Draw 3 bottom (Front) side bars
        for (c in 0..2) {
            val fill = stickerCodeColor(bottomBars.getOrElse(c) { 0 })
            val left = coreMargin + c * (cell + gap)
            val top = coreMargin + coreSize + gap
            drawRoundRect(
                color = fill,
                topLeft = Offset(left, top),
                size = Size(cell, barThick),
                cornerRadius = barRadius
            )
            drawRoundRect(
                color = coreBlack,
                topLeft = Offset(left, top),
                size = Size(cell, barThick),
                cornerRadius = barRadius,
                style = Stroke(width = strokePx)
            )
        }

        // Draw 3 left side bars
        for (r in 0..2) {
            val fill = stickerCodeColor(leftBars.getOrElse(r) { 0 })
            val left = coreMargin - gap - barThick
            val top = coreMargin + r * (cell + gap)
            drawRoundRect(
                color = fill,
                topLeft = Offset(left, top),
                size = Size(barThick, cell),
                cornerRadius = barRadius
            )
            drawRoundRect(
                color = coreBlack,
                topLeft = Offset(left, top),
                size = Size(barThick, cell),
                cornerRadius = barRadius,
                style = Stroke(width = strokePx)
            )
        }

        // Draw 3 right side bars
        for (r in 0..2) {
            val fill = stickerCodeColor(rightBars.getOrElse(r) { 0 })
            val left = coreMargin + coreSize + gap
            val top = coreMargin + r * (cell + gap)
            drawRoundRect(
                color = fill,
                topLeft = Offset(left, top),
                size = Size(barThick, cell),
                cornerRadius = barRadius
            )
            drawRoundRect(
                color = coreBlack,
                topLeft = Offset(left, top),
                size = Size(barThick, cell),
                cornerRadius = barRadius,
                style = Stroke(width = strokePx)
            )
        }
    }
}
