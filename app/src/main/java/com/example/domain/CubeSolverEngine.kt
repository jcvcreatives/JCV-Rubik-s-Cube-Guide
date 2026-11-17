package com.example.domain

import com.example.model.CubeColor
import com.example.model.CubeFace
import com.example.model.CubeGeometry
import com.example.model.CubeMove
import com.example.model.CubeState
import com.example.model.CubeValidationResult
import com.example.model.SolvePhase
import com.example.model.SolveStep
import kotlin.random.Random

data class PresetScramble(
    val id: String,
    val name: String,
    val subtitle: String,
    val movesNotation: String
)

object CubeSolverEngine {

    val PRESET_SCRAMBLES = listOf(
        PresetScramble(
            id = "quick_6",
            name = "6-Move Speed Calibration",
            subtitle = "Ideal for testing live camera scanning & optimal 6-step guide",
            movesNotation = "R U F' L D2 R'"
        ),
        PresetScramble(
            id = "tactical_8",
            name = "8-Move Tactical Scramble",
            subtitle = "Balanced multi-face scramble with optimal meet-in-the-middle path",
            movesNotation = "F R U' B L2 D F' R2"
        ),
        PresetScramble(
            id = "f2l_practice",
            name = "12-Move Full Layer Scramble",
            subtitle = "Multi-stage layer-by-layer solve demonstration",
            movesNotation = "R U R' U' F' U F D R U' R' D'"
        ),
        PresetScramble(
            id = "checkerboard",
            name = "Checkerboard Pattern",
            subtitle = "Classic symmetrical 6-face alternating color pattern",
            movesNotation = "U2 D2 R2 L2 F2 B2"
        )
    )

    private val OPPOSITE_COLORS = mapOf(
        CubeColor.WHITE to CubeColor.YELLOW,
        CubeColor.YELLOW to CubeColor.WHITE,
        CubeColor.RED to CubeColor.ORANGE,
        CubeColor.ORANGE to CubeColor.RED,
        CubeColor.GREEN to CubeColor.BLUE,
        CubeColor.BLUE to CubeColor.GREEN
    )

    /**
     * Validates that a 54-sticker cube state has 9 of each color and valid edge/corner piece combinations.
     */
    fun validate(state: CubeState): CubeValidationResult {
        val counts = CubeColor.entries.associateWith { 0 }.toMutableMap()
        for (color in state.stickers) {
            counts[color] = (counts[color] ?: 0) + 1
        }

        val wrongColors = counts.filterValues { it != 9 }
        if (wrongColors.isNotEmpty()) {
            val over = wrongColors.filterValues { it > 9 }
                .entries.joinToString { "${it.key.displayName} (${it.value})" }
            val under = wrongColors.filterValues { it < 9 }
                .entries.joinToString { "${it.key.displayName} (${it.value})" }
            return CubeValidationResult(
                isValid = false,
                colorCounts = counts,
                errorSummary = "Sticker count mismatch: Every color needs exactly 9 stickers.",
                actionableSuggestion = buildString {
                    if (over.isNotEmpty()) append("Too many: $over. ")
                    if (under.isNotEmpty()) append("Missing: $under. ")
                    append("Open the Cube Net tab and tap the miscolored sticker to fix it.")
                }
            )
        }

        // Verify centers match canonical faces
        for (face in CubeFace.entries) {
            if (state.stickerAt(face, 4) != face.centerColor) {
                return CubeValidationResult(
                    isValid = false,
                    colorCounts = counts,
                    errorSummary = "${face.title} center must be ${face.centerColor.displayName}.",
                    actionableSuggestion = "Ensure center stickers match standard orientation."
                )
            }
        }

        // Verify 12 edges do not contain identical or opposite colors on the same piece
        val edgeStickersByCoord = CubeGeometry.stickers
            .withIndex()
            .filter { (_, s) -> (kotlin.math.abs(s.x) + kotlin.math.abs(s.y) + kotlin.math.abs(s.z)) == 2 }
            .groupBy { (_, s) -> Triple(s.x, s.y, s.z) }

        val seenEdges = mutableSetOf<Set<CubeColor>>()
        for ((_, group) in edgeStickersByCoord) {
            val c1 = state.stickers[group[0].index]
            val c2 = state.stickers[group[1].index]
            if (c1 == c2 || OPPOSITE_COLORS[c1] == c2) {
                val faces = "${group[0].value.face.code}/${group[1].value.face.code}"
                return CubeValidationResult(
                    isValid = false,
                    colorCounts = counts,
                    errorSummary = "Impossible edge piece at $faces (${c1.displayName} & ${c2.displayName}).",
                    actionableSuggestion = "${c1.displayName} and ${c2.displayName} cannot share a single edge piece. Check the $faces edge in the Cube Net editor."
                )
            }
            val pair = setOf(c1, c2)
            if (!seenEdges.add(pair)) {
                return CubeValidationResult(
                    isValid = false,
                    colorCounts = counts,
                    errorSummary = "Duplicate ${c1.displayName}-${c2.displayName} edge piece detected.",
                    actionableSuggestion = "Two edge pieces have the same color pair. Re-scan or adjust in the Cube Net tab."
                )
            }
        }

        // Verify 8 corners do not contain identical or opposite colors on the same piece
        val cornerStickersByCoord = CubeGeometry.stickers
            .withIndex()
            .filter { (_, s) -> (kotlin.math.abs(s.x) + kotlin.math.abs(s.y) + kotlin.math.abs(s.z)) == 3 }
            .groupBy { (_, s) -> Triple(s.x, s.y, s.z) }

        val seenCorners = mutableSetOf<Set<CubeColor>>()
        for ((_, group) in cornerStickersByCoord) {
            val colors = group.map { state.stickers[it.index] }
            if (colors.distinct().size < 3 ||
                colors.any { c -> OPPOSITE_COLORS[c] in colors }
            ) {
                val faces = group.joinToString("/") { it.value.face.code.toString() }
                return CubeValidationResult(
                    isValid = false,
                    colorCounts = counts,
                    errorSummary = "Impossible corner piece at $faces.",
                    actionableSuggestion = "Check the corner stickers at $faces in the Cube Net tab."
                )
            }
            if (!seenCorners.add(colors.toSet())) {
                return CubeValidationResult(
                    isValid = false,
                    colorCounts = counts,
                    errorSummary = "Duplicate corner piece (${colors.joinToString("-") { it.displayName }}).",
                    actionableSuggestion = "Inspect corner stickers in the Cube Net tab."
                )
            }
        }

        return CubeValidationResult(
            isValid = true,
            colorCounts = counts
        )
    }

