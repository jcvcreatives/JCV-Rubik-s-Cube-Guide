package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.LiveGridSample
import com.example.domain.CubeSolverEngine
import com.example.domain.PresetScramble
import com.example.model.CubeColor
import com.example.model.CubeFace
import com.example.model.CubeMove
import com.example.model.CubeState
import com.example.model.CubeValidationResult
import com.example.model.SolveStep
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab(val route: String) {
    SOLVE_GUIDE("solve_guide"),
    CUBE_NET("cube_net"),
    SCANNER("scanner")
}

data class CubeSolverUiState(
    val activeTab: AppTab = AppTab.SOLVE_GUIDE,
    val cubeState: CubeState,
    val currentScanFace: CubeFace = CubeFace.F,
    val scannedFaces: Set<CubeFace> = emptySet(),
    val liveDetectedColors: List<CubeColor>,
    val centerHue: Float = 138f,
    val centerSat: Float = 0.82f,
    val centerVal: Float = 0.85f,
    val stabilityProgress: Float = 0f,
    val autoCaptureEnabled: Boolean = false,
    val torchEnabled: Boolean = false,
    val selectedPaintColor: CubeColor = CubeColor.WHITE,
    val validationResult: CubeValidationResult,
    val solveSteps: List<SolveStep>,
    val currentStepIndex: Int = 0,
    val isAutoPlaying: Boolean = false,
    val isCameraVerifySheetOpen: Boolean = false,
    val cameraVerifyMatchCount: Int = 0,
    val preferNoCameraMode: Boolean = true,
    val isNoCameraGuideExpanded: Boolean = true,
    val isDarkTheme: Boolean = false,
    val activeScrambleLabel: String = "6-Move Speed Calibration (R U F' L D2 R')",
    val statusMessage: String? = "Follow the step-by-step instructions & 3D move guide below, or edit your cube in Manual Net."
) {
    val displayedGuideCubeState: CubeState
        get() = when {
            solveSteps.isEmpty() -> cubeState
            currentStepIndex <= 0 -> solveSteps.first().stateBefore
            currentStepIndex >= solveSteps.size -> solveSteps.last().stateAfter
            else -> solveSteps[currentStepIndex].stateBefore
        }

    val currentActiveSolveStep: SolveStep?
        get() = solveSteps.getOrNull(currentStepIndex)

    val isGuideAtSolvedEnd: Boolean
        get() = solveSteps.isNotEmpty() && currentStepIndex >= solveSteps.size
}

class CubeSolverViewModel : ViewModel() {

    private val initialMoves = CubeMove.parseSequence(CubeSolverEngine.PRESET_SCRAMBLES.first().movesNotation)
    private var scrambleHistory: List<CubeMove> = initialMoves
    private val initialCube = CubeState.solved().applyMoves(initialMoves)
    private val initialValidation = CubeSolverEngine.validate(initialCube)
    private val initialSteps = CubeSolverEngine.solve(initialCube, initialMoves)

    private val _uiState = MutableStateFlow(
        CubeSolverUiState(
            cubeState = initialCube,
            liveDetectedColors = initialCube.faceStickers(CubeFace.F),
            validationResult = initialValidation,
            solveSteps = initialSteps
        )
    )
    val uiState: StateFlow<CubeSolverUiState> = _uiState.asStateFlow()

    private var autoPlayJob: Job? = null
    private var lastAutoCaptureTimeMs: Long = 0L

