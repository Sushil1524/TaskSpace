package com.sg.taskspace

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sg.taskspace.ui.navigation.TaskSpaceNavGraph
import com.sg.taskspace.ui.screens.AppLockScreen
import com.sg.taskspace.ui.theme.TaskSpaceTheme
import com.sg.taskspace.ui.viewmodel.TaskViewModel

class MainActivity : AppCompatActivity() {

    private val taskViewModel: TaskViewModel by viewModels { TaskViewModel.Factory }

    /** Maps safe 16-bit code → original 32-bit code sent by ActivityResultRegistry */
    private val requestCodeMap = HashMap<Int, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.decorView.setBackgroundColor(android.graphics.Color.BLACK)

        taskViewModel.checkAndApplyAppLockOnForeground()

        setContent {
            val userPreferences by taskViewModel.userPreferences.collectAsState()
            val isAppLocked by taskViewModel.isAppLocked.collectAsState()

            val themeMode = userPreferences?.themeMode ?: "AMOLED Dark"
            val fontFamily = userPreferences?.fontFamily ?: "Default"
            val colorPalette = userPreferences?.colorPalette ?: "Indigo"

            TaskSpaceTheme(themeMode = themeMode, fontFamily = fontFamily, colorPalette = colorPalette) {
                if (isAppLocked && userPreferences?.isAppLockEnabled == true && !userPreferences?.appLockPinHash.isNullOrBlank()) {
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

    override fun startActivityForResult(intent: Intent, requestCode: Int) {
        if (requestCode == -1) {
            super.startActivityForResult(intent, requestCode)
            return
        }
        val safeCode = requestCode and 0xFFFF
        requestCodeMap[safeCode] = requestCode
        super.startActivityForResult(intent, safeCode)
    }

    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        if (requestCode == -1) {
            super.startActivityForResult(intent, requestCode, options)
            return
        }
        val safeCode = requestCode and 0xFFFF
        requestCodeMap[safeCode] = requestCode
        super.startActivityForResult(intent, safeCode, options)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val originalCode = requestCodeMap.remove(requestCode) ?: requestCode
        super.onActivityResult(originalCode, resultCode, data)
    }
    // ──────────────────────────────────────────────────────────────────────────

    override fun onStart() {
        super.onStart()
        taskViewModel.checkAndApplyAppLockOnForeground()
    }

    override fun onStop() {
        super.onStop()
        taskViewModel.recordBackgroundTimestamp()
    }
}
