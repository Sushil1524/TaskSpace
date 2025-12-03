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
                onTaskClick = { taskId -> navController.navigate("task_detail/$taskId") },
                onWeeklyTasksClick = { navController.navigate("weekly_tasks") }
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
        composable("weekly_tasks") {
            WeeklyTasksScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onTaskClick = { taskId -> navController.navigate("task_detail/$taskId") }
            )
        }
    }
}
