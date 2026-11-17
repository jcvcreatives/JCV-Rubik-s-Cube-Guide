package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.screens.CubeNetEditorScreen
import com.example.ui.screens.SolverGuideScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppTab
import com.example.viewmodel.CubeSolverUiState
import com.example.viewmodel.CubeSolverViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CubeLensRoot()
        }
    }
}

@Composable
fun CubeLensRoot(
    viewModel: CubeSolverViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MyApplicationTheme(darkTheme = uiState.isDarkTheme) {
        CubeLensApp(uiState = uiState, viewModel = viewModel)
    }
}

@Composable
fun CubeLensApp(
    uiState: CubeSolverUiState,
    viewModel: CubeSolverViewModel
) {
    val colors = MaterialTheme.colorScheme

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val isExpanded = maxWidth >= 680.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = colors.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                CubeLensTopBar(
                    uiState = uiState,
                    onToggleTheme = viewModel::toggleDarkTheme
                )
            },
            bottomBar = {
                if (!isExpanded) {
                    CubeLensBottomBar(
                        activeTab = uiState.activeTab,
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
private fun CubeLensTopBar(
    uiState: CubeSolverUiState,
    onToggleTheme: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        color = colors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = when {
                            uiState.cubeState.isSolved() -> "Solved"
                            uiState.validationResult.isValid -> "${uiState.solveSteps.size} moves"
                            else -> "Check net"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            uiState.cubeState.isSolved() -> colors.tertiary
                            uiState.validationResult.isValid -> colors.onSurfaceVariant
                            else -> colors.error
                        }
                    )

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(BorderStroke(1.dp, colors.outline), RoundedCornerShape(6.dp))
                            .clickable { onToggleTheme() }
                            .testTag("theme_toggle_button"),
                        color = colors.surfaceVariant
                    ) {
                        Text(
                            text = if (uiState.isDarkTheme) "Light" else "Dark",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = colors.outline)
        }
    }
}

@Composable
private fun CubeLensBottomBar(
    activeTab: AppTab,
    onSelectTab: (AppTab) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column {
        HorizontalDivider(color = colors.outline)
        NavigationBar(
            containerColor = colors.surface,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = activeTab == AppTab.SOLVE_GUIDE,
                onClick = { onSelectTab(AppTab.SOLVE_GUIDE) },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.MenuBook,
                        contentDescription = stringResource(R.string.nav_solver_guide)
                    )
                },
                label = { Text(stringResource(R.string.nav_solver_guide)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onSurface,
                    selectedTextColor = colors.onSurface,
                    unselectedIconColor = colors.onSurfaceVariant,
                    unselectedTextColor = colors.onSurfaceVariant,
                    indicatorColor = colors.surfaceVariant
                ),
                modifier = Modifier.testTag("nav_tab_guide")
            )

            NavigationBarItem(
                selected = activeTab == AppTab.CUBE_NET,
                onClick = { onSelectTab(AppTab.CUBE_NET) },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.GridOn,
                        contentDescription = stringResource(R.string.nav_net_editor)
                    )
                },
                label = { Text(stringResource(R.string.nav_net_editor)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onSurface,
                    selectedTextColor = colors.onSurface,
                    unselectedIconColor = colors.onSurfaceVariant,
                    unselectedTextColor = colors.onSurfaceVariant,
                    indicatorColor = colors.surfaceVariant
                ),
                modifier = Modifier.testTag("nav_tab_net")
            )

            NavigationBarItem(
                selected = activeTab == AppTab.SCANNER,
                onClick = { onSelectTab(AppTab.SCANNER) },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = stringResource(R.string.nav_scanner)
                    )
                },
                label = { Text(stringResource(R.string.nav_scanner)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onSurface,
                    selectedTextColor = colors.onSurface,
                    unselectedIconColor = colors.onSurfaceVariant,
                    unselectedTextColor = colors.onSurfaceVariant,
                    indicatorColor = colors.surfaceVariant
                ),
                modifier = Modifier.testTag("nav_tab_scanner")
            )
        }
    }
}

@Composable
private fun CubeLensNavigationRail(
    activeTab: AppTab,
    onSelectTab: (AppTab) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    NavigationRail(
        containerColor = colors.surface,
        modifier = Modifier.fillMaxHeight()
    ) {
        NavigationRailItem(
            selected = activeTab == AppTab.SOLVE_GUIDE,
            onClick = { onSelectTab(AppTab.SOLVE_GUIDE) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.MenuBook,
                    contentDescription = stringResource(R.string.nav_solver_guide)
                )
            },
            label = { Text(stringResource(R.string.nav_solver_guide)) },
            modifier = Modifier.testTag("rail_tab_guide")
        )

        NavigationRailItem(
            selected = activeTab == AppTab.CUBE_NET,
            onClick = { onSelectTab(AppTab.CUBE_NET) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.GridOn,
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
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = stringResource(R.string.nav_scanner)
                )
            },
            label = { Text(stringResource(R.string.nav_scanner)) },
            modifier = Modifier.testTag("rail_tab_scanner")
        )
    }
}
