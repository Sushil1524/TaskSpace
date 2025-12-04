package com.sg.taskspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sg.taskspace.ui.navigation.TaskSpaceNavGraph
import com.sg.taskspace.ui.theme.TaskSpaceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.decorView.setBackgroundColor(android.graphics.Color.BLACK) // Fix white flash on navigation
        setContent {
            TaskSpaceTheme {
                TaskSpaceNavGraph()
            }
        }
    }
}
