package com.sg.taskspace

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sg.taskspace.ui.navigation.TaskSpaceNavGraph
import com.sg.taskspace.ui.screens.AppLockScreen
import com.sg.taskspace.ui.theme.TaskSpaceTheme
import com.sg.taskspace.ui.viewmodel.TaskViewModel

class MainActivity : FragmentActivity() {

    private var viewModelInstance: TaskViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.decorView.setBackgroundColor(android.graphics.Color.BLACK) // Fix white flash on navigation
        setContent {
            val taskViewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory)
            viewModelInstance = taskViewModel

            val userPreferences by taskViewModel.userPreferences.collectAsState()
            val isAppLocked by taskViewModel.isAppLocked.collectAsState()

            val themeMode = userPreferences?.themeMode ?: "AMOLED Dark"
            val fontFamily = userPreferences?.fontFamily ?: "Default"
            val colorPalette = userPreferences?.colorPalette ?: "Indigo"

            TaskSpaceTheme(themeMode = themeMode, fontFamily = fontFamily, colorPalette = colorPalette) {
                if (isAppLocked && userPreferences?.isAppLockEnabled == true) {
                    AppLockScreen(
                        storedPinHash = userPreferences?.appLockPinHash ?: "",
                        isBiometricEnabled = userPreferences?.isBiometricEnabled ?: true,
                        onUnlockSuccess = { taskViewModel.unlockApp() }
                    )
                } else {
                    TaskSpaceNavGraph(taskViewModel = taskViewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModelInstance?.checkAndApplyAppLockOnForeground()
    }

    override fun onStop() {
        super.onStop()
        viewModelInstance?.recordBackgroundTimestamp()
    }
}
