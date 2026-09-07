package com.sg.taskspace

import android.app.Application
import com.sg.taskspace.data.AppContainer
import com.sg.taskspace.data.AppDataContainer
import com.sg.taskspace.util.AudioHapticHelper

class TaskSpaceApplication : Application() {
    lateinit var container: AppContainer

    companion object {
        lateinit var instance: TaskSpaceApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppDataContainer(this)
        AudioHapticHelper.ensureInitialized(this)
    }
}