    /**
     * Generates a random scramble of [moveCount] non-redundant turns and returns both the state and move list.
     */
    fun generateRandomScramble(moveCount: Int = 7, random: Random = Random.Default): Pair<CubeState, List<CubeMove>> {
        val moves = mutableListOf<CubeMove>()
        var lastFace: CubeFace? = null
        repeat(moveCount) {
            val candidates = CubeMove.entries.filter { it.face != lastFace }
            val chosen = candidates[random.nextInt(candidates.size)]
            moves.add(chosen)
            lastFace = chosen.face
        }
        val state = CubeState.solved().applyMoves(moves)
        return state to moves
    }

    /**
     * Simplifies algebraic move sequences by combining or cancelling adjacent turns of the same face.
     */
    fun simplifyMoves(moves: List<CubeMove>): List<CubeMove> {
        val stack = mutableListOf<CubeMove>()
        for (move in moves) {
            if (stack.isNotEmpty() && stack.last().face == move.face) {
                val prev = stack.removeAt(stack.lastIndex)
                val combined = CubeMove.fromFaceAndTurns(move.face, prev.turnsCw + move.turnsCw)
                if (combined != null) {
                    stack.add(combined)
                }
            } else {
                stack.add(move)
            }
        }
        return stack
    }

    /**
     * Solves a valid [CubeState] and returns the step-by-step interactive solution.
     * Uses Bidirectional Meet-in-the-Middle BFS (up to 8 moves optimal) first, then falls back
     * to a 7-stage Layer-by-Layer + Subgroup Solver for deeper states.
     */
    fun solve(
        initialState: CubeState,
        knownScrambleHistory: List<CubeMove>? = null
    ): List<SolveStep> {
        if (initialState.isSolved()) return emptyList()

        // 1. Try Bidirectional Meet-in-the-Middle BFS (finds optimal 1..8 move solutions in milliseconds)
        val bidirectionalMoves = bidirectionalShortestPath(initialState, maxHalfDepth = 4)
        if (bidirectionalMoves != null) {
            return buildSolveSteps(
                initialState = initialState,
                movesWithPhase = bidirectionalMoves.map { it to SolvePhase.DIRECT_OPTIMAL }
            )
        }

        // 2. Try Staged Layer-by-Layer Subgroup Solver
        val stagedSolution = solveLayerByLayer(initialState)
        if (stagedSolution != null) {
            return buildSolveSteps(initialState, stagedSolution)
        }

        // 3. If the cube came from a deep scramble or custom net edit with known history, invert & simplify
        if (!knownScrambleHistory.isNullOrEmpty()) {
            val inverted = simplifyMoves(knownScrambleHistory.reversed().map { it.inverse() })
            if (initialState.applyMoves(inverted).isSolved()) {
                return buildSolveSteps(
                    initialState = initialState,
                    movesWithPhase = inverted.map { it to SolvePhase.DIRECT_OPTIMAL }
                )
            }
        }

        return emptyList()
    }

