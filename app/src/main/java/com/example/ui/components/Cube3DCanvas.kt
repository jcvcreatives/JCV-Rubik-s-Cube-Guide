package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
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
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private data class Vec3(val x: Float, val y: Float, val z: Float)

private data class StickerPolygon(
    val points2D: List<Offset>,
    val avgZ: Float,
    val fillColor: Color
)

/**
 * 60fps hardware-accelerated 3D Rubik's Cube renderer with live 3D slice rotation
 * animation for the active [highlightedMove] and smooth touch-drag orbit control.
 */
@Composable
fun Cube3DCanvas(
    cubeState: CubeState,
    modifier: Modifier = Modifier,
    highlightedMove: CubeMove? = null,
    initialYawDeg: Float = -32f,
    initialPitchDeg: Float = 22f
) {
    var yawDeg by remember { mutableFloatStateOf(initialYawDeg) }
    var pitchDeg by remember { mutableFloatStateOf(initialPitchDeg) }

    // 60fps smooth slice turn animation (0.0 -> 1.0 -> hold -> reset)
    val transition = rememberInfiniteTransition(label = "sliceTurn60fps")
    val turnProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1600
                0f at 0 using FastOutSlowInEasing
                0f at 220 using FastOutSlowInEasing
                1f at 1150 using FastOutSlowInEasing
                1f at 1600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "turnProgress"
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

        val activeFace = highlightedMove?.face
        val targetAngleRad = when (highlightedMove?.turnsCw) {
            1 -> (-Math.PI / 2.0).toFloat()
            2 -> (-Math.PI).toFloat()
            3 -> (Math.PI / 2.0).toFloat()
            else -> 0f
        }
        val currentSliceAngle = targetAngleRad * turnProgress
        val cosSlice = cos(currentSliceAngle)
        val sinSlice = sin(currentSliceAngle)

        // Rotates a 3D point around the active face normal using Rodrigues' rotation formula at 60fps
        fun rotateAroundFaceAxis(v: Vec3, face: CubeFace): Vec3 {
            val kx = face.nx.toFloat()
            val ky = face.ny.toFloat()
            val kz = face.nz.toFloat()
            val dot = v.x * kx + v.y * ky + v.z * kz
            val cx = ky * v.z - kz * v.y
            val cy = kz * v.x - kx * v.z
            val cz = kx * v.y - ky * v.x
            return Vec3(
                x = v.x * cosSlice + cx * sinSlice + kx * dot * (1f - cosSlice),
                y = v.y * cosSlice + cy * sinSlice + ky * dot * (1f - cosSlice),
                z = v.z * cosSlice + cz * sinSlice + kz * dot * (1f - cosSlice)
            )
        }

        fun transform3D(v: Vec3): Vec3 {
            val x1 = v.x * cosY + v.z * sinY
            val z1 = -v.x * sinY + v.z * cosY
            val y1 = v.y
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

        val polygons = ArrayList<StickerPolygon>(108)

        for ((idx, s) in CubeGeometry.stickers.withIndex()) {
            val color = cubeState.stickers[idx].composeColor
            val isOnActiveSlice = if (activeFace != null) {
                (s.x * activeFace.nx + s.y * activeFace.ny + s.z * activeFace.nz) == 1
            } else {
                false
            }

            val baseNormal = Vec3(s.nx.toFloat(), s.ny.toFloat(), s.nz.toFloat())
            val (baseU, baseV) = when (s.face) {
                CubeFace.F -> Vec3(1f, 0f, 0f) to Vec3(0f, 1f, 0f)
                CubeFace.B -> Vec3(-1f, 0f, 0f) to Vec3(0f, 1f, 0f)
                CubeFace.R -> Vec3(0f, 0f, -1f) to Vec3(0f, 1f, 0f)
                CubeFace.L -> Vec3(0f, 0f, 1f) to Vec3(0f, 1f, 0f)
                CubeFace.U -> Vec3(1f, 0f, 0f) to Vec3(0f, 0f, -1f)
                CubeFace.D -> Vec3(1f, 0f, 0f) to Vec3(0f, 0f, 1f)
            }

            val cubieSize = 0.66f
            val rawCenter3D = Vec3(
                x = s.x * cubieSize + baseNormal.x * (cubieSize * 0.5f),
                y = s.y * cubieSize + baseNormal.y * (cubieSize * 0.5f),
                z = s.z * cubieSize + baseNormal.z * (cubieSize * 0.5f)
            )

            val center3D = if (isOnActiveSlice && activeFace != null) {
                rotateAroundFaceAxis(rawCenter3D, activeFace)
            } else {
                rawCenter3D
            }
            val normal = if (isOnActiveSlice && activeFace != null) {
                rotateAroundFaceAxis(baseNormal, activeFace)
            } else {
                baseNormal
            }
            val uVec = if (isOnActiveSlice && activeFace != null) {
                rotateAroundFaceAxis(baseU, activeFace)
            } else {
                baseU
            }
            val vVec = if (isOnActiveSlice && activeFace != null) {
                rotateAroundFaceAxis(baseV, activeFace)
            } else {
                baseV
            }

            val camNormal = transform3D(normal)
            val camCenter = transform3D(center3D)
            val viewVecZ = 6.2f - camCenter.z
            val dotView = -camCenter.x * camNormal.x - camCenter.y * camNormal.y + viewVecZ * camNormal.z
            if (dotView <= 0f) continue

            // Matte charcoal cubie casing
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
                    fillColor = Color(0xFF1A1918)
                )
            )

            // Matte sticker tile
            val stickerHalf = cubieSize * 0.42f
            val stickerLift = 0.008f
            val stickerCornersCam = listOf(
                Vec3(center3D.x + normal.x * stickerLift - uVec.x * stickerHalf - vVec.x * stickerHalf, center3D.y + normal.y * stickerLift - uVec.y * stickerHalf - vVec.y * stickerHalf, center3D.z + normal.z * stickerLift - uVec.z * stickerHalf - vVec.z * stickerHalf),
                Vec3(center3D.x + normal.x * stickerLift + uVec.x * stickerHalf - vVec.x * stickerHalf, center3D.y + normal.y * stickerLift + uVec.y * stickerHalf - vVec.y * stickerHalf, center3D.z + normal.z * stickerLift + uVec.z * stickerHalf - vVec.z * stickerHalf),
                Vec3(center3D.x + normal.x * stickerLift + uVec.x * stickerHalf + vVec.x * stickerHalf, center3D.y + normal.y * stickerLift + uVec.y * stickerHalf + vVec.y * stickerHalf, center3D.z + normal.z * stickerLift + uVec.z * stickerHalf + vVec.z * stickerHalf),
                Vec3(center3D.x + normal.x * stickerLift - uVec.x * stickerHalf + vVec.x * stickerHalf, center3D.y + normal.y * stickerLift - uVec.y * stickerHalf + vVec.y * stickerHalf, center3D.z + normal.z * stickerLift - uVec.z * stickerHalf + vVec.z * stickerHalf)
            ).map { transform3D(it) }

            val lightFactor = (0.90f + 0.10f * ((camNormal.y * 0.5f + camNormal.z * 0.8f).coerceIn(-1f, 1f)))
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
                    fillColor = shadedColor
                )
            )
        }

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
            drawPath(
                path = path,
                color = Color(0xFF1A1918),
                style = Stroke(width = 1.1f, join = StrokeJoin.Round)
            )
        }

        // Minimalist black-and-white geometric direction arrow
        if (highlightedMove != null) {
            val f = highlightedMove.face
            val n = Vec3(f.nx.toFloat(), f.ny.toFloat(), f.nz.toFloat())
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
            val sweepAngle = if (isDouble) Math.PI.toFloat() * 1.25f else Math.PI.toFloat() * 0.78f
            val dirSign = if (isCcw) -1f else 1f
            val arcRadius = 0.68f
            val liftDist = 1.12f

            val arcPoints = (0..20).map { step ->
                val frac = step / 20f
                val theta = (-0.4f * sweepAngle + frac * sweepAngle) * dirSign
                val c = cos(theta) * arcRadius
                val s = sin(theta) * arcRadius
                val p3d = Vec3(
                    x = n.x * liftDist + t1.x * c + t2.x * s,
                    y = n.y * liftDist + t1.y * c + t2.y * s,
                    z = n.z * liftDist + t1.z * c + t2.z * s
                )
                project(transform3D(p3d))
            }

            val arcPath = Path().apply {
                moveTo(arcPoints.first().x, arcPoints.first().y)
                for (i in 1 until arcPoints.size) {
                    lineTo(arcPoints[i].x, arcPoints[i].y)
                }
            }

            drawPath(
                path = arcPath,
                color = Color.White,
                style = Stroke(width = 7.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = arcPath,
                color = Color(0xFF141413),
                style = Stroke(width = 3.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            val tip = arcPoints.last()
            val prev = arcPoints[arcPoints.size - 3]
            val dx = tip.x - prev.x
            val dy = tip.y - prev.y
            val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val ux = dx / len
            val uy = dy / len
            val headLen = minDim * 0.045f
            val headWidth = minDim * 0.026f

            val leftWing = Offset(
                x = tip.x - ux * headLen - uy * headWidth,
                y = tip.y - uy * headLen + ux * headWidth
            )
            val rightWing = Offset(
                x = tip.x - ux * headLen + uy * headWidth,
                y = tip.y - uy * headLen - ux * headWidth
            )

            val headPath = Path().apply {
                moveTo(tip.x + ux * 2.5f, tip.y + uy * 2.5f)
                lineTo(leftWing.x, leftWing.y)
                lineTo(rightWing.x, rightWing.y)
                close()
            }
            drawPath(
                path = headPath,
                color = Color.White,
                style = Stroke(width = 3f, join = StrokeJoin.Round)
            )
            drawPath(path = headPath, color = Color(0xFF141413))
        }
    }
}
