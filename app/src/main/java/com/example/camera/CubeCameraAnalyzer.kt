package com.example.camera

import android.graphics.Color as AndroidColor
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.model.CubeColor
import kotlin.math.max
import kotlin.math.min

data class LiveGridSample(
    val colors: List<CubeColor>,
    val centerHue: Float,
    val centerSat: Float,
    val centerVal: Float,
    val stabilityProgress: Float
)

/**
 * Real-time CameraX ImageAnalysis.Analyzer that extracts YUV_420_888 pixel patches
 * at the 9 grid coordinates of the centered 3x3 Rubik's Cube viewfinder, accounts for
 * sensor rotation degrees, converts YUV -> RGB -> HSV, and classifies all 9 stickers.
 */
class CubeCameraAnalyzer(
    private val expectedCenterColor: () -> CubeColor,
    private val onGridDetected: (LiveGridSample) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastAnalyzedTimestamp = 0L
    private var previousDetected: List<CubeColor>? = null
    private var consecutiveStableFrames = 0

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        // Analyze at ~12 FPS for responsive, battery-friendly telemetry
        if (now - lastAnalyzedTimestamp < 80L) {
            image.close()
            return
        }
        lastAnalyzedTimestamp = now

        try {
            if (image.planes.size < 3) {
                image.close()
                return
            }

            val width = image.width
            val height = image.height
            val rotation = image.imageInfo.rotationDegrees

            // The viewfinder is a centered square occupying 56% of the smaller image dimension
            val detected = ArrayList<CubeColor>(9)
            var centerH = 0f
            var centerS = 0f
            var centerV = 0f

            for (row in 0..2) {
                for (col in 0..2) {
                    // Normalized screen-space coordinates in [-0.28f, +0.28f] around center (0.5, 0.5)
                    val normU = (col - 1) * 0.18f
                    val normV = (row - 1) * 0.18f

                    // Map portrait viewfinder offset (normU, normV) to sensor buffer coordinates
                    val (sensorNormX, sensorNormY) = when (rotation) {
                        90 -> Pair(0.5f + normV, 0.5f - normU)
                        180 -> Pair(0.5f - normU, 0.5f - normV)
                        270 -> Pair(0.5f - normV, 0.5f + normU)
                        else -> Pair(0.5f + normU, 0.5f + normV)
                    }

                    val cx = (sensorNormX * width).toInt().coerceIn(4, width - 5)
                    val cy = (sensorNormY * height).toInt().coerceIn(4, height - 5)

                    val hsv = samplePatchHsv(image, cx, cy)
                    if (row == 1 && col == 1) {
                        centerH = hsv[0]
                        centerS = hsv[1]
                        centerV = hsv[2]
                        // Lock center sticker to the target face center color for reliable orientation
                        detected.add(expectedCenterColor())
                    } else {
                        detected.add(CubeColor.fromHsv(hsv[0], hsv[1], hsv[2]))
                    }
                }
            }

            if (previousDetected == detected) {
                consecutiveStableFrames = (consecutiveStableFrames + 1).coerceAtMost(12)
            } else {
                previousDetected = detected
                consecutiveStableFrames = 1
            }

            val stability = (consecutiveStableFrames / 10f).coerceIn(0f, 1f)
            onGridDetected(
                LiveGridSample(
                    colors = detected,
                    centerHue = centerH,
                    centerSat = centerS,
                    centerVal = centerV,
                    stabilityProgress = stability
                )
            )
        } catch (_: Exception) {
            // Safely ignore transient buffer closure exceptions
        } finally {
            image.close()
        }
    }

    private fun samplePatchHsv(image: ImageProxy, centerX: Int, centerY: Int): FloatArray {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        var rSum = 0
        var gSum = 0
        var bSum = 0
        var count = 0

        for (dy in -2..2) {
            for (dx in -2..2) {
                val x = centerX + dx * 2
                val y = centerY + dy * 2
                val yIndex = y * yRowStride + x * yPixelStride
                val uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride

                if (yIndex in 0 until yBuffer.limit() &&
                    uvIndex in 0 until uBuffer.limit() &&
                    uvIndex in 0 until vBuffer.limit()
                ) {
                    val yVal = (yBuffer.get(yIndex).toInt() and 0xFF)
                    val uVal = (uBuffer.get(uvIndex).toInt() and 0xFF) - 128
                    val vVal = (vBuffer.get(uvIndex).toInt() and 0xFF) - 128

                    val r = (yVal + 1.370705f * vVal).toInt().coerceIn(0, 255)
                    val g = (yVal - 0.337633f * uVal - 0.698001f * vVal).toInt().coerceIn(0, 255)
                    val b = (yVal + 1.732446f * uVal).toInt().coerceIn(0, 255)

                    rSum += r
                    gSum += g
                    bSum += b
                    count++
                }
            }
        }

        val hsv = FloatArray(3)
        if (count > 0) {
            AndroidColor.RGBToHSV(rSum / count, gSum / count, bSum / count, hsv)
        }
        return hsv
    }
}
