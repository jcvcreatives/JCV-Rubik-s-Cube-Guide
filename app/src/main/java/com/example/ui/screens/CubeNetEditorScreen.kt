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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Live 3D Cube Preview + Validation Status Card
            ValidationAndMini3DCard(
                cubeState = uiState.cubeState,
                validation = uiState.validationResult,
                onComputeAndSolve = onComputeAndSolve
            )

            // 2. No-Camera Manual Net Orientation Instructions
            NoCameraNetOrientationCard()

            // 3. Color Paint Palette Picker
            ColorPaletteSelectorCard(
                selectedColor = uiState.selectedPaintColor,
                colorCounts = uiState.validationResult.colorCounts,
                onSelectColor = onSelectPaintColor
            )

            // 4. Unfolded 54-Sticker 2D Cross Net
            UnfoldedCubeNetCard(
                cubeState = uiState.cubeState,
                onTapSticker = onPaintSticker
            )

            // 5. Interactive Layer Turn Simulator & Reset Controls
            ManualMoveSimulatorCard(
                onApplyMove = onApplyManualMove,
                onRandomScramble = onRandomScramble,
                onResetSolved = onResetSolved
            )
        }
    }
}

@Composable
private fun NoCameraNetOrientationCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("net_no_camera_guide_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "NO-CAMERA MANUAL NET ENTRY GUIDE",
                style = MaterialTheme.typography.labelMedium,
                color = NeonOrange
            )
            Text(
                text = "Hold your physical cube with GREEN in front (F) and WHITE on top (U):",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "• U (White): Tilt the top toward you (Blue edge at the very top).\n" +
                    "• L (Orange), F (Green), R (Red), B (Blue): Keep White on top as you inspect around the middle.\n" +
                    "• D (Yellow): Tilt the bottom toward you (Green edge at the top).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (validation.isValid) EmeraldSuccess.copy(alpha = 0.6f) else CrimsonError.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Cube3DCanvas(
                cubeState = cubeState,
                modifier = Modifier
                    .size(118.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SlateSurfaceElevated)
                    .testTag("net_mini_3d_cube")
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (validation.isValid) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = if (validation.isValid) EmeraldSuccess else CrimsonError,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (validation.isValid) "VALID 3×3×3 STATE" else "NEEDS STICKER FIX",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (validation.isValid) EmeraldSuccess else CrimsonError
                    )
                }

                if (validation.isValid) {
                    Text(
                        text = if (cubeState.isSolved()) {
                            "All 54 stickers are in their solved positions."
                        } else {
                            "All 6 color counts (9 each) and 20 cubie pieces are physically solvable."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onComputeAndSolve,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("net_solve_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonOrange,
                            contentColor = Color(0xFF1F0B00)
                        )
                    ) {
                        Text(
                            text = "Open 3D Solve Guide",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Text(
                        text = validation.errorSummary ?: "Invalid sticker distribution",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!validation.actionableSuggestion.isNullOrBlank()) {
                        Text(
                            text = validation.actionableSuggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "SELECT STICKER BRUSH COLOR (TARGET: 9 PER COLOR)",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
            }

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
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) SlateSurfaceElevated else Color.Transparent)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) ElectricCyan else GlassBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectColor(color) }
                            .padding(vertical = 8.dp)
                            .testTag("palette_color_${color.name}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(color.composeColor)
                                .border(1.5.dp, Color.Black.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = color.shortCode.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (color.textDark) Color(0xFF0B0E14) else Color.White
                            )
                        }
                        Text(
                            text = "$count/9",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (countOk) EmeraldSuccess else CrimsonError
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "54-STICKER UNFOLDED CUBE NET (TAP ANY OUTER STICKER TO PAINT)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val faceSize = 76.dp
            val gap = 6.dp

            Column(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                // Top Row: [spacer] [U]
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Spacer(modifier = Modifier.size(faceSize))
                    MiniFaceGrid(
                        face = CubeFace.U,
                        stickers = cubeState.faceStickers(CubeFace.U),
                        size = faceSize,
                        onTapSticker = onTapSticker
                    )
                }

                // Middle Row: [L] [F] [R] [B]
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

                // Bottom Row: [spacer] [D]
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
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0A0D12))
            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
            .padding(3.dp),
        verticalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        for (row in 0..2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(2.5.dp)
            ) {
                for (col in 0..2) {
                    val idx = row * 3 + col
                    val color = stickers[idx]
                    val isCenter = idx == 4

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
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
                                color = if (color.textDark) Color(0xFF0B0E14) else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualMoveSimulatorCard(
    onApplyMove: (CubeMove) -> Unit,
    onRandomScramble: () -> Unit,
    onResetSolved: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, GlassBorder)
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
                    text = "MANUAL 3D LAYER TURN SIMULATOR",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onRandomScramble,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scramble", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onResetSolved,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("reset_solved_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Solved", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

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
                    FilledTonalButton(
                        onClick = { onApplyMove(move) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
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
