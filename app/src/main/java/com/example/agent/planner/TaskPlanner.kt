package com.example.agent.planner

import com.example.agent.model.*
import com.example.agent.safety.PermissionManager
import com.example.agent.tools.ToolRegistry
import com.example.ai.GroqApiClient
import java.util.UUID

class TaskPlanner(
    private val toolRegistry: ToolRegistry,
    private val permissionManager: PermissionManager,
    private val groqClient: GroqApiClient
) {

    suspend fun createPlan(
        objective: String,
        apiKey: String
    ): ExecutionPlan {
        val trimmed = objective.trim()
        val planId = UUID.randomUUID().toString()

        // Match common high-level workflows
        return when {
            trimmed.contains("organize", ignoreCase = true) && trimmed.contains("project", ignoreCase = true) -> {
                createProjectOrganizationPlan(planId, trimmed)
            }
            trimmed.contains("screen", ignoreCase = true) || trimmed.contains("error", ignoreCase = true) -> {
                createScreenDiagnosticPlan(planId, trimmed)
            }
            trimmed.contains("vision", ignoreCase = true) || trimmed.contains("see", ignoreCase = true) || trimmed.contains("camera", ignoreCase = true) -> {
                createVisionDiagnosticPlan(planId, trimmed)
            }
            trimmed.contains("battery", ignoreCase = true) || trimmed.contains("status", ignoreCase = true) || trimmed.contains("diagnostic", ignoreCase = true) -> {
                createSystemDiagnosticsPlan(planId, trimmed)
            }
            trimmed.contains("export", ignoreCase = true) || trimmed.contains("backup", ignoreCase = true) -> {
                createDataExportPlan(planId, trimmed)
            }
            trimmed.contains("calculate", ignoreCase = true) || trimmed.contains("math", ignoreCase = true) || trimmed.matches(Regex(".*[0-9]+.*[\\+\\-\\*/].*")) -> {
                createCalculationPlan(planId, trimmed)
            }
            else -> {
                createGeneralTaskPlan(planId, trimmed)
            }
        }
    }

    private fun createProjectOrganizationPlan(planId: String, objective: String): ExecutionPlan {
        val steps = listOf(
            PlanStep(
                id = "$planId-1",
                stepIndex = 1,
                name = "Analyze Active Tasks & Projects",
                description = "Query active missions, priority levels, and category distribution in database.",
                toolId = "task_manager",
                toolArgs = mapOf("action" to "list"),
                riskLevel = RiskLevel.LOW,
                requiredPermission = "Database Read"
            ),
            PlanStep(
                id = "$planId-2",
                stepIndex = 2,
                name = "Retrieve User Preferences",
                description = "Access long-term memory for preferred organization hierarchy and project focal points.",
                toolId = "memory_manager",
                toolArgs = mapOf("action" to "get", "key" to "current_focus_project"),
                riskLevel = RiskLevel.LOW,
                requiredPermission = "Memory Read"
            ),
            PlanStep(
                id = "$planId-3",
                stepIndex = 3,
                name = "Structure New Project Milestones",
                description = "Create structured tracking items for Project 'Clean Slate' with Critical priority.",
                toolId = "task_manager",
                toolArgs = mapOf(
                    "action" to "create",
                    "title" to "Execute Project Clean Slate Review",
                    "description" to "Audit dependencies and clean obsolete files.",
                    "category" to "Project",
                    "priority" to "Critical"
                ),
                riskLevel = RiskLevel.LOW,
                requiredPermission = "Database Write"
            ),
            PlanStep(
                id = "$planId-4",
                stepIndex = 4,
                name = "Archive Organization Log Memo",
                description = "Store a classified note summarizing the restructuring outcome and next milestones.",
                toolId = "notes_manager",
                toolArgs = mapOf(
                    "action" to "create",
                    "title" to "Project Organization Milestone Record",
                    "content" to "Project structure refreshed by JARVIS Autonomous Agent. Tasks synchronized.",
                    "tags" to "Projects, AgentReport"
                ),
                riskLevel = RiskLevel.LOW,
                requiredPermission = "Database Write"
            )
        )

        return ExecutionPlan(
            id = planId,
            objective = objective,
            reasoning = "Decomposed into 4 sequential operations: audit existing records, pull user focus from memory, construct project tasks, and generate an archival memo.",
            steps = steps
        )
    }

    private fun createScreenDiagnosticPlan(planId: String, objective: String): ExecutionPlan {
        val steps = listOf(
            PlanStep(
                id = "$planId-1",
                stepIndex = 1,
                name = "Check Screen Intelligence Permissions",
                description = "Verify user consent and ensure active visual sharing indicator is enabled.",
                toolId = "screen_intelligence",
                toolArgs = mapOf("command" to "analyze_screen"),
                riskLevel = RiskLevel.LOW,
                requiredPermission = "Screen Telemetry Consent"
            ),
            PlanStep(
                id = "$planId-2",
                stepIndex = 2,
                name = "OCR & Interface Element Scan",
                description = "Parse view hierarchy, locate error stack traces, and identify interactive UI components.",
                toolId = "screen_intelligence",
                toolArgs = mapOf("command" to "explain_error"),
                riskLevel = RiskLevel.LOW,
                requiredPermission = "Vision Optical Access"
            ),
            PlanStep(
                id = "$planId-3",
                stepIndex = 3,
                name = "Synthesize Fix Recommendations",
                description = "Cross-reference detected error signatures against technical developer knowledge.",
                toolId = "web_search",
                toolArgs = mapOf("query" to "NullPointer in Android CameraX Optical Pipeline buffer fix"),
                riskLevel = RiskLevel.LOW,
                requiredPermission = "Knowledge Base Search"
            )
        )

        return ExecutionPlan(
            id = planId,
            objective = objective,
            reasoning = "Screen analysis workflow requires consent validation, OCR extraction of visual crash trace, and knowledge retrieval for corrective guidance.",
            steps = steps
        )
    }

    private fun createVisionDiagnosticPlan(planId: String, objective: String): ExecutionPlan {
        val steps = listOf(
            PlanStep(
                id = "$planId-1",
                stepIndex = 1,
                name = "Activate Optical Sensors",
                description = "Initialize CameraX video pipeline and assess ambient lighting index.",
                toolId = "camera_vision",
                toolArgs = mapOf("command" to "what_do_you_see"),
                riskLevel = RiskLevel.LOW,
                requiredPermission = "android.permission.CAMERA"
            ),
            PlanStep(
                id = "$planId-2",
                stepIndex = 2,
                name = "Perform OCR & Object Detection",
                description = "Evaluate optical frame using Groq LLaMA 3.2 Vision model or local sensory fallback.",
                toolId = "camera_vision",
                toolArgs = mapOf("command" to "identify_object"),
                riskLevel = RiskLevel.LOW,
                requiredPermission = "Optical Frame Processing"
            )
        )

        return ExecutionPlan(
            id = planId,
            objective = objective,
            reasoning = "Visual processing pipeline engages hardware camera sensors, measures luminance, and computes multimodal neural inferences.",
            steps = steps
        )
    }

    private fun createSystemDiagnosticsPlan(planId: String, objective: String): ExecutionPlan {
        val steps = listOf(
            PlanStep(
                id = "$planId-1",
                stepIndex = 1,
                name = "Read Arc Reactor & Power Telemetry",
                description = "Inspect device battery percentage, charging state, and thermals.",
                toolId = "device_hardware_command",
                toolArgs = mapOf("action" to "battery"),
                riskLevel = RiskLevel.LOW,
                requiredPermission = "System Telemetry"
            ),
            PlanStep(
                id = "$planId-2",
                stepIndex = 2,
                name = "Query Temporal Reference",
                description = "Synchronize local time and calculate mission elapsed timestamps.",
                toolId = "time_date",
                toolArgs = mapOf("query" to "current_time"),
                riskLevel = RiskLevel.LOW,
                requiredPermission = "System Chronometer"
            ),
            PlanStep(
                id = "$planId-3",
                stepIndex = 3,
                name = "Inspect Audio Output Levels",
                description = "Determine current media volume stream levels.",
                toolId = "device_hardware_command",
                toolArgs = mapOf("action" to "volume"),
                riskLevel = RiskLevel.LOW,
                requiredPermission = "Audio Subsystem"
            )
        )

        return ExecutionPlan(
            id = planId,
            objective = objective,
            reasoning = "Comprehensive hardware telemetry audit inspecting power cells, temporal reference, and sound subsystems.",
            steps = steps
        )
    }

    private fun createDataExportPlan(planId: String, objective: String): ExecutionPlan {
        val steps = listOf(
            PlanStep(
                id = "$planId-1",
                stepIndex = 1,
                name = "Audit Database Records",
                description = "Gather pending tasks, notes, and memory entries for serialization.",
                toolId = "task_manager",
                toolArgs = mapOf("action" to "list"),
                riskLevel = RiskLevel.LOW,
                requiredPermission = "Database Read"
            ),
            PlanStep(
                id = "$planId-2",
                stepIndex = 2,
                name = "Export Encrypted Archive",
                description = "Format and export classified records into Markdown summary. (REQUIRES USER CONFIRMATION)",
                toolId = "file_data_export",
                toolArgs = mapOf("format" to "markdown"),
                riskLevel = RiskLevel.REQUIRE_CONFIRMATION,
                requiredPermission = "Storage Write / Human Confirmation"
            )
        )

        return ExecutionPlan(
            id = planId,
            objective = objective,
            reasoning = "Export protocol collects database records and prompts for human authorization before executing persistent export operations.",
            steps = steps
        )
    }

    private fun createCalculationPlan(planId: String, objective: String): ExecutionPlan {
        val expr = objective.replace(Regex("(?i)(calculate|what is|jarvis|math)"), "").trim()
        val steps = listOf(
            PlanStep(
                id = "$planId-1",
                stepIndex = 1,
                name = "Execute Precision Math Computation",
                description = "Run arithmetic and scientific evaluation for '$expr'.",
                toolId = "calculator",
                toolArgs = mapOf("expression" to if (expr.isBlank()) "42 * 105" else expr),
                riskLevel = RiskLevel.LOW,
                requiredPermission = "Math Engine"
            )
        )

        return ExecutionPlan(
            id = planId,
            objective = objective,
            reasoning = "Single-step precision mathematical execution.",
            steps = steps
        )
    }

    private fun createGeneralTaskPlan(planId: String, objective: String): ExecutionPlan {
        val steps = listOf(
            PlanStep(
                id = "$planId-1",
                stepIndex = 1,
                name = "Synthesize Context & Knowledge",
                description = "Analyze user intent and query relevant records.",
                toolId = "web_search",
                toolArgs = mapOf("query" to objective),
                riskLevel = RiskLevel.LOW,
                requiredPermission = "Search Engine"
            ),
            PlanStep(
                id = "$planId-2",
                stepIndex = 2,
                name = "Update Tactical Mission Task",
                description = "Record actionable objective into task list.",
                toolId = "task_manager",
                toolArgs = mapOf(
                    "action" to "create",
                    "title" to objective.take(50),
                    "description" to "Auto-generated task from user directive: $objective",
                    "category" to "Autonomous",
                    "priority" to "Normal"
                ),
                riskLevel = RiskLevel.LOW,
                requiredPermission = "Database Write"
            )
        )

        return ExecutionPlan(
            id = planId,
            objective = objective,
            reasoning = "General directive parsed: synthesized context through knowledge base and logged into database missions.",
            steps = steps
        )
    }
}
