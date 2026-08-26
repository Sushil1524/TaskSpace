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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sg.taskspace.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val userPreferences by viewModel.userPreferences.collectAsState()
    val context = LocalContext.current
    var showNameDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }
    var showAutoBackupDialog by remember { mutableStateOf(false) }

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
        Toast.makeText(
            context,
            if (granted) "Notifications enabled!" else "Notifications permission denied.",
            Toast.LENGTH_SHORT
        ).show()
    }

    // Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportData(it) { success ->
                Toast.makeText(
                    context,
                    if (success) "Backup successful!" else "Backup failed.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importData(it) { success ->
                Toast.makeText(
                    context,
                    if (success) "Data restored successfully!" else "Restore failed.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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
        val fonts = listOf("Default", "Rounded", "Serif", "Monospace")
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("Choose Font", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    fonts.forEach { font ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.saveFontFamily(font)
                                    showFontDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(font, style = MaterialTheme.typography.bodyLarge)
                            if (currentFont == font) {
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

    var showSwipeRightDialog by remember { mutableStateOf(false) }
    var showSwipeLeftDialog by remember { mutableStateOf(false) }

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
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.saveNotificationsEnabled(enabled)
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
                onClick = { exportLauncher.launch("taskspace_backup_${System.currentTimeMillis()}.json") }
            )
            SettingsItem(
                icon = Icons.Default.Restore,
                title = "Restore Data",
                subtitle = "Import tasks, journal, habits, goals & projects from a JSON file",
                onClick = { importLauncher.launch(arrayOf("application/json")) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // ── About ──
            SettingsSectionTitle("About")
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Version",
                subtitle = "v1.3.1 — TaskSpace",
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
