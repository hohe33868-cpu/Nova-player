package com.example.agent.execution

import com.example.agent.model.*
import com.example.agent.monitor.ActivityMonitor
import com.example.agent.safety.PermissionManager
import com.example.agent.tools.ToolRegistry
import com.example.data.local.entities.PlanEntity
import com.example.data.repository.JarvisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ExecutionEngine(
    private val toolRegistry: ToolRegistry,
    private val permissionManager: PermissionManager,
    private val activityMonitor: ActivityMonitor,
    private val repository: JarvisRepository
) {

    suspend fun executePlan(plan: ExecutionPlan): ExecutionPlan = withContext(Dispatchers.Default) {
        activityMonitor.updateObjective(plan.objective, plan.reasoning)
        activityMonitor.setPlan(plan)

        val updatedSteps = plan.steps.toMutableList()
        val executionLog = StringBuilder()
        var hasFailure = false

        for (i in updatedSteps.indices) {
            val step = updatedSteps[i]
            activityMonitor.startStep(step)
            delay(400) // Brief tactical pause for visual HUD scanning

            val risk = permissionManager.evaluateRisk(step.toolId, step.toolArgs)

            if (risk == RiskLevel.REQUIRE_CONFIRMATION) {
                // Update step to WAITING_APPROVAL
                val waitingStep = step.copy(status = StepStatus.WAITING_APPROVAL)
                updatedSteps[i] = waitingStep
                activityMonitor.finishStep(waitingStep)

                val approved = permissionManager.requestHumanAuthorization(
                    stepId = step.id,
                    actionName = step.name,
                    toolId = step.toolId,
                    details = step.description,
                    affectedData = "Tool '${step.toolId}' with parameters: ${step.toolArgs}"
                )

                if (!approved) {
                    val skippedStep = step.copy(
                        status = StepStatus.SKIPPED,
                        resultSummary = "Human operator declined authorization for this step."
                    )
                    updatedSteps[i] = skippedStep
                    activityMonitor.finishStep(skippedStep)

                    repository.logAction(
                        toolName = step.toolId,
                        description = step.name,
                        status = "REJECTED",
                        riskLevel = "HIGH",
                        wasApproved = false,
                        details = "User rejected authorization."
                    )
                    executionLog.appendLine("• Step ${step.stepIndex} [SKIPPED]: Rejected by human authorization.")
                    continue
                }
            }

            // Execute the permitted tool
            val runningStep = step.copy(status = StepStatus.RUNNING)
            updatedSteps[i] = runningStep
            activityMonitor.finishStep(runningStep)

            val result = toolRegistry.executeTool(step.toolId, step.toolArgs)

            if (result.isSuccess) {
                val completedStep = step.copy(
                    status = StepStatus.COMPLETED,
                    resultSummary = result.output
                )
                updatedSteps[i] = completedStep
                activityMonitor.finishStep(completedStep)

                repository.logAction(
                    toolName = step.toolId,
                    description = step.name,
                    status = "SUCCESS",
                    riskLevel = if (risk == RiskLevel.REQUIRE_CONFIRMATION) "HIGH" else "LOW",
                    wasApproved = (risk == RiskLevel.REQUIRE_CONFIRMATION),
                    details = result.output
                )
                executionLog.appendLine("• Step ${step.stepIndex} [COMPLETED]: ${result.output}")
            } else {
                hasFailure = true
                val failedStep = step.copy(
                    status = StepStatus.FAILED,
                    errorMessage = result.errorMessage ?: result.output
                )
                updatedSteps[i] = failedStep
                activityMonitor.finishStep(failedStep)
                activityMonitor.setError(result.output)

                repository.logAction(
                    toolName = step.toolId,
                    description = step.name,
                    status = "FAILED",
                    riskLevel = if (risk == RiskLevel.REQUIRE_CONFIRMATION) "HIGH" else "LOW",
                    wasApproved = false,
                    details = result.output
                )
                executionLog.appendLine("• Step ${step.stepIndex} [FAILED]: ${result.output}")
                break // Halt pipeline on unhandled step failure
            }
        }

        val completedCount = updatedSteps.count { it.status == StepStatus.COMPLETED }
        val finalReport = buildString {
            appendLine("JARVIS MISSION EXECUTION REPORT:")
            appendLine("Objective: \"${plan.objective}\"")
            appendLine("Status: ${if (hasFailure) "HALTED (Error detected)" else "SUCCESS (All objectives met)"}")
            appendLine("Progress: $completedCount / ${updatedSteps.size} steps executed.")
            appendLine()
            appendLine("SUMMARY OF ACTIONS:")
            append(executionLog.toString())
        }

        activityMonitor.completePlan(finalReport)

        val completedPlan = plan.copy(
            steps = updatedSteps,
            isComplete = !hasFailure,
            hasFailed = hasFailure,
            finalReport = finalReport
        )

        // Save into Room database
        repository.savePlan(
            PlanEntity(
                goal = plan.objective,
                currentStepIndex = completedCount,
                totalSteps = updatedSteps.size,
                status = if (hasFailure) "FAILED" else "COMPLETED",
                reasoning = plan.reasoning,
                stepsJson = finalReport,
                completedAt = System.currentTimeMillis()
            )
        )

        return@withContext completedPlan
    }
}
