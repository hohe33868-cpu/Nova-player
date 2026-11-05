package com.example.agent.monitor

import com.example.agent.model.ExecutionPlan
import com.example.agent.model.HumanApprovalRequest
import com.example.agent.model.PlanStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AgentActivityState(
    val currentObjective: String = "JARVIS Core Online — Standing by for directives",
    val aiReasoningSummary: String = "Neural sensory arrays, optical CameraX pipelines, and tool execution engines are fully calibrated.",
    val activeToolName: String? = null,
    val currentOperation: String = "Idle / Monitoring",
    val activePlan: ExecutionPlan? = null,
    val completedStepsCount: Int = 0,
    val totalStepsCount: Int = 0,
    val pendingApproval: HumanApprovalRequest? = null,
    val recentError: String? = null,
    val finalReport: String? = null,
    val isRunning: Boolean = false
)

class ActivityMonitor {

    private val _state = MutableStateFlow(AgentActivityState())
    val state: StateFlow<AgentActivityState> = _state.asStateFlow()

    fun updateObjective(objective: String, reasoning: String) {
        _state.value = _state.value.copy(
            currentObjective = objective,
            aiReasoningSummary = reasoning,
            recentError = null,
            finalReport = null
        )
    }

    fun setPlan(plan: ExecutionPlan) {
        _state.value = _state.value.copy(
            activePlan = plan,
            totalStepsCount = plan.steps.size,
            completedStepsCount = plan.steps.count { it.status == com.example.agent.model.StepStatus.COMPLETED }
        )
    }

    fun startStep(step: PlanStep) {
        _state.value = _state.value.copy(
            activeToolName = step.toolId,
            currentOperation = "Executing Step ${step.stepIndex}/${_state.value.totalStepsCount}: ${step.name}",
            isRunning = true
        )
    }

    fun finishStep(step: PlanStep) {
        val currentPlan = _state.value.activePlan
        val updatedPlan = currentPlan?.copy(
            steps = currentPlan.steps.map { if (it.id == step.id) step else it }
        )
        val completed = updatedPlan?.steps?.count { it.status == com.example.agent.model.StepStatus.COMPLETED } ?: 0
        _state.value = _state.value.copy(
            activePlan = updatedPlan,
            completedStepsCount = completed,
            activeToolName = null,
            currentOperation = "Step ${step.stepIndex} Finished (${step.status.name})"
        )
    }

    fun setPendingApproval(request: HumanApprovalRequest?) {
        _state.value = _state.value.copy(
            pendingApproval = request,
            currentOperation = if (request != null) "WAITING FOR HUMAN AUTHORIZATION: ${request.actionName}" else _state.value.currentOperation
        )
    }

    fun setError(error: String) {
        _state.value = _state.value.copy(
            recentError = error,
            isRunning = false,
            currentOperation = "Execution Halted: $error"
        )
    }

    fun completePlan(report: String) {
        val currentPlan = _state.value.activePlan
        _state.value = _state.value.copy(
            activePlan = currentPlan?.copy(isComplete = true, finalReport = report),
            finalReport = report,
            isRunning = false,
            activeToolName = null,
            currentOperation = "Objective Completed Successfully"
        )
    }

    fun resetToIdle() {
        _state.value = AgentActivityState()
    }
}
