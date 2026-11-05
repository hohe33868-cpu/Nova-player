package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.ModelCapabilityValidator
import com.example.data.local.NovaDatabase
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.repository.JarvisRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: NovaDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, NovaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun readStringFromContext_matchesAppName() {
        val appName = context.getString(R.string.app_name)
        assertEquals("JARVIS AI", appName)
    }

    @Test
    fun modelCapabilityValidator_correctlyValidatesVisionModels() {
        assertTrue(ModelCapabilityValidator.isVisionCapable("llama-3.2-11b-vision-preview"))
        assertTrue(ModelCapabilityValidator.isVisionCapable("llama-3.2-90b-vision-preview"))
        assertFalse(ModelCapabilityValidator.isVisionCapable("llama-3.3-70b-versatile"))
        assertFalse(ModelCapabilityValidator.isVisionCapable("mixtral-8x7b-32768"))
    }

    @Test
    fun jarvisRepository_tasksAndMemoryCrudWorks() = runBlocking {
        val repo = JarvisRepository(database)
        val taskId = repo.insertTask("Scan Perimeter", "Engage optical sensors", "Security", "High")
        assertTrue(taskId > 0)

        val pending = repo.getPendingTasks()
        assertEquals(1, pending.size)
        assertEquals("Scan Perimeter", pending[0].title)

        repo.setTaskCompleted(taskId, true)
        val pendingAfter = repo.getPendingTasks()
        assertEquals(0, pendingAfter.size)

        repo.setMemory("user_callsign", "Iron Man", "UserIdentity")
        val recalled = repo.getMemory("user_callsign")
        assertEquals("Iron Man", recalled)
    }
}
