package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.CubeColor
import com.example.model.CubeFace
import com.example.model.CubeMove
import com.example.model.CubeState
import com.example.model.CubeValidationResult
import com.example.ui.components.Cube3DCanvas
import com.example.viewmodel.CubeSolverUiState

@Composable
fun CubeNetEditorScreen(
    uiState: CubeSolverUiState,
    onSelectPaintColor: (CubeColor) -> Unit,
    onPaintSticker: (CubeFace, Int) -> Unit,
    onApplyManualMove: (CubeMove) -> Unit,
    onRandomScramble: () -> Unit,
    onResetSolved: () -> Unit,
    onComputeAndSolve: () -> Unit,
    onNavigateBackToScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBackToScanner()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 580.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ValidationAndMini3DCard(
                cubeState = uiState.cubeState,
                validation = uiState.validationResult,
                onComputeAndSolve = onComputeAndSolve
            )

            ColorPaletteSelectorCard(
                selectedColor = uiState.selectedPaintColor,
                colorCounts = uiState.validationResult.colorCounts,
                onSelectColor = onSelectPaintColor
            )

            UnfoldedCubeNetCard(
                cubeState = uiState.cubeState,
                onTapSticker = onPaintSticker
            )

            NoCameraNetOrientationCard()

            ManualMoveSimulatorCard(
                onApplyMove = onApplyManualMove,
                onRandomScramble = onRandomScramble,
                onResetSolved = onResetSolved
            )
        }
    }
}

@Composable
private fun ValidationAndMini3DCard(
    cubeState: CubeState,
    validation: CubeValidationResult,
    onComputeAndSolve: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Cube3DCanvas(
                cubeState = cubeState,
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceVariant)
                    .testTag("net_mini_3d_cube")
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (validation.isValid) "Valid Cube State" else "Sticker Count Mismatch",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (validation.isValid) colors.tertiary else colors.error
                )

                if (validation.isValid) {
                    Text(
                        text = if (cubeState.isSolved()) {
                            "All 54 stickers are in their solved positions."
                        } else {
                            "All 6 colors have 9 stickers and valid edge/corner pieces."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                    Button(
                        onClick = onComputeAndSolve,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("net_solve_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        )
                    ) {
                        Text(
                            text = "Solve Cube",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    Text(
                        text = validation.errorSummary ?: "Each color must appear 9 times.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface
                    )
                    if (!validation.actionableSuggestion.isNullOrBlank()) {
                        Text(
                            text = validation.actionableSuggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPaletteSelectorCard(
    selectedColor: CubeColor,
    colorCounts: Map<CubeColor, Int>,
    onSelectColor: (CubeColor) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Sticker Brush (9 per color)",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (color in CubeColor.entries) {
                    val isSelected = color == selectedColor
                    val count = colorCounts[color] ?: 0
                    val countOk = count == 9

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.surfaceVariant else Color.Transparent)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) colors.primary else colors.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectColor(color) }
                            .padding(vertical = 8.dp)
                            .testTag("palette_color_${color.name}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(color.composeColor)
                                .border(1.dp, Color(0xFF1A1918).copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = color.shortCode.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (color.textDark) Color(0xFF141413) else Color.White
                            )
                        }
                        Text(
                            text = "$count/9",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (countOk) colors.onSurfaceVariant else colors.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnfoldedCubeNetCard(
    cubeState: CubeState,
    onTapSticker: (CubeFace, Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Unfolded 54-Sticker Net (Tap to paint)",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant
            )

            val faceSize = 74.dp
            val gap = 6.dp

            Column(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Spacer(modifier = Modifier.size(faceSize))
                    MiniFaceGrid(
                        face = CubeFace.U,
                        stickers = cubeState.faceStickers(CubeFace.U),
                        size = faceSize,
                        onTapSticker = onTapSticker
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (face in listOf(CubeFace.L, CubeFace.F, CubeFace.R, CubeFace.B)) {
                        MiniFaceGrid(
                            face = face,
                            stickers = cubeState.faceStickers(face),
                            size = faceSize,
                            onTapSticker = onTapSticker
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Spacer(modifier = Modifier.size(faceSize))
                    MiniFaceGrid(
                        face = CubeFace.D,
                        stickers = cubeState.faceStickers(CubeFace.D),
                        size = faceSize,
                        onTapSticker = onTapSticker
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniFaceGrid(
    face: CubeFace,
    stickers: List<CubeColor>,
    size: Dp,
    onTapSticker: (CubeFace, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1A1918))
            .padding(3.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (row in 0..2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (col in 0..2) {
                    val idx = row * 3 + col
                    val color = stickers[idx]
                    val isCenter = idx == 4

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(3.dp))
                            .background(color.composeColor)
                            .clickable(enabled = !isCenter) { onTapSticker(face, idx) }
                            .testTag("net_sticker_${face.code}_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCenter) {
                            Text(
                                text = face.code.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (color.textDark) Color(0xFF141413) else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoCameraNetOrientationCard() {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("net_no_camera_guide_card"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Net Orientation Guide",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface
            )
            Text(
                text = "Hold GREEN in front (F) and WHITE on top (U):\n" +
                    "• U (White): Tilt top toward you (Blue edge at top).\n" +
                    "• L, F, R, B: Keep White on top as you inspect around the middle.\n" +
                    "• D (Yellow): Tilt bottom toward you (Green edge at top).",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ManualMoveSimulatorCard(
    onApplyMove: (CubeMove) -> Unit,
    onRandomScramble: () -> Unit,
    onResetSolved: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manual Layer Turns",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onRandomScramble) {
                        Text("Scramble", style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(
                        onClick = onResetSolved,
                        modifier = Modifier.testTag("reset_solved_button")
                    ) {
                        Text("Reset", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            HorizontalDivider(color = colors.outline)

            val quickMoves = listOf(
                CubeMove.U, CubeMove.U_PRIME,
                CubeMove.R, CubeMove.R_PRIME,
                CubeMove.F, CubeMove.F_PRIME,
                CubeMove.D, CubeMove.D_PRIME,
                CubeMove.L, CubeMove.L_PRIME,
                CubeMove.B, CubeMove.B_PRIME
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (move in quickMoves) {
                    OutlinedButton(
                        onClick = { onApplyMove(move) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, colors.outline),
                        modifier = Modifier.testTag("sim_move_${move.notation}")
                    ) {
                        Text(
                            text = move.notation,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
