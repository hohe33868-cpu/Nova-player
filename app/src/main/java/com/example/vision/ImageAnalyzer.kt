package com.example.vision

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

data class ImageMetrics(
    val width: Int,
    val height: Int,
    val averageLuminance: Float,
    val isLowLight: Boolean,
    val sharpnessScore: Float,
    val dominantColors: List<String>,
    val detectedAspect: String,
    val offlineAnalysisSummary: String
)

class ImageAnalyzer {

    suspend fun analyzeBitmap(bitmap: Bitmap): ImageMetrics = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height

        // Downsample for rapid histogram/pixel inspection
        val sampleW = 64
        val sampleH = 64
        val scaled = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, false)

        var totalLum = 0.0
        var totalVariance = 0.0
        val colorCounts = mutableMapOf<String, Int>()

        for (y in 0 until sampleH) {
            for (x in 0 until sampleW) {
                val pixel = scaled.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                // Perceived luminance formula
                val lum = (0.299 * r + 0.587 * g + 0.114 * b)
                totalLum += lum

                // Group color bucket into 16 levels
                val bucketR = (r / 64) * 64
                val bucketG = (g / 64) * 64
                val bucketB = (b / 64) * 64
                val hex = String.format("#%02X%02X%02X", bucketR, bucketG, bucketB)
                colorCounts[hex] = (colorCounts[hex] ?: 0) + 1
            }
        }

        val pixelCount = sampleW * sampleH
        val avgLum = (totalLum / pixelCount).toFloat()

        // Approximate variance / edge sharpness
        for (y in 1 until sampleH - 1) {
            for (x in 1 until sampleW - 1) {
                val p = scaled.getPixel(x, y)
                val right = scaled.getPixel(x + 1, y)
                val down = scaled.getPixel(x, y + 1)
                val diffR = Math.abs(Color.red(p) - Color.red(right)) + Math.abs(Color.red(p) - Color.red(down))
                totalVariance += diffR
            }
        }
        val sharpness = ((totalVariance / pixelCount) / 255.0 * 100.0).toFloat().coerceIn(0f, 100f)

        val topColors = colorCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        val aspectDesc = when {
            width > height * 1.3 -> "Landscape (${width}x${height})"
            height > width * 1.3 -> "Portrait (${width}x${height})"
            else -> "Square / Balanced (${width}x${height})"
        }

        val lightingDesc = if (avgLum < 50) "Low-light environment (Night/Shadow)" else if (avgLum > 180) "High-brightness direct lighting" else "Optimal ambient illumination"
        val sharpnessDesc = if (sharpness > 20) "High edge definition / Clear focus" else "Soft focus / Motion blur detected"

        val summary = "Resolution: ${width}x$height | Illumination: ${avgLum.toInt()}/255 ($lightingDesc) | Focus: ${sharpness.toInt()}% ($sharpnessDesc) | Dominant Tones: ${topColors.joinToString(", ")}"

        ImageMetrics(
            width = width,
            height = height,
            averageLuminance = avgLum,
            isLowLight = avgLum < 50,
            sharpnessScore = sharpness,
            dominantColors = topColors,
            detectedAspect = aspectDesc,
            offlineAnalysisSummary = summary
        )
    }
}
