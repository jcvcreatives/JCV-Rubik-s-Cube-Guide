package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.screens.CubeNetEditorScreen
import com.example.ui.screens.SolverGuideScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.viewmodel.AppTab
import com.example.viewmodel.CubeSolverUiState
import com.example.viewmodel.CubeSolverViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CubeLensApp()
            }
        }
    }
}

@Composable
fun CubeLensApp(
    viewModel: CubeSolverViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val isExpanded = maxWidth >= 680.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                CubeLensTopBar(uiState = uiState)
            },
            bottomBar = {
                if (!isExpanded) {
                    CubeLensBottomBar(
                        activeTab = uiState.activeTab,
                        scannedFaceCount = uiState.scannedFaces.size,
                        solveStepCount = uiState.solveSteps.size,
                        onSelectTab = viewModel::selectTab
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpanded) {
                    CubeLensNavigationRail(
                        activeTab = uiState.activeTab,
                        scannedFaceCount = uiState.scannedFaces.size,
                        solveStepCount = uiState.solveSteps.size,
                        onSelectTab = viewModel::selectTab
                    )
                }

                Crossfade(
                    targetState = uiState.activeTab,
                    label = "screenCrossfade",
                    modifier = Modifier.weight(1f)
                ) { tab ->
                    when (tab) {
                        AppTab.SOLVE_GUIDE -> SolverGuideScreen(
                            uiState = uiState,
                            onPrevStep = viewModel::prevSolveStep,
                            onNextStep = viewModel::nextSolveStep,
                            onJumpToStep = viewModel::jumpToSolveStep,
                            onToggleAutoPlay = viewModel::toggleAutoPlay,
                            onToggleCameraVerify = viewModel::toggleCameraVerifySheet,
                            onCameraSample = viewModel::onCameraGridSample,
                            onGenerateNewScramble = viewModel::generateRandomScramble,
                            onLoadPresetScramble = viewModel::loadPresetScramble,
                            onOpenManualNet = { viewModel.selectTab(AppTab.CUBE_NET) },
                            onOpenOptionalCamera = { viewModel.selectTab(AppTab.SCANNER) }
                        )

                        AppTab.CUBE_NET -> CubeNetEditorScreen(
                            uiState = uiState,
                            onSelectPaintColor = viewModel::selectPaintColor,
                            onPaintSticker = viewModel::paintNetSticker,
                            onApplyManualMove = viewModel::applyManualMove,
                            onRandomScramble = viewModel::generateRandomScramble,
                            onResetSolved = viewModel::resetToSolvedCube,
                            onComputeAndSolve = viewModel::computeAndOpenSolveGuide,
                            onNavigateBackToScanner = { viewModel.selectTab(AppTab.SOLVE_GUIDE) }
                        )

                        AppTab.SCANNER -> CameraScannerScreen(
                            uiState = uiState,
                            onSelectScanFace = viewModel::selectScanFace,
                            onCameraSample = viewModel::onCameraGridSample,
                            onCyclePreviewSticker = viewModel::cycleLivePreviewSticker,
                            onSelectPaintColor = viewModel::selectPaintColor,
                            onCaptureCurrentFace = viewModel::captureCurrentFace,
                            onResetScan = viewModel::resetScanProgress,
                            onToggleAutoCapture = viewModel::toggleAutoCapture,
                            onToggleTorch = viewModel::toggleTorch,
                            onToggleNoCameraMode = viewModel::toggleNoCameraMode,
                            onToggleNoCameraGuide = viewModel::toggleNoCameraGuide,
                            onGenerateRandomScramble = viewModel::generateRandomScramble,
                            onLoadPresetScramble = viewModel::loadPresetScramble,
                            onOpenNetEditor = { viewModel.selectTab(AppTab.CUBE_NET) },
                            onComputeAndSolve = viewModel::computeAndOpenSolveGuide,
                            onNavigateBackToGuide = { viewModel.selectTab(AppTab.SOLVE_GUIDE) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CubeLensTopBar(uiState: CubeSolverUiState) {
    Surface(
        color = SlateSurface,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan)
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = SlateSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (uiState.validationResult.isValid) EmeraldSuccess.copy(alpha = 0.6f)
                        else NeonOrange
                    )
                ) {
                    Text(
                        text = when {
                            uiState.cubeState.isSolved() -> "SOLVED (54/54)"
                            uiState.validationResult.isValid -> "${uiState.solveSteps.size} MOVES TO SOLVE"
                            else -> "EDIT NET NEEDED"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (uiState.validationResult.isValid) EmeraldSuccess else NeonOrange,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (!uiState.statusMessage.isNullOrBlank()) {
                Text(
                    text = uiState.statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CubeLensBottomBar(
    activeTab: AppTab,
    scannedFaceCount: Int,
    solveStepCount: Int,
    onSelectTab: (AppTab) -> Unit
) {
    NavigationBar(
        containerColor = SlateSurface,
        tonalElevation = 8.dp
    ) {
        // Tab 1 (Main): Solve Guide & Instructions
        NavigationBarItem(
            selected = activeTab == AppTab.SOLVE_GUIDE,
            onClick = { onSelectTab(AppTab.SOLVE_GUIDE) },
            icon = {
                BadgedBox(
                    badge = {
                        if (solveStepCount > 0) {
                            Badge(containerColor = NeonOrange, contentColor = Color.Black) {
                                Text("$solveStepCount")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (activeTab == AppTab.SOLVE_GUIDE) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                        contentDescription = stringResource(R.string.nav_solver_guide)
                    )
                }
            },
            label = { Text(stringResource(R.string.nav_solver_guide)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ElectricCyan,
                selectedTextColor = ElectricCyan,
                indicatorColor = SlateSurfaceElevated
            ),
            modifier = Modifier.testTag("nav_tab_guide")
        )

        // Tab 2: Manual Cube Net
        NavigationBarItem(
            selected = activeTab == AppTab.CUBE_NET,
            onClick = { onSelectTab(AppTab.CUBE_NET) },
            icon = {
                Icon(
                    imageVector = if (activeTab == AppTab.CUBE_NET) Icons.Filled.GridOn else Icons.Outlined.GridOn,
                    contentDescription = stringResource(R.string.nav_net_editor)
                )
            },
            label = { Text(stringResource(R.string.nav_net_editor)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ElectricCyan,
                selectedTextColor = ElectricCyan,
                indicatorColor = SlateSurfaceElevated
            ),
            modifier = Modifier.testTag("nav_tab_net")
        )

        // Tab 3 (Optional): Camera Scanner
        NavigationBarItem(
            selected = activeTab == AppTab.SCANNER,
            onClick = { onSelectTab(AppTab.SCANNER) },
            icon = {
                BadgedBox(
                    badge = {
                        if (scannedFaceCount in 1..5) {
                            Badge(containerColor = ElectricCyan, contentColor = Color.Black) {
                                Text("$scannedFaceCount/6")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (activeTab == AppTab.SCANNER) Icons.Filled.CameraAlt else Icons.Outlined.CameraAlt,
                        contentDescription = stringResource(R.string.nav_scanner)
                    )
                }
            },
            label = { Text(stringResource(R.string.nav_scanner)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ElectricCyan,
                selectedTextColor = ElectricCyan,
                indicatorColor = SlateSurfaceElevated
            ),
            modifier = Modifier.testTag("nav_tab_scanner")
        )
    }
}

@Composable
private fun CubeLensNavigationRail(
    activeTab: AppTab,
    scannedFaceCount: Int,
    solveStepCount: Int,
    onSelectTab: (AppTab) -> Unit
) {
    NavigationRail(
        containerColor = SlateSurface,
        modifier = Modifier.fillMaxHeight()
    ) {
        NavigationRailItem(
            selected = activeTab == AppTab.SOLVE_GUIDE,
            onClick = { onSelectTab(AppTab.SOLVE_GUIDE) },
            icon = {
                Icon(
                    imageVector = if (activeTab == AppTab.SOLVE_GUIDE) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                    contentDescription = stringResource(R.string.nav_solver_guide)
                )
            },
            label = { Text("${stringResource(R.string.nav_solver_guide)} ($solveStepCount)") },
            modifier = Modifier.testTag("rail_tab_guide")
        )

        NavigationRailItem(
            selected = activeTab == AppTab.CUBE_NET,
            onClick = { onSelectTab(AppTab.CUBE_NET) },
            icon = {
                Icon(
                    imageVector = if (activeTab == AppTab.CUBE_NET) Icons.Filled.GridOn else Icons.Outlined.GridOn,
                    contentDescription = stringResource(R.string.nav_net_editor)
                )
            },
            label = { Text(stringResource(R.string.nav_net_editor)) },
            modifier = Modifier.testTag("rail_tab_net")
        )

        NavigationRailItem(
            selected = activeTab == AppTab.SCANNER,
            onClick = { onSelectTab(AppTab.SCANNER) },
            icon = {
                Icon(
                    imageVector = if (activeTab == AppTab.SCANNER) Icons.Filled.CameraAlt else Icons.Outlined.CameraAlt,
                    contentDescription = stringResource(R.string.nav_scanner)
                )
            },
            label = { Text("${stringResource(R.string.nav_scanner)} ($scannedFaceCount/6)") },
            modifier = Modifier.testTag("rail_tab_scanner")
        )
    }
}
