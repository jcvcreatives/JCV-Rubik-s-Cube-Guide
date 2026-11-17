package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.example.model.CubeFace
import com.example.viewmodel.CubeSolverUiState
import java.util.concurrent.Executors

@Composable
fun CameraScannerScreen(
    uiState: CubeSolverUiState,
    onSelectScanFace: (CubeFace) -> Unit,
    onCameraSample: (LiveGridSample) -> Unit,
    onCyclePreviewSticker: (Int) -> Unit,
    onSelectPaintColor: (CubeColor) -> Unit,
    onCaptureCurrentFace: () -> Unit,
    onResetScan: () -> Unit,
    onToggleAutoCapture: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleNoCameraMode: () -> Unit,
    onToggleNoCameraGuide: () -> Unit,
    onGenerateRandomScramble: () -> Unit,
    onLoadPresetScramble: (PresetScramble) -> Unit,
    onOpenNetEditor: () -> Unit,
    onComputeAndSolve: () -> Unit,
    onNavigateBackToGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBackToGuide()
    }

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
    }

    val scrollState = rememberScrollState()
    val useLiveCamera = hasCameraPermission && !uiState.preferNoCameraMode

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
            NoCameraInstructionsCard(
                preferNoCameraMode = uiState.preferNoCameraMode,
                isGuideExpanded = uiState.isNoCameraGuideExpanded,
                onToggleNoCameraMode = onToggleNoCameraMode,
                onToggleGuide = onToggleNoCameraGuide,
                onOpenNetEditor = onOpenNetEditor
            )

            ScanFaceRibbonCard(
                currentScanFace = uiState.currentScanFace,
                scannedFaces = uiState.scannedFaces,
                onSelectFace = onSelectScanFace,
                onResetScan = onResetScan
            )

            if (useLiveCamera) {
                LiveCameraViewfinderCard(
                    uiState = uiState,
                    onCameraSample = onCameraSample,
                    onCyclePreviewSticker = onCyclePreviewSticker,
                    onToggleAutoCapture = onToggleAutoCapture,
                    onToggleTorch = onToggleTorch
                )
            } else {
                MinimalManualFacePadCard(
                    uiState = uiState,
                    hasCameraPermission = hasCameraPermission,
                    onRequestCameraPermission = {
                        if (uiState.preferNoCameraMode) {
                            onToggleNoCameraMode()
                        }
                        if (!hasCameraPermission) {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onSelectPaintColor = onSelectPaintColor,
                    onCyclePreviewSticker = onCyclePreviewSticker
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCaptureCurrentFace,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("capture_face_button"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, colors.outline)
                ) {
                    Text(
                        text = "Save ${uiState.currentScanFace.code} Face (${uiState.scannedFaces.size}/6)",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = onComputeAndSolve,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("compute_solution_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.btn_solve_now),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            QuickScrambleDeckCard(
                activeScrambleLabel = uiState.activeScrambleLabel,
                onGenerateRandomScramble = onGenerateRandomScramble,
                onLoadPreset = onLoadPresetScramble
            )
        }
    }
}

@Composable
private fun NoCameraInstructionsCard(
    preferNoCameraMode: Boolean,
    isGuideExpanded: Boolean,
    onToggleNoCameraMode: () -> Unit,
    onToggleGuide: () -> Unit,
    onOpenNetEditor: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("no_camera_instructions_card"),
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
                        text = stringResource(R.string.no_camera_mode_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurface
                    )
                    Text(
                        text = if (preferNoCameraMode) {
                            "Camera is off. Enter colors by tapping the 3×3 grid."
                        } else {
                            "Live camera color detection is enabled."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }

                Switch(
                    checked = preferNoCameraMode,
                    onCheckedChange = { onToggleNoCameraMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.onPrimary,
                        checkedTrackColor = colors.primary
                    ),
                    modifier = Modifier.testTag("no_camera_mode_switch")
                )
            }

            HorizontalDivider(color = colors.outline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manual Setup Instructions",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurface
                )
                TextButton(
                    onClick = onToggleGuide,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.testTag("toggle_no_camera_guide_button")
                ) {
                    Text(
                        text = if (isGuideExpanded) "Hide" else "Show",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            AnimatedVisibility(visible = isGuideExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. Hold GREEN center in front and WHITE center on top.\n" +
                            "2. Select a brush color below and tap the 3×3 squares for each face (F, R, B, L, U, D), or edit all 6 faces at once in Manual Net.\n" +
                            "3. Tap Compute Solution to open the 60fps 3D step-by-step guide.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onOpenNetEditor,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, colors.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("guide_open_net_button")
                    ) {
                        Text("Open Full 54-Sticker Net")
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanFaceRibbonCard(
    currentScanFace: CubeFace,
    scannedFaces: Set<CubeFace>,
    onSelectFace: (CubeFace) -> Unit,
    onResetScan: () -> Unit
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
                        text = "Select Face (${scannedFaces.size}/6 saved)",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurface
                    )
                    Text(
                        text = currentScanFace.scanInstruction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onResetScan,
                    modifier = Modifier.testTag("reset_scan_button")
                ) {
                    Text("Reset", style = MaterialTheme.typography.labelMedium)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (face in CubeFace.SCAN_ORDER) {
                    val isSelected = face == currentScanFace
                    val isCaptured = face in scannedFaces

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectFace(face) }
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) colors.primary else colors.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .testTag("scan_face_chip_${face.code}"),
                        color = if (isSelected) colors.surfaceVariant else colors.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(face.centerColor.composeColor)
                                    .border(1.dp, Color(0xFF1A1918).copy(alpha = 0.35f), CircleShape)
                            )
                            Text(
                                text = if (isCaptured) "${face.code} ✓" else "${face.code} · ${face.centerColor.displayName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isCaptured) colors.tertiary else colors.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveCameraViewfinderCard(
    uiState: CubeSolverUiState,
    onCameraSample: (LiveGridSample) -> Unit,
    onCyclePreviewSticker: (Int) -> Unit,
    onToggleAutoCapture: () -> Unit,
    onToggleTorch: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val latestScanFace by rememberUpdatedState(uiState.currentScanFace)
    val latestOnCameraSample by rememberUpdatedState(onCameraSample)
    var boundCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(
                            cameraExecutor,
                            CubeCameraAnalyzer(
                                expectedCenterColor = { latestScanFace.centerColor },
                                onGridDetected = { sample -> latestOnCameraSample(sample) }
                            )
                        )
                    }

                cameraProvider.unbindAll()
                boundCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.w("CubeCameraScanner", "CameraX bind failed: ${e.message}")
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (_: Exception) {
            }
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(uiState.torchEnabled, boundCamera) {
        try {
            boundCamera?.cameraControl?.enableTorch(uiState.torchEnabled)
        } catch (_: Exception) {
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.05f)
                    .background(Color(0xFF141413))
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("camera_preview_view")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${uiState.currentScanFace.title} (${uiState.currentScanFace.centerColor.displayName})",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF141413),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onToggleAutoCapture() }
                                .testTag("auto_capture_toggle"),
                            color = if (uiState.autoCaptureEnabled) Color(0xFF141413) else Color.White.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = if (uiState.autoCaptureEnabled) "Auto: On" else "Auto: Off",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (uiState.autoCaptureEnabled) Color.White else Color(0xFF141413),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onToggleTorch() }
                                .testTag("torch_toggle_button"),
                            color = if (uiState.torchEnabled) Color(0xFF141413) else Color.White.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = if (uiState.torchEnabled) "Light: On" else "Light: Off",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (uiState.torchEnabled) Color.White else Color(0xFF141413),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                ArViewfinderGridOverlay(
                    face = uiState.currentScanFace,
                    detectedColors = uiState.liveDetectedColors,
                    onTapCell = onCyclePreviewSticker,
                    modifier = Modifier.align(Alignment.Center)
                )

                val animatedStability by animateFloatAsState(
                    targetValue = uiState.stabilityProgress,
                    label = "stability"
                )
                LinearProgressIndicator(
                    progress = { animatedStability },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter),
                    color = colors.tertiary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun MinimalManualFacePadCard(
    uiState: CubeSolverUiState,
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    onSelectPaintColor: (CubeColor) -> Unit,
    onCyclePreviewSticker: (Int) -> Unit
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${uiState.currentScanFace.title} (${uiState.currentScanFace.centerColor.displayName})",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurface
                    )
                    Text(
                        text = "Pick a color swatch below and tap the 3×3 grid squares.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onRequestCameraPermission,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, colors.outline),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("enable_camera_button")
                ) {
                    Text(
                        text = if (hasCameraPermission) "Use Camera" else stringResource(R.string.btn_grant_camera),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (color in CubeColor.entries) {
                    val isSelected = color == uiState.selectedPaintColor
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.composeColor)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) colors.primary else Color(0xFF141413).copy(alpha = 0.25f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onSelectPaintColor(color) }
                            .testTag("scanner_brush_${color.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = color.shortCode.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (color.textDark) Color(0xFF141413) else Color.White
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ArViewfinderGridOverlay(
                    face = uiState.currentScanFace,
                    detectedColors = uiState.liveDetectedColors,
                    onTapCell = onCyclePreviewSticker
                )
            }
        }
    }
}

@Composable
fun ArViewfinderGridOverlay(
    face: CubeFace,
    detectedColors: List<CubeColor>,
    onTapCell: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        NeighborBadge(label = "Top: ${face.topNeighborLabel}")

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeighborBadge(label = face.leftNeighborLabel.take(1))

            Box(
                modifier = Modifier
                    .size(192.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A1918))
                    .padding(6.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    for (row in 0..2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            for (col in 0..2) {
                                val idx = row * 3 + col
                                val color = detectedColors.getOrElse(idx) { face.centerColor }
                                val isCenter = idx == 4

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(color.composeColor)
                                        .clickable(enabled = !isCenter) { onTapCell(idx) }
                                        .testTag("reticle_cell_$idx"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isCenter) "${face.code}" else "${color.shortCode}",
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

            NeighborBadge(label = face.rightNeighborLabel.take(1))
        }

        NeighborBadge(label = "Bottom: ${face.bottomNeighborLabel}")
    }
}

@Composable
private fun NeighborBadge(label: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceVariant,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun QuickScrambleDeckCard(
    activeScrambleLabel: String,
    onGenerateRandomScramble: () -> Unit,
    onLoadPreset: (PresetScramble) -> Unit
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
                    text = "Preset Scrambles",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface
                )
                TextButton(
                    onClick = onGenerateRandomScramble,
                    modifier = Modifier.testTag("random_scramble_button")
                ) {
                    Text("Random Scramble", style = MaterialTheme.typography.labelMedium)
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
                            .clickable { onLoadPreset(preset) }
                            .border(
                                width = 1.dp,
                                color = if (selected) colors.primary else colors.outline,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .testTag("preset_chip_${preset.id}"),
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
