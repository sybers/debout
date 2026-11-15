package fr.sybers.debout

import android.app.Application

class DeboutApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val android.content.Context.appContainer: AppContainer
    get() = (applicationContext as DeboutApplication).container
