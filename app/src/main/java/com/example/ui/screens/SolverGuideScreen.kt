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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.viewmodel.CubeSolverUiState
import java.util.concurrent.Executors

private data class StageInstructionItem(
    val number: String,
    val title: String,
    val goal: String,
    val algorithm: String,
    val howToPerform: String
)

private val LAYER_BY_LAYER_MANUAL = listOf(
    StageInstructionItem(
        number = "01",
        title = "White Top Cross",
        goal = "Align the 4 White edges around the White (U) center and match their side centers.",
        algorithm = "F2 · R2 · B2 · L2",
        howToPerform = "Place each White edge on the bottom layer beneath its matching side center color, then rotate that face 180° to bring it to the top."
    ),
    StageInstructionItem(
        number = "02",
        title = "First Layer Corners",
        goal = "Insert all 4 White corner pieces into the top layer.",
        algorithm = "R' D' R D",
        howToPerform = "Position a White corner directly below its target top corner slot and repeat R' D' R D until it locks in with White facing up."
    ),
    StageInstructionItem(
        number = "03",
        title = "Middle Layer Edges",
        goal = "Solve the 4 vertical middle edges without disturbing the top layer.",
        algorithm = "Right: D' R' D R D F D' F'   |   Left: D L D' L' D' F' D F",
        howToPerform = "Align a non-Yellow bottom edge with its side center to form a vertical T, then apply the Right or Left insertion sequence."
    ),
    StageInstructionItem(
        number = "04",
        title = "Bottom Yellow Cross",
        goal = "Orient all 4 bottom edges so Yellow faces down.",
        algorithm = "F D R D' R' F'",
        howToPerform = "Apply F D R D' R' F' to advance the bottom Yellow pattern from Dot → L-Shape → Line → Cross."
    ),
    StageInstructionItem(
        number = "05",
        title = "Align Yellow Edges",
        goal = "Match all 4 bottom Yellow edges with the Front, Right, Back, and Left centers.",
        algorithm = "R D R' D R D2 R'",
        howToPerform = "Hold two matched bottom edges at the Back and Right faces, then execute R D R' D R D2 R'."
    ),
    StageInstructionItem(
        number = "06",
        title = "Position Final Corners",
        goal = "Cycle the 4 bottom corners into their correct 3-color locations.",
        algorithm = "D R D' L' D R' D' L",
        howToPerform = "Hold one correctly positioned bottom corner at Front-Right-Bottom and apply D R D' L' D R' D' L until all 4 corners are in place."
    ),
    StageInstructionItem(
        number = "07",
        title = "Orient Final Corners",
        goal = "Twist each remaining corner in place until all 54 stickers match.",
        algorithm = "(R U R' U') × 2  +  D",
        howToPerform = "Hold an unsolved corner at Front-Right-Bottom, repeat R U R' U' R U R' U' until Yellow faces down, then turn D to bring the next corner into position."
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
    val colors = MaterialTheme.colorScheme
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 580.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                            text = "3D Guide (Drag to rotate)",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurfaceVariant
                        )

                        Text(
                            text = if (totalSteps == 0) {
                                "Solved"
                            } else {
                                "Step ${uiState.currentStepIndex.coerceAtMost(totalSteps)} of $totalSteps"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(248.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Cube3DCanvas(
                            cubeState = uiState.displayedGuideCubeState,
                            highlightedMove = activeStep?.move,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("guide_3d_cube_canvas")
                        )

                        Text(
                            text = "Front: Green (F) · Top: White (U) · Right: Red (R)",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                        )
                    }

                    if (totalSteps > 0) {
                        LinearProgressIndicator(
                            progress = {
                                (uiState.currentStepIndex.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = if (uiState.isGuideAtSolvedEnd) colors.tertiary else colors.primary,
                            trackColor = colors.surfaceVariant
                        )
                    }
                }
            }

            if (activeStep != null) {
                StepInstructionCard(step = activeStep)
            } else {
                SolvedCelebrationCard(
                    totalMovesCompleted = totalSteps,
                    onReplaySteps = { onJumpToStep(0) },
                    onNewScramble = onGenerateNewScramble
                )
            }

            if (totalSteps > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onPrevStep,
                        enabled = uiState.currentStepIndex > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("prev_step_button"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, colors.outline)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_prev_step),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = onToggleAutoPlay,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("autoplay_step_button"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, colors.outline)
                    ) {
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
                            .height(46.dp)
                            .testTag("next_step_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.btn_next_step),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            if (totalSteps > 0) {
                MoveSequenceTimelineCard(
                    steps = uiState.solveSteps,
                    currentStepIndex = uiState.currentStepIndex,
                    onSelectStep = onJumpToStep
                )
            }

            MainInstructionsOverviewCard(
                activeScrambleLabel = uiState.activeScrambleLabel,
                onOpenManualNet = onOpenManualNet,
                onOpenOptionalCamera = onOpenOptionalCamera,
                onGenerateNewScramble = onGenerateNewScramble,
                onLoadPresetScramble = onLoadPresetScramble
            )

            CompleteSolvingInstructionsDeck()

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
private fun StepInstructionCard(step: SolveStep) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_step_card"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outline)
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
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(step.move.face.centerColor.composeColor)
                        .border(1.dp, Color(0xFF1A1918).copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = step.move.notation,
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (step.move.face.centerColor.textDark) Color(0xFF141413) else Color.White
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${step.phase.title} · Move ${step.stepNumber} of ${step.totalSteps}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )

                    Text(
                        text = step.move.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurface
                    )

                    Text(
                        text = step.move.coachingInstruction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = colors.outline)

            Text(
                text = "${step.holdOrientationHint} · Active face: ${step.move.face.title} (${step.move.face.centerColor.displayName})",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
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
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("main_instructions_overview_card"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cube Setup",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurface
                    )
                    Text(
                        text = activeScrambleLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onGenerateNewScramble,
                    modifier = Modifier.testTag("main_random_scramble_button")
                ) {
                    Text("Scramble", style = MaterialTheme.typography.labelMedium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenManualNet,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("main_open_manual_net_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    )
                ) {
                    Text("Manual Net", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onOpenOptionalCamera,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("main_open_optional_camera_button"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, colors.outline)
                ) {
                    Text("Camera (Optional)", style = MaterialTheme.typography.labelLarge)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (preset in CubeSolverEngine.PRESET_SCRAMBLES) {
                    val selected = activeScrambleLabel.startsWith(preset.name)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onLoadPresetScramble(preset) }
                            .border(
                                width = 1.dp,
                                color = if (selected) colors.primary else colors.outline,
                                shape = RoundedCornerShape(6.dp)
                            ),
                        color = if (selected) colors.surfaceVariant else colors.surface
                    ) {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompleteSolvingInstructionsDeck() {
    val colors = MaterialTheme.colorScheme
    var isStagesExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("complete_solving_instructions_deck"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Solving Instructions & Notation",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface
            )

            Text(
                text = "Hold your cube with the GREEN center in front (F) and WHITE center on top (U). Keep this orientation fixed for every move—only twist the indicated outer layer.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FaceLegendPill("F · Front", CubeColor.GREEN)
                FaceLegendPill("U · Top", CubeColor.WHITE)
                FaceLegendPill("R · Right", CubeColor.RED)
                FaceLegendPill("L · Left", CubeColor.ORANGE)
                FaceLegendPill("D · Bottom", CubeColor.YELLOW)
                FaceLegendPill("B · Back", CubeColor.BLUE)
            }

            HorizontalDivider(color = colors.outline)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NotationLegendRow(
                    symbol = "U  D  R  L  F  B",
                    explanation = "90° clockwise turn looking directly at that face."
                )
                NotationLegendRow(
                    symbol = "U' D' R' L' F' B'",
                    explanation = "90° counter-clockwise (prime) turn looking at that face."
                )
                NotationLegendRow(
                    symbol = "U2 D2 R2 L2 F2 B2",
                    explanation = "180° half-turn in either direction."
                )
            }

            HorizontalDivider(color = colors.outline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "7-Stage Manual Method",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface
                )
                TextButton(
                    onClick = { isStagesExpanded = !isStagesExpanded },
                    modifier = Modifier.testTag("toggle_7stage_manual_button")
                ) {
                    Text(
                        text = if (isStagesExpanded) "Hide Stages" else "Show Stages",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            AnimatedVisibility(visible = isStagesExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for ((idx, stage) in LAYER_BY_LAYER_MANUAL.withIndex()) {
                        if (idx > 0) {
                            HorizontalDivider(color = colors.outline)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${stage.number}. ${stage.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.onSurface
                                )
                                Text(
                                    text = stage.algorithm,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onSurface
                                )
                            }
                            Text(
                                text = stage.goal,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurface
                            )
                            Text(
                                text = stage.howToPerform,
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

@Composable
private fun FaceLegendPill(label: String, color: CubeColor) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color.composeColor)
                    .border(1.dp, Color(0xFF1A1918).copy(alpha = 0.35f), CircleShape)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurface
            )
        }
    }
}

@Composable
private fun NotationLegendRow(symbol: String, explanation: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurface,
            modifier = Modifier.width(132.dp)
        )
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SolvedCelebrationCard(
    totalMovesCompleted: Int,
    onReplaySteps: () -> Unit,
    onNewScramble: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("solved_celebration_card"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.tertiary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.status_solved),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.tertiary,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (totalMovesCompleted > 0) {
                    "Completed all $totalMovesCompleted moves."
                } else {
                    "Enter colors in Manual Net or load a scramble to generate a step-by-step solution."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (totalMovesCompleted > 0) {
                    OutlinedButton(
                        onClick = onReplaySteps,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, colors.outline),
                        modifier = Modifier.testTag("replay_steps_button")
                    ) {
                        Text("Replay")
                    }
                }
                Button(
                    onClick = onNewScramble,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    modifier = Modifier.testTag("solved_new_scramble_button")
                ) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Optional Camera Check",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface
                    )
                    Text(
                        text = "Point ${activeStep.move.face.title} at camera after ${activeStep.move.notation} to auto-advance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onToggleVerify,
                    modifier = Modifier.testTag("camera_verify_toggle_button"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, colors.outline),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isOpen) "Hide" else "Open Camera",
                        style = MaterialTheme.typography.labelMedium
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
                        .height(110.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceVariant)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Matched: $matchCount / 9 stickers",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (matchCount >= 8) colors.tertiary else colors.onSurface
                        )
                        LinearProgressIndicator(
                            progress = { (matchCount / 9f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = colors.tertiary,
                            trackColor = colors.outline
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
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Move Sequence (${steps.size} steps)",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for ((idx, step) in steps.withIndex()) {
                    val isCurrent = idx == currentStepIndex
                    val isDone = idx < currentStepIndex

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelectStep(idx) }
                            .border(
                                width = if (isCurrent) 1.5.dp else 1.dp,
                                color = if (isCurrent) colors.primary else colors.outline,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .testTag("timeline_step_chip_$idx"),
                        color = if (isCurrent) colors.primary else if (isDone) colors.surfaceVariant else colors.surface
                    ) {
                        Text(
                            text = "${idx + 1}. ${step.move.notation}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCurrent) colors.onPrimary else colors.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                val isAtSolved = currentStepIndex >= steps.size
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectStep(steps.size) }
                        .border(
                            width = 1.dp,
                            color = if (isAtSolved) colors.tertiary else colors.outline,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .testTag("timeline_solved_chip"),
                    color = if (isAtSolved) colors.tertiary else colors.surface
                ) {
                    Text(
                        text = "Solved",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isAtSolved) colors.onTertiary else colors.tertiary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
