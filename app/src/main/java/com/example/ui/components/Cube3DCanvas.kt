package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.CubeFace
import com.example.model.CubeGeometry
import com.example.model.CubeMove
import com.example.model.CubeState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonOrange
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private data class Vec3(val x: Float, val y: Float, val z: Float)

private data class StickerPolygon(
    val points2D: List<Offset>,
    val avgZ: Float,
    val fillColor: Color,
    val isHighlightedLayer: Boolean,
    val isCenter: Boolean
)

/**
 * Interactive 3D perspective Rubik's Cube renderer with touch-drag orbit control
 * and animated 3D layer-turn trajectory arrows for the active [highlightedMove].
 */
@Composable
fun Cube3DCanvas(
    cubeState: CubeState,
    modifier: Modifier = Modifier,
    highlightedMove: CubeMove? = null,
    initialYawDeg: Float = -34f,
    initialPitchDeg: Float = 24f
) {
    var yawDeg by remember { mutableFloatStateOf(initialYawDeg) }
    var pitchDeg by remember { mutableFloatStateOf(initialPitchDeg) }

    val infiniteTransition = rememberInfiniteTransition(label = "movePulse")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulsePhase"
    )

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                yawDeg = (yawDeg + dragAmount.x * 0.45f) % 360f
                pitchDeg = (pitchDeg + dragAmount.y * 0.35f).coerceIn(-68f, 68f)
            }
        }
    ) {
        val w = size.width
        val h = size.height
        val minDim = min(w, h)
        val scale = minDim * 0.25f
        val center = Offset(w * 0.5f, h * 0.52f)

        val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()
        val cosY = cos(yawRad)
        val sinY = sin(yawRad)
        val cosP = cos(pitchRad)
        val sinP = sin(pitchRad)

        // Rotates a 3D world point (where +X=Right, +Y=Up, +Z=Front) into camera space
        fun transform3D(v: Vec3): Vec3 {
            // Yaw around Y axis
            val x1 = v.x * cosY + v.z * sinY
            val z1 = -v.x * sinY + v.z * cosY
            val y1 = v.y
            // Pitch around X axis
            val y2 = y1 * cosP - z1 * sinP
            val z2 = y1 * sinP + z1 * cosP
            return Vec3(x1, y2, z2)
        }

        fun project(vCam: Vec3): Offset {
            val cameraDist = 6.2f
            val perspective = cameraDist / (cameraDist - vCam.z).coerceAtLeast(1.5f)
            return Offset(
                x = center.x + vCam.x * scale * perspective,
                y = center.y - vCam.y * scale * perspective
            )
        }

        // Subtle ambient glow behind the 3D cube
        drawCircle(
            color = ElectricCyan.copy(alpha = 0.08f),
            radius = minDim * 0.44f,
            center = center
        )

        val polygons = ArrayList<StickerPolygon>(108)
        val activeFace = highlightedMove?.face

        for ((idx, s) in CubeGeometry.stickers.withIndex()) {
            val color = cubeState.stickers[idx].composeColor
            val isOnHighlightedLayer = if (activeFace != null) {
                (s.x * activeFace.nx + s.y * activeFace.ny + s.z * activeFace.nz) == 1
            } else {
                false
            }

            // Build tangent vectors uVec and vVec on the sticker's plane
            val normal = Vec3(s.nx.toFloat(), s.ny.toFloat(), s.nz.toFloat())
            val (uVec, vVec) = when (s.face) {
                CubeFace.F -> Vec3(1f, 0f, 0f) to Vec3(0f, 1f, 0f)
                CubeFace.B -> Vec3(-1f, 0f, 0f) to Vec3(0f, 1f, 0f)
                CubeFace.R -> Vec3(0f, 0f, -1f) to Vec3(0f, 1f, 0f)
                CubeFace.L -> Vec3(0f, 0f, 1f) to Vec3(0f, 1f, 0f)
                CubeFace.U -> Vec3(1f, 0f, 0f) to Vec3(0f, 0f, -1f)
                CubeFace.D -> Vec3(1f, 0f, 0f) to Vec3(0f, 0f, 1f)
            }

            // Center of this sticker in [-1.5, +1.5] cube coordinates (each cubie has width 0.66f)
            val cubieSize = 0.66f
            val center3D = Vec3(
                x = s.x * cubieSize + normal.x * (cubieSize * 0.5f),
                y = s.y * cubieSize + normal.y * (cubieSize * 0.5f),
                z = s.z * cubieSize + normal.z * (cubieSize * 0.5f)
            )

            // Backface culling check in camera space
            val camNormal = transform3D(normal)
            val camCenter = transform3D(center3D)
            // Vector from point to camera at (0, 0, 6.2)
            val viewVecZ = 6.2f - camCenter.z
            val dotView = -camCenter.x * camNormal.x - camCenter.y * camNormal.y + viewVecZ * camNormal.z
            if (dotView <= 0f) continue

            // 1. Black plastic cubie backing quad (halfSize = 0.33f)
            val baseHalf = cubieSize * 0.495f
            val baseCornersCam = listOf(
                Vec3(center3D.x - uVec.x * baseHalf - vVec.x * baseHalf, center3D.y - uVec.y * baseHalf - vVec.y * baseHalf, center3D.z - uVec.z * baseHalf - vVec.z * baseHalf),
                Vec3(center3D.x + uVec.x * baseHalf - vVec.x * baseHalf, center3D.y + uVec.y * baseHalf - vVec.y * baseHalf, center3D.z + uVec.z * baseHalf - vVec.z * baseHalf),
                Vec3(center3D.x + uVec.x * baseHalf + vVec.x * baseHalf, center3D.y + uVec.y * baseHalf + vVec.y * baseHalf, center3D.z + uVec.z * baseHalf + vVec.z * baseHalf),
                Vec3(center3D.x - uVec.x * baseHalf + vVec.x * baseHalf, center3D.y - uVec.y * baseHalf + vVec.y * baseHalf, center3D.z - uVec.z * baseHalf + vVec.z * baseHalf)
            ).map { transform3D(it) }

            polygons.add(
                StickerPolygon(
                    points2D = baseCornersCam.map { project(it) },
                    avgZ = baseCornersCam.map { it.z }.average().toFloat() - 0.01f,
                    fillColor = if (isOnHighlightedLayer) Color(0xFF1F2B3E) else Color(0xFF0E131D),
                    isHighlightedLayer = isOnHighlightedLayer,
                    isCenter = false
                )
            )

            // 2. Colored glossy sticker quad (inset slightly for crisp bevel)
            val stickerHalf = cubieSize * 0.415f
            val stickerLift = 0.008f
            val stickerCornersCam = listOf(
                Vec3(center3D.x + normal.x * stickerLift - uVec.x * stickerHalf - vVec.x * stickerHalf, center3D.y + normal.y * stickerLift - uVec.y * stickerHalf - vVec.y * stickerHalf, center3D.z + normal.z * stickerLift - uVec.z * stickerHalf - vVec.z * stickerHalf),
                Vec3(center3D.x + normal.x * stickerLift + uVec.x * stickerHalf - vVec.x * stickerHalf, center3D.y + normal.y * stickerLift + uVec.y * stickerHalf - vVec.y * stickerHalf, center3D.z + normal.z * stickerLift + uVec.z * stickerHalf - vVec.z * stickerHalf),
                Vec3(center3D.x + normal.x * stickerLift + uVec.x * stickerHalf + vVec.x * stickerHalf, center3D.y + normal.y * stickerLift + uVec.y * stickerHalf + vVec.y * stickerHalf, center3D.z + normal.z * stickerLift + uVec.z * stickerHalf + vVec.z * stickerHalf),
                Vec3(center3D.x + normal.x * stickerLift - uVec.x * stickerHalf + vVec.x * stickerHalf, center3D.y + normal.y * stickerLift - uVec.y * stickerHalf + vVec.y * stickerHalf, center3D.z + normal.z * stickerLift - uVec.z * stickerHalf + vVec.z * stickerHalf)
            ).map { transform3D(it) }

            // Directional lighting shade
            val lightFactor = (0.78f + 0.22f * ((camNormal.y * 0.5f + camNormal.z * 0.8f).coerceIn(-1f, 1f)))
            val shadedColor = Color(
                red = (color.red * lightFactor).coerceIn(0f, 1f),
                green = (color.green * lightFactor).coerceIn(0f, 1f),
                blue = (color.blue * lightFactor).coerceIn(0f, 1f),
                alpha = 1f
            )

            polygons.add(
                StickerPolygon(
                    points2D = stickerCornersCam.map { project(it) },
                    avgZ = stickerCornersCam.map { it.z }.average().toFloat(),
                    fillColor = shadedColor,
                    isHighlightedLayer = isOnHighlightedLayer,
                    isCenter = (s.row == 1 && s.col == 1)
                )
            )
        }

        // Painter's algorithm sort: back-to-front by avgZ
        polygons.sortBy { it.avgZ }

        val path = Path()
        for (poly in polygons) {
            path.reset()
            val pts = poly.points2D
            path.moveTo(pts[0].x, pts[0].y)
            path.lineTo(pts[1].x, pts[1].y)
            path.lineTo(pts[2].x, pts[2].y)
            path.lineTo(pts[3].x, pts[3].y)
            path.close()

            drawPath(path = path, color = poly.fillColor)

            if (poly.isHighlightedLayer) {
                val glowAlpha = 0.45f + 0.45f * sin(pulsePhase * Math.PI.toFloat() * 2f)
                drawPath(
                    path = path,
                    color = ElectricCyan.copy(alpha = glowAlpha.coerceIn(0.2f, 0.9f)),
                    style = Stroke(width = 2.6f, join = StrokeJoin.Round)
                )
            } else {
                drawPath(
                    path = path,
                    color = Color.Black.copy(alpha = 0.55f),
                    style = Stroke(width = 1.4f, join = StrokeJoin.Round)
                )
            }

            // Center orientation rivet dot
            if (poly.isCenter) {
                val cx = pts.map { it.x }.average().toFloat()
                val cy = pts.map { it.y }.average().toFloat()
                drawCircle(
                    color = Color.Black.copy(alpha = 0.28f),
                    radius = minDim * 0.012f,
                    center = Offset(cx, cy)
                )
            }
        }

        // Draw 3D Curved Rotation Arrow overlay for highlightedMove
        if (highlightedMove != null) {
            val f = highlightedMove.face
            val n = Vec3(f.nx.toFloat(), f.ny.toFloat(), f.nz.toFloat())
            // Orthonormal basis (t1, t2) on the face plane such that t1 x t2 = -n (clockwise looking at face)
            val (t1, t2) = when (f) {
                CubeFace.F -> Vec3(0f, 1f, 0f) to Vec3(1f, 0f, 0f)
                CubeFace.B -> Vec3(0f, 1f, 0f) to Vec3(-1f, 0f, 0f)
                CubeFace.R -> Vec3(0f, 1f, 0f) to Vec3(0f, 0f, -1f)
                CubeFace.L -> Vec3(0f, 1f, 0f) to Vec3(0f, 0f, 1f)
                CubeFace.U -> Vec3(0f, 0f, -1f) to Vec3(1f, 0f, 0f)
                CubeFace.D -> Vec3(0f, 0f, 1f) to Vec3(1f, 0f, 0f)
            }

            val isCcw = highlightedMove.turnsCw == 3
            val isDouble = highlightedMove.turnsCw == 2
            val sweepAngle = if (isDouble) Math.PI.toFloat() * 1.35f else Math.PI.toFloat() * 0.85f
            val dirSign = if (isCcw) -1f else 1f
            val arcRadius = 0.72f
            val liftDist = 1.12f

            val arcPoints = (0..20).map { step ->
                val frac = step / 20f
                val theta = (-0.42f * sweepAngle + frac * sweepAngle) * dirSign
                val c = cos(theta) * arcRadius
                val s = sin(theta) * arcRadius
                val p3d = Vec3(
                    x = n.x * liftDist + t1.x * c + t2.x * s,
                    y = n.y * liftDist + t1.y * c + t2.y * s,
                    z = n.z * liftDist + t1.z * c + t2.z * s
                )
                project(transform3D(p3d))
            }

            val arrowColor = if (isDouble) NeonOrange else ElectricCyan
            val arcPath = Path().apply {
                moveTo(arcPoints.first().x, arcPoints.first().y)
                for (i in 1 until arcPoints.size) {
                    lineTo(arcPoints[i].x, arcPoints[i].y)
                }
            }

            // Outer glow stroke
            drawPath(
                path = arcPath,
                color = Color.Black.copy(alpha = 0.65f),
                style = Stroke(width = 11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = arcPath,
                color = arrowColor,
                style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Arrowhead at the end of the 3D arc
            val tip = arcPoints.last()
            val prev = arcPoints[arcPoints.size - 3]
            val dx = tip.x - prev.x
            val dy = tip.y - prev.y
            val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val ux = dx / len
            val uy = dy / len
            val headLen = minDim * 0.055f
            val headWidth = minDim * 0.032f

            val leftWing = Offset(
                x = tip.x - ux * headLen - uy * headWidth,
                y = tip.y - uy * headLen + ux * headWidth
            )
            val rightWing = Offset(
                x = tip.x - ux * headLen + uy * headWidth,
                y = tip.y - uy * headLen - ux * headWidth
            )

            val headPath = Path().apply {
                moveTo(tip.x + ux * 4f, tip.y + uy * 4f)
                lineTo(leftWing.x, leftWing.y)
                lineTo(rightWing.x, rightWing.y)
                close()
            }
            drawPath(path = headPath, color = arrowColor)
        }
    }
}
