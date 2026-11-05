package com.example.ai

data class ModelInfo(
    val id: String,
    val name: String,
    val provider: String,
    val supportsVision: Boolean,
    val maxTokens: Int,
    val description: String
)

object ModelCapabilityValidator {

    val AVAILABLE_MODELS = listOf(
        ModelInfo(
            id = "llama-3.2-11b-vision-preview",
            name = "LLaMA 3.2 11B Vision",
            provider = "Groq",
            supportsVision = true,
            maxTokens = 8192,
            description = "Fast multimodal model capable of high-accuracy optical OCR, scene reasoning, and object identification."
        ),
        ModelInfo(
            id = "llama-3.2-90b-vision-preview",
            name = "LLaMA 3.2 90B Vision",
            provider = "Groq",
            supportsVision = true,
            maxTokens = 8192,
            description = "Ultra-dense multimodal model for nuanced document inspection, code screenshot analysis, and deep spatial reasoning."
        ),
        ModelInfo(
            id = "llama-3.3-70b-versatile",
            name = "LLaMA 3.3 70B Versatile",
            provider = "Groq",
            supportsVision = false,
            maxTokens = 32768,
            description = "High-speed text-only flagship model. Exceptional reasoning, tool planning, and multi-step task decomposition."
        ),
        ModelInfo(
            id = "llama-3.1-8b-instant",
            name = "LLaMA 3.1 8B Instant",
            provider = "Groq",
            supportsVision = false,
            maxTokens = 8192,
            description = "Sub-second response text model. Ideal for quick calculations, summarization, and lightweight tool dispatching."
        ),
        ModelInfo(
            id = "mixtral-8x7b-32768",
            name = "Mixtral 8x7B MoE",
            provider = "Groq",
            supportsVision = false,
            maxTokens = 32768,
            description = "Mixture of Experts text model for complex reasoning and technical analysis."
        )
    )

    fun getModel(id: String): ModelInfo? {
        return AVAILABLE_MODELS.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }

    fun isVisionCapable(modelId: String): Boolean {
        val model = getModel(modelId)
        return model?.supportsVision ?: (modelId.contains("vision", ignoreCase = true) || modelId.contains("gemini", ignoreCase = true))
    }

    fun getLimitationExplanation(modelId: String): String {
        val model = getModel(modelId)
        return if (model != null && !model.supportsVision) {
            "Model '${model.name}' (${model.id}) does not support visual input (images/camera frames). It is optimized exclusively for text reasoning and planning. To process camera frames, OCR, or screenshots, please switch to a vision-capable model such as 'llama-3.2-11b-vision-preview' or 'llama-3.2-90b-vision-preview'."
        } else {
            "The currently selected provider or model ($modelId) lacks optical perception capabilities. Please select a verified vision model in Settings."
        }
    }
}