    /**
     * Bidirectional BFS up to depth 2 * maxHalfDepth (8 moves).
     */
    private fun bidirectionalShortestPath(start: CubeState, maxHalfDepth: Int = 4): List<CubeMove>? {
        val solved = CubeState.solved()
        if (start == solved) return emptyList()

        // Forward frontier: State -> List<CubeMove> from start
        val forwardVisited = HashMap<CubeState, List<CubeMove>>(4096)
        var forwardFrontier = listOf(start to emptyList<CubeMove>())
        forwardVisited[start] = emptyList()

        // Backward frontier: State -> List<CubeMove> needed to reach solved from State
        val backwardVisited = HashMap<CubeState, List<CubeMove>>(4096)
        var backwardFrontier = listOf(solved to emptyList<CubeMove>())
        backwardVisited[solved] = emptyList()

        for (depth in 1..maxHalfDepth) {
            // Expand forward 1 ply
            val nextForward = ArrayList<Pair<CubeState, List<CubeMove>>>()
            for ((state, path) in forwardFrontier) {
                val lastFace = path.lastOrNull()?.face
                for (move in CubeMove.entries) {
                    if (move.face == lastFace) continue
                    val nextState = state.applyMove(move)
                    if (nextState in forwardVisited) continue
                    val nextPath = path + move
                    val backPath = backwardVisited[nextState]
                    if (backPath != null) {
                        return simplifyMoves(nextPath + backPath)
                    }
                    forwardVisited[nextState] = nextPath
                    if (depth < maxHalfDepth) {
                        nextForward.add(nextState to nextPath)
                    }
                }
            }
            forwardFrontier = nextForward

            // Expand backward 1 ply
            val nextBackward = ArrayList<Pair<CubeState, List<CubeMove>>>()
            for ((state, suffixToSolved) in backwardFrontier) {
                val firstFaceOfSuffix = suffixToSolved.firstOrNull()?.face
                for (move in CubeMove.entries) {
                    if (move.face == firstFaceOfSuffix) continue
                    // If we apply move.inverse() to `state`, then applying `move` takes `prevState` -> `state`
                    val prevState = state.applyMove(move.inverse())
                    if (prevState in backwardVisited) continue
                    val nextSuffix = listOf(move) + suffixToSolved
                    val fwdPath = forwardVisited[prevState]
                    if (fwdPath != null) {
                        return simplifyMoves(fwdPath + nextSuffix)
                    }
                    backwardVisited[prevState] = nextSuffix
                    if (depth < maxHalfDepth) {
                        nextBackward.add(prevState to nextSuffix)
                    }
                }
            }
            backwardFrontier = nextBackward
        }
        return null
    }

