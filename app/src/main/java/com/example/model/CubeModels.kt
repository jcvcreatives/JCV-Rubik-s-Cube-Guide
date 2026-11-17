package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.StickerBlue
import com.example.ui.theme.StickerGreen
import com.example.ui.theme.StickerOrange
import com.example.ui.theme.StickerRed
import com.example.ui.theme.StickerWhite
import com.example.ui.theme.StickerYellow
import kotlin.math.abs

/**
 * Represents the 6 standard colors of a Rubik's Cube and their canonical home faces.
 */
enum class CubeColor(
    val displayName: String,
    val shortCode: Char,
    val composeColor: Color,
    val textDark: Boolean,
    val refHue: Float,
    val refSat: Float,
    val refVal: Float
) {
    WHITE("White", 'W', StickerWhite, true, 0f, 0.08f, 0.92f),
    YELLOW("Yellow", 'Y', StickerYellow, true, 52f, 0.85f, 0.95f),
    RED("Red", 'R', StickerRed, false, 356f, 0.88f, 0.88f),
    ORANGE("Orange", 'O', StickerOrange, true, 24f, 0.90f, 0.95f),
    GREEN("Green", 'G', StickerGreen, true, 138f, 0.82f, 0.85f),
    BLUE("Blue", 'B', StickerBlue, false, 218f, 0.86f, 0.88f);

    companion object {
        /**
         * Classifies an HSV sample from the camera stream into one of the 6 Rubik's Cube colors.
         */
        fun fromHsv(hue: Float, sat: Float, value: Float): CubeColor {
            // High brightness & low saturation -> WHITE
            if (sat < 0.26f && value > 0.42f) {
                return WHITE
            }
            // Also handle slightly warm indoor lighting white
            if (sat < 0.34f && value > 0.78f && (hue < 38f || hue > 190f)) {
                return WHITE
            }

            // Classify chromatic stickers primarily by Hue angle (0..360)
            return when {
                hue >= 332f || hue < 11f -> RED
                hue in 11f..40f -> ORANGE
                hue in 40f..76f -> YELLOW
                hue in 76f..172f -> GREEN
                hue in 172f..275f -> BLUE
                else -> {
                    // Magenta/purple reflection fallback: compare closeness to Red vs Blue
                    if (hue >= 275f && hue < 332f) {
                        if (hue > 302f) RED else BLUE
                    } else {
                        RED
                    }
                }
            }
        }
    }
}

/**
 * Represents the 6 faces of the Rubik's Cube in standard WCA Singmaster orientation:
 * U = Up (White), R = Right (Red), F = Front (Green),
 * D = Down (Yellow), L = Left (Orange), B = Back (Blue).
 */
enum class CubeFace(
    val code: Char,
    val title: String,
    val centerColor: CubeColor,
    val nx: Int,
    val ny: Int,
    val nz: Int,
    val scanInstruction: String,
    val topNeighborLabel: String,
    val bottomNeighborLabel: String,
    val leftNeighborLabel: String,
    val rightNeighborLabel: String
) {
    F(
        code = 'F',
        title = "Front Face",
        centerColor = CubeColor.GREEN,
        nx = 0, ny = 0, nz = 1,
        scanInstruction = "Point GREEN center at camera with WHITE center on top",
        topNeighborLabel = "U (White)",
        bottomNeighborLabel = "D (Yellow)",
        leftNeighborLabel = "L (Orange)",
        rightNeighborLabel = "R (Red)"
    ),
    R(
        code = 'R',
        title = "Right Face",
        centerColor = CubeColor.RED,
        nx = 1, ny = 0, nz = 0,
        scanInstruction = "Rotate cube left: Point RED center at camera with WHITE on top",
        topNeighborLabel = "U (White)",
        bottomNeighborLabel = "D (Yellow)",
        leftNeighborLabel = "F (Green)",
        rightNeighborLabel = "B (Blue)"
    ),
    B(
        code = 'B',
        title = "Back Face",
        centerColor = CubeColor.BLUE,
        nx = 0, ny = 0, nz = -1,
        scanInstruction = "Rotate cube left again: Point BLUE center at camera with WHITE on top",
        topNeighborLabel = "U (White)",
        bottomNeighborLabel = "D (Yellow)",
        leftNeighborLabel = "R (Red)",
        rightNeighborLabel = "L (Orange)"
    ),
    L(
        code = 'L',
        title = "Left Face",
        centerColor = CubeColor.ORANGE,
        nx = -1, ny = 0, nz = 0,
        scanInstruction = "Rotate cube left again: Point ORANGE center at camera with WHITE on top",
        topNeighborLabel = "U (White)",
        bottomNeighborLabel = "D (Yellow)",
        leftNeighborLabel = "B (Blue)",
        rightNeighborLabel = "F (Green)"
    ),
    U(
        code = 'U',
        title = "Up (Top) Face",
        centerColor = CubeColor.WHITE,
        nx = 0, ny = 1, nz = 0,
        scanInstruction = "Tilt top toward camera: Point WHITE center at camera with BLUE at top, GREEN at bottom",
        topNeighborLabel = "B (Blue)",
        bottomNeighborLabel = "F (Green)",
        leftNeighborLabel = "L (Orange)",
        rightNeighborLabel = "R (Red)"
    ),
    D(
        code = 'D',
        title = "Down (Bottom) Face",
        centerColor = CubeColor.YELLOW,
        nx = 0, ny = -1, nz = 0,
        scanInstruction = "Tilt bottom toward camera: Point YELLOW center at camera with GREEN at top, BLUE at bottom",
        topNeighborLabel = "F (Green)",
        bottomNeighborLabel = "B (Blue)",
        leftNeighborLabel = "L (Orange)",
        rightNeighborLabel = "R (Red)"
    );

    companion object {
        val SCAN_ORDER = listOf(F, R, B, L, U, D)
    }
}

