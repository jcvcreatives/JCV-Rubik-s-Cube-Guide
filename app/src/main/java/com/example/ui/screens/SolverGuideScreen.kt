package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.camera.CubeCameraAnalyzer
import com.example.camera.LiveGridSample
import com.example.domain.CubeSolverEngine
import com.example.domain.PresetScramble
import com.example.model.CubeColor
import com.example.model.SolveStep
import com.example.ui.components.Cube3DCanvas
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.viewmodel.CubeSolverUiState
import java.util.concurrent.Executors

private data class StageInstructionItem(
    val stageBadge: String,
    val title: String,
    val goal: String,
    val algorithm: String,
    val howToPerform: String
)

private val LAYER_BY_LAYER_MANUAL = listOf(
    StageInstructionItem(
        stageBadge = "STAGE 1",
        title = "White Top Cross",
        goal = "Place all 4 White edge pieces around the White (U) center so their side colors also match the Front (Green), Right (Red), Back (Blue), and Left (Orange) centers.",
        algorithm = "F2 / R2 / U / D setups",
        howToPerform = "Find each White edge in the bottom or middle layer, align it under its matching side center color, and turn that face 180° (F2, R2, B2, or L2) to bring it up to the White cross."
    ),
    StageInstructionItem(
        stageBadge = "STAGE 2",
        title = "First Layer White Corners",
        goal = "Insert all 4 White corner pieces into the top layer so the entire White face and top rim match.",
        algorithm = "R' D' R  or  F D F'",
        howToPerform = "Position a White corner in the bottom layer directly beneath its target top corner slot. Repeat the Right Trigger (R' D' R D) until the corner locks into place with White facing up."
    ),
    StageInstructionItem(
        stageBadge = "STAGE 3",
        title = "Middle Layer Edges",
        goal = "Solve the 4 vertical middle-layer edges (Green-Red, Red-Blue, Blue-Orange, Orange-Green) without disturbing the solved White top.",
        algorithm = "Right: D' R' D R D F D' F'  |  Left: D L D' L' D' F' D F",
        howToPerform = "Find an edge on the bottom (D) layer that has NO Yellow sticker. Rotate D until its side sticker matches its center forming a vertical T, then run the Right or Left insertion sequence."
    ),
    StageInstructionItem(
        stageBadge = "STAGE 4",
        title = "Bottom Yellow Cross",
        goal = "Orient all 4 bottom-layer edges so Yellow faces straight down on the D face.",
        algorithm = "F D R D' R' F'",
        howToPerform = "Repeat F D R D' R' F' to advance the bottom Yellow pattern: Center Dot → L-Shape (hold at back-left) → Horizontal Line → Full Yellow Cross."
    ),
    StageInstructionItem(
        stageBadge = "STAGE 5",
        title = "Align Yellow Cross Edges",
        goal = "Swap bottom Yellow edges until all 4 match the Front (Green), Right (Red), Back (Blue), and Left (Orange) centers.",
        algorithm = "R D R' D R D2 R'  (Sune)",
        howToPerform = "Rotate D until at least 2 bottom edges match their side centers. Hold one matching edge at the Back and one at the Right, then execute R D R' D R D2 R'."
    ),
    StageInstructionItem(
        stageBadge = "STAGE 6",
        title = "Position Final Yellow Corners",
        goal = "Move the 4 bottom corners into their correct 3-color corner locations (even if twisted).",
        algorithm = "D R D' L' D R' D' L  (Niklas)",
        howToPerform = "Find one bottom corner that is already in its correct 3-color spot. Hold it at the Front-Right-Bottom position and run D R D' L' D R' D' L once or twice until all 4 corners are in position."
    ),
    StageInstructionItem(
        stageBadge = "STAGE 7",
        title = "Twist Final Corners to Solve",
        goal = "Rotate each misoriented bottom corner in place until all 54 stickers are solved.",
        algorithm = "(R U R' U') × 2  +  D",
        howToPerform = "Hold an unsolved Yellow corner at Front-Right-Bottom. Repeat R U R' U' R U R' U' until Yellow faces down, then turn ONLY the bottom layer (D) to bring the next twisted corner to Front-Right-Bottom."
    )
)

