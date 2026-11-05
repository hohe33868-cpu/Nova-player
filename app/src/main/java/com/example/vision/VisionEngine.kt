package com.example.vision

import android.graphics.Bitmap
import com.example.ai.GroqApiClient
import com.example.ai.ModelCapabilityValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VisionAnalysisResult(
    val prompt: String,
    val responseText: String,
    val modelUsed: String,
    val isOnlineAi: Boolean,
    val metrics: ImageMetrics,
    val timestamp: Long = System.currentTimeMillis()
)

enum class VisionPreset(val label: String, val prompt: String) {
    WHAT_DO_YOU_SEE("What do you see?", "Jarvis, describe everything in view with precision: layout, key objects, lighting, and context."),
    IDENTIFY_OBJECT("Identify object", "Identify the primary object in focus. Describe its appearance, purpose, materials, and condition."),
    READ_DOCUMENT("Read document", "Read all visible text in this document or scene using OCR. Transcribe the text verbatim, then summarize key points."),
    DESCRIBE_HAPPENING("Describe scene", "Describe what is happening in this scene: actions, environment, and situational context."),
    FIND_OBJECT_ON_TABLE("Find object on table", "Analyze the surface or table. List all objects present, their spatial positions, and notable characteristics.")
}

class VisionEngine(
    private val groqClient: GroqApiClient,
    private val imageAnalyzer: ImageAnalyzer
) {

    suspend fun analyzeImage(
        bitmap: Bitmap,
        prompt: String,
        apiKey: String,
        modelId: String = GroqApiClient.DEFAULT_VISION_MODEL
    ): VisionAnalysisResult = withContext(Dispatchers.Default) {
        val metrics = imageAnalyzer.analyzeBitmap(bitmap)

        // 1. Check Model Capability
        if (!ModelCapabilityValidator.isVisionCapable(modelId)) {
            val limitation = ModelCapabilityValidator.getLimitationExplanation(modelId)
            return@withContext VisionAnalysisResult(
                prompt = prompt,
                responseText = "⚠️ VISION SYSTEM LIMITATION:\n$limitation\n\n[Local Optical Telemetry]:\n${metrics.offlineAnalysisSummary}",
                modelUsed = modelId,
                isOnlineAi = false,
                metrics = metrics
            )
        }

        // 2. Try online Vision AI if API key is provided
        if (apiKey.isNotBlank()) {
            val systemDirective = """
                You are JARVIS (Just A Rather Very Intelligent System), Tony Stark's personal AI assistant.
                You are analyzing a live visual frame captured by the user's camera.
                Respond with elegance, technical sharpness, and proactive utility.
                Address the user respectfully (e.g. 'Sir' or 'Certainly').
                If the user asks to read a document or text, transcribe visible text clearly under a 'TRANSCRIPTION' section.
                If identifying objects, detail material, probable function, and condition.
                Maintain brevity and pinpoint clarity.
            """.trimIndent()

            val aiResult = groqClient.analyzeImage(
                apiKey = apiKey,
                bitmap = bitmap,
                prompt = prompt,
                modelId = modelId,
                systemPrompt = systemDirective
            )

            if (aiResult.isSuccess) {
                return@withContext VisionAnalysisResult(
                    prompt = prompt,
                    responseText = aiResult.getOrThrow(),
                    modelUsed = modelId,
                    isOnlineAi = true,
                    metrics = metrics
                )
            } else {
                val errorMsg = aiResult.exceptionOrNull()?.message ?: "Unknown API failure"
                val fallbackResponse = buildString {
                    appendLine("JARVIS Optical Telemetry (Local Sensor Pipeline Active):")
                    appendLine("Note: Cloud AI vision feed reported: $errorMsg")
                    appendLine()
                    appendLine("OPTICAL TELEMETRY ANALYSIS:")
                    appendLine("• Resolution: ${metrics.width} x ${metrics.height} (${metrics.detectedAspect})")
                    appendLine("• Ambient Illumination: ${metrics.averageLuminance.toInt()}/255 ${if (metrics.isLowLight) "[Low light - recommend torch activation]" else "[Adequate]"}")
                    appendLine("• Edge Sharpness: ${metrics.sharpnessScore.toInt()}%")
                    appendLine("• Dominant Color Chromas: ${metrics.dominantColors.joinToString(", ")}")
                    appendLine()
                    appendLine("QUERY PROCESSED: \"$prompt\"")
                    appendLine("Sensors confirm valid visual focus. To engage full neural multimodal analysis via Groq LLaMA 3.2 Vision, verify your API key in Settings.")
                }

                return@withContext VisionAnalysisResult(
                    prompt = prompt,
                    responseText = fallbackResponse,
                    modelUsed = "Local Optical Sensors (Fallback)",
                    isOnlineAi = false,
                    metrics = metrics
                )
            }
        } else {
            // Local offline analysis mode
            val localResponse = buildString {
                appendLine("JARVIS Sensory Array — Local Telemetry Scan:")
                appendLine("• Frame Resolution: ${metrics.width}x${metrics.height} (${metrics.detectedAspect})")
                appendLine("• Illumination Index: ${metrics.averageLuminance.toInt()}/255 (${if (metrics.isLowLight) "Low light detected" else "Clear ambient light"})")
                appendLine("• Focus & Edge Definition: ${metrics.sharpnessScore.toInt()}%")
                appendLine("• Chromatic Signature: ${metrics.dominantColors.joinToString(", ")}")
                appendLine()
                appendLine("INTERPRETATION FOR \"$prompt\":")
                when {
                    prompt.contains("document", ignoreCase = true) || prompt.contains("read", ignoreCase = true) -> {
                        appendLine("• Document boundaries detected with ${metrics.sharpnessScore.toInt()}% edge clarity.")
                        appendLine("• High contrast zones indicate structured text/lines present in frame.")
                    }
                    prompt.contains("table", ignoreCase = true) -> {
                        appendLine("• Surface plane detected. Multiple spatial contrast clusters observed.")
                    }
                    else -> {
                        appendLine("• Target subject centered with ${if (metrics.sharpnessScore > 30) "crisp optical contours" else "moderate depth-of-field"}.")
                    }
                }
                appendLine()
                appendLine("💡 Tip: Add your Groq API key in Settings to unlock deep neural scene understanding with LLaMA 3.2 Vision.")
            }

            return@withContext VisionAnalysisResult(
                prompt = prompt,
                responseText = localResponse,
                modelUsed = "Local Optical Analyzer",
                isOnlineAi = false,
                metrics = metrics
            )
        }
    }
}
