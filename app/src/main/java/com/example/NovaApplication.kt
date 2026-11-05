package com.example

import android.app.Application
import com.example.agent.AutonomousAgent
import com.example.agent.execution.ExecutionEngine
import com.example.agent.monitor.ActivityMonitor
import com.example.agent.planner.TaskPlanner
import com.example.agent.safety.PermissionManager
import com.example.agent.tools.ToolRegistry
import com.example.ai.GroqApiClient
import com.example.data.local.NovaDatabase
import com.example.data.repository.JarvisRepository
import com.example.data.repository.MediaRepository
import com.example.data.repository.PreferencesRepository
import com.example.player.PlayerManager
import com.example.vision.CameraManager
import com.example.vision.ImageAnalyzer
import com.example.vision.ScreenIntelligence
import com.example.vision.VisionEngine
import com.example.voice.JarvisVoiceController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NovaApplication : Application() {
    private val applicationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    lateinit var database: NovaDatabase
        private set

    lateinit var jarvisRepository: JarvisRepository
        private set

    lateinit var preferencesRepository: PreferencesRepository
        private set

    lateinit var mediaRepository: MediaRepository
        private set

    lateinit var groqClient: GroqApiClient
        private set

    lateinit var cameraManager: CameraManager
        private set

    lateinit var imageAnalyzer: ImageAnalyzer
        private set

    lateinit var visionEngine: VisionEngine
        private set

    lateinit var screenIntelligence: ScreenIntelligence
        private set

    lateinit var toolRegistry: ToolRegistry
        private set

    lateinit var permissionManager: PermissionManager
        private set

    lateinit var activityMonitor: ActivityMonitor
        private set

    lateinit var taskPlanner: TaskPlanner
        private set

    lateinit var executionEngine: ExecutionEngine
        private set

    lateinit var autonomousAgent: AutonomousAgent
        private set

    lateinit var voiceController: JarvisVoiceController
        private set

    override fun onCreate() {
        super.onCreate()

        database = NovaDatabase.getInstance(this)
        jarvisRepository = JarvisRepository(database)
        preferencesRepository = PreferencesRepository(this)
        mediaRepository = MediaRepository(this, database)

        // Groq AI client
        groqClient = GroqApiClient()

        // Vision Subsystems
        cameraManager = CameraManager(this)
        imageAnalyzer = ImageAnalyzer()
        visionEngine = VisionEngine(groqClient, imageAnalyzer)
        screenIntelligence = ScreenIntelligence(this, groqClient)

        // Safety & Tools
        permissionManager = PermissionManager(jarvisRepository)
        toolRegistry = ToolRegistry(
            context = this,
            repository = jarvisRepository,
            cameraManager = cameraManager,
            visionEngine = visionEngine,
            screenIntelligence = screenIntelligence
        )

        // Planner, Execution & Activity Monitor
        activityMonitor = ActivityMonitor()
        taskPlanner = TaskPlanner(toolRegistry, permissionManager, groqClient)
        executionEngine = ExecutionEngine(toolRegistry, permissionManager, activityMonitor, jarvisRepository)

        autonomousAgent = AutonomousAgent(
            repository = jarvisRepository,
            toolRegistry = toolRegistry,
            permissionManager = permissionManager,
            taskPlanner = taskPlanner,
            executionEngine = executionEngine,
            activityMonitor = activityMonitor,
            groqClient = groqClient
        )

        voiceController = JarvisVoiceController(this)

        PlayerManager.initialize(this, mediaRepository, preferencesRepository)

        // Seed initial mission data
        applicationScope.launch {
            jarvisRepository.initializeDefaultDataIfEmpty()
        }
    }
}