    fun toggleDarkTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun selectTab(tab: AppTab) {
        if (tab != AppTab.SOLVE_GUIDE) {
            stopAutoPlay()
        }
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun selectScanFace(face: CubeFace) {
        _uiState.update { state ->
            state.copy(
                currentScanFace = face,
                liveDetectedColors = state.cubeState.faceStickers(face),
                stabilityProgress = 0f,
                statusMessage = face.scanInstruction
            )
        }
    }

    fun toggleNoCameraMode() {
        _uiState.update { state ->
            val next = !state.preferNoCameraMode
            state.copy(
                preferNoCameraMode = next,
                autoCaptureEnabled = if (next) false else state.autoCaptureEnabled,
                statusMessage = if (next) {
                    "No-Camera Manual Mode active: pick a brush color or tap cells to enter all 6 faces."
                } else {
                    "Camera Scanner Mode active: align each face inside the 3×3 AR viewfinder."
                }
            )
        }
    }

    fun toggleNoCameraGuide() {
        _uiState.update { it.copy(isNoCameraGuideExpanded = !it.isNoCameraGuideExpanded) }
    }

    fun toggleAutoCapture() {
        _uiState.update { state ->
            val next = !state.autoCaptureEnabled
            state.copy(
                autoCaptureEnabled = next,
                statusMessage = if (next) {
                    "Auto-Capture Lock active: hold cube steady in grid to lock each face"
                } else {
                    "Manual capture mode: tap Capture Face when colors match"
                }
            )
        }
    }

    fun toggleTorch() {
        _uiState.update { it.copy(torchEnabled = !it.torchEnabled) }
    }

    /**
     * Called by CameraX [CubeCameraAnalyzer] on background/main thread with live 3x3 HSV classification.
     */
    fun onCameraGridSample(sample: LiveGridSample) {
        val current = _uiState.value
        if (current.preferNoCameraMode && current.activeTab == AppTab.SCANNER) {
            return
        }

        // If user is in Solve Guide Camera Verification mode, compare live camera grid against expected Front face
        if (current.activeTab == AppTab.SOLVE_GUIDE && current.isCameraVerifySheetOpen) {
            val activeStep = current.currentActiveSolveStep
            if (activeStep != null) {
                val expectedFront = activeStep.stateAfter.faceStickers(activeStep.move.face)
                val matches = sample.colors.indices.count { i -> sample.colors[i] == expectedFront[i] }
                _uiState.update {
                    it.copy(
                        liveDetectedColors = sample.colors,
                        cameraVerifyMatchCount = matches,
                        stabilityProgress = sample.stabilityProgress
                    )
                }
                if (matches >= 8 && sample.stabilityProgress >= 0.6f) {
                    val now = System.currentTimeMillis()
                    if (now - lastAutoCaptureTimeMs > 1300L) {
                        lastAutoCaptureTimeMs = now
                        nextSolveStep()
                    }
                }
            }
            return
        }

        // Standard Scanner tab live feed
        _uiState.update { state ->
            state.copy(
                liveDetectedColors = sample.colors,
                centerHue = sample.centerHue,
                centerSat = sample.centerSat,
                centerVal = sample.centerVal,
                stabilityProgress = sample.stabilityProgress
            )
        }

        if (current.autoCaptureEnabled && sample.stabilityProgress >= 1.0f) {
            val now = System.currentTimeMillis()
            if (now - lastAutoCaptureTimeMs > 1600L) {
                lastAutoCaptureTimeMs = now
                captureCurrentFace()
            }
        }
    }

    /**
     * Allows user to tap any of the 8 outer stickers in the scanner/manual 3x3 grid.
     * In No-Camera Manual Mode, applies the selected brush color (or cycles if already that color).
     */
    fun cycleLivePreviewSticker(indexInFace: Int) {
        if (indexInFace == 4) return
        _uiState.update { state ->
            val mutable = state.liveDetectedColors.toMutableList()
            if (state.preferNoCameraMode && mutable[indexInFace] != state.selectedPaintColor) {
                mutable[indexInFace] = state.selectedPaintColor
            } else {
                val nextOrdinal = (mutable[indexInFace].ordinal + 1) % CubeColor.entries.size
                mutable[indexInFace] = CubeColor.entries[nextOrdinal]
            }
            state.copy(liveDetectedColors = mutable)
        }
    }

    /**
     * Locks the 9 stickers currently in [liveDetectedColors] onto [currentScanFace] and advances
     * to the next un-scanned face in [CubeFace.SCAN_ORDER].
     */
    fun captureCurrentFace() {
        _uiState.update { state ->
            val face = state.currentScanFace
            val updatedCube = state.cubeState.withFace(face, state.liveDetectedColors)
            val updatedScanned = state.scannedFaces + face
            val validation = CubeSolverEngine.validate(updatedCube)

            val remaining = CubeFace.SCAN_ORDER.filter { it !in updatedScanned }
            val nextFace = remaining.firstOrNull() ?: CubeFace.F
            val allSixDone = updatedScanned.size == 6

            val solvedSteps = if (validation.isValid) {
                CubeSolverEngine.solve(updatedCube, scrambleHistory)
            } else {
                state.solveSteps
            }

            state.copy(
                cubeState = updatedCube,
                scannedFaces = updatedScanned,
                currentScanFace = nextFace,
                liveDetectedColors = updatedCube.faceStickers(nextFace),
                stabilityProgress = 0f,
                validationResult = validation,
                solveSteps = solvedSteps,
                currentStepIndex = 0,
                statusMessage = when {
                    allSixDone && validation.isValid ->
                        "All 6 faces captured & verified! Tap 'Solve Guide' to start solving."
                    allSixDone && !validation.isValid ->
                        "All 6 faces scanned — check Cube Net to fix sticker count mismatch."
                    else ->
                        "Captured ${face.title} (${updatedScanned.size}/6). Next: ${nextFace.scanInstruction}"
                }
            )
        }
    }

    fun resetScanProgress() {
        _uiState.update { state ->
            state.copy(
                scannedFaces = emptySet(),
                currentScanFace = CubeFace.F,
                liveDetectedColors = state.cubeState.faceStickers(CubeFace.F),
                stabilityProgress = 0f,
                statusMessage = "Scan reset. Point GREEN center at camera with WHITE on top."
            )
        }
    }

    fun selectPaintColor(color: CubeColor) {
        _uiState.update { it.copy(selectedPaintColor = color) }
    }

    fun paintNetSticker(face: CubeFace, indexInFace: Int) {
        if (indexInFace == 4) {
            _uiState.update {
                it.copy(statusMessage = "Center stickers are fixed orientation anchors (${face.centerColor.displayName}).")
            }
            return
        }
        // Editing individual stickers invalidates preset move history
        scrambleHistory = emptyList()
        _uiState.update { state ->
            val updatedCube = state.cubeState.withSticker(face, indexInFace, state.selectedPaintColor)
            val validation = CubeSolverEngine.validate(updatedCube)
            val steps = if (validation.isValid) {
                CubeSolverEngine.solve(updatedCube, null)
            } else {
                emptyList()
            }
            state.copy(
                cubeState = updatedCube,
                liveDetectedColors = if (face == state.currentScanFace) {
                    updatedCube.faceStickers(face)
                } else {
                    state.liveDetectedColors
                },
                validationResult = validation,
                solveSteps = steps,
                currentStepIndex = 0,
                activeScrambleLabel = "Custom Scanned / Edited Cube",
                statusMessage = if (validation.isValid) {
                    "Cube state valid! Solution computed (${steps.size} moves)."
                } else {
                    validation.errorSummary
                }
            )
        }
    }

    /**
     * Applies a real 3D layer turn directly to the current cube state (great for testing or manual setup).
     */
    fun applyManualMove(move: CubeMove) {
        stopAutoPlay()
        scrambleHistory = CubeSolverEngine.simplifyMoves(scrambleHistory + move)
        _uiState.update { state ->
            val updatedCube = state.cubeState.applyMove(move)
            val validation = CubeSolverEngine.validate(updatedCube)
            val steps = CubeSolverEngine.solve(updatedCube, scrambleHistory)
            state.copy(
                cubeState = updatedCube,
                liveDetectedColors = updatedCube.faceStickers(state.currentScanFace),
                validationResult = validation,
                solveSteps = steps,
                currentStepIndex = 0,
                statusMessage = "Applied move ${move.notation} (${move.title}) • ${steps.size} moves to solve"
            )
        }
    }

    fun loadPresetScramble(preset: PresetScramble) {
        stopAutoPlay()
        val moves = CubeMove.parseSequence(preset.movesNotation)
        scrambleHistory = moves
        val newCube = CubeState.solved().applyMoves(moves)
        val validation = CubeSolverEngine.validate(newCube)
        val steps = CubeSolverEngine.solve(newCube, moves)

        _uiState.update { state ->
            state.copy(
                cubeState = newCube,
                scannedFaces = CubeFace.entries.toSet(),
                liveDetectedColors = newCube.faceStickers(state.currentScanFace),
                validationResult = validation,
                solveSteps = steps,
                currentStepIndex = 0,
                activeScrambleLabel = "${preset.name} (${preset.movesNotation})",
                statusMessage = "Loaded '${preset.name}' — ${steps.size}-step solution ready!"
            )
        }
    }

    fun generateRandomScramble() {
        stopAutoPlay()
        val (newCube, moves) = CubeSolverEngine.generateRandomScramble(moveCount = 7)
        scrambleHistory = moves
        val notation = moves.joinToString(" ") { it.notation }
        val validation = CubeSolverEngine.validate(newCube)
        val steps = CubeSolverEngine.solve(newCube, moves)

        _uiState.update { state ->
            state.copy(
                cubeState = newCube,
                scannedFaces = CubeFace.entries.toSet(),
                liveDetectedColors = newCube.faceStickers(state.currentScanFace),
                validationResult = validation,
                solveSteps = steps,
                currentStepIndex = 0,
                activeScrambleLabel = "Random Scramble ($notation)",
                statusMessage = "Scrambled ($notation) — ${steps.size} optimal moves computed!"
            )
        }
    }

    fun resetToSolvedCube() {
        stopAutoPlay()
        scrambleHistory = emptyList()
        val solved = CubeState.solved()
        val validation = CubeSolverEngine.validate(solved)
        _uiState.update { state ->
            state.copy(
                cubeState = solved,
                scannedFaces = emptySet(),
                liveDetectedColors = solved.faceStickers(state.currentScanFace),
                validationResult = validation,
                solveSteps = emptyList(),
                currentStepIndex = 0,
                activeScrambleLabel = "Solved Reference Cube",
                statusMessage = "Cube reset to solved state."
            )
        }
    }

    fun computeAndOpenSolveGuide() {
        stopAutoPlay()
        val state = _uiState.value
        val validation = CubeSolverEngine.validate(state.cubeState)
        if (!validation.isValid) {
            _uiState.update {
                it.copy(
                    validationResult = validation,
                    activeTab = AppTab.CUBE_NET,
                    statusMessage = validation.errorSummary
                )
            }
            return
        }
        val steps = CubeSolverEngine.solve(state.cubeState, scrambleHistory)
        _uiState.update {
            it.copy(
                validationResult = validation,
                solveSteps = steps,
                currentStepIndex = 0,
                activeTab = AppTab.SOLVE_GUIDE,
                statusMessage = if (steps.isEmpty()) {
                    "Cube is already solved!"
                } else {
                    "Follow the ${steps.size} guided 3D steps below."
                }
            )
        }
    }

    fun jumpToSolveStep(index: Int) {
        stopAutoPlay()
        _uiState.update { state ->
            val coerced = index.coerceIn(0, state.solveSteps.size)
            state.copy(
                currentStepIndex = coerced,
                cameraVerifyMatchCount = 0
            )
        }
    }

    fun nextSolveStep() {
        _uiState.update { state ->
            if (state.currentStepIndex < state.solveSteps.size) {
                state.copy(
                    currentStepIndex = state.currentStepIndex + 1,
                    cameraVerifyMatchCount = 0
                )
            } else {
                state.copy(isAutoPlaying = false)
            }
        }
    }

    fun prevSolveStep() {
        stopAutoPlay()
        _uiState.update { state ->
            if (state.currentStepIndex > 0) {
                state.copy(
                    currentStepIndex = state.currentStepIndex - 1,
                    cameraVerifyMatchCount = 0
                )
            } else {
                state
            }
        }
    }

    fun toggleAutoPlay() {
        if (_uiState.value.isAutoPlaying) {
            stopAutoPlay()
            return
        }
        if (_uiState.value.solveSteps.isEmpty()) return
        if (_uiState.value.currentStepIndex >= _uiState.value.solveSteps.size) {
            _uiState.update { it.copy(currentStepIndex = 0) }
        }
        _uiState.update { it.copy(isAutoPlaying = true) }
        autoPlayJob = viewModelScope.launch {
            while (_uiState.value.isAutoPlaying &&
                _uiState.value.currentStepIndex < _uiState.value.solveSteps.size
            ) {
                delay(1650L)
                if (_uiState.value.isAutoPlaying) {
                    nextSolveStep()
                }
            }
            _uiState.update { it.copy(isAutoPlaying = false) }
        }
    }

    private fun stopAutoPlay() {
        autoPlayJob?.cancel()
        autoPlayJob = null
        _uiState.update { it.copy(isAutoPlaying = false) }
    }

    fun toggleCameraVerifySheet() {
        stopAutoPlay()
        _uiState.update {
            it.copy(
                isCameraVerifySheetOpen = !it.isCameraVerifySheetOpen,
                cameraVerifyMatchCount = 0
            )
        }
    }
}
