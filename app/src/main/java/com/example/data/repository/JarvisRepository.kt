package com.example.data.repository

import com.example.data.local.NovaDatabase
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JarvisRepository(private val database: NovaDatabase) {

    val allTasks: Flow<List<TaskEntity>> = database.taskDao().getAllTasks()
    val allNotes: Flow<List<NoteEntity>> = database.noteDao().getAllNotes()
    val allMemories: Flow<List<MemoryEntity>> = database.memoryDao().getAllMemories()
    val recentActionLogs: Flow<List<ActionLogEntity>> = database.actionLogDao().getRecentLogs()
    val latestPlan: Flow<PlanEntity?> = database.planDao().getLatestPlan()
    val allPlans: Flow<List<PlanEntity>> = database.planDao().getAllPlans()

    suspend fun initializeDefaultDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingTasks = database.taskDao().getAllTasks().firstOrNull()
        if (existingTasks.isNullOrEmpty()) {
            database.taskDao().insertTask(
                TaskEntity(
                    title = "Initialize Vision & Sensor Diagnostics",
                    description = "Verify camera feeds, optical OCR pipeline, and ambient light sensors.",
                    category = "System",
                    isCompleted = true,
                    priority = "High"
                )
            )
            database.taskDao().insertTask(
                TaskEntity(
                    title = "Organize Project 'Clean Slate'",
                    description = "Structure workspace folders, index system documentation, and run safety audit.",
                    category = "Project",
                    isCompleted = false,
                    priority = "Critical"
                )
            )
            database.taskDao().insertTask(
                TaskEntity(
                    title = "Review Screen Intelligence Telemetry",
                    description = "Inspect OCR element bounding boxes and error trace analyzer.",
                    category = "Diagnostics",
                    isCompleted = false,
                    priority = "Normal"
                )
            )
        }

        val existingNotes = database.noteDao().getAllNotes().firstOrNull()
        if (existingNotes.isNullOrEmpty()) {
            database.noteDao().insertNote(
                NoteEntity(
                    title = "Mark 85 Armor Telemetry Protocols",
                    content = "Arc Reactor output: 100% nominal. Visual spectrum feed routed to Groq Vision LLaMA 3.2. Optical lens sensors calibrated.",
                    tags = "Diagnostics, Armor, ArcReactor"
                )
            )
            database.noteDao().insertNote(
                NoteEntity(
                    title = "Safety Directives & Authorization Rules",
                    content = "Rule 1: Always request human approval before destructive file ops or sending external transmissions. Rule 2: Honest reporting over simulation.",
                    tags = "Safety, Protocols"
                )
            )
        }

        val existingMemories = database.memoryDao().getAllMemories().firstOrNull()
        if (existingMemories.isNullOrEmpty()) {
            database.memoryDao().insertMemory(
                MemoryEntity(
                    key = "user_title",
                    value = "Sir",
                    category = "Preference",
                    confidence = 1.0f
                )
            )
            database.memoryDao().insertMemory(
                MemoryEntity(
                    key = "preferred_vision_model",
                    value = "llama-3.2-11b-vision-preview",
                    category = "AI_Config",
                    confidence = 0.95f
                )
            )
            database.memoryDao().insertMemory(
                MemoryEntity(
                    key = "current_focus_project",
                    value = "Autonomous Multi-Step Agent & Computer Vision HUD",
                    category = "Project",
                    confidence = 0.98f
                )
            )
        }
    }

    // Task Operations
    suspend fun insertTask(title: String, description: String = "", category: String = "General", priority: String = "Normal"): Long = withContext(Dispatchers.IO) {
        database.taskDao().insertTask(
            TaskEntity(
                title = title,
                description = description,
                category = category,
                priority = priority
            )
        )
    }

    suspend fun setTaskCompleted(id: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        database.taskDao().setTaskCompleted(id, completed)
    }

    suspend fun deleteTask(id: Long) = withContext(Dispatchers.IO) {
        database.taskDao().deleteTaskById(id)
    }

    suspend fun getPendingTasks(): List<TaskEntity> = withContext(Dispatchers.IO) {
        database.taskDao().getPendingTasks()
    }

    // Note Operations
    suspend fun insertNote(title: String, content: String, tags: String = ""): Long = withContext(Dispatchers.IO) {
        database.noteDao().insertNote(
            NoteEntity(
                title = title,
                content = content,
                tags = tags
            )
        )
    }

    suspend fun searchNotes(query: String): List<NoteEntity> = withContext(Dispatchers.IO) {
        database.noteDao().searchNotes(query)
    }

    suspend fun deleteNote(id: Long) = withContext(Dispatchers.IO) {
        database.noteDao().deleteNoteById(id)
    }

    // Memory Operations
    suspend fun setMemory(key: String, value: String, category: String = "Preference"): Long = withContext(Dispatchers.IO) {
        val existing = database.memoryDao().getMemoryByKey(key)
        if (existing != null) {
            database.memoryDao().insertMemory(existing.copy(value = value, category = category, updatedAt = System.currentTimeMillis()))
            existing.id
        } else {
            database.memoryDao().insertMemory(
                MemoryEntity(
                    key = key,
                    value = value,
                    category = category
                )
            )
        }
    }

    suspend fun getMemory(key: String): String? = withContext(Dispatchers.IO) {
        database.memoryDao().getMemoryByKey(key)?.value
    }

    suspend fun deleteMemory(id: Long) = withContext(Dispatchers.IO) {
        database.memoryDao().deleteMemoryById(id)
    }

    // Action Logs
    suspend fun logAction(
        toolName: String,
        description: String,
        status: String,
        riskLevel: String,
        wasApproved: Boolean = false,
        details: String = ""
    ): Long = withContext(Dispatchers.IO) {
        database.actionLogDao().insertLog(
            ActionLogEntity(
                toolName = toolName,
                actionDescription = description,
                status = status,
                riskLevel = riskLevel,
                wasHumanApproved = wasApproved,
                details = details
            )
        )
    }

    suspend fun clearActionLogs() = withContext(Dispatchers.IO) {
        database.actionLogDao().clearLogs()
    }

    // Plans
    suspend fun savePlan(plan: PlanEntity): Long = withContext(Dispatchers.IO) {
        database.planDao().insertPlan(plan)
    }

    suspend fun updatePlan(plan: PlanEntity) = withContext(Dispatchers.IO) {
        database.planDao().updatePlan(plan)
    }
}