    /**
     * 7-Stage Layer-by-Layer + Subgroup Macro Solver.
     */
    private fun solveLayerByLayer(initialState: CubeState): List<Pair<CubeMove, SolvePhase>>? {
        var current = initialState
        val allMoves = mutableListOf<Pair<CubeMove, SolvePhase>>()

        fun appendMoves(moves: List<CubeMove>, phase: SolvePhase) {
            for (m in moves) {
                current = current.applyMove(m)
                allMoves.add(m to phase)
            }
        }

        val solvedRef = CubeState.solved()

        // Helper: checks if a set of 3D coordinates (x, y, z) has all its stickers matching solvedRef
        fun areCoordsSolved(state: CubeState, coords: List<Triple<Int, Int, Int>>): Boolean {
            for ((idx, s) in CubeGeometry.stickers.withIndex()) {
                if (Triple(s.x, s.y, s.z) in coords) {
                    if (state.stickers[idx] != solvedRef.stickers[idx]) return false
                }
            }
            return true
        }

        // Helper: BFS over candidate move sequences (macros) up to maxMacroSteps
        fun searchMacroPath(
            start: CubeState,
            macros: List<List<CubeMove>>,
            maxDepth: Int,
            goalPredicate: (CubeState) -> Boolean
        ): List<CubeMove>? {
            if (goalPredicate(start)) return emptyList()
            val visited = HashSet<CubeState>()
            val queue = ArrayDeque<Pair<CubeState, List<CubeMove>>>()
            visited.add(start)
            queue.add(start to emptyList())

            while (queue.isNotEmpty()) {
                val (st, path) = queue.removeFirst()
                for (macro in macros) {
                    val nextSt = st.applyMoves(macro)
                    if (goalPredicate(nextSt)) {
                        return simplifyMoves(path + macro)
                    }
                    if (path.size + macro.size <= maxDepth && visited.add(nextSt)) {
                        queue.add(nextSt to (path + macro))
                    }
                }
            }
            return null
        }

        val singleTurnMacros = CubeMove.entries.map { listOf(it) }

        // STAGE 1: White Cross on U (coords: (0,1,1), (1,1,0), (0,1,-1), (-1,1,0))
        val whiteCrossCoords = listOf(
            Triple(0, 1, 1),
            Triple(1, 1, 0),
            Triple(0, 1, -1),
            Triple(-1, 1, 0)
        )
        val solvedCrossCoords = mutableListOf<Triple<Int, Int, Int>>()
        for (edgeCoord in whiteCrossCoords) {
            solvedCrossCoords.add(edgeCoord)
            val steps = searchMacroPath(
                start = current,
                macros = singleTurnMacros,
                maxDepth = 5
            ) { st -> areCoordsSolved(st, solvedCrossCoords) } ?: return null
            appendMoves(steps, SolvePhase.WHITE_CROSS)
        }

        // STAGE 2: White Corners on U ((1,1,1), (1,1,-1), (-1,1,-1), (-1,1,1))
        val dTurns = listOf(listOf(CubeMove.D), listOf(CubeMove.D_PRIME), listOf(CubeMove.D2))
        val cornerTriggers = listOf(
            "R' D' R", "F D F'", "R' D2 R D R' D' R",
            "B' D' B", "R D R'", "B' D2 B D B' D' B",
            "L' D' L", "B D B'", "L' D2 L D L' D' L",
            "F' D' F", "L D L'", "F' D2 F D F' D' F"
        ).map { CubeMove.parseSequence(it) } + dTurns

        val whiteCornerCoords = listOf(
            Triple(1, 1, 1),
            Triple(1, 1, -1),
            Triple(-1, 1, -1),
            Triple(-1, 1, 1)
        )
        val solvedStage2Coords = whiteCrossCoords.toMutableList()
        for (cornerCoord in whiteCornerCoords) {
            solvedStage2Coords.add(cornerCoord)
            val steps = searchMacroPath(
                start = current,
                macros = cornerTriggers,
                maxDepth = 16
            ) { st -> areCoordsSolved(st, solvedStage2Coords) } ?: return null
            appendMoves(steps, SolvePhase.WHITE_CORNERS)
        }

        // STAGE 3: Middle Layer Edges ((1,0,1), (1,0,-1), (-1,0,-1), (-1,0,1))
        val middleTriggers = listOf(
            "D' R' D R D F D' F'", "D F D' F' D' R' D R",
            "D' B' D B D R D' R'", "D R D' R' D' B' D B",
            "D' L' D L D B D' B'", "D B D' B' D' L' D L",
            "D' F' D F D L D' L'", "D L D' L' D' F' D F"
        ).map { CubeMove.parseSequence(it) } + dTurns

        val middleEdgeCoords = listOf(
            Triple(1, 0, 1),
            Triple(1, 0, -1),
            Triple(-1, 0, -1),
            Triple(-1, 0, 1)
        )
        val solvedStage3Coords = solvedStage2Coords.toMutableList()
        for (midCoord in middleEdgeCoords) {
            solvedStage3Coords.add(midCoord)
            val steps = searchMacroPath(
                start = current,
                macros = middleTriggers,
                maxDepth = 24
            ) { st -> areCoordsSolved(st, solvedStage3Coords) } ?: return null
            appendMoves(steps, SolvePhase.MIDDLE_EDGES)
        }

        // STAGE 4: Bottom Yellow Cross Orientation (all 4 D-face edge stickers are Yellow)
        val yellowCrossMacros = listOf(
            "F D R D' R' F'",
            "R D B D' B' R'",
            "B D L D' L' B'",
            "L D F D' F' L'"
        ).map { CubeMove.parseSequence(it) } + dTurns

        val dEdgeIndices = listOf(1, 3, 5, 7).map { CubeFace.D.ordinal * 9 + it }
        val stage4Steps = searchMacroPath(
            start = current,
            macros = yellowCrossMacros,
            maxDepth = 18
        ) { st -> dEdgeIndices.all { st.stickers[it] == CubeColor.YELLOW } } ?: return null
        appendMoves(stage4Steps, SolvePhase.YELLOW_CROSS)

        // STAGE 5: Bottom Yellow Edge Permutation
        val yellowEdgeCoords = listOf(
            Triple(0, -1, 1),
            Triple(1, -1, 0),
            Triple(0, -1, -1),
            Triple(-1, -1, 0)
        )
        val suneMacros = listOf(
            "R D R' D R D2 R'",
            "F D F' D F D2 F'",
            "B D B' D B D2 B'",
            "L D L' D L D2 L'"
        ).map { CubeMove.parseSequence(it) } + dTurns

        val stage5Steps = searchMacroPath(
            start = current,
            macros = suneMacros,
            maxDepth = 22
        ) { st -> areCoordsSolved(st, yellowEdgeCoords) } ?: return null
        appendMoves(stage5Steps, SolvePhase.YELLOW_EDGES)

        // STAGE 6: Position Bottom Yellow Corners (check that each bottom corner has its 3 home colors in any twist)
        val yellowCornerCoords = listOf(
            Triple(1, -1, 1),
            Triple(1, -1, -1),
            Triple(-1, -1, -1),
            Triple(-1, -1, 1)
        )
        val niklasMacros = listOf(
            "D R D' L' D R' D' L",
            "D F D' B' D F' D' B",
            "D B D' F' D B' D' F",
            "D L D' R' D L' D' R"
        ).map { CubeMove.parseSequence(it) }

        fun areBottomCornersInPosition(st: CubeState): Boolean {
            for (coord in yellowCornerCoords) {
                val indices = CubeGeometry.stickers.withIndex()
                    .filter { (_, s) -> Triple(s.x, s.y, s.z) == coord }
                    .map { it.index }
                val currentSet = indices.map { st.stickers[it] }.toSet()
                val solvedSet = indices.map { solvedRef.stickers[it] }.toSet()
                if (currentSet != solvedSet) return false
            }
            return true
        }

        val stage6Steps = searchMacroPath(
            start = current,
            macros = niklasMacros,
            maxDepth = 24
        ) { st -> areBottomCornersInPosition(st) } ?: return null
        appendMoves(stage6Steps, SolvePhase.YELLOW_CORNERS_PERM)

        // STAGE 7: Twist Bottom Yellow Corners using standard R U R' U' * 2 commutator + D setup turns
        val twistDoubleSexy = CubeMove.parseSequence("R U R' U' R U R' U'")
        var safety = 0
        while (!current.isSolved() && safety < 24) {
            safety++
            // Check if the front-right-bottom corner (1, -1, 1) has YELLOW on the D face (row=0, col=2 on D -> index 2)
            val dfrYellowStickerIdx = CubeGeometry.stickerIndex(CubeFace.D, 0, 2)
            if (current.stickers[dfrYellowStickerIdx] != CubeColor.YELLOW) {
                appendMoves(twistDoubleSexy, SolvePhase.YELLOW_CORNERS_ORIENT)
            } else {
                appendMoves(listOf(CubeMove.D), SolvePhase.YELLOW_CORNERS_ORIENT)
            }
        }

        // Align D layer if needed
        for (dAlign in listOf(emptyList(), listOf(CubeMove.D), listOf(CubeMove.D_PRIME), listOf(CubeMove.D2))) {
            if (current.applyMoves(dAlign).isSolved()) {
                appendMoves(dAlign, SolvePhase.YELLOW_CORNERS_ORIENT)
                break
            }
        }

        if (!current.isSolved()) return null

        // Compress consecutive same-face moves while preserving phase tags
        val compressed = mutableListOf<Pair<CubeMove, SolvePhase>>()
        for ((move, phase) in allMoves) {
            if (compressed.isNotEmpty() && compressed.last().first.face == move.face) {
                val (prevMove, prevPhase) = compressed.removeAt(compressed.lastIndex)
                val merged = CubeMove.fromFaceAndTurns(move.face, prevMove.turnsCw + move.turnsCw)
                if (merged != null) {
                    compressed.add(merged to prevPhase)
                }
            } else {
                compressed.add(move to phase)
            }
        }
        return compressed
    }

    private fun buildSolveSteps(
        initialState: CubeState,
        movesWithPhase: List<Pair<CubeMove, SolvePhase>>
    ): List<SolveStep> {
        val total = movesWithPhase.size
        var runningState = initialState
        return movesWithPhase.mapIndexed { idx, (move, phase) ->
            val before = runningState
            val after = before.applyMove(move)
            runningState = after
            SolveStep(
                stepNumber = idx + 1,
                totalSteps = total,
                move = move,
                phase = phase,
                stateBefore = before,
                stateAfter = after,
                holdOrientationHint = "Hold GREEN center facing you and WHITE center on top"
            )
        }
    }
}
