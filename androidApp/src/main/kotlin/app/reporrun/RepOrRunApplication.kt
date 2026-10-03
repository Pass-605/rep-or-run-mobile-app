package app.reporrun

import android.app.Application
import app.reporrun.di.startRepOrRun

/** Arranca el grafo de dependencias antes que cualquier actividad. */
class RepOrRunApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startRepOrRun(this)
    }
}
