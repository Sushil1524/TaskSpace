package com.sg.taskspace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sg.taskspace.ui.viewmodel.TaskViewModel

@Composable
fun OnboardingScreen(
    viewModel: TaskViewModel,
    onOnboardingComplete: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Skip Button (Top Right)
            if (currentStep < 3) {
                TextButton(
                    onClick = {
                        viewModel.completeOnboarding()
                        onOnboardingComplete()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(1f))

                when (currentStep) {
                    0 -> TutorialSlide(
                        title = "Organize Your Life",
                        description = "Add daily and weekly tasks to keep track of your goals. Set priorities and categories to stay focused.",
                        icon = Icons.Default.Check // Placeholder icon
                    )
                    1 -> TutorialSlide(
                        title = "Track Your Progress",
                        description = "Mark tasks as done and watch your streak grow. See your weekly progress at a glance.",
                        icon = Icons.Default.Check // Placeholder
                    )
                    2 -> TutorialSlide(
                        title = "Gain Insights",
                        description = "View detailed analytics about your productivity. Discover your best days and areas for improvement.",
                        icon = Icons.Default.Check // Placeholder
                    )
                    3 -> NameInputSlide(
                        name = name,
                        onNameChange = { name = it }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    if (currentStep > 0) {
                        TextButton(onClick = { currentStep-- }) {
                            Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(64.dp)) // Placeholder
                    }

                    // Page Indicators (Only for tutorial steps)
                    if (currentStep < 3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(3) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (currentStep == index) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                )
                            }
                        }
                    }

                    // Next / Get Started Button
                    Button(
                        onClick = {
                            if (currentStep < 3) {
                                currentStep++
                            } else {
                                viewModel.saveUserName(name.ifBlank { "User" })
                                viewModel.completeOnboarding()
                                onOnboardingComplete()
                            }
                        },
                        enabled = if (currentStep == 3) name.isNotBlank() else true
                    ) {
                        Text(if (currentStep == 3) "Get Started" else "Next")
                        if (currentStep < 3) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TutorialSlide(title: String, description: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Placeholder for an image or illustration
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun NameInputSlide(name: String, onNameChange: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "What should we call you?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Your Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Used for exporting and importing data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
