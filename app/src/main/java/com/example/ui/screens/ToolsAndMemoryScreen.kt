package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.Dialog
import com.example.agent.model.RiskLevel
import com.example.agent.model.ToolDefinition
import com.example.agent.tools.ToolRegistry
import com.example.data.local.entities.ActionLogEntity
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.NoteEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.repository.JarvisRepository
import com.example.ui.components.StatusTelemetryBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MemorySubTab(val label: String) {
    TOOLS("Tools (11)"),
    MEMORY("Long-Term Memory"),
    TASKS("Missions"),
    NOTES("Classified Notes"),
    LOGS("Audit Logs")
}

@Composable
fun ToolsAndMemoryScreen(
    toolRegistry: ToolRegistry,
    repository: JarvisRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(MemorySubTab.TOOLS) }

    val tasks by repository.allTasks.collectAsState(initial = emptyList())
    val notes by repository.allNotes.collectAsState(initial = emptyList())
    val memories by repository.allMemories.collectAsState(initial = emptyList())
    val actionLogs by repository.recentActionLogs.collectAsState(initial = emptyList())

    // Dialog States
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var testToolSelected by remember { mutableStateOf<ToolDefinition?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisDarkBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tab Selector Row
        ScrollableTabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor = JarvisDarkSurface,
            contentColor = JarvisCyan,
            edgePadding = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, JarvisBorder, RoundedCornerShape(10.dp))
        ) {
            MemorySubTab.values().forEach { tab ->
                val isSelected = activeTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { activeTab = tab },
                    text = {
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) JarvisCyan else JarvisOnSurfaceVariant
                        )
                    }
                )
            }
        }

        // Tab Contents
        when (activeTab) {
            MemorySubTab.TOOLS -> {
                ToolExplorerTab(
                    tools = toolRegistry.getAllTools(),
                    onTestTool = { tool -> testToolSelected = tool }
                )
            }
            MemorySubTab.MEMORY -> {
                MemoryTab(
                    memories = memories,
                    onAddClick = { showAddMemoryDialog = true },
                    onDeleteClick = { id -> coroutineScope.launch { repository.deleteMemory(id) } }
                )
            }
            MemorySubTab.TASKS -> {
                TasksTab(
                    tasks = tasks,
                    onToggleComplete = { id, comp -> coroutineScope.launch { repository.setTaskCompleted(id, comp) } },
                    onDeleteTask = { id -> coroutineScope.launch { repository.deleteTask(id) } },
                    onAddTaskClick = { showAddTaskDialog = true }
                )
            }
            MemorySubTab.NOTES -> {
                NotesTab(
                    notes = notes,
                    onDeleteNote = { id -> coroutineScope.launch { repository.deleteNote(id) } },
                    onAddNoteClick = { showAddNoteDialog = true }
                )
            }
            MemorySubTab.LOGS -> {
                LogsTab(
                    logs = actionLogs,
                    onClearLogs = { coroutineScope.launch { repository.clearActionLogs() } }
                )
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Mission") }
        var priority by remember { mutableStateOf("Normal") }

        Dialog(onDismissRequest = { showAddTaskDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, JarvisCyan, RoundedCornerShape(14.dp)),
                color = JarvisDarkBg
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("CREATE NEW MISSION TASK", color = JarvisCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisCyan, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisCyan, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showAddTaskDialog = false }) { Text("Cancel", color = JarvisOnSurfaceVariant) }
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    coroutineScope.launch {
                                        repository.insertTask(title, category = category, priority = priority)
                                    }
                                    showAddTaskDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black)
                        ) {
                            Text("Create", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Add Memory Dialog
    if (showAddMemoryDialog) {
        var key by remember { mutableStateOf("") }
        var value by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Preference") }

        Dialog(onDismissRequest = { showAddMemoryDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, JarvisCyan, RoundedCornerShape(14.dp)),
                color = JarvisDarkBg
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("STORE NEW LONG-TERM MEMORY", color = JarvisCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text("Key Identifier") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisCyan, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Memory Fact / Value") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisCyan, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddMemoryDialog = false }) { Text("Cancel", color = JarvisOnSurfaceVariant) }
                        Button(
                            onClick = {
                                if (key.isNotBlank()) {
                                    coroutineScope.launch { repository.setMemory(key, value, category) }
                                    showAddMemoryDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black)
                        ) {
                            Text("Store", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Add Note Dialog
    if (showAddNoteDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var tags by remember { mutableStateOf("Classified") }

        Dialog(onDismissRequest = { showAddNoteDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, JarvisCyan, RoundedCornerShape(14.dp)),
                color = JarvisDarkBg
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ARCHIVE CLASSIFIED NOTE", color = JarvisCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisCyan, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Content") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisCyan, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddNoteDialog = false }) { Text("Cancel", color = JarvisOnSurfaceVariant) }
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    coroutineScope.launch { repository.insertNote(title, content, tags) }
                                    showAddNoteDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black)
                        ) {
                            Text("Archive", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Tool Test Dialog Playground
    testToolSelected?.let { tool ->
        var inputArg by remember { mutableStateOf("") }
        var testResult by remember { mutableStateOf<String?>(null) }
        var isExecuting by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { testToolSelected = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, JarvisCyan, RoundedCornerShape(14.dp)),
                color = JarvisDarkBg
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("TEST TOOL: [${tool.id.uppercase()}]", color = JarvisCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(tool.description, color = JarvisOnSurface, fontSize = 11.sp)
                    Text("Schema: ${tool.inputSchemaDesc}", color = JarvisGold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)

                    OutlinedTextField(
                        value = inputArg,
                        onValueChange = { inputArg = it },
                        label = { Text("Argument (e.g. math expression or query)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisCyan, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    testResult?.let { res ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(JarvisDarkSurface)
                                .border(1.dp, JarvisBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(res, color = JarvisEmerald, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { testToolSelected = null }) { Text("Close", color = JarvisOnSurfaceVariant) }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isExecuting = true
                                    val args = when (tool.id) {
                                        "calculator" -> mapOf("expression" to if (inputArg.isBlank()) "24 * 7 + 12" else inputArg)
                                        "time_date" -> mapOf("query" to "current_time")
                                        "web_search" -> mapOf("query" to if (inputArg.isBlank()) "Stark Industries Arc Reactor" else inputArg)
                                        "device_hardware_command" -> mapOf("action" to if (inputArg.isBlank()) "battery" else inputArg)
                                        else -> mapOf("query" to inputArg, "action" to "list", "key" to inputArg)
                                    }
                                    val out = toolRegistry.executeTool(tool.id, args)
                                    testResult = out.output
                                    isExecuting = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black)
                        ) {
                            Text("Execute Tool", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolExplorerTab(
    tools: List<ToolDefinition>,
    onTestTool: (ToolDefinition) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tools, key = { it.id }) { tool ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, JarvisBorder, RoundedCornerShape(10.dp)),
                color = JarvisDarkSurface
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tool.name,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (tool.riskLevel == RiskLevel.LOW) JarvisEmerald.copy(alpha = 0.2f) else JarvisAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (tool.riskLevel == RiskLevel.LOW) "LOW RISK" else "REQUIRES APPROVAL",
                                color = if (tool.riskLevel == RiskLevel.LOW) JarvisEmerald else JarvisAmber,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(text = tool.description, color = JarvisOnSurface, fontSize = 11.sp, lineHeight = 15.sp)

                    Text(
                        text = "ID: ${tool.id} | Category: ${tool.category}",
                        color = JarvisCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(
                            onClick = { onTestTool(tool) },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(JarvisCyan))
                        ) {
                            Text("Test Tool", color = JarvisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryTab(
    memories: List<MemoryEntity>,
    onAddClick: () -> Unit,
    onDeleteClick: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PERSISTENT MEMORY SLOTS (${memories.size})", color = JarvisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Memory", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(memories, key = { it.id }) { memory ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, JarvisBorder, RoundedCornerShape(8.dp)),
                    color = JarvisDarkSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = memory.key,
                                color = JarvisGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = memory.value,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Category: ${memory.category} • Confidence: ${(memory.confidence * 100).toInt()}%",
                                color = JarvisOnSurfaceVariant,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        IconButton(onClick = { onDeleteClick(memory.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = JarvisCrimson, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TasksTab(
    tasks: List<TaskEntity>,
    onToggleComplete: (Long, Boolean) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onAddTaskClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MISSION TASKS (${tasks.size})", color = JarvisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Button(
                onClick = onAddTaskClick,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Mission", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(tasks, key = { it.id }) { task ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, if (task.isCompleted) JarvisBorder else JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    color = JarvisDarkSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { onToggleComplete(task.id, it) },
                            colors = CheckboxDefaults.colors(checkedColor = JarvisEmerald, uncheckedColor = JarvisOnSurfaceVariant)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                color = if (task.isCompleted) JarvisOnSurfaceVariant else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (task.description.isNotBlank()) {
                                Text(task.description, color = JarvisOnSurfaceVariant, fontSize = 11.sp)
                            }
                            Text(
                                text = "[#${task.id}] Priority: ${task.priority} • Category: ${task.category}",
                                color = JarvisGold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        IconButton(onClick = { onDeleteTask(task.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = JarvisCrimson, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotesTab(
    notes: List<NoteEntity>,
    onDeleteNote: (Long) -> Unit,
    onAddNoteClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CLASSIFIED NOTES ARCHIVE (${notes.size})", color = JarvisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Button(
                onClick = onAddNoteClick,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Archive Note", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(notes, key = { it.id }) { note ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, JarvisBorder, RoundedCornerShape(10.dp)),
                    color = JarvisDarkSurface
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(note.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onDeleteNote(note.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = JarvisCrimson, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(note.content, color = JarvisOnSurface, fontSize = 11.sp, lineHeight = 16.sp)
                        Text("Tags: ${note.tags}", color = JarvisCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun LogsTab(
    logs: List<ActionLogEntity>,
    onClearLogs: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SECURITY & ACTION AUDIT TRAIL", color = JarvisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            TextButton(onClick = onClearLogs) {
                Text("Clear Trail", color = JarvisCrimson, fontSize = 11.sp)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(logs, key = { it.id }) { log ->
                val statusColor = when (log.status) {
                    "SUCCESS" -> JarvisEmerald
                    "REJECTED" -> JarvisCrimson
                    "FAILED" -> JarvisCrimson
                    else -> JarvisAmber
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .border(0.5.dp, JarvisBorder, RoundedCornerShape(6.dp)),
                    color = JarvisDarkSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = dateFormat.format(Date(log.timestamp)),
                                    color = JarvisOnSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "[${log.toolName}]",
                                    color = JarvisGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(log.actionDescription, color = Color.White, fontSize = 11.sp)
                            if (log.details.isNotBlank()) {
                                Text(log.details.take(100), color = JarvisOnSurfaceVariant, fontSize = 10.sp)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(statusColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(log.status, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}
