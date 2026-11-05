package com.example.agent.safety

import com.example.agent.model.HumanApprovalRequest
import com.example.agent.model.RiskLevel
import com.example.data.repository.JarvisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PermissionManager(private val repository: JarvisRepository) {

    // Settings
    var requireApprovalForExternalNavigation: Boolean = true
    var requireApprovalForDeletions: Boolean = true
    var requireApprovalForDataExport: Boolean = true

    private val _pendingApproval = MutableStateFlow<HumanApprovalRequest?>(null)
    val pendingApproval: StateFlow<HumanApprovalRequest?> = _pendingApproval.asStateFlow()

    private var activeContinuation: ((Boolean) -> Unit)? = null

    fun evaluateRisk(toolId: String, args: Map<String, String>): RiskLevel {
        val action = args["action"].orEmpty().lowercase()
        return when {
            action == "delete" -> RiskLevel.REQUIRE_CONFIRMATION
            toolId == "browser_navigation" && requireApprovalForExternalNavigation -> RiskLevel.REQUIRE_CONFIRMATION
            toolId == "file_data_export" && requireApprovalForDataExport -> RiskLevel.REQUIRE_CONFIRMATION
            args.containsKey("delete_id") || args.containsKey("destroy") -> RiskLevel.REQUIRE_CONFIRMATION
            else -> RiskLevel.LOW
        }
    }

    suspend fun requestHumanAuthorization(
        stepId: String,
        actionName: String,
        toolId: String,
        details: String,
        affectedData: String
    ): Boolean = suspendCoroutine { cont ->
        val req = HumanApprovalRequest(
            requestId = UUID.randomUUID().toString(),
            stepId = stepId,
            actionName = actionName,
            toolId = toolId,
            riskLevel = RiskLevel.REQUIRE_CONFIRMATION,
            details = details,
            affectedData = affectedData
        )

        activeContinuation = { approved ->
            _pendingApproval.value = null
            activeContinuation = null
            cont.resume(approved)
        }

        _pendingApproval.value = req
    }

    fun approveCurrentAction() {
        activeContinuation?.invoke(true)
    }

    fun rejectCurrentAction() {
        activeContinuation?.invoke(false)
    }

    fun cancelExecution() {
        activeContinuation?.invoke(false)
    }
}
