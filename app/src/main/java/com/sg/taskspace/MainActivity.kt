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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val taskViewModel: TaskViewModel by viewModels { TaskViewModel.Factory }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    /** Maps safe 16-bit code → original 32-bit code sent by ActivityResultRegistry */
    private val requestCodeMap = HashMap<Int, Int>()

    private val navigateToRoute = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.decorView.setBackgroundColor(android.graphics.Color.BLACK)

        com.sg.taskspace.util.NotificationHelper.createNotificationChannel(this)
        com.sg.taskspace.util.NotificationHelper.updateDailyDashboardNotification(this)
        com.sg.taskspace.util.NotificationHelper.scheduleDailyEveningReminder(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        taskViewModel.checkAndApplyAppLockOnForeground()
        handleIntent(intent)

        setContent {
            val userPreferences by taskViewModel.userPreferences.collectAsState()
            val isAppLocked by taskViewModel.isAppLocked.collectAsState()
            val targetRoute by navigateToRoute.collectAsState()

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
                    TaskSpaceNavGraph(
                        taskViewModel = taskViewModel,
                        initialNavigateTo = targetRoute,
                        onNavigated = { navigateToRoute.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val navTarget = intent?.getStringExtra("EXTRA_NAVIGATE_TO")
            ?: if (intent?.action == com.sg.taskspace.util.NotificationHelper.ACTION_ADD_TASK) "add_task" else null
        if (!navTarget.isNullOrBlank()) {
            navigateToRoute.value = navTarget
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun startActivityForResult(intent: Intent, requestCode: Int) {
        if (requestCode == -1) {
            super.startActivityForResult(intent, requestCode)
            return
        }
        val safeCode = requestCode and 0xFFFF
        requestCodeMap[safeCode] = requestCode
        super.startActivityForResult(intent, safeCode)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        if (requestCode == -1) {
            super.startActivityForResult(intent, requestCode, options)
            return
        }
        val safeCode = requestCode and 0xFFFF
        requestCodeMap[safeCode] = requestCode
        super.startActivityForResult(intent, safeCode, options)
    }

    @Deprecated("Deprecated in Java")
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
