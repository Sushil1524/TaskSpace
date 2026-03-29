package com.sg.taskspace.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sg.taskspace.ui.screens.AchievementsScreen
import com.sg.taskspace.ui.viewmodel.TaskViewModel
import com.sg.taskspace.ui.viewmodel.AchievementsViewModel
import com.sg.taskspace.ui.viewmodel.HabitViewModel
import com.sg.taskspace.ui.viewmodel.GoalViewModel
import com.sg.taskspace.ui.viewmodel.JournalViewModel
import com.sg.taskspace.ui.screens.HomeScreen
import com.sg.taskspace.ui.screens.TaskDetailScreen
import com.sg.taskspace.ui.screens.InsightsScreen
import com.sg.taskspace.ui.screens.HistoryScreen
import com.sg.taskspace.ui.screens.WeeklyTasksScreen
import com.sg.taskspace.ui.screens.SettingsScreen
import com.sg.taskspace.ui.screens.HabitScreen
import com.sg.taskspace.ui.screens.GoalScreen
import com.sg.taskspace.ui.screens.JournalScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSpaceNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    taskViewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory)
) {
    val achievementsViewModel: AchievementsViewModel = viewModel(factory = AchievementsViewModel.Factory)
    val habitViewModel: HabitViewModel = viewModel(factory = HabitViewModel.Factory)
    val goalViewModel: GoalViewModel = viewModel(factory = GoalViewModel.Factory)
    val journalViewModel: JournalViewModel = viewModel(factory = JournalViewModel.Factory)

    val userPreferences by taskViewModel.userPreferences.collectAsState()
    val currentPrefs = userPreferences

    if (currentPrefs == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator()
        }
    } else {
        val startDestination = if (currentPrefs.isOnboardingCompleted) "home" else "onboarding"

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier
        ) {
            composable("onboarding") {
                com.sg.taskspace.ui.screens.OnboardingScreen(
                    viewModel = taskViewModel,
                    onOnboardingComplete = {
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreen(
                    viewModel = taskViewModel,
                    onWeeklyTasksClick = { navController.navigate("weekly_tasks") },
                    onHistoryClick = { navController.navigate("history") },
                    onAchievementsClick = { navController.navigate("achievements") },
                    onInsightsClick = { navController.navigate("insights") },
                    onSettingsClick = { navController.navigate("settings") },
                    onHabitsClick = { navController.navigate("habits") },
                    onGoalsClick = { navController.navigate("goals") },
                    onJournalClick = { navController.navigate("journal") }
                )
            }
            composable(
                route = "achievements",
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(300)
                    ) +
                        scaleIn(
                            initialScale = 0.95f,
                            animationSpec = tween(300)
                        )
                },
                exitTransition = {
                    fadeOut(
                        animationSpec = tween(300)
                    ) +
                        scaleOut(
                            targetScale = 0.95f,
                            animationSpec = tween(300)
                        )
                },
                popEnterTransition = {
                    fadeIn(
                        animationSpec = tween(300)
                    ) +
                        scaleIn(
                            initialScale = 0.95f,
                            animationSpec = tween(300)
                        )
                },
                popExitTransition = {
                    fadeOut(
                        animationSpec = tween(300)
                    ) +
                        scaleOut(
                            targetScale = 0.95f,
                            animationSpec = tween(300)
                        )
                }
            ) {
                AchievementsScreen(
                    viewModel = achievementsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("habits") {
                HabitScreen(
                    viewModel = habitViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("goals") {
                GoalScreen(
                    viewModel = goalViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("journal") {
                JournalScreen(
                    viewModel = journalViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("task_detail/{taskId}") { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId")
                if (taskId != null) {
                    TaskDetailScreen(
                        taskId = taskId,
                        viewModel = taskViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
            composable(
                route = "weekly_tasks",
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(
                            300
                        )
                    ) +
                            scaleIn(
                                initialScale = 0.95f,
                                animationSpec = tween(300)
                            )
                },
                exitTransition = {
                    fadeOut(
                        animationSpec = tween(
                            300
                        )
                    ) +
                            scaleOut(
                                targetScale = 0.95f,
                                animationSpec = tween(300)
                            )
                },
                popEnterTransition = {
                    fadeIn(
                        animationSpec = tween(
                            300
                        )
                    ) +
                            scaleIn(
                                initialScale = 0.95f,
                                animationSpec = tween(300)
                            )
                },
                popExitTransition = {
                    fadeOut(
                        animationSpec = tween(
                            300
                        )
                    ) +
                            scaleOut(
                                targetScale = 0.95f,
                                animationSpec = tween(300)
                            )
                }
            ) {
                WeeklyTasksScreen(
                    viewModel = taskViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "history",
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(
                            300
                        )
                    ) +
                            scaleIn(
                                initialScale = 0.95f,
                                animationSpec = tween(300)
                            )
                },
                exitTransition = {
                    fadeOut(
                        animationSpec = tween(
                            300
                        )
                    ) +
                            scaleOut(
                                targetScale = 0.95f,
                                animationSpec = tween(300)
                            )
                },
                popEnterTransition = {
                    fadeIn(
                        animationSpec = tween(
                            300
                        )
                    ) +
                            scaleIn(
                                initialScale = 0.95f,
                                animationSpec = tween(300)
                            )
                },
                popExitTransition = {
                    fadeOut(
                        animationSpec = tween(
                            300
                        )
                    ) +
                            scaleOut(
                                targetScale = 0.95f,
                                animationSpec = tween(300)
                            )
                }
            ) {
                HistoryScreen(
                    viewModel = taskViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "insights",
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(
                            300
                        )
                    ) +
                            scaleIn(
                                initialScale = 0.95f,
                                animationSpec = tween(300)
                            )
                },
                exitTransition = {
                    fadeOut(
                        animationSpec = tween(
                            300
                        )
                    ) +
                            scaleOut(
                                targetScale = 0.95f,
                                animationSpec = tween(300)
                            )
                },
                popEnterTransition = {
                    fadeIn(
                        animationSpec = tween(
                            300
                        )
                    ) +
                            scaleIn(
                                initialScale = 0.95f,
                                animationSpec = tween(300)
                            )
                },
                popExitTransition = {
                    fadeOut(
                        animationSpec = tween(
                            300
                        )
                    ) +
                            scaleOut(
                                targetScale = 0.95f,
                                animationSpec = tween(300)
                            )
                }
            ) {
                InsightsScreen(
                    viewModel = taskViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "settings",
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(300)
                    ) + slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    )
                },
                exitTransition = {
                    fadeOut(
                        animationSpec = tween(300)
                    ) + slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300)
                    )
                },
                popEnterTransition = {
                    fadeIn(
                        animationSpec = tween(300)
                    ) + slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(300)
                    )
                },
                popExitTransition = {
                    fadeOut(
                        animationSpec = tween(300)
                    ) + slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(300)
                    )
                }
            ) {
                SettingsScreen(
                    viewModel = taskViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
