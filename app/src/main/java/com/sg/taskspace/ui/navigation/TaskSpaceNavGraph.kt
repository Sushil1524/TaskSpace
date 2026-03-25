package com.sg.taskspace.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sg.taskspace.ui.viewmodel.TaskViewModel
import com.sg.taskspace.ui.screens.HomeScreen
import com.sg.taskspace.ui.screens.TaskDetailScreen
import com.sg.taskspace.ui.screens.WeeklyTasksScreen

@Composable
fun TaskSpaceNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: TaskViewModel = viewModel(factory = TaskViewModel.Companion.Factory)
) {
    val userPreferences by viewModel.userPreferences.collectAsState()
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
                    viewModel = viewModel,
                    onOnboardingComplete = {
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onWeeklyTasksClick = { navController.navigate("weekly_tasks") },
                    onHistoryClick = { navController.navigate("history") },
                    onInsightsClick = { navController.navigate("insights") },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable("task_detail/{taskId}") { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId")
                if (taskId != null) {
                    TaskDetailScreen(
                        taskId = taskId,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
            composable(
                route = "weekly_tasks",
                enterTransition = {
                    androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(
                            300
                        )
                    ) +
                            androidx.compose.animation.scaleIn(
                                initialScale = 0.95f,
                                animationSpec = androidx.compose.animation.core.tween(300)
                            )
                },
                exitTransition = {
                    androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(
                            300
                        )
                    ) +
                            androidx.compose.animation.scaleOut(
                                targetScale = 0.95f,
                                animationSpec = androidx.compose.animation.core.tween(300)
                            )
                },
                popEnterTransition = {
                    androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(
                            300
                        )
                    ) +
                            androidx.compose.animation.scaleIn(
                                initialScale = 0.95f,
                                animationSpec = androidx.compose.animation.core.tween(300)
                            )
                },
                popExitTransition = {
                    androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(
                            300
                        )
                    ) +
                            androidx.compose.animation.scaleOut(
                                targetScale = 0.95f,
                                animationSpec = androidx.compose.animation.core.tween(300)
                            )
                }
            ) {
                WeeklyTasksScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "history",
                enterTransition = {
                    androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(
                            300
                        )
                    ) +
                            androidx.compose.animation.scaleIn(
                                initialScale = 0.95f,
                                animationSpec = androidx.compose.animation.core.tween(300)
                            )
                },
                exitTransition = {
                    androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(
                            300
                        )
                    ) +
                            androidx.compose.animation.scaleOut(
                                targetScale = 0.95f,
                                animationSpec = androidx.compose.animation.core.tween(300)
                            )
                },
                popEnterTransition = {
                    androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(
                            300
                        )
                    ) +
                            androidx.compose.animation.scaleIn(
                                initialScale = 0.95f,
                                animationSpec = androidx.compose.animation.core.tween(300)
                            )
                },
                popExitTransition = {
                    androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(
                            300
                        )
                    ) +
                            androidx.compose.animation.scaleOut(
                                targetScale = 0.95f,
                                animationSpec = androidx.compose.animation.core.tween(300)
                            )
                }
            ) {
                com.sg.taskspace.ui.screens.HistoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "insights",
                enterTransition = {
                    androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(
                            300
                        )
                    ) +
                            androidx.compose.animation.scaleIn(
                                initialScale = 0.95f,
                                animationSpec = androidx.compose.animation.core.tween(300)
                            )
                },
                exitTransition = {
                    androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(
                            300
                        )
                    ) +
                            androidx.compose.animation.scaleOut(
                                targetScale = 0.95f,
                                animationSpec = androidx.compose.animation.core.tween(300)
                            )
                },
                popEnterTransition = {
                    androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(
                            300
                        )
                    ) +
                            androidx.compose.animation.scaleIn(
                                initialScale = 0.95f,
                                animationSpec = androidx.compose.animation.core.tween(300)
                            )
                },
                popExitTransition = {
                    androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(
                            300
                        )
                    ) +
                            androidx.compose.animation.scaleOut(
                                targetScale = 0.95f,
                                animationSpec = androidx.compose.animation.core.tween(300)
                            )
                }
            ) {
                com.sg.taskspace.ui.screens.InsightsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "settings",
                enterTransition = {
                    androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ) + androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = androidx.compose.animation.core.tween(300)
                    )
                },
                exitTransition = {
                    androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ) + androidx.compose.animation.slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = androidx.compose.animation.core.tween(300)
                    )
                },
                popEnterTransition = {
                    androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ) + androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = androidx.compose.animation.core.tween(300)
                    )
                },
                popExitTransition = {
                    androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ) + androidx.compose.animation.slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = androidx.compose.animation.core.tween(300)
                    )
                }
            ) {
                com.sg.taskspace.ui.screens.SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}