@Composable
fun SolverGuideScreen(
    uiState: CubeSolverUiState,
    onPrevStep: () -> Unit,
    onNextStep: () -> Unit,
    onJumpToStep: (Int) -> Unit,
    onToggleAutoPlay: () -> Unit,
    onToggleCameraVerify: () -> Unit,
    onCameraSample: (LiveGridSample) -> Unit,
    onGenerateNewScramble: () -> Unit,
    onLoadPresetScramble: (PresetScramble) -> Unit,
    onOpenManualNet: () -> Unit,
    onOpenOptionalCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted && !uiState.isCameraVerifySheetOpen) {
            onToggleCameraVerify()
        }
    }

    val scrollState = rememberScrollState()
    val activeStep = uiState.currentActiveSolveStep
    val totalSteps = uiState.solveSteps.size

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
            // 1. Main Instructions & Cube Setup Header Card (Manual First, Camera Optional)
            MainInstructionsOverviewCard(
                activeScrambleLabel = uiState.activeScrambleLabel,
                onOpenManualNet = onOpenManualNet,
                onOpenOptionalCamera = onOpenOptionalCamera,
                onGenerateNewScramble = onGenerateNewScramble,
                onLoadPresetScramble = onLoadPresetScramble
            )

            // 2. Interactive 3D Cube Viewport with Animated Rotation Arrows
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.5.dp, ElectricCyan.copy(alpha = 0.55f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "DRAG 3D CUBE TO ORBIT • FOLLOW GLOWING ARROW",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                        }

                        Text(
                            text = if (totalSteps == 0) {
                                "SOLVED"
                            } else {
                                "STEP ${uiState.currentStepIndex.coerceAtMost(totalSteps)}/$totalSteps"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonOrange
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SlateSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Cube3DCanvas(
                            cubeState = uiState.displayedGuideCubeState,
                            highlightedMove = activeStep?.move,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("guide_3d_cube_canvas")
                        )

                        // Orientation Legend Badge in bottom-left corner
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Text(
                                text = "Hold: Green (F) Front • White (U) Top • Red (R) Right",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (totalSteps > 0) {
                        LinearProgressIndicator(
                            progress = {
                                (uiState.currentStepIndex.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (uiState.isGuideAtSolvedEnd) EmeraldSuccess else ElectricCyan,
                            trackColor = SlateSurfaceElevated
                        )
                    }
                }
            }

            // 3. Current Step Coaching & Notation Card (or Solved Celebration Card)
            if (activeStep != null) {
                StepInstructionCard(step = activeStep)
            } else {
                SolvedCelebrationCard(
                    totalMovesCompleted = totalSteps,
                    onReplaySteps = { onJumpToStep(0) },
                    onNewScramble = onGenerateNewScramble
                )
            }

            // 4. Step Playback Controls Row (Prev, Auto-Play/Pause, Next)
            if (totalSteps > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onPrevStep,
                        enabled = uiState.currentStepIndex > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("prev_step_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(R.string.btn_prev_step),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    FilledTonalButton(
                        onClick = onToggleAutoPlay,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("autoplay_step_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isAutoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isAutoPlaying) {
                                stringResource(R.string.btn_pause_play)
                            } else {
                                stringResource(R.string.btn_auto_play)
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Button(
                        onClick = onNextStep,
                        enabled = uiState.currentStepIndex < totalSteps,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(50.dp)
                            .testTag("next_step_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF00262B)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.btn_next_step),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = null
                        )
                    }
                }
            }

            // 5. Scrollable Move Timeline Ribbon
            if (totalSteps > 0) {
                MoveSequenceTimelineCard(
                    steps = uiState.solveSteps,
                    currentStepIndex = uiState.currentStepIndex,
                    onSelectStep = onJumpToStep
                )
            }

            // 6. MAIN FEATURE: Complete Step-by-Step Solving Instructions, Orientation & 7-Stage Manual
            CompleteSolvingInstructionsDeck()

            // 7. Optional Camera Step Verification (Explicitly Optional at the bottom)
            if (activeStep != null) {
                CameraStepVerifySection(
                    activeStep = activeStep,
                    isOpen = uiState.isCameraVerifySheetOpen,
                    hasCameraPermission = hasCameraPermission,
                    matchCount = uiState.cameraVerifyMatchCount,
                    onToggleVerify = {
                        if (!hasCameraPermission) {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            onToggleCameraVerify()
                        }
                    },
                    onCameraSample = onCameraSample
                )
            }
        }
    }
}

@Composable
private fun MainInstructionsOverviewCard(
    activeScrambleLabel: String,
    onOpenManualNet: () -> Unit,
    onOpenOptionalCamera: () -> Unit,
    onGenerateNewScramble: () -> Unit,
    onLoadPresetScramble: (PresetScramble) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("main_instructions_overview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, NeonOrange.copy(alpha = 0.7f))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonOrange.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = NeonOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "STEP-BY-STEP SOLVING INSTRUCTIONS (MAIN)",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonOrange
                        )
                        Text(
                            text = "Hold GREEN in front & WHITE on top — follow the 3D moves below",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Primary Manual Entry vs Optional Camera Setup Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenManualNet,
                    modifier = Modifier
                        .weight(1.15f)
                        .height(44.dp)
                        .testTag("main_open_manual_net_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF00262B)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Enter Colors Manually",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = onOpenOptionalCamera,
                    modifier = Modifier
                        .weight(0.85f)
                        .height(44.dp)
                        .testTag("main_open_optional_camera_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GlassBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Camera (Optional)",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Preset Practice Scrambles Row right on the Main Screen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onGenerateNewScramble,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("main_random_scramble_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Random Scramble", style = MaterialTheme.typography.labelMedium)
                }

                for (preset in CubeSolverEngine.PRESET_SCRAMBLES) {
                    FilterChip(
                        selected = activeScrambleLabel.startsWith(preset.name),
                        onClick = { onLoadPresetScramble(preset) },
                        label = {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                            selectedLabelColor = ElectricCyan
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StepInstructionCard(step: SolveStep) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_step_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Giant Notation Badge
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(step.move.face.centerColor.composeColor)
                        .border(2.dp, Color.Black.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = step.move.notation,
                        style = MaterialTheme.typography.displayMedium,
                        color = if (step.move.face.centerColor.textDark) Color(0xFF0B0E14) else Color.White
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = ElectricCyan.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = step.phase.badgeText,
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Instruction ${step.stepNumber} of ${step.totalSteps}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = step.move.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = step.move.coachingInstruction,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = SlateSurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Text(
                    text = "HOW TO HOLD: ${step.holdOrientationHint} (Target Face: ${step.move.face.title} • ${step.move.face.centerColor.displayName})",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CompleteSolvingInstructionsDeck() {
    var isStagesExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("complete_solving_instructions_deck"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section A: Orientation & Notation Guide
            Text(
                text = "COMPLETE SOLVING INSTRUCTIONS & NOTATION GUIDE",
                style = MaterialTheme.typography.labelMedium,
                color = NeonOrange
            )
            Text(
                text = "1. Standard Cube Orientation (No Camera Required)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Always hold your Rubik's Cube in the fixed home position before performing any move: GREEN center facing you (Front) and WHITE center facing up (Top). Never turn the whole cube in your hands during a solve sequence — only twist the outer layer indicated.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 6 Face Reference Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FaceLegendPill("F = Front", CubeColor.GREEN)
                FaceLegendPill("U = Up (Top)", CubeColor.WHITE)
                FaceLegendPill("R = Right", CubeColor.RED)
                FaceLegendPill("L = Left", CubeColor.ORANGE)
                FaceLegendPill("D = Down", CubeColor.YELLOW)
                FaceLegendPill("B = Back", CubeColor.BLUE)
            }

            // Section B: Move Notation Rules
            Text(
                text = "2. How to Read Move Letters (U, D, R, L, F, B)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NotationLegendRow(
                    symbol = "U / D / R / L / F / B",
                    explanation = "90° Clockwise turn (imagine looking directly at that face like a wall clock)."
                )
                NotationLegendRow(
                    symbol = "U' / D' / R' / L' / F' / B'",
                    explanation = "90° Counter-Clockwise ('Prime') turn when looking directly at that face."
                )
                NotationLegendRow(
                    symbol = "U2 / D2 / R2 / L2 / F2 / B2",
                    explanation = "180° Half-turn (twist that face twice in either direction)."
                )
            }

            // Section C: 7-Stage Layer-by-Layer Manual & Algorithms
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "3. The 7-Stage Layer-by-Layer Solving Method",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Step-by-step manual instructions & algorithms to solve any cube from scratch",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { isStagesExpanded = !isStagesExpanded },
                    modifier = Modifier.testTag("toggle_7stage_manual_button")
                ) {
                    Icon(
                        imageVector = if (isStagesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle 7-Stage Method",
                        tint = ElectricCyan
                    )
                }
            }

            AnimatedVisibility(visible = isStagesExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (stage in LAYER_BY_LAYER_MANUAL) {
                        Surface(
                            color = SlateSurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = ElectricCyan.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = stage.stageBadge,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = ElectricCyan,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = stage.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Text(
                                    text = "Goal: ${stage.goal}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Surface(
                                    color = Color(0xFF0B0E14),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "Algorithm: ${stage.algorithm}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = NeonOrange,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                Text(
                                    text = stage.howToPerform,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun FaceLegendPill(label: String, color: CubeColor) {
    Surface(
        color = SlateSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color.composeColor)
                    .border(1.dp, Color.Black.copy(alpha = 0.5f), CircleShape)
            )
            Text(
                text = "$label (${color.displayName})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun NotationLegendRow(symbol: String, explanation: String) {
    Surface(
        color = SlateSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.labelMedium,
                color = ElectricCyan,
                modifier = Modifier.width(125.dp)
            )
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SolvedCelebrationCard(
    totalMovesCompleted: Int,
    onReplaySteps: () -> Unit,
    onNewScramble: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("solved_celebration_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.5.dp, EmeraldSuccess)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = EmeraldSuccess,
                modifier = Modifier.size(42.dp)
            )
            Text(
                text = stringResource(R.string.status_solved),
                style = MaterialTheme.typography.headlineMedium,
                color = EmeraldSuccess,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (totalMovesCompleted > 0) {
                    "All $totalMovesCompleted guided moves completed — every face matches its center color."
                } else {
                    "Enter your cube colors in Manual Net or load a scramble to generate a 3D step-by-step guide."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (totalMovesCompleted > 0) {
                    OutlinedButton(
                        onClick = onReplaySteps,
                        modifier = Modifier.testTag("replay_steps_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Replay Guide")
                    }
                }
                Button(
                    onClick = onNewScramble,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF00262B)
                    ),
                    modifier = Modifier.testTag("solved_new_scramble_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Scramble")
                }
            }
        }
    }
}

@Composable
private fun CameraStepVerifySection(
    activeStep: SolveStep,
    isOpen: Boolean,
    hasCameraPermission: Boolean,
    matchCount: Int,
    onToggleVerify: () -> Unit,
    onCameraSample: (LiveGridSample) -> Unit
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "OPTIONAL CAMERA STEP CHECK",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Optional: Point ${activeStep.move.face.title} at camera after performing ${activeStep.move.notation} to auto-advance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onToggleVerify,
                    modifier = Modifier.testTag("camera_verify_toggle_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOpen) "Hide Camera" else "Optional Camera",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            AnimatedVisibility(visible = isOpen && hasCameraPermission) {
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                val previewView = remember { PreviewView(context) }
                val executor = remember { Executors.newSingleThreadExecutor() }
                val latestStep by rememberUpdatedState(activeStep)
                val latestOnSample by rememberUpdatedState(onCameraSample)

                DisposableEffect(lifecycleOwner) {
                    val providerFuture = ProcessCameraProvider.getInstance(context)
                    val listener = Runnable {
                        try {
                            val provider = providerFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(
                                        executor,
                                        CubeCameraAnalyzer(
                                            expectedCenterColor = { latestStep.move.face.centerColor },
                                            onGridDetected = { sample -> latestOnSample(sample) }
                                        )
                                    )
                                }
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                        } catch (e: Exception) {
                            Log.w("StepVerifyCamera", "Camera bind failed: ${e.message}")
                        }
                    }
                    providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))
                    onDispose {
                        try {
                            providerFuture.get().unbindAll()
                        } catch (_: Exception) {
                        }
                        executor.shutdown()
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SlateSurfaceElevated)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Matching ${activeStep.move.face.title}: $matchCount / 9 stickers",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (matchCount >= 8) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                        )
                        LinearProgressIndicator(
                            progress = { (matchCount / 9f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (matchCount >= 8) EmeraldSuccess else ElectricCyan,
                            trackColor = SlateSurface
                        )
                        Text(
                            text = if (matchCount >= 8) {
                                "Step verified! Advancing to next move..."
                            } else {
                                "Perform move '${activeStep.move.notation}' and hold ${activeStep.move.face.centerColor.displayName} center steady."
                            },
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
private fun MoveSequenceTimelineCard(
    steps: List<SolveStep>,
    currentStepIndex: Int,
    onSelectStep: (Int) -> Unit
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
            Text(
                text = "FULL SOLUTION SEQUENCE (${steps.size} MOVES — TAP TO SCRUB)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for ((idx, step) in steps.withIndex()) {
                    val isCurrent = idx == currentStepIndex
                    val isDone = idx < currentStepIndex

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectStep(idx) }
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = when {
                                    isCurrent -> ElectricCyan
                                    isDone -> EmeraldSuccess.copy(alpha = 0.7f)
                                    else -> GlassBorder
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .testTag("timeline_step_chip_$idx"),
                        color = if (isCurrent) SlateSurfaceElevated else SlateSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${idx + 1}.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = step.move.notation,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isCurrent -> ElectricCyan
                                    isDone -> EmeraldSuccess
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }

                // Final Solved Marker Chip
                val isAtSolved = currentStepIndex >= steps.size
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectStep(steps.size) }
                        .border(
                            width = if (isAtSolved) 2.dp else 1.dp,
                            color = if (isAtSolved) EmeraldSuccess else GlassBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag("timeline_solved_chip"),
                    color = if (isAtSolved) EmeraldSuccess.copy(alpha = 0.2f) else SlateSurface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "SOLVED",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldSuccess
                        )
                    }
                }
            }
        }
    }
}