/**
 * 3D integer coordinate + outward surface normal for a single sticker on the 3x3x3 cube.
 */
data class Sticker3D(
    val face: CubeFace,
    val row: Int,
    val col: Int,
    val x: Int,
    val y: Int,
    val z: Int,
    val nx: Int,
    val ny: Int,
    val nz: Int
)

/**
 * Exact 3D geometric LUT for all 54 stickers (6 faces * 9 cells).
 */
object CubeGeometry {
    val stickers: List<Sticker3D> = buildList {
        for (face in CubeFace.entries) {
            for (row in 0..2) {
                for (col in 0..2) {
                    val (x, y, z) = when (face) {
                        CubeFace.F -> Triple(col - 1, 1 - row, 1)
                        CubeFace.B -> Triple(1 - col, 1 - row, -1)
                        CubeFace.R -> Triple(1, 1 - row, 1 - col)
                        CubeFace.L -> Triple(-1, 1 - row, col - 1)
                        CubeFace.U -> Triple(col - 1, 1, row - 1)
                        CubeFace.D -> Triple(col - 1, -1, 1 - row)
                    }
                    add(Sticker3D(face, row, col, x, y, z, face.nx, face.ny, face.nz))
                }
            }
        }
    }

    fun stickerIndex(face: CubeFace, row: Int, col: Int): Int =
        face.ordinal * 9 + row * 3 + col

    private fun findStickerIndexBy3D(x: Int, y: Int, z: Int, nx: Int, ny: Int, nz: Int): Int {
        val idx = stickers.indexOfFirst {
            it.x == x && it.y == y && it.z == z && it.nx == nx && it.ny == ny && it.nz == nz
        }
        check(idx >= 0) { "Invalid 3D sticker coordinate ($x,$y,$z) normal ($nx,$ny,$nz)" }
        return idx
    }

    /**
     * Rotates vector (vx, vy, vz) 90 degrees clockwise when looking directly at face with outward normal (fx, fy, fz).
     * Formula: v_rot = (v . f) * f + (v x f)
     */
    private fun rotateCwAroundFaceNormal(
        vx: Int, vy: Int, vz: Int,
        fx: Int, fy: Int, fz: Int
    ): Triple<Int, Int, Int> {
        val dot = vx * fx + vy * fy + vz * fz
        val cx = vy * fz - vz * fy
        val cy = vz * fx - vx * fz
        val cz = vx * fy - vy * fx
        return Triple(dot * fx + cx, dot * fy + cy, dot * fz + cz)
    }

