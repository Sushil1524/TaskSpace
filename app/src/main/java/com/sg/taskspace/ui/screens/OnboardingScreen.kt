package com.sg.taskspace.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sg.taskspace.ui.theme.PriorityHigh
import com.sg.taskspace.ui.theme.PriorityLow
import com.sg.taskspace.ui.theme.PriorityMedium
import com.sg.taskspace.ui.theme.PrimaryIndigo
import com.sg.taskspace.ui.theme.SecondaryEmerald
import com.sg.taskspace.ui.viewmodel.TaskViewModel

// Each slide: title, subtitle, description, accent color, and a composable illustration
private data class OnboardingSlide(
    val title: String,
    val subtitle: String,
    val description: String,
    val accentColor: Color,
    val illustrationIcons: List<Pair<ImageVector, Color>>, // icon + tint
    val badge: String? = null
)

private val slides = listOf(
    OnboardingSlide(
        title = "Capture Every Task",
        subtitle = "YOUR FOCUS ENGINE",
        description = "Add tasks with priorities, time estimates, and tags. Group them under projects for a clear bigger picture.",
        accentColor = PrimaryIndigo,
        illustrationIcons = listOf(
            Icons.Default.CheckCircle to Color(0xFF6366F1),
            Icons.Default.Schedule to Color(0xFF818CF8),
            Icons.AutoMirrored.Filled.Label to Color(0xFFA5B4FC)
        ),
        badge = "📋 Tasks"
    ),
    OnboardingSlide(
        title = "Track Your Progress",
        subtitle = "SEE YOUR WEEK CLEARLY",
        description = "Watch your weekly streak grow. Active tasks sit up front, completed tasks slide into 'Finished' — satisfying every time.",
        accentColor = SecondaryEmerald,
        illustrationIcons = listOf(
            Icons.Default.BarChart to Color(0xFF10B981),
            Icons.Default.CheckCircle to Color(0xFF34D399),
            Icons.Default.EmojiEvents to Color(0xFF6EE7B7)
        ),
        badge = "📈 Progress"
    ),
    OnboardingSlide(
        title = "Build Habits & Goals",
        subtitle = "GROW EVERY DAY",
        description = "Track recurring habits, set meaningful goals, and journal your thoughts. All in one app, no switching required.",
        accentColor = PriorityMedium,
        illustrationIcons = listOf(
            Icons.Default.Loop to Color(0xFFF59E0B),
            Icons.Default.Flag to Color(0xFFFBBF24),
            Icons.Default.Book to Color(0xFFFDE68A)
        ),
        badge = "🔥 Habits"
    ),
    OnboardingSlide(
        title = "Smart Insights",
        subtitle = "DATA-DRIVEN CLARITY",
        description = "Discover your most productive days, completion trends, and category breakdowns. Understand yourself better.",
        accentColor = PriorityHigh,
        illustrationIcons = listOf(
            Icons.Default.PieChart to Color(0xFFEF4444),
            Icons.Default.BarChart to Color(0xFFF87171),
            Icons.AutoMirrored.Filled.ShowChart to Color(0xFFFCA5A5)
        ),
        badge = "🧠 Insights"
    )
)

@Composable
fun OnboardingScreen(
    viewModel: TaskViewModel,
    onOnboardingComplete: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    val totalSteps = slides.size + 1 // slides + name step

    val accentColor by animateColorAsState(
        targetValue = if (currentStep < slides.size) slides[currentStep].accentColor else PrimaryIndigo,
        animationSpec = tween(400), label = "accent"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Subtle gradient blob in background
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-60).dp)
                    .alpha(0.12f)
                    .background(
                        Brush.radialGradient(listOf(accentColor, Color.Transparent)),
                        CircleShape
                    )
            )

            // Skip button
            if (currentStep < slides.size) {
                TextButton(
                    onClick = {
                        viewModel.completeOnboarding()
                        onOnboardingComplete()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .statusBarsPadding()
                ) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(0.5f))

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { it } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()) togetherWith
                                (slideOutHorizontally { it } + fadeOut())
                        }
                    },
                    label = "slide_content"
                ) { step ->
                    if (step < slides.size) {
                        SlideContent(slide = slides[step], accentColor = accentColor)
                    } else {
                        NameInputSlideNew(name = name, onNameChange = { name = it }, accentColor = accentColor)
                    }
                }

                Spacer(modifier = Modifier.weight(0.5f))

                // Page indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 28.dp)
                ) {
                    repeat(totalSteps) { index ->
                        val isActive = index == currentStep
                        val width by animateDpAsState(
                            targetValue = if (isActive) 24.dp else 8.dp,
                            animationSpec = tween(300), label = "dot_width"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isActive) accentColor
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                // Navigation row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(88.dp))
                    }

                    Button(
                        onClick = {
                            if (currentStep < totalSteps - 1) {
                                currentStep++
                            } else {
                                viewModel.saveUserName(name.ifBlank { "User" })
                                viewModel.completeOnboarding()
                                onOnboardingComplete()
                            }
                        },
                        enabled = if (currentStep == totalSteps - 1) name.isNotBlank() else true,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = if (currentStep == totalSteps - 1) "Let's Go! 🚀" else "Next",
                            fontWeight = FontWeight.Bold
                        )
                        if (currentStep < totalSteps - 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlideContent(slide: OnboardingSlide, accentColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Unique illustration: three icons arranged in a composition
        Box(
            modifier = Modifier
                .size(220.dp)
                .background(
                    color = accentColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(32.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Center large icon
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = slide.illustrationIcons[0].first,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = slide.illustrationIcons[0].second
                )
            }

            // Top-left mini icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .align(Alignment.TopStart)
                    .offset(x = 16.dp, y = 16.dp)
                    .background(slide.illustrationIcons[1].second.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = slide.illustrationIcons[1].first,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = slide.illustrationIcons[1].second
                )
            }

            // Bottom-right mini icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-16).dp, y = (-16).dp)
                    .background(slide.illustrationIcons[2].second.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = slide.illustrationIcons[2].first,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = slide.illustrationIcons[2].second
                )
            }

            // Badge chip
            if (slide.badge != null) {
                Surface(
                    color = accentColor.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = 8.dp)
                ) {
                    Text(
                        text = slide.badge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Subtitle label
        Text(
            text = slide.subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = accentColor,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = slide.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = slide.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun NameInputSlideNew(name: String, onNameChange: (String) -> Unit, accentColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Illustration: Person icon with a greeting vibe
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(accentColor.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "LAST STEP",
            style = MaterialTheme.typography.labelMedium,
            color = accentColor,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "What's your name?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "We'll use this to personalize\nyour TaskSpace experience.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Your Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}
