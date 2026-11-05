package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GroqApiClient
import com.example.ui.components.HumanApprovalModal
import com.example.ui.screens.*
import com.example.ui.theme.*

enum class JarvisTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    CORE("JARVIS", Icons.Filled.SmartToy, Icons.Outlined.SmartToy),
    VISION("Vision", Icons.Filled.CameraAlt, Icons.Outlined.CameraAlt),
    SCREEN("Screen Intel", Icons.Filled.Screenshot, Icons.Outlined.Screenshot),
    AGENT("Agent HUD", Icons.Filled.Psychology, Icons.Outlined.Psychology),
    MEMORY("Tools & Memory", Icons.Filled.Memory, Icons.Outlined.Memory)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val app = applicationContext as NovaApplication

            // Observe global state
            var currentTab by remember { mutableStateOf(JarvisTab.CORE) }
            var isSettingsOpen by remember { mutableStateOf(false) }

            // Config state
            var apiKey by remember { mutableStateOf("") }
            var selectedModelId by remember { mutableStateOf(GroqApiClient.DEFAULT_VISION_MODEL) }

            val pendingApproval by app.permissionManager.pendingApproval.collectAsState()

            NovaPlayerTheme(
                themeMode = com.example.data.model.ThemeMode.DARK
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = JarvisDarkBg
                ) {
                    // Back handler for settings or returning to Core
                    if (isSettingsOpen) {
                        BackHandler {
                            isSettingsOpen = false
                        }
                    } else if (currentTab != JarvisTab.CORE) {
                        BackHandler {
                            currentTab = JarvisTab.CORE
                        }
                    }

                    // Global Human Approval Modal
                    pendingApproval?.let { req ->
                        HumanApprovalModal(
                            request = req,
                            onApprove = { app.permissionManager.approveCurrentAction() },
                            onReject = { app.permissionManager.rejectCurrentAction() },
                            onCancelExecution = { app.permissionManager.cancelExecution() }
                        )
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets.safeDrawing,
                        topBar = {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = JarvisDarkSurface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Adjust,
                                            contentDescription = "Core Logo",
                                            tint = JarvisCyan,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = "J.A.R.V.I.S.",
                                            color = JarvisCyan,
                                            fontSize = 16.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            letterSpacing = 2.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }

                                    IconButton(
                                        onClick = { isSettingsOpen = !isSettingsOpen },
                                        modifier = Modifier.testTag("top_settings_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isSettingsOpen) Icons.Default.Close else Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            tint = if (isSettingsOpen) JarvisCyan else JarvisOnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            if (!isSettingsOpen) {
                                NavigationBar(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .testTag("main_navigation_bar"),
                                    containerColor = JarvisDarkSurface,
                                    contentColor = JarvisCyan
                                ) {
                                    JarvisTab.values().forEach { tab ->
                                        val isSelected = currentTab == tab
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = { currentTab = tab },
                                            icon = {
                                                Icon(
                                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.title
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = tab.title,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color.Black,
                                                selectedTextColor = JarvisCyan,
                                                indicatorColor = JarvisCyan,
                                                unselectedIconColor = JarvisOnSurfaceVariant,
                                                unselectedTextColor = JarvisOnSurfaceVariant
                                            ),
                                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (isSettingsOpen) {
                                JarvisSettingsScreen(
                                    currentApiKey = apiKey,
                                    onApiKeyChange = { apiKey = it },
                                    currentModelId = selectedModelId,
                                    onModelChange = { selectedModelId = it },
                                    voiceController = app.voiceController,
                                    permissionManager = app.permissionManager
                                )
                            } else {
                                when (currentTab) {
                                    JarvisTab.CORE -> {
                                        JarvisCoreScreen(
                                            agent = app.autonomousAgent,
                                            voiceController = app.voiceController,
                                            apiKey = apiKey,
                                            onNavigateToVision = { currentTab = JarvisTab.VISION },
                                            onNavigateToScreenIntel = { currentTab = JarvisTab.SCREEN },
                                            onNavigateToActivityCenter = { currentTab = JarvisTab.AGENT }
                                        )
                                    }

                                    JarvisTab.VISION -> {
                                        VisionScreen(
                                            cameraManager = app.cameraManager,
                                            visionEngine = app.visionEngine,
                                            voiceController = app.voiceController,
                                            apiKey = apiKey,
                                            selectedModelId = selectedModelId
                                        )
                                    }

                                    JarvisTab.SCREEN -> {
                                        ScreenIntelligenceScreen(
                                            screenIntelligence = app.screenIntelligence,
                                            voiceController = app.voiceController,
                                            apiKey = apiKey,
                                            selectedModelId = selectedModelId
                                        )
                                    }

                                    JarvisTab.AGENT -> {
                                        AgentActivityCenterScreen(
                                            agent = app.autonomousAgent,
                                            permissionManager = app.permissionManager,
                                            apiKey = apiKey
                                        )
                                    }

                                    JarvisTab.MEMORY -> {
                                        ToolsAndMemoryScreen(
                                            toolRegistry = app.toolRegistry,
                                            repository = app.jarvisRepository
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (applicationContext as? NovaApplication)?.cameraManager?.unbind()
        (applicationContext as? NovaApplication)?.voiceController?.release()
    }
}
