package com.sg.taskspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sg.taskspace.ui.TaskSpaceNavGraph
import com.sg.taskspace.ui.theme.TaskSpaceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskSpaceTheme {
                TaskSpaceNavGraph()
            }
        }
    }
}
