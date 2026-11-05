package com.example.agent.model

enum class StepStatus {
    PENDING,
    RUNNING,
    WAITING_APPROVAL,
    COMPLETED,
    FAILED,
    SKIPPED
}

data class PlanStep(
    val id: String,
    val stepIndex: Int,
    val name: String,
    val description: String,
    val toolId: String,
    val toolArgs: Map<String, String>,
    val riskLevel: RiskLevel,
    val requiredPermission: String,
    val status: StepStatus = StepStatus.PENDING,
    val resultSummary: String? = null,
    val errorMessage: String? = null
)

data class ExecutionPlan(
    val id: String,
    val objective: String,
    val reasoning: String,
    val steps: List<PlanStep>,
    val currentStepIndex: Int = 0,
    val isComplete: Boolean = false,
    val hasFailed: Boolean = false,
    val finalReport: String? = null
) {
    val progress: Float
        get() = if (steps.isEmpty()) 0f else {
            val completed = steps.count { it.status == StepStatus.COMPLETED }
            completed.toFloat() / steps.size.toFloat()
        }
}

data class HumanApprovalRequest(
    val requestId: String,
    val stepId: String,
    val actionName: String,
    val toolId: String,
    val riskLevel: RiskLevel,
    val details: String,
    val affectedData: String,
    val requestedAt: Long = System.currentTimeMillis()
)
