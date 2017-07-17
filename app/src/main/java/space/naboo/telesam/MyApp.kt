package space.naboo.telesam

import android.app.Application
import space.naboo.telesam.db.AppDatabase
import space.naboo.telesam.db.DatabaseHelper
import space.naboo.telesam.telegram.KotlogramWrapper

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
        database = DatabaseHelper(this).appDatabase
        kotlogram = KotlogramWrapper()
    }

}
