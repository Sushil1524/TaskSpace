package com.sg.taskspace.ui.navigation

import androidx.compose.runtime.Composable
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
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onWeeklyTasksClick = { navController.navigate("weekly_tasks") },
                onHistoryClick = { navController.navigate("history") },
                onInsightsClick = { navController.navigate("insights") }
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
    }
}