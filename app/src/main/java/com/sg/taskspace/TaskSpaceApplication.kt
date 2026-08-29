package com.sg.taskspace

import android.app.Application
import com.sg.taskspace.data.AppContainer
import com.sg.taskspace.data.AppDataContainer
import com.sg.taskspace.util.AudioHapticHelper

class TaskSpaceApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
        AudioHapticHelper.ensureInitialized(this)
    }
}