    /**
     * Precomputes the exact 54-sticker permutation table for a 90° clockwise turn of [face].
     * result[newIndex] = oldIndex
     */
    fun computeCwPermutation(face: CubeFace): IntArray {
        val perm = IntArray(54) { it }
        val fx = face.nx
        val fy = face.ny
        val fz = face.nz
        for (oldIdx in stickers.indices) {
            val s = stickers[oldIdx]
            // Check if sticker lies on the rotating layer (position dot faceNormal == 1)
            if (s.x * fx + s.y * fy + s.z * fz == 1) {
                val (nxPos, nyPos, nzPos) = rotateCwAroundFaceNormal(s.x, s.y, s.z, fx, fy, fz)
                val (nxNorm, nyNorm, nzNorm) = rotateCwAroundFaceNormal(s.nx, s.ny, s.nz, fx, fy, fz)
                val newIdx = findStickerIndexBy3D(nxPos, nyPos, nzPos, nxNorm, nyNorm, nzNorm)
                perm[newIdx] = oldIdx
            }
        }
        return perm
    }

    fun combinePermutations(first: IntArray, second: IntArray): IntArray {
        return IntArray(54) { idx -> first[second[idx]] }
    }
}

/**
 * Standard WCA Singmaster moves.
 * [turnsCw] is 1 for 90° CW, 2 for 180°, 3 for 90° CCW (prime).
 */
enum class CubeMove(
    val notation: String,
    val face: CubeFace,
    val turnsCw: Int,
    val title: String,
    val coachingInstruction: String
) {
    U("U", CubeFace.U, 1, "Top 90° Clockwise", "Push the top layer to the left with your right index finger."),
    U_PRIME("U'", CubeFace.U, 3, "Top 90° Counter-Clockwise", "Push the top layer to the right with your left index finger."),
    U2("U2", CubeFace.U, 2, "Top 180° Double Turn", "Rotate the top White layer halfway around (180°)."),

    D("D", CubeFace.D, 1, "Bottom 90° Clockwise", "Push the bottom layer to the right with your left ring finger."),
    D_PRIME("D'", CubeFace.D, 3, "Bottom 90° Counter-Clockwise", "Push the bottom layer to the left with your right ring finger."),
    D2("D2", CubeFace.D, 2, "Bottom 180° Double Turn", "Rotate the bottom Yellow layer halfway around (180°)."),

    R("R", CubeFace.R, 1, "Right 90° Upward (CW)", "Turn the right layer upward away from you."),
    R_PRIME("R'", CubeFace.R, 3, "Right 90° Downward (CCW)", "Pull the right layer downward toward you."),
    R2("R2", CubeFace.R, 2, "Right 180° Double Turn", "Rotate the right Red layer halfway around (180°)."),

    L("L", CubeFace.L, 1, "Left 90° Downward (CW)", "Pull the left layer downward toward you."),
    L_PRIME("L'", CubeFace.L, 3, "Left 90° Upward (CCW)", "Turn the left layer upward away from you."),
    L2("L2", CubeFace.L, 2, "Left 180° Double Turn", "Rotate the left Orange layer halfway around (180°)."),

    F("F", CubeFace.F, 1, "Front 90° Clockwise", "Turn the front Green face 90° clockwise like a clock hand."),
    F_PRIME("F'", CubeFace.F, 3, "Front 90° Counter-Clockwise", "Turn the front Green face 90° counter-clockwise."),
    F2("F2", CubeFace.F, 2, "Front 180° Double Turn", "Rotate the front Green face halfway around (180°)."),

    B("B", CubeFace.B, 1, "Back 90° Clockwise", "Turn the back Blue layer 90° clockwise (top goes left)."),
    B_PRIME("B'", CubeFace.B, 3, "Back 90° Counter-Clockwise", "Turn the back Blue layer 90° counter-clockwise (top goes right)."),
    B2("B2", CubeFace.B, 2, "Back 180° Double Turn", "Rotate the back Blue face halfway around (180°).");

    val permutation: IntArray by lazy {
        val cw1 = CubeGeometry.computeCwPermutation(face)
        val cw2 = CubeGeometry.combinePermutations(cw1, cw1)
        val cw3 = CubeGeometry.combinePermutations(cw2, cw1)
        when (turnsCw) {
            1 -> cw1
            2 -> cw2
            3 -> cw3
            else -> cw1
        }
    }

    fun inverse(): CubeMove = fromFaceAndTurns(face, (4 - turnsCw) % 4)!!

    companion object {
        fun fromFaceAndTurns(face: CubeFace, turnsCw: Int): CubeMove? {
            val normalized = ((turnsCw % 4) + 4) % 4
            if (normalized == 0) return null
            return entries.first { it.face == face && it.turnsCw == normalized }
        }

        fun parseSequence(sequence: String): List<CubeMove> {
            return sequence.trim()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .mapNotNull { token -> entries.find { it.notation.equals(token, ignoreCase = true) } }
        }
    }
}

