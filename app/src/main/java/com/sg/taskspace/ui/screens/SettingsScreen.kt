package com.sg.taskspace.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.sg.taskspace.BuildConfig
import com.sg.taskspace.ui.viewmodel.TaskViewModel
import com.sg.taskspace.ui.components.BackupDiffDialog
import com.sg.taskspace.data.BackupDiffResult
import com.sg.taskspace.data.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTrash: () -> Unit = {}
) {
    val userPreferences by viewModel.userPreferences.collectAsState()
    val context = LocalContext.current
    var showNameDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }
    var showAutoBackupDialog by remember { mutableStateOf(false) }
    var showSwipeRightDialog by remember { mutableStateOf(false) }
    var showSwipeLeftDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showAutoLockTimeoutDialog by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showVacationDialog by remember { mutableStateOf(false) }
    // Backup diff state
    var backupDiffResult by remember { mutableStateOf<BackupDiffResult?>(null) }
    var isRestoring by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val currentTheme = userPreferences?.themeMode ?: "AMOLED Dark"
    val currentFont = userPreferences?.fontFamily ?: "Default"
    val currentPalette = userPreferences?.colorPalette ?: "Indigo"
    val isSecureBackup = userPreferences?.isSecureBackupEnabled ?: true
    val autoBackupFrequency = userPreferences?.autoBackupFrequency ?: "Daily"
    val notificationsEnabled = userPreferences?.notificationsEnabled ?: false

    // Notification permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.saveNotificationsEnabled(granted)
        if (!granted) {
            showPermissionDeniedDialog = true
        } else {
            Toast.makeText(context, "Notifications enabled!", Toast.LENGTH_SHORT).show()
        }
    }

    // Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportData(it) { result ->
                result.fold(
                    onSuccess = {
                        Toast.makeText(context, "✅ Backup created successfully!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(context, "❌ Backup failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    // Import Launcher — opens file picker, then shows diff preview before committing
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            pendingRestoreUri = selectedUri
            viewModel.previewImport(selectedUri) { result ->
                result.fold(
                    onSuccess = { diff ->
                        backupDiffResult = diff
                    },
                    onFailure = { e ->
                        Toast.makeText(
                            context,
                            "Cannot read backup: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    // ── Backup Diff Preview Dialog ──
    backupDiffResult?.let { diff ->
        BackupDiffDialog(
            diff = diff,
            isRestoring = isRestoring,
            onDismiss = {
                backupDiffResult = null
                pendingRestoreUri = null
            },
            onConfirmWithSelection = { selectionConfig ->
                isRestoring = true
                viewModel.importDataWithSelection(diff.parsedBackup, selectionConfig) { success ->
                    isRestoring = false
                    backupDiffResult = null
                    pendingRestoreUri = null
                    Toast.makeText(
                        context,
                        if (success) "✅ Selected data restored successfully!" else "❌ Restore failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // Theme Picker Dialog
    if (showThemeDialog) {
        val themes = listOf("AMOLED Dark", "Dark", "Light", "System")
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    themes.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.saveThemeMode(theme)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(theme, style = MaterialTheme.typography.bodyLarge)
                            if (currentTheme == theme) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Font Picker Dialog
    if (showFontDialog) {
        val fontOptions = listOf(
            Triple("Default", "System default clean font", com.sg.taskspace.ui.theme.getFontFamily("Default")),
            Triple("Inter", "Bundled: modern, crisp & ultra-legible", com.sg.taskspace.ui.theme.getFontFamily("Inter")),
            Triple("Outfit", "Bundled: smooth geometric rounded sans", com.sg.taskspace.ui.theme.getFontFamily("Outfit")),
            Triple("Condensed", "Built-in: compact, sleek modern sans", com.sg.taskspace.ui.theme.getFontFamily("Condensed")),
            Triple("Serif", "Built-in: classic, refined editorial look", com.sg.taskspace.ui.theme.getFontFamily("Serif")),
            Triple("Monospace", "Built-in: clean developer & code style", com.sg.taskspace.ui.theme.getFontFamily("Monospace"))
        )
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("Choose Font Family", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    fontOptions.forEach { (fontKey, subtitle, family) ->
                        val isSelected = currentFont == fontKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.saveFontFamily(fontKey)
                                    showFontDialog = false
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fontKey,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontFamily = family,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFontDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Color Palette Dialog
    if (showPaletteDialog) {
        val palettes = listOf("Indigo", "Emerald", "Rose", "Amber", "Violet", "Teal", "Monochrome")
        AlertDialog(
            onDismissRequest = { showPaletteDialog = false },
            title = { Text("Color Palette", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    palettes.forEach { palette ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.saveColorPalette(palette)
                                    showPaletteDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(palette, style = MaterialTheme.typography.bodyLarge)
                            if (currentPalette == palette) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPaletteDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Auto Backup Dialog
    if (showAutoBackupDialog) {
        val frequencies = listOf("Daily", "Weekly", "Off")
        AlertDialog(
            onDismissRequest = { showAutoBackupDialog = false },
            title = { Text("Auto Backup Frequency", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    frequencies.forEach { freq ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.saveAutoBackupFrequency(freq)
                                    showAutoBackupDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(freq, style = MaterialTheme.typography.bodyLarge)
                            if (autoBackupFrequency == freq) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAutoBackupDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    val swipeRightAction = userPreferences?.swipeRightAction ?: "Complete"
    val swipeLeftAction = userPreferences?.swipeLeftAction ?: "Edit"

    // Swipe Right Action Dialog
    if (showSwipeRightDialog) {
        val swipeOptions = listOf("Complete", "Snooze", "Edit", "Delete", "None")
        AlertDialog(
            onDismissRequest = { showSwipeRightDialog = false },
            title = { Text("Swipe Right Action", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    swipeOptions.forEach { action ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.saveSwipeRightAction(action)
                                    showSwipeRightDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(action, style = MaterialTheme.typography.bodyLarge)
                            if (swipeRightAction == action) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSwipeRightDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Swipe Left Action Dialog
    if (showSwipeLeftDialog) {
        val swipeOptions = listOf("Delete", "Complete", "Snooze", "Edit", "None")
        AlertDialog(
            onDismissRequest = { showSwipeLeftDialog = false },
            title = { Text("Swipe Left Action", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    swipeOptions.forEach { action ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.saveSwipeLeftAction(action)
                                    showSwipeLeftDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(action, style = MaterialTheme.typography.bodyLarge)
                            if (swipeLeftAction == action) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSwipeLeftDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    val autoLockTimeout = userPreferences?.autoLockTimeout ?: "Immediately"

    // PIN Setup Dialog
    if (showPinSetupDialog) {
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            title = { Text("Set 4-Digit Security PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) newPin = it },
                        label = { Text("4-Digit PIN") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) confirmPin = it },
                        label = { Text("Confirm PIN") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError != null) {
                        Text(text = pinError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPin.length != 4) {
                        pinError = "PIN must be exactly 4 digits"
                    } else if (newPin != confirmPin) {
                        pinError = "PINs do not match"
                    } else {
                        viewModel.saveAppLockPin(newPin)
                        showPinSetupDialog = false
                    }
                }) { Text("Save PIN") }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetupDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Auto Lock Timeout Dialog
    if (showAutoLockTimeoutDialog) {
        val timeoutOptions = listOf("Immediately", "1 minute", "5 minutes", "15 minutes", "30 minutes")
        AlertDialog(
            onDismissRequest = { showAutoLockTimeoutDialog = false },
            title = { Text("Auto-Lock Timeout", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    timeoutOptions.forEach { timeout ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.saveAutoLockTimeout(timeout)
                                    showAutoLockTimeoutDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(timeout, style = MaterialTheme.typography.bodyLarge)
                            if (autoLockTimeout == timeout) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAutoLockTimeoutDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Permission Denied Rationale Dialog
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Notification Permission Required", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "TaskSpace requires notification permission to trigger scheduled task reminders. Please grant permission in System Settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = {
                    showPermissionDeniedDialog = false
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showVacationDialog && userPreferences != null) {
        com.sg.taskspace.ui.components.VacationModeDialog(
            userPreferences = userPreferences!!,
            onDismiss = { showVacationDialog = false },
            onSave = { enabled, startDate, endDate, reason, freezeHabits, pauseReminders ->
                viewModel.setVacationMode(enabled, startDate, endDate, reason, freezeHabits, pauseReminders)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Profile ──
            SettingsSectionTitle("Profile")
            SettingsItem(
                icon = Icons.Default.Person,
                title = "Name",
                subtitle = userPreferences?.userName ?: "User",
                onClick = { showNameDialog = true },
                trailingIcon = Icons.Default.Edit
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ── Lifestyle & Vacation Mode ──
            SettingsSectionTitle("Lifestyle & Break")
            SettingsItem(
                icon = Icons.Default.CalendarMonth,
                title = "Vacation Mode",
                subtitle = if (userPreferences?.isVacationModeEnabled == true) {
                    "ACTIVE • ${userPreferences?.vacationReason ?: "Vacation"}"
                } else {
                    "Disabled • Freeze streaks & pause alerts"
                },
                onClick = { showVacationDialog = true },
                trailingIcon = Icons.Default.ChevronRight
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ── Appearance ──
            SettingsSectionTitle("Appearance")
            SettingsItem(
                icon = Icons.Default.Palette,
                title = "Theme",
                subtitle = currentTheme,
                onClick = { showThemeDialog = true },
                trailingIcon = Icons.Default.ChevronRight
            )
            SettingsItem(
                icon = Icons.Default.TextFields,
                title = "Font",
                subtitle = currentFont,
                onClick = { showFontDialog = true },
                trailingIcon = Icons.Default.ChevronRight
            )
            SettingsItem(
                icon = Icons.Default.FormatPaint,
                title = "Color Palette",
                subtitle = currentPalette,
                onClick = { showPaletteDialog = true },
                trailingIcon = Icons.Default.ChevronRight
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ── Notifications & Feedback ──
            SettingsSectionTitle("Notifications & Feedback")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column {
                        Text("Task Reminders", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("Receive notifications for scheduled tasks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    viewModel.saveNotificationsEnabled(true)
                                } else {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                viewModel.saveNotificationsEnabled(true)
                            }
                        } else {
                            viewModel.saveNotificationsEnabled(false)
                        }
                    }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.Dashboard, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column {
                        Text("Persistent Daily Dashboard", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("Show silent progress & quick actions in notification shade", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = userPreferences?.persistentDailyDashboardEnabled ?: false,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    viewModel.saveNotificationsEnabled(true)
                                    viewModel.savePersistentDailyDashboardEnabled(true)
                                } else {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                viewModel.saveNotificationsEnabled(true)
                                viewModel.savePersistentDailyDashboardEnabled(true)
                            }
                        } else {
                            viewModel.savePersistentDailyDashboardEnabled(false)
                        }
                    }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column {
                        Text("Haptic & Sound Effects", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("Play chimes & haptic vibration on task completion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = userPreferences?.soundEffectsEnabled ?: true,
                    onCheckedChange = { viewModel.saveSoundEffectsEnabled(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ── Task Gestures & Swipe Actions ──
            SettingsSectionTitle("Task Gestures & Swipe Actions")
            SettingsItem(
                icon = Icons.Default.Swipe,
                title = "Swipe Right Action",
                subtitle = "Action when swiping task right: $swipeRightAction",
                onClick = { showSwipeRightDialog = true },
                trailingIcon = Icons.Default.ChevronRight
            )
            SettingsItem(
                icon = Icons.Default.Swipe,
                title = "Swipe Left Action",
                subtitle = "Action when swiping task left: $swipeLeftAction",
                onClick = { showSwipeLeftDialog = true },
                trailingIcon = Icons.Default.ChevronRight
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ── App Lock & Security ──
            SettingsSectionTitle("App Lock & Security")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column {
                        Text("App Lock", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(if (userPreferences?.isAppLockEnabled == true) "PIN & Biometric protection active" else "Protect app with PIN or fingerprint", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = userPreferences?.isAppLockEnabled ?: false,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            showPinSetupDialog = true
                        } else {
                            viewModel.saveAppLockEnabled(false)
                        }
                    }
                )
            }
            if (userPreferences?.isAppLockEnabled == true) {
                SettingsItem(
                    icon = Icons.Default.Pin,
                    title = "Change PIN",
                    subtitle = "Update 4-digit security PIN",
                    onClick = { showPinSetupDialog = true },
                    trailingIcon = Icons.Default.ChevronRight
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column {
                            Text("Biometric Unlock", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text("Use fingerprint or face recognition", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = userPreferences?.isBiometricEnabled ?: true,
                        onCheckedChange = { viewModel.saveBiometricEnabled(it) }
                    )
                }
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = "Auto-Lock Timeout",
                    subtitle = userPreferences?.autoLockTimeout ?: "Immediately",
                    onClick = { showAutoLockTimeoutDialog = true },
                    trailingIcon = Icons.Default.ChevronRight
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ── Data & Backup ──
            SettingsSectionTitle("Data & Backup")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column {
                        Text("Secure Backup", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("Encrypt backup files for enhanced privacy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = isSecureBackup,
                    onCheckedChange = { viewModel.saveSecureBackupEnabled(it) }
                )
            }
            SettingsItem(
                icon = Icons.Default.Autorenew,
                title = "Auto Backup Frequency",
                subtitle = userPreferences?.autoBackupFrequency ?: "Daily",
                onClick = { showAutoBackupDialog = true },
                trailingIcon = Icons.Default.ChevronRight
            )
            SettingsItem(
                icon = Icons.Default.Backup,
                title = "Backup Data Now",
                subtitle = "Export tasks, journal, habits, goals & projects to a JSON file",
                onClick = {
                    runCatching {
                        exportLauncher.launch("taskspace_backup_${System.currentTimeMillis()}.json")
                    }.onFailure { e ->
                        e.printStackTrace()
                        Toast.makeText(context, "Backup launcher error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            )
            SettingsItem(
                icon = Icons.Default.Restore,
                title = "Restore Data",
                subtitle = "Import tasks, journal, habits, goals & projects from a JSON file",
                onClick = {
                    runCatching {
                        importLauncher.launch(arrayOf("*/*"))
                    }.onFailure { e ->
                        e.printStackTrace()
                        Toast.makeText(context, "Restore launcher error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            )
            SettingsItem(
                icon = Icons.Default.Delete,
                title = "Trash",
                subtitle = "View and restore soft-deleted items (30-day retention)",
                onClick = { onNavigateToTrash() },
                trailingIcon = Icons.Default.ChevronRight
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ── About ──
            SettingsSectionTitle("About")
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Version",
                subtitle = "v${BuildConfig.VERSION_NAME} - TaskSpace",  
                onClick = { }
            )
        }
    }

    // Name Edit Dialog
    if (showNameDialog) {
        var newName by remember { mutableStateOf(userPreferences?.userName ?: "") }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Edit Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveUserName(newName.ifBlank { "User" })
                    showNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingIcon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailingIcon != null) {
            Icon(imageVector = trailingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
