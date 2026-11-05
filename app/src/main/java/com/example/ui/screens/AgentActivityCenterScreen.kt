package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agent.AutonomousAgent
import com.example.agent.model.PlanStep
import com.example.agent.model.RiskLevel
import com.example.agent.model.StepStatus
import com.example.agent.safety.PermissionManager
import com.example.ui.components.HumanApprovalModal
import com.example.ui.components.StatusTelemetryBadge
import com.example.ui.components.TacticalProgressBar
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AgentActivityCenterScreen(
    agent: AutonomousAgent,
    permissionManager: PermissionManager,
    apiKey: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activityState by agent.activityMonitor.state.collectAsState()
    val pendingApproval by permissionManager.pendingApproval.collectAsState()

    // Trigger confirmation modal if pending
    pendingApproval?.let { req ->
        HumanApprovalModal(
            request = req,
            onApprove = { permissionManager.approveCurrentAction() },
            onReject = { permissionManager.rejectCurrentAction() },
            onCancelExecution = { permissionManager.cancelExecution() }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisDarkBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Telemetry Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusTelemetryBadge(
                label = "Agent Core",
                statusText = if (activityState.isRunning) "EXECUTING PLAN" else "IDLE / MONITORING",
                color = if (activityState.isRunning) JarvisGold else JarvisEmerald
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (activityState.activeToolName != null) {
                    StatusTelemetryBadge(
                        label = "Active Tool",
                        statusText = activityState.activeToolName!!.uppercase(),
                        color = JarvisCyan
                    )
                }
            }
        }

        // Objective & Reasoning Holographic Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, JarvisBorder, RoundedCornerShape(14.dp))
                .testTag("agent_objective_card"),
            color = JarvisDarkSurface
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Objective",
                        tint = JarvisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "MISSION OBJECTIVE:",
                        color = JarvisCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = activityState.currentObjective,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Divider(color = JarvisBorder, thickness = 0.5.dp)

                Text(
                    text = "AI REASONING & STRATEGY:",
                    color = JarvisGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = activityState.aiReasoningSummary,
                    color = JarvisOnSurface,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                // Current Operation Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisDarkSurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (activityState.isRunning) JarvisCyan else JarvisEmerald)
                    )
                    Text(
                        text = activityState.currentOperation,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Progress Bar
        val progress = if (activityState.totalStepsCount > 0) {
            activityState.completedStepsCount.toFloat() / activityState.totalStepsCount.toFloat()
        } else 0f

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "EXECUTION PROGRESS",
                    color = JarvisCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${activityState.completedStepsCount} / ${activityState.totalStepsCount} STEPS (${(progress * 100).toInt()}%)",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            TacticalProgressBar(progress = progress)
        }

        // Quick Launch Sample Missions Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        agent.processUserDirective("Jarvis, help me organize my projects.", apiKey)
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant, contentColor = JarvisCyan),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(JarvisBorder)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("Organize Projects", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        agent.processUserDirective("Run Arc Reactor & power diagnostics.", apiKey)
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant, contentColor = JarvisGold),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(JarvisBorder)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("System Diagnostics", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        agent.processUserDirective("Export classified system archive.", apiKey)
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant, contentColor = JarvisCrimson),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(JarvisBorder)),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("Export Archive", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Execution Plan Steps List
        Text(
            text = "DECOMPOSED PLAN STEPS & TOOLS:",
            color = JarvisCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val steps = activityState.activePlan?.steps ?: emptyList()

            if (steps.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, JarvisBorder, RoundedCornerShape(10.dp)),
                        color = JarvisDarkSurface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PendingActions, contentDescription = null, tint = JarvisOnSurfaceVariant, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "NO ACTIVE EXECUTION PLAN",
                                color = JarvisOnSurfaceVariant,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Ask JARVIS a multi-step objective or tap a mission preset above.",
                                color = JarvisOnSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                items(steps, key = { it.id }) { step ->
                    PlanStepRow(step = step)
                }
            }

            // Final Report Display
            activityState.finalReport?.let { report ->
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, JarvisEmerald, RoundedCornerShape(12.dp))
                            .testTag("agent_final_report_card"),
                        color = JarvisDarkSurface
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Complete", tint = JarvisEmerald, modifier = Modifier.size(18.dp))
                                    Text("FINAL EXECUTION REPORT", color = JarvisEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                IconButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("JARVIS Mission Report", report))
                                        Toast.makeText(context, "Mission report copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Report", tint = JarvisOnSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }

                            Divider(color = JarvisBorder, thickness = 0.5.dp)

                            Text(
                                text = report,
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlanStepRow(step: PlanStep) {
    val statusColor = when (step.status) {
        StepStatus.COMPLETED -> JarvisEmerald
        StepStatus.RUNNING -> JarvisCyan
        StepStatus.WAITING_APPROVAL -> JarvisCrimson
        StepStatus.FAILED -> JarvisCrimson
        StepStatus.SKIPPED -> JarvisAmber
        StepStatus.PENDING -> JarvisOnSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (step.status == StepStatus.RUNNING) JarvisCyan else JarvisBorder, RoundedCornerShape(10.dp)),
        color = JarvisDarkSurface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.2f))
                            .border(1.dp, statusColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${step.stepIndex}",
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = step.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = step.status.name,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Text(
                text = step.description,
                color = JarvisOnSurface,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOOL: [${step.toolId}]",
                    color = JarvisGold,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (step.riskLevel == RiskLevel.REQUIRE_CONFIRMATION) {
                    Text(
                        text = "REQUIRES CONFIRMATION",
                        color = JarvisAmber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            step.resultSummary?.let { summary ->
                Divider(color = JarvisBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                Text(
                    text = "Output: $summary",
                    color = JarvisEmerald,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
