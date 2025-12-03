package com.sg.taskspace

import android.app.Application
import com.sg.taskspace.data.AppContainer
import com.sg.taskspace.data.AppDataContainer

class TaskSpaceApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}
