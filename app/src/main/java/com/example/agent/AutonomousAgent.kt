package com.example.agent

import com.example.agent.execution.ExecutionEngine
import com.example.agent.model.ExecutionPlan
import com.example.agent.monitor.ActivityMonitor
import com.example.agent.planner.TaskPlanner
import com.example.agent.safety.PermissionManager
import com.example.agent.tools.ToolRegistry
import com.example.ai.GroqApiClient
import com.example.data.repository.JarvisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class ChatMessage(
    val id: String,
    val sender: String, // "USER" or "JARVIS"
    val text: String,
    val isAgentPlanResult: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class AutonomousAgent(
    private val repository: JarvisRepository,
    private val toolRegistry: ToolRegistry,
    private val permissionManager: PermissionManager,
    private val taskPlanner: TaskPlanner,
    private val executionEngine: ExecutionEngine,
    val activityMonitor: ActivityMonitor,
    private val groqClient: GroqApiClient
) {

    private val _conversationHistory = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "init-1",
                sender = "JARVIS",
                text = "Good day, sir. Systems are fully operational. CameraX vision pipelines, screen intelligence sensors, and the autonomous multi-step planner stand ready. How may I be of service?"
            )
        )
    )
    val conversationHistory: StateFlow<List<ChatMessage>> = _conversationHistory.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    suspend fun processUserDirective(
        directive: String,
        apiKey: String
    ): String = withContext(Dispatchers.Default) {
        val trimmed = directive.trim()
        if (trimmed.isBlank()) return@withContext "Directive was empty, sir."

        _isProcessing.value = true

        // Append user message
        val userMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            sender = "USER",
            text = trimmed
        )
        _conversationHistory.value = _conversationHistory.value + userMsg

        // Check if user is asking for multi-step task execution
        val isMultiStep = isMultiStepObjective(trimmed)

        val responseText: String = if (isMultiStep) {
            // Plan and execute via Autonomous Agent
            val plan = taskPlanner.createPlan(trimmed, apiKey)
            val resultPlan = executionEngine.executePlan(plan)
            resultPlan.finalReport ?: "Task plan executed, sir."
        } else {
            // Check direct tool shortcuts (e.g. math or time)
            if (trimmed.startsWith("calculate", ignoreCase = true) || trimmed.matches(Regex(".*[0-9]+.*[\\+\\-\\*/].*"))) {
                val mathRes = toolRegistry.executeTool("calculator", mapOf("expression" to trimmed.replace(Regex("(?i)calculate"), "").trim()))
                if (mathRes.isSuccess) mathRes.output else "I was unable to calculate that, sir: ${mathRes.output}"
            } else if (trimmed.contains("time", ignoreCase = true) || trimmed.contains("date", ignoreCase = true)) {
                val timeRes = toolRegistry.executeTool("time_date", mapOf("query" to "current_time"))
                timeRes.output
            } else if (trimmed.contains("battery", ignoreCase = true)) {
                val devRes = toolRegistry.executeTool("device_hardware_command", mapOf("action" to "battery"))
                devRes.output
            } else if (trimmed.contains("torch", ignoreCase = true) || trimmed.contains("flashlight", ignoreCase = true)) {
                val torchRes = toolRegistry.executeTool("device_hardware_command", mapOf("action" to "torch_toggle"))
                torchRes.output
            } else if (apiKey.isNotBlank()) {
                // Generative conversational query with JARVIS Persona
                val systemPrompt = """
                    You are JARVIS (Just A Rather Very Intelligent System), the sophisticated AI created by Tony Stark.
                    You speak with understated British elegance, dry wit, impeccable tact, and razor-sharp intellect.
                    Address the user as 'Sir' or 'Ma'am' with natural deference.
                    Keep responses succinct, practical, and candid.
                    Never fabricate results: if you cannot perform a physical device operation or don't know something, candidly explain what is needed.
                """.trimIndent()

                val messages = listOf(
                    "system" to systemPrompt,
                    "user" to trimmed
                )

                val aiResult = groqClient.chatCompletion(apiKey, messages)
                if (aiResult.isSuccess) {
                    aiResult.getOrThrow()
                } else {
                    "I encountered an anomaly in the cloud neural link, sir (${aiResult.exceptionOrNull()?.message}). However, all local sensory and tool execution arrays remain at your disposal."
                }
            } else {
                "Understood, sir. I have processed your input: \"$trimmed\". To engage advanced generative dialogue and deep vision reasoning, please configure your Groq API key in Settings. Meanwhile, all local tools (Calculator, Camera Vision, Screen Intelligence, Tasks, and Hardware Telemetry) are active."
            }
        }

        val jarvisMsg = ChatMessage(
            id = (System.currentTimeMillis() + 1).toString(),
            sender = "JARVIS",
            text = responseText,
            isAgentPlanResult = isMultiStep
        )
        _conversationHistory.value = _conversationHistory.value + jarvisMsg
        _isProcessing.value = false

        responseText
    }

    private fun isMultiStepObjective(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("organize") ||
                lower.contains("plan") ||
                lower.contains("backup") ||
                lower.contains("export") ||
                lower.contains("diagnostic") ||
                lower.contains("audit") ||
                lower.contains("analyze my screen") ||
                lower.contains("multistep") ||
                (lower.contains("help me") && lower.length > 15)
    }

    fun getProactiveSuggestions(): List<String> {
        return listOf(
            "Jarvis, help me organize my projects.",
            "Jarvis, what do you see?",
            "Jarvis, analyze my screen.",
            "Run Arc Reactor & power diagnostics.",
            "Explain this error.",
            "Calculate 15% of 850 + sqrt(144)"
        )
    }

    fun clearHistory() {
        _conversationHistory.value = listOf(
            ChatMessage(
                id = System.currentTimeMillis().toString(),
                sender = "JARVIS",
                text = "Mission records cleared. Standing by for fresh instructions, sir."
            )
        )
    }
}
