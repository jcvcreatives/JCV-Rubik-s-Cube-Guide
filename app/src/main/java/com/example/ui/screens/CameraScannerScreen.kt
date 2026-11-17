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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.viewmodel.CubeSolverUiState
import java.util.concurrent.Executors

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults

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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier.widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Dedicated No-Camera Manual Mode Toggle & Step-by-Step Instructions Card
            NoCameraInstructionsCard(
                preferNoCameraMode = uiState.preferNoCameraMode,
                isGuideExpanded = uiState.isNoCameraGuideExpanded,
                onToggleNoCameraMode = onToggleNoCameraMode,
                onToggleGuide = onToggleNoCameraGuide,
                onOpenNetEditor = onOpenNetEditor
            )

            // Top 6-Face Scan Progress Ribbon
            ScanFaceRibbonCard(
                currentScanFace = uiState.currentScanFace,
                scannedFaces = uiState.scannedFaces,
                onSelectFace = onSelectScanFace,
                onResetScan = onResetScan
            )

            // Main Camera Viewfinder or Manual / No-Camera Interactive 3x3 Reticle Card
            if (useLiveCamera) {
                LiveCameraViewfinderCard(
                    uiState = uiState,
                    onCameraSample = onCameraSample,
                    onCyclePreviewSticker = onCyclePreviewSticker,
                    onCaptureCurrentFace = onCaptureCurrentFace,
                    onToggleAutoCapture = onToggleAutoCapture,
                    onToggleTorch = onToggleTorch
                )
            } else {
                CameraPermissionAndInteractiveReticleCard(
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
                    onCyclePreviewSticker = onCyclePreviewSticker,
                    onOpenNetEditor = onOpenNetEditor
                )
            }

            // Primary Capture & Solve CTA Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCaptureCurrentFace,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("capture_face_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (useLiveCamera) Icons.Default.CameraAlt else Icons.Default.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lock ${uiState.currentScanFace.code} Face (${uiState.scannedFaces.size}/6)",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = onComputeAndSolve,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("compute_solution_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonOrange,
                        contentColor = Color(0xFF1F0B00)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.btn_solve_now),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Quick Test Scrambles & Presets Section (so users can immediately test solving steps)
            QuickScrambleDeckCard(
                activeScrambleLabel = uiState.activeScrambleLabel,
                solveStepCount = uiState.solveSteps.size,
                onGenerateRandomScramble = onGenerateRandomScramble,
                onLoadPreset = onLoadPresetScramble,
                onComputeAndSolve = onComputeAndSolve
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("no_camera_instructions_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(
            width = 1.dp,
            color = if (preferNoCameraMode) NeonOrange.copy(alpha = 0.8f) else GlassBorder
        )
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
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (preferNoCameraMode) NeonOrange.copy(alpha = 0.2f)
                                else ElectricCyan.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (preferNoCameraMode) Icons.Default.NoPhotography else Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = if (preferNoCameraMode) NeonOrange else ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.no_camera_guide_header),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (preferNoCameraMode) NeonOrange else ElectricCyan
                        )
                        Text(
                            text = stringResource(R.string.no_camera_mode_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Switch(
                        checked = preferNoCameraMode,
                        onCheckedChange = { onToggleNoCameraMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF1F0B00),
                            checkedTrackColor = NeonOrange
                        ),
                        modifier = Modifier.testTag("no_camera_mode_switch")
                    )
                    IconButton(
                        onClick = onToggleGuide,
                        modifier = Modifier.testTag("toggle_no_camera_guide_button")
                    ) {
                        Icon(
                            imageVector = if (isGuideExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle No-Camera Instructions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isGuideExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NoCameraStepRow(
                        badge = "1",
                        title = stringResource(R.string.no_camera_step1_title),
                        description = stringResource(R.string.no_camera_step1_desc)
                    )
                    NoCameraStepRow(
                        badge = "2",
                        title = stringResource(R.string.no_camera_step2_title),
                        description = stringResource(R.string.no_camera_step2_desc)
                    )
                    NoCameraStepRow(
                        badge = "3",
                        title = stringResource(R.string.no_camera_step3_title),
                        description = stringResource(R.string.no_camera_step3_desc)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                if (!preferNoCameraMode) onToggleNoCameraMode()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("activate_manual_grid_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (preferNoCameraMode) "Using 3×3 Manual Grid" else "Use 3×3 Manual Grid",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenNetEditor,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("guide_open_net_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open Full Cube Net",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoCameraStepRow(
    badge: String,
    title: String,
    description: String
) {
    Surface(
        color = SlateSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(ElectricCyan.copy(alpha = 0.2f))
                    .border(1.dp, ElectricCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        text = "6-FACE CAMERA SCAN SEQUENCE",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                    Text(
                        text = currentScanFace.scanInstruction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onResetScan,
                    modifier = Modifier.testTag("reset_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.btn_rescan_all),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (face in CubeFace.SCAN_ORDER) {
                    val isSelected = face == currentScanFace
                    val isCaptured = face in scannedFaces
                    val borderColor by animateColorAsState(
                        targetValue = when {
                            isSelected -> ElectricCyan
                            isCaptured -> EmeraldSuccess.copy(alpha = 0.7f)
                            else -> GlassBorder
                        },
                        label = "faceBorder"
                    )

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectFace(face) }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .testTag("scan_face_chip_${face.code}"),
                        color = if (isSelected) SlateSurfaceElevated else SlateSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(face.centerColor.composeColor)
                                    .border(1.dp, Color.Black.copy(alpha = 0.5f), CircleShape)
                            )
                            Text(
                                text = "${face.code} • ${face.centerColor.displayName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurface
                            )
                            if (isCaptured) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Captured",
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(16.dp)
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
private fun LiveCameraViewfinderCard(
    uiState: CubeSolverUiState,
    onCameraSample: (LiveGridSample) -> Unit,
    onCyclePreviewSticker: (Int) -> Unit,
    onCaptureCurrentFace: () -> Unit,
    onToggleAutoCapture: () -> Unit,
    onToggleTorch: () -> Unit
) {
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.5.dp, ElectricCyan.copy(alpha = 0.55f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.05f)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("camera_preview_view")
                )

                // Dark vignette gradient around the 3x3 AR reticle
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.68f)
                                )
                            )
                        )
                )

                // Top HUD bar inside camera view
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Text(
                            text = "TARGET: ${uiState.currentScanFace.title.uppercase()} (${uiState.currentScanFace.centerColor.displayName})",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onToggleAutoCapture,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.autoCaptureEnabled) ElectricCyan.copy(alpha = 0.25f)
                                    else Color.Black.copy(alpha = 0.6f)
                                )
                                .testTag("auto_capture_toggle")
                        ) {
                            Icon(
                                imageVector = if (uiState.autoCaptureEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Toggle Auto-Capture Lock",
                                tint = if (uiState.autoCaptureEnabled) ElectricCyan else Color.White
                            )
                        }

                        IconButton(
                            onClick = onToggleTorch,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.torchEnabled) NeonOrange.copy(alpha = 0.3f)
                                    else Color.Black.copy(alpha = 0.6f)
                                )
                                .testTag("torch_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = stringResource(R.string.cd_torch_toggle),
                                tint = if (uiState.torchEnabled) NeonOrange else Color.White
                            )
                        }
                    }
                }

                // Centered 3x3 AR Grid Overlay with Neighbor Orientation Labels
                ArViewfinderGridOverlay(
                    face = uiState.currentScanFace,
                    detectedColors = uiState.liveDetectedColors,
                    onTapCell = onCyclePreviewSticker,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Bottom Stability Lock Progress & HSV Telemetry
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val animatedStability by animateFloatAsState(
                        targetValue = uiState.stabilityProgress,
                        label = "stability"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.autoCaptureEnabled) {
                                "AUTO-LOCK STABILITY: ${(animatedStability * 100).toInt()}%"
                            } else {
                                "TAP ANY CELL TO FINE-TUNE COLOR BEFORE LOCKING"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (animatedStability >= 0.8f) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "HSV ${uiState.centerHue.toInt()}° • ${(uiState.centerSat * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                    }
                    LinearProgressIndicator(
                        progress = { animatedStability },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (animatedStability >= 0.8f) EmeraldSuccess else ElectricCyan,
                        trackColor = SlateSurfaceElevated
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionAndInteractiveReticleCard(
    uiState: CubeSolverUiState,
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    onSelectPaintColor: (CubeColor) -> Unit,
    onCyclePreviewSticker: (Int) -> Unit,
    onOpenNetEditor: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!uiState.preferNoCameraMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_cube_hero_1794872801916),
                        contentDescription = stringResource(R.string.cd_hero_banner),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.2f),
                                        SlateSurface.copy(alpha = 0.96f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.permission_camera_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.permission_camera_rationale),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "CAMERA-FREE MANUAL 3×3 FACE PAD",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonOrange
                    )
                    Text(
                        text = "Look at your cube's ${uiState.currentScanFace.title} (${uiState.currentScanFace.centerColor.displayName} center) with ${uiState.currentScanFace.topNeighborLabel} on top. Pick a brush color below and tap the 8 outer stickers to match.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRequestCameraPermission,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("enable_camera_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF00262B)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasCameraPermission && uiState.preferNoCameraMode) {
                            "Switch to Live Camera"
                        } else {
                            stringResource(R.string.btn_grant_camera)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = onOpenNetEditor,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("open_net_editor_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cube Net",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // 6-Color Manual Brush Bar for Camera-Free Entry
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "MANUAL STICKER BRUSH (SELECT COLOR OR TAP CELLS TO CYCLE):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (color in CubeColor.entries) {
                        val isSelected = color == uiState.selectedPaintColor
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(color.composeColor)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) ElectricCyan else Color.Black.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectPaintColor(color) }
                                .testTag("scanner_brush_${color.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = color.shortCode.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (color.textDark) Color(0xFF0B0E14) else Color.White
                            )
                        }
                    }
                }
            }

            // Interactive 3x3 AR Reticle Preview (works immediately without any camera!)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "INTERACTIVE 3×3 FACE GRID (${uiState.currentScanFace.title.uppercase()}) — TAP CELLS TO PAINT",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan,
                    textAlign = TextAlign.Center
                )
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
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Top neighbor orientation pill
        NeighborBadge(label = "▲ TOP: ${face.topNeighborLabel}")

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NeighborBadge(label = "◀ ${face.leftNeighborLabel.take(1)}")

            // 3x3 Sticker Reticle Box
            Box(
                modifier = Modifier
                    .size(196.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(2.dp, ElectricCyan, RoundedCornerShape(18.dp))
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (row in 0..2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0..2) {
                                val idx = row * 3 + col
                                val color = detectedColors.getOrElse(idx) { face.centerColor }
                                val isCenter = idx == 4

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(color.composeColor.copy(alpha = 0.90f))
                                        .border(
                                            width = if (isCenter) 2.5.dp else 1.5.dp,
                                            color = if (isCenter) ElectricCyan else Color.Black.copy(alpha = 0.65f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable(enabled = !isCenter) { onTapCell(idx) }
                                        .testTag("reticle_cell_$idx"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isCenter) "${face.code}" else "${color.shortCode}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (color.textDark) Color(0xFF0B0E14) else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            NeighborBadge(label = "${face.rightNeighborLabel.take(1)} ▶")
        }

        // Bottom neighbor orientation pill
        NeighborBadge(label = "▼ BOTTOM: ${face.bottomNeighborLabel}")
    }
}

@Composable
private fun NeighborBadge(label: String) {
    Surface(
        color = Color.Black.copy(alpha = 0.72f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun QuickScrambleDeckCard(
    activeScrambleLabel: String,
    solveStepCount: Int,
    onGenerateRandomScramble: () -> Unit,
    onLoadPreset: (PresetScramble) -> Unit,
    onComputeAndSolve: () -> Unit
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
                        text = "INSTANT CALIBRATION & TEST SCRAMBLES",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonOrange
                    )
                    Text(
                        text = activeScrambleLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FilledTonalButton(
                    onClick = onGenerateRandomScramble,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("random_scramble_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Random",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (preset in CubeSolverEngine.PRESET_SCRAMBLES) {
                    FilterChip(
                        selected = activeScrambleLabel.startsWith(preset.name),
                        onClick = { onLoadPreset(preset) },
                        label = {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                            selectedLabelColor = ElectricCyan
                        ),
                        modifier = Modifier.testTag("preset_chip_${preset.id}")
                    )
                }
            }

            if (solveStepCount > 0) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onComputeAndSolve() },
                    color = SlateSurfaceElevated,
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Solution ready: $solveStepCount guided 3D steps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmeraldSuccess
                        )
                        Text(
                            text = "OPEN GUIDE ▶",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                    }
                }
            }
        }
    }
}
