package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
                    text = "Match your top layer pattern to the 2D diagram below (yellow = oriented sticker, gray = unoriented, outer bars = side stickers). Tap any card to load it onto the 3D cube.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }

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

            Text(
                text = "Notation tip: lowercase 'r' means turn the Right two layers together (wide R); 'f' means Front two layers together.",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant
            )

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
fun VisualCaseFlashcard(
    cubeCase: VisualCubeCase,
    onPracticeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val highlightYellow = Color(0xFFF6D31B)
    val badgeInk = Color(0xFF141413)

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