/**
 * Immutable 54-sticker state of the 3x3x3 Rubik's Cube.
 */
data class CubeState(
    val stickers: List<CubeColor>
) {
    init {
        require(stickers.size == 54) { "CubeState requires exactly 54 stickers" }
    }

    fun faceStickers(face: CubeFace): List<CubeColor> {
        val start = face.ordinal * 9
        return stickers.subList(start, start + 9)
    }

    fun stickerAt(face: CubeFace, row: Int, col: Int): CubeColor =
        stickers[face.ordinal * 9 + row * 3 + col]

    fun stickerAt(face: CubeFace, indexInFace: Int): CubeColor =
        stickers[face.ordinal * 9 + indexInFace]

    fun withFace(face: CubeFace, colors: List<CubeColor>): CubeState {
        require(colors.size == 9)
        val mutable = stickers.toMutableList()
        val start = face.ordinal * 9
        for (i in 0 until 9) {
            mutable[start + i] = if (i == 4) face.centerColor else colors[i]
        }
        return CubeState(mutable)
    }

    fun withSticker(face: CubeFace, indexInFace: Int, color: CubeColor): CubeState {
        if (indexInFace == 4) return this // Center stickers are fixed reference points
        val mutable = stickers.toMutableList()
        mutable[face.ordinal * 9 + indexInFace] = color
        return CubeState(mutable)
    }

    fun applyMove(move: CubeMove): CubeState {
        val perm = move.permutation
        val next = List(54) { newIdx -> stickers[perm[newIdx]] }
        return CubeState(next)
    }

    fun applyMoves(moves: Iterable<CubeMove>): CubeState {
        var current = this
        for (move in moves) {
            current = current.applyMove(move)
        }
        return current
    }

    fun isSolved(): Boolean {
        for (face in CubeFace.entries) {
            val target = face.centerColor
            val start = face.ordinal * 9
            for (i in 0 until 9) {
                if (stickers[start + i] != target) return false
            }
        }
        return true
    }

    fun solvedStickerCount(): Int {
        var count = 0
        for (face in CubeFace.entries) {
            val target = face.centerColor
            val start = face.ordinal * 9
            for (i in 0 until 9) {
                if (stickers[start + i] == target) count++
            }
        }
        return count
    }

    companion object {
        fun solved(): CubeState {
            val list = buildList(54) {
                for (face in CubeFace.entries) {
                    repeat(9) { add(face.centerColor) }
                }
            }
            return CubeState(list)
        }
    }
}

enum class SolvePhase(val title: String, val badgeText: String) {
    DIRECT_OPTIMAL("Optimal Shortest Path", "OPTIMAL"),
    WHITE_CROSS("Stage 1 • White Top Cross", "STAGE 1"),
    WHITE_CORNERS("Stage 2 • First Layer Corners", "STAGE 2"),
    MIDDLE_EDGES("Stage 3 • Middle Layer Edges", "STAGE 3"),
    YELLOW_CROSS("Stage 4 • Bottom Yellow Cross", "STAGE 4"),
    YELLOW_EDGES("Stage 5 • Align Yellow Edges", "STAGE 5"),
    YELLOW_CORNERS_PERM("Stage 6 • Position Yellow Corners", "STAGE 6"),
    YELLOW_CORNERS_ORIENT("Stage 7 • Twist Final Corners", "STAGE 7")
}

data class SolveStep(
    val stepNumber: Int,
    val totalSteps: Int,
    val move: CubeMove,
    val phase: SolvePhase,
    val stateBefore: CubeState,
    val stateAfter: CubeState,
    val holdOrientationHint: String
)

data class CubeValidationResult(
    val isValid: Boolean,
    val colorCounts: Map<CubeColor, Int>,
    val errorSummary: String? = null,
    val actionableSuggestion: String? = null
)
