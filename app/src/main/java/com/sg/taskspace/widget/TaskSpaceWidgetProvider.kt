package com.sg.taskspace.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.sg.taskspace.MainActivity
import com.sg.taskspace.R
import com.sg.taskspace.data.AppDatabase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskSpaceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_taskspace)

            // Intent to open MainActivity on + tap
            val addIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val addPendingIntent = PendingIntent.getActivity(
                context,
                0,
                addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_add_button, addPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_title, addPendingIntent)

            // Query database in background
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val todayIso = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                    val allTasks = db.taskDao().getAllTasksSync()
                    val tasks = allTasks.filter { it.createdForDate == todayIso }

                    val activeTasks = tasks.filter { !it.isCompleted }
                    val completedCount = tasks.count { it.isCompleted }

                    val previewText = when {
                        tasks.isEmpty() -> "No tasks scheduled for today.\nTap + to add one!"
                        activeTasks.isEmpty() -> "🎉 All ${tasks.size} tasks completed today!"
                        else -> "• ${activeTasks.first().title}" + if (activeTasks.size > 1) "\n+ ${activeTasks.size - 1} more active tasks" else ""
                    }

                    views.setTextViewText(R.id.widget_task_preview, previewText)
                    views.setTextViewText(R.id.widget_task_count, "$completedCount / ${tasks.size} Done")

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
