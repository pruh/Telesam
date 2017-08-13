package space.naboo.telesam

import android.app.Application
import android.content.Context
import io.reactivex.plugins.RxJavaPlugins
import space.naboo.telesam.db.AppDatabase
import space.naboo.telesam.db.DatabaseHelper
import space.naboo.telesam.telegram.KotlogramWrapper
import timber.log.Timber

class MyApp : Application() {

    companion object {
        lateinit var instance: MyApp
            private set

        lateinit var database: AppDatabase
            private set

        lateinit var kotlogram: KotlogramWrapper
            private set
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        initTimber(this)

        database = DatabaseHelper(this).appDatabase
        kotlogram = KotlogramWrapper()

        setRxGlobalErrorHandler()
    }

    private fun initTimber(context: Context) {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())

            // todo add some persistent tree that I can check, like rolling file appender
        } else {
            // todo plant Production Tree
        }
    }

    private fun setRxGlobalErrorHandler() {
        RxJavaPlugins.setErrorHandler { e ->
            Timber.w(e, "Caught global exception")
        }
    }

}
