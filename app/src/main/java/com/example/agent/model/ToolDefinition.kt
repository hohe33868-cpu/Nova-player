package com.example.agent.model

enum class RiskLevel {
    LOW,
    REQUIRE_CONFIRMATION
}

data class ToolDefinition(
    val id: String,
    val name: String,
    val description: String,
    val category: String, // Math, System, Data, Vision, Navigation, Search
    val inputSchemaDesc: String,
    val riskLevel: RiskLevel,
    val requiredPermissionDesc: String = "Standard",
    val isPermittedByDefault: Boolean = (riskLevel == RiskLevel.LOW)
)

data class ToolExecutionResult(
    val toolId: String,
    val isSuccess: Boolean,
    val output: String,
    val metadata: Map<String, String> = emptyMap(),
    val errorMessage: String? = null
)
