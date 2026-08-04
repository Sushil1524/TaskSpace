package com.sg.taskspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sg.taskspace.ui.navigation.TaskSpaceNavGraph
import com.sg.taskspace.ui.theme.TaskSpaceTheme
import com.sg.taskspace.ui.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.decorView.setBackgroundColor(android.graphics.Color.BLACK) // Fix white flash on navigation
        setContent {
            val taskViewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory)
            val userPreferences by taskViewModel.userPreferences.collectAsState()
            val themeMode = userPreferences?.themeMode ?: "AMOLED Dark"
            val fontFamily = userPreferences?.fontFamily ?: "Default"

            TaskSpaceTheme(themeMode = themeMode, fontFamily = fontFamily) {
                TaskSpaceNavGraph(taskViewModel = taskViewModel)
            }
        }
    }
}
