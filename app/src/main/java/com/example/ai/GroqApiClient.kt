package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GroqApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        const val ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
        const val DEFAULT_VISION_MODEL = "llama-3.2-11b-vision-preview"
        const val DEFAULT_TEXT_MODEL = "llama-3.3-70b-versatile"
    }

    suspend fun analyzeImage(
        apiKey: String,
        bitmap: Bitmap,
        prompt: String,
        modelId: String = DEFAULT_VISION_MODEL,
        systemPrompt: String = "You are JARVIS, Tony Stark's advanced visual AI assistant. Provide concise, highly perceptive, and sophisticated analysis. When reading documents or text, extract the exact OCR text followed by a concise summary. When recognizing objects or scenes, pinpoint layout, objects, condition, and actionable insights. Be direct, respectful, and articulate."
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!ModelCapabilityValidator.isVisionCapable(modelId)) {
            return@withContext Result.failure(
                IllegalArgumentException(ModelCapabilityValidator.getLimitationExplanation(modelId))
            )
        }

        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Groq API Key is not configured. Please enter your API key in Settings, or use offline local analysis mode.")
            )
        }

        try {
            // Compress bitmap to JPEG base64
            val outputStream = ByteArrayOutputStream()
            // Scale if very large to prevent memory overhead
            val scaledBitmap = if (bitmap.width > 1280 || bitmap.height > 1280) {
                val ratio = 1280f / maxOf(bitmap.width, bitmap.height)
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
            } else {
                bitmap
            }
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            val dataUrl = "data:image/jpeg;base64,$base64Image"

            val jsonBody = JSONObject().apply {
                put("model", modelId)
                put("temperature", 0.3)
                put("max_tokens", 1024)

                val messages = JSONArray()

                // System message
                messages.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })

                // User multimodal message
                val userContent = JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", prompt)
                    })
                    put(JSONObject().apply {
                        put("type", "image_url")
                        put("image_url", JSONObject().apply {
                            put("url", dataUrl)
                        })
                    })
                }

                messages.put(JSONObject().apply {
                    put("role", "user")
                    put("content", userContent)
                })

                put("messages", messages)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errJson = JSONObject(responseBody)
                        errJson.optJSONObject("error")?.optString("message") ?: responseBody
                    } catch (e: Exception) {
                        responseBody
                    }
                    return@withContext Result.failure(Exception("Groq API error (${response.code}): $errorMsg"))
                }

                val responseJson = JSONObject(responseBody)
                val choices = responseJson.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content").orEmpty()
                    Result.success(content)
                } else {
                    Result.failure(Exception("Received empty choices payload from Groq."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun chatCompletion(
        apiKey: String,
        messages: List<Pair<String, String>>, // role to content
        modelId: String = DEFAULT_TEXT_MODEL,
        temperature: Double = 0.5,
        maxTokens: Int = 1024
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Groq API Key is missing. Configure in JARVIS Settings.")
            )
        }

        try {
            val jsonBody = JSONObject().apply {
                put("model", modelId)
                put("temperature", temperature)
                put("max_tokens", maxTokens)

                val msgsArray = JSONArray()
                messages.forEach { (role, content) ->
                    msgsArray.put(JSONObject().apply {
                        put("role", role)
                        put("content", content)
                    })
                }
                put("messages", msgsArray)
            }

            val request = Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val err = try {
                        JSONObject(responseBody).optJSONObject("error")?.optString("message") ?: responseBody
                    } catch (e: Exception) {
                        responseBody
                    }
                    return@withContext Result.failure(Exception("Groq API error (${response.code}): $err"))
                }

                val json = JSONObject(responseBody)
                val choices = json.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content").orEmpty()
                    Result.success(content)
                } else {
                    Result.failure(Exception("No response choices returned."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